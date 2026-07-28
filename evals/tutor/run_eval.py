#!/usr/bin/env python3
"""Run LangCoach's versioned tutor-quality suite against OpenAI Realtime."""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import pathlib
import re
import ssl
import statistics
import textwrap
import time
import urllib.request
from datetime import datetime, timezone
from typing import Any

import websocket


ROOT = pathlib.Path(__file__).resolve().parents[2]
PROMPT_SOURCE = ROOT / "shared/domain/src/commonMain/kotlin/com/xemniz/langcoach/domain/session/TutorPrompt.kt"
DEFAULT_DATASET = pathlib.Path(__file__).with_name("tutor-eval-v1.2.0.json")
LOCAL_PROPERTIES = ROOT / "local.properties"
DEFAULT_REALTIME_MODEL = "gpt-realtime-2.1"
DEFAULT_JUDGE_MODEL = "gpt-4.1-mini"


def api_key() -> str:
    if value := os.environ.get("OPENAI_API_KEY"):
        return value
    if LOCAL_PROPERTIES.exists():
        match = re.search(
            r"^OPENAI_API_KEY=(.+)$",
            LOCAL_PROPERTIES.read_text(encoding="utf-8"),
            flags=re.MULTILINE,
        )
        if match:
            return match.group(1).strip()
    raise RuntimeError("Set OPENAI_API_KEY or add OPENAI_API_KEY to local.properties")


def load_prompt_template() -> tuple[str, str]:
    source = PROMPT_SOURCE.read_text(encoding="utf-8")
    version_match = re.search(r'const val VERSION = "([^"]+)"', source)
    template_match = re.search(
        r'private val TEMPLATE = """\n(.*?)\n"""\.trimIndent\(\)',
        source,
        flags=re.DOTALL,
    )
    if not version_match or not template_match:
        raise RuntimeError("Could not read the canonical prompt/version from TutorPrompt.kt")
    return version_match.group(1), textwrap.dedent(template_match.group(1))


def render_prompt(template: str, context: dict[str, str]) -> str:
    prompt = template
    for key, value in context.items():
        prompt = prompt.replace("{{" + key + "}}", value)
    if "{{" in prompt:
        raise RuntimeError("Prompt contains an unresolved template value")
    return prompt


def render_case_prompt(template: str, case: dict[str, Any]) -> str:
    context = dict(case["context"])
    expectations = case["expectations"]
    context["session_plan"] = case.get(
        "session_plan",
        "\n".join(
            (
                f"Primary objective: {expectations['goal']}",
                "Opening direction: Use the supplied learner context and continue naturally.",
                "Success evidence: " + "; ".join(expectations.get("must_do", [])),
                "Treat this as a light intention, not a script.",
            )
        ),
    )
    return render_prompt(template, context)


def send_json(ws: websocket.WebSocket, payload: dict[str, Any]) -> None:
    ws.send(json.dumps(payload, separators=(",", ":")))


def event_text(response: dict[str, Any]) -> str:
    parts: list[str] = []
    for item in response.get("output", []):
        for content in item.get("content", []):
            value = content.get("text") or content.get("transcript")
            if value:
                parts.append(value)
    return "".join(parts)


def assistant_turn(ws: websocket.WebSocket) -> str:
    send_json(ws, {"type": "response.create"})
    deltas: list[str] = []
    deadline = time.monotonic() + 45
    while time.monotonic() < deadline:
        event = json.loads(ws.recv())
        kind = event.get("type")
        if kind == "error":
            raise RuntimeError(event.get("error", {}).get("message", "Realtime API error"))
        if kind in ("response.output_text.delta", "response.output_audio_transcript.delta"):
            deltas.append(event.get("delta", ""))
        elif kind == "response.done":
            response = event.get("response", {})
            if response.get("status") != "completed":
                raise RuntimeError(f"Realtime response ended with {response.get('status')}")
            return "".join(deltas).strip() or event_text(response).strip()
    raise RuntimeError("Realtime response timed out")


def add_message(ws: websocket.WebSocket, role: str, value: str) -> None:
    content_type = "input_text" if role == "user" else "output_text"
    send_json(
        ws,
        {
            "type": "conversation.item.create",
            "item": {
                "type": "message",
                "role": role,
                "content": [{"type": content_type, "text": value}],
            },
        },
    )


def run_case(
    key: str,
    model: str,
    prompt: str,
    case: dict[str, Any],
) -> list[dict[str, str]]:
    ws = websocket.create_connection(
        f"wss://api.openai.com/v1/realtime?model={model}",
        header=[f"Authorization: Bearer {key}"],
        timeout=45,
        sslopt={"cert_reqs": ssl.CERT_REQUIRED},
    )
    try:
        send_json(
            ws,
            {
                "type": "session.update",
                "session": {
                    "type": "realtime",
                    "instructions": prompt,
                    "output_modalities": ["text"],
                },
            },
        )
        while True:
            event = json.loads(ws.recv())
            if event.get("type") == "error":
                raise RuntimeError(event.get("error", {}).get("message", "Realtime API error"))
            if event.get("type") == "session.updated":
                break

        transcript: list[dict[str, str]] = []
        for turn in case.get("history", []):
            add_message(ws, turn["role"], turn["text"])
            transcript.append({"role": turn["role"], "text": turn["text"]})

        if case.get("open_with_assistant"):
            transcript.append({"role": "assistant", "text": assistant_turn(ws)})

        for user_text in case.get("turns", []):
            add_message(ws, "user", user_text)
            transcript.append({"role": "user", "text": user_text})
            transcript.append({"role": "assistant", "text": assistant_turn(ws)})
        return transcript
    finally:
        ws.close()


def deterministic_checks(transcript: list[dict[str, str]]) -> dict[str, Any]:
    assistant = [turn["text"] for turn in transcript if turn["role"] == "assistant"]
    # A quoted model phrase is not a second conversational question. The qualitative grader still
    # sees it and can penalize unnecessary repetition, while this deterministic gate counts only
    # questions the learner is actually expected to answer.
    unquoted = [re.sub(r'“[^”]*”|"[^"]*"', "", text) for text in assistant]
    questions_per_turn = [text.count("?") for text in unquoted]
    normalized_questions = [
        re.sub(r"\W+", " ", match.group(0).lower()).strip()
        for text in unquoted
        for match in re.finditer(r"[^?]*\?", text)
    ]
    repeated_questions = sorted(
        {question for question in normalized_questions if normalized_questions.count(question) > 1}
    )
    forbidden_opening = False
    if assistant:
        first = assistant[0].lower()
        forbidden_opening = any(
            phrase in first
            for phrase in (
                "¿cómo estás",
                "qué te gustaría hablar",
                "de qué te gustaría hablar",
            )
        )
    return {
        "assistant_turns": len(assistant),
        "max_questions_in_one_turn": max(questions_per_turn, default=0),
        "repeated_questions": repeated_questions,
        "forbidden_opening": forbidden_opening,
    }


def post_json(url: str, key: str, payload: dict[str, Any]) -> dict[str, Any]:
    request = urllib.request.Request(
        url,
        data=json.dumps(payload).encode("utf-8"),
        headers={
            "Authorization": f"Bearer {key}",
            "Content-Type": "application/json",
        },
        method="POST",
    )
    with urllib.request.urlopen(request, timeout=60) as response:
        return json.load(response)


def grade_case(
    key: str,
    model: str,
    dimensions: dict[str, str],
    case: dict[str, Any],
    transcript: list[dict[str, str]],
    checks: dict[str, Any],
) -> dict[str, Any]:
    dimension_names = list(dimensions)
    score_properties = {
        name: {"type": "integer", "minimum": 1, "maximum": 5}
        for name in dimension_names
    }
    schema = {
        "type": "object",
        "properties": {
            "scores": {
                "type": "object",
                "properties": score_properties,
                "required": dimension_names,
                "additionalProperties": False,
            },
            "hard_violation": {"type": "boolean"},
            "reason": {"type": "string"},
            "best_evidence": {"type": "string"},
            "improvement": {"type": "string"},
        },
        "required": ["scores", "hard_violation", "reason", "best_evidence", "improvement"],
        "additionalProperties": False,
    }
    rubric = "\n".join(f"- {name}: {description}" for name, description in dimensions.items())
    input_payload = {
        "case": case,
        "transcript": transcript,
        "deterministic_checks": checks,
    }
    response = post_json(
        "https://api.openai.com/v1/chat/completions",
        key,
        {
            "model": model,
            "messages": [
                {
                    "role": "system",
                    "content": (
                        "You are a strict evaluator of a conversational language tutor. "
                        "Judge only behavior visible in the supplied transcript. A score of 4 "
                        "means the behavior clearly meets the rubric; 5 is exemplary; 3 is "
                        "mixed. Set hard_violation only when a case-specific must_not condition "
                        "is clearly violated. Do not reward verbosity.\n\nRUBRIC:\n" + rubric
                    ),
                },
                {
                    "role": "user",
                    "content": json.dumps(input_payload, ensure_ascii=False, indent=2),
                },
            ],
            "response_format": {
                "type": "json_schema",
                "json_schema": {
                    "name": "tutor_quality_grade",
                    "strict": True,
                    "schema": schema,
                },
            },
        },
    )
    return json.loads(response["choices"][0]["message"]["content"])


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--dataset", type=pathlib.Path, default=DEFAULT_DATASET)
    parser.add_argument("--case", action="append", dest="case_ids")
    parser.add_argument("--split", choices=["regression", "holdout"])
    parser.add_argument("--realtime-model", default=DEFAULT_REALTIME_MODEL)
    parser.add_argument("--judge-model", default=DEFAULT_JUDGE_MODEL)
    parser.add_argument("--output", type=pathlib.Path)
    args = parser.parse_args()

    dataset = json.loads(args.dataset.read_text(encoding="utf-8"))
    prompt_version, template = load_prompt_template()
    if dataset["prompt_version"] != prompt_version:
        raise RuntimeError(
            f"Dataset expects {dataset['prompt_version']}, canonical prompt is {prompt_version}"
        )
    prompt_sha256 = hashlib.sha256(template.encode("utf-8")).hexdigest()
    if dataset["prompt_sha256"] != prompt_sha256:
        raise RuntimeError(
            "The canonical prompt changed without a prompt version bump. "
            f"Expected SHA-256 {dataset['prompt_sha256']}, got {prompt_sha256}"
        )
    cases = dataset["cases"]
    if args.case_ids:
        selected = set(args.case_ids)
        cases = [case for case in cases if case["id"] in selected]
    if args.split:
        cases = [case for case in cases if case["split"] == args.split]
    if not cases:
        raise RuntimeError("No eval cases selected")

    key = api_key()
    results: list[dict[str, Any]] = []
    for index, case in enumerate(cases, start=1):
        print(f"[{index}/{len(cases)}] {case['id']}", flush=True)
        prompt = render_case_prompt(template, case)
        transcript = run_case(key, args.realtime_model, prompt, case)
        checks = deterministic_checks(transcript)
        grade = grade_case(
            key,
            args.judge_model,
            dataset["dimensions"],
            case,
            transcript,
            checks,
        )
        relevant = case["expectations"]["relevant_dimensions"]
        case_score = statistics.mean(grade["scores"][name] for name in relevant)
        passed = (
            case_score >= dataset["pass_policy"]["minimum_case_score"]
            and not grade["hard_violation"]
            and checks["max_questions_in_one_turn"] <= 1
            and not checks["repeated_questions"]
            and not checks["forbidden_opening"]
        )
        result = {
            "id": case["id"],
            "split": case["split"],
            "tags": case["tags"],
            "score": round(case_score, 2),
            "passed": passed,
            "grade": grade,
            "deterministic_checks": checks,
            "transcript": transcript,
        }
        results.append(result)
        print(
            f"  score={result['score']:.2f} passed={passed} "
            f"hard_violation={grade['hard_violation']}",
            flush=True,
        )

    suite_score = statistics.mean(result["score"] for result in results)
    hard_violations = sum(result["grade"]["hard_violation"] for result in results)
    passed_cases = sum(result["passed"] for result in results)
    suite_passed = (
        suite_score >= dataset["pass_policy"]["minimum_suite_score"]
        and hard_violations <= dataset["pass_policy"]["hard_violations_allowed"]
        and passed_cases == len(results)
    )
    report = {
        "eval_version": dataset["version"],
        "prompt_version": prompt_version,
        "prompt_sha256": prompt_sha256,
        "run_at": datetime.now(timezone.utc).isoformat(),
        "realtime_model": args.realtime_model,
        "judge_model": args.judge_model,
        "summary": {
            "suite_score": round(suite_score, 2),
            "passed_cases": passed_cases,
            "total_cases": len(results),
            "hard_violations": hard_violations,
            "passed": suite_passed,
        },
        "results": results,
    }
    rendered = json.dumps(report, ensure_ascii=False, indent=2)
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(rendered + "\n", encoding="utf-8")
        print(f"Wrote {args.output}")
    print(json.dumps(report["summary"], indent=2))
    return 0 if suite_passed else 1


if __name__ == "__main__":
    raise SystemExit(main())
