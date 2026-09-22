import json
import sqlite3
import tempfile
import unittest
from pathlib import Path

from scripts.export_android_transcripts import (
    database_listing_command,
    read_session,
    render_markdown,
    write_session_files,
)


class ExportAndroidTranscriptsTest(unittest.TestCase):
    def setUp(self):
        self.connection = sqlite3.connect(":memory:")
        self.connection.executescript(
            """
            CREATE TABLE session_summaries (
                id INTEGER PRIMARY KEY,
                startedAt INTEGER NOT NULL,
                endedAt INTEGER,
                summary TEXT,
                objectiveDescription TEXT,
                strength TEXT,
                nextStep TEXT,
                assignment TEXT
            );
            CREATE TABLE session_turns (
                sessionId INTEGER NOT NULL,
                turnId TEXT NOT NULL,
                position INTEGER NOT NULL,
                speaker TEXT NOT NULL,
                text TEXT NOT NULL
            );
            INSERT INTO session_summaries VALUES
                (1, 1000, 2000, 'Older', NULL, NULL, NULL, NULL),
                (2, 3000, 4000, 'Latest lesson', 'Use quiero', 'Clear intent', 'Retest unaided', 'Write three lines');
            INSERT INTO session_turns VALUES
                (2, 'turn-2', 1, 'Learner', 'Quiero practicar.'),
                (2, 'turn-1', 0, 'Tutor', '¿Qué quieres practicar?');
            """
        )

    def tearDown(self):
        self.connection.close()

    def test_reads_latest_session_with_turns_in_conversation_order(self):
        session = read_session(self.connection)

        self.assertEqual(2, session["id"])
        self.assertEqual(
            ["turn-1", "turn-2"],
            [turn["turnId"] for turn in session["turns"]],
        )

    def test_writes_machine_readable_and_human_readable_exports(self):
        session = read_session(self.connection)

        with tempfile.TemporaryDirectory() as directory:
            json_path, markdown_path = write_session_files(session, Path(directory))

            exported = json.loads(json_path.read_text())
            self.assertEqual("Latest lesson", exported["summary"])
            self.assertIn("**Tutor:** ¿Qué quieres practicar?", markdown_path.read_text())
            self.assertIn("**Learner:** Quiero practicar.", markdown_path.read_text())
            self.assertEqual(render_markdown(session), markdown_path.read_text())

    def test_lists_database_directory_without_remote_shell_reparsing(self):
        self.assertEqual(
            [
                "adb",
                "-s",
                "device-1",
                "shell",
                "run-as",
                "com.xemniz.langcoach",
                "ls",
                "databases",
            ],
            database_listing_command("adb", "device-1", "com.xemniz.langcoach"),
        )


if __name__ == "__main__":
    unittest.main()
