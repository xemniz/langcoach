#!/usr/bin/env python3
"""Download retained LangCoach sessions from a debuggable Android install."""

from __future__ import annotations

import argparse
import io
import json
import shutil
import sqlite3
import subprocess
import tarfile
import tempfile
from datetime import datetime
from pathlib import Path
from typing import Any, Iterable


DEFAULT_PACKAGE = "com.xemniz.langcoach"
DEFAULT_OUTPUT_DIRECTORY = Path("exports/transcripts")


class ExportError(RuntimeError):
    pass


def read_session(
    connection: sqlite3.Connection,
    session_id: int | None = None,
) -> dict[str, Any]:
    connection.row_factory = sqlite3.Row
    if session_id is None:
        row = connection.execute(
            """
            SELECT * FROM session_summaries
            WHERE endedAt IS NOT NULL
            ORDER BY endedAt DESC, id DESC
            LIMIT 1
            """
        ).fetchone()
    else:
        row = connection.execute(
            "SELECT * FROM session_summaries WHERE id = ?",
            (session_id,),
        ).fetchone()

    if row is None:
        requested = "latest completed session" if session_id is None else f"session {session_id}"
        raise ExportError(f"Could not find {requested}")

    session = dict(row)
    session["turns"] = [
        dict(turn)
        for turn in connection.execute(
            """
            SELECT turnId, position, speaker, text
            FROM session_turns
            WHERE sessionId = ?
            ORDER BY position ASC
            """,
            (session["id"],),
        ).fetchall()
    ]
    return session


def completed_session_ids(connection: sqlite3.Connection) -> list[int]:
    return [
        int(row[0])
        for row in connection.execute(
            """
            SELECT id FROM session_summaries
            WHERE endedAt IS NOT NULL
            ORDER BY endedAt DESC, id DESC
            """
        ).fetchall()
    ]


def render_markdown(session: dict[str, Any]) -> str:
    lines = [f"# LangCoach session {session['id']}", ""]
    metadata = (
        ("Started", _format_timestamp(session.get("startedAt"))),
        ("Ended", _format_timestamp(session.get("endedAt"))),
        ("Target language", session.get("targetLang")),
        ("Level", session.get("level")),
        ("Objective", session.get("objectiveDescription")),
        ("Outcome", session.get("objectiveOutcome")),
    )
    for label, value in metadata:
        if value not in (None, ""):
            lines.append(f"- **{label}:** {value}")

    for heading, key in (
        ("Summary", "summary"),
        ("Strength", "strength"),
        ("Next step", "nextStep"),
        ("Assignment", "assignment"),
    ):
        value = session.get(key)
        if value:
            lines.extend(("", f"## {heading}", "", str(value)))

    lines.extend(("", "## Transcript", ""))
    turns = session.get("turns", [])
    if turns:
        for turn in turns:
            lines.append(f"**{turn['speaker']}:** {turn['text']}")
            lines.append("")
    else:
        lines.append("_No transcript turns were retained for this session._")
        lines.append("")
    return "\n".join(lines)


def write_session_files(
    session: dict[str, Any],
    output_directory: Path,
) -> tuple[Path, Path]:
    output_directory.mkdir(parents=True, exist_ok=True)
    timestamp = _filename_timestamp(session.get("startedAt"))
    stem = f"langcoach-session-{session['id']}-{timestamp}"
    json_path = output_directory / f"{stem}.json"
    markdown_path = output_directory / f"{stem}.md"
    json_path.write_text(
        json.dumps(session, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    markdown_path.write_text(render_markdown(session), encoding="utf-8")
    return json_path, markdown_path


def connected_device_serial(adb: str, requested_serial: str | None) -> str:
    result = _run([adb, "devices"], text=True)
    serials = []
    for line in result.stdout.splitlines()[1:]:
        fields = line.split()
        if len(fields) >= 2 and fields[1] == "device":
            serials.append(fields[0])

    if requested_serial:
        if requested_serial not in serials:
            raise ExportError(f"Android device {requested_serial} is not connected and authorized")
        return requested_serial
    if not serials:
        raise ExportError("No authorized Android device is connected")
    if len(serials) > 1:
        raise ExportError("Multiple Android devices are connected; pass --serial")
    return serials[0]


def database_listing_command(adb: str, serial: str, package: str) -> list[str]:
    return [
        adb,
        "-s",
        serial,
        "shell",
        "run-as",
        package,
        "ls",
        "databases",
    ]


def snapshot_database(adb: str, serial: str, package: str, destination: Path) -> Path:
    list_result = _run(database_listing_command(adb, serial, package), text=True)
    remote_files = [
        f"databases/{name}"
        for line in list_result.stdout.splitlines()
        if (name := line.strip()).startswith("langcoach.db")
    ]
    if "databases/langcoach.db" not in remote_files:
        raise ExportError(f"No LangCoach database found in {package}")

    result = _run(
        [adb, "-s", serial, "exec-out", "run-as", package, "tar", "-cf", "-", *remote_files],
        text=False,
    )
    with tarfile.open(fileobj=io.BytesIO(result.stdout), mode="r:") as archive:
        archive.extractall(destination, filter="data")
    return destination / "databases" / "langcoach.db"


def export_sessions(
    database_path: Path,
    output_directory: Path,
    session_id: int | None,
    export_all: bool,
) -> list[tuple[Path, Path]]:
    connection = sqlite3.connect(database_path)
    try:
        ids: Iterable[int | None]
        ids = completed_session_ids(connection) if export_all else [session_id]
        return [
            write_session_files(read_session(connection, current_id), output_directory)
            for current_id in ids
        ]
    finally:
        connection.close()


def _format_timestamp(milliseconds: Any) -> str | None:
    if milliseconds is None:
        return None
    return datetime.fromtimestamp(int(milliseconds) / 1000).astimezone().isoformat(timespec="seconds")


def _filename_timestamp(milliseconds: Any) -> str:
    if milliseconds is None:
        return "unknown-time"
    return datetime.fromtimestamp(int(milliseconds) / 1000).astimezone().strftime("%Y%m%dT%H%M%S")


def _run(command: list[str], text: bool) -> subprocess.CompletedProcess:
    try:
        return subprocess.run(
            command,
            check=True,
            capture_output=True,
            text=text,
        )
    except FileNotFoundError as error:
        raise ExportError(f"Command not found: {command[0]}") from error
    except subprocess.CalledProcessError as error:
        detail = error.stderr if text else error.stderr.decode("utf-8", errors="replace")
        raise ExportError(detail.strip() or f"Command failed: {' '.join(command)}") from error


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Download retained LangCoach transcripts from an Android device."
    )
    parser.add_argument("--serial", help="ADB device serial; inferred when one device is connected")
    parser.add_argument("--package", default=DEFAULT_PACKAGE, help="Android application ID")
    parser.add_argument(
        "--output",
        type=Path,
        default=DEFAULT_OUTPUT_DIRECTORY,
        help=f"Export directory (default: {DEFAULT_OUTPUT_DIRECTORY})",
    )
    selection = parser.add_mutually_exclusive_group()
    selection.add_argument("--session-id", type=int, help="Export one session by database ID")
    selection.add_argument("--all", action="store_true", help="Export every completed session")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    adb = shutil.which("adb")
    if adb is None:
        raise ExportError("adb is not installed or is not on PATH")
    serial = connected_device_serial(adb, args.serial)

    with tempfile.TemporaryDirectory(prefix="langcoach-export-") as directory:
        database_path = snapshot_database(adb, serial, args.package, Path(directory))
        exported = export_sessions(
            database_path=database_path,
            output_directory=args.output,
            session_id=args.session_id,
            export_all=args.all,
        )

    for json_path, markdown_path in exported:
        print(json_path)
        print(markdown_path)
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except ExportError as error:
        raise SystemExit(f"error: {error}")
