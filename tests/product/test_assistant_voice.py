from __future__ import annotations

from http.server import ThreadingHTTPServer
import json
from pathlib import Path
from tempfile import TemporaryDirectory
from threading import Thread
import unittest
from unittest.mock import patch
from urllib.error import HTTPError
from urllib.request import Request, urlopen

from tradercockpit.app_server import make_handler
from tradercockpit.assistant import OPENROUTER_API_KEY_ENV, assistant_status_record
from tradercockpit.assistant_voice import (
    ASSISTANT_TRANSCRIPT_SCHEMA,
    ASSISTANT_VOICE_STATUS_SCHEMA,
    DEFAULT_STT_MODEL,
    OPENROUTER_AUDIO_TRANSCRIPTIONS_URL,
    audio_format_for_content_type,
    transcribe_audio,
    transcribe_response,
    voice_status_record,
)
from tradercockpit.research_custody import FileResearchCustodyStore


class VoiceStatusTests(unittest.TestCase):
    def test_voice_is_unavailable_without_operator_credential(self) -> None:
        record = voice_status_record({})
        self.assertEqual(record["schema"], ASSISTANT_VOICE_STATUS_SCHEMA)
        self.assertEqual(record["status"], "unavailable")
        self.assertEqual(record["reason_code"], "provider_not_configured")
        self.assertEqual(record["capture"], "desktop_microphone")
        self.assertFalse(record["native_mutation"])
        self.assertEqual(assistant_status_record({})["voice"]["status"], "unavailable")

    def test_voice_is_ready_with_the_same_operator_key(self) -> None:
        record = voice_status_record({
            OPENROUTER_API_KEY_ENV: "sk-or-test",
            "TRADERCOCKPIT_STT_MODEL": "openai/whisper-1",
        })
        self.assertEqual(record["status"], "ready")
        self.assertIsNone(record["reason_code"])
        self.assertEqual(record["stt_model"], DEFAULT_STT_MODEL)
        self.assertEqual(assistant_status_record({OPENROUTER_API_KEY_ENV: "sk-or-test"})["voice"]["status"], "ready")


class TranscribeTests(unittest.TestCase):
    def test_content_types_are_allowlisted(self) -> None:
        self.assertEqual(audio_format_for_content_type("audio/webm;codecs=opus"), "webm")
        self.assertEqual(audio_format_for_content_type("audio/wav"), "wav")
        self.assertIsNone(audio_format_for_content_type("application/json"))
        self.assertIsNone(audio_format_for_content_type("text/plain"))

    def test_transcribe_posts_base64_audio_and_returns_transcript_only(self) -> None:
        calls = []

        def transport(url, body, headers):
            calls.append((url, json.loads(body), headers))
            return 200, json.dumps({"text": "  Compile the current plan. ", "model": DEFAULT_STT_MODEL}).encode()

        result = transcribe_audio(
            b"RIFF....",
            content_type="audio/wav",
            environ={OPENROUTER_API_KEY_ENV: "sk-or-test"},
            transport=transport,
        )
        self.assertEqual(result["schema"], ASSISTANT_TRANSCRIPT_SCHEMA)
        self.assertEqual(result["transcript"], "Compile the current plan.")
        self.assertFalse(result["native_mutation"])
        url, payload, headers = calls[0]
        self.assertEqual(url, OPENROUTER_AUDIO_TRANSCRIPTIONS_URL)
        self.assertEqual(payload["model"], DEFAULT_STT_MODEL)
        self.assertEqual(payload["input_audio"]["format"], "wav")
        self.assertNotIn("sk-or-test", json.dumps(payload))
        self.assertEqual(headers["Authorization"], "Bearer sk-or-test")

    def test_transcribe_fails_closed_without_key_or_with_invented_type(self) -> None:
        status, payload = transcribe_response(b"abcd", content_type="audio/webm", environ={})
        self.assertEqual(status, 503)
        self.assertEqual(payload["reason_code"], "provider_not_configured")

        status, payload = transcribe_response(
            b"abcd",
            content_type="application/xml",
            environ={OPENROUTER_API_KEY_ENV: "sk-or-test"},
        )
        self.assertEqual(status, 400)
        self.assertEqual(payload["reason_code"], "audio_type_unsupported")

        status, payload = transcribe_response(
            b"",
            content_type="audio/webm",
            environ={OPENROUTER_API_KEY_ENV: "sk-or-test"},
        )
        self.assertEqual(status, 400)
        self.assertEqual(payload["reason_code"], "audio_invalid")

        status, payload = transcribe_response(
            b"abcd",
            content_type=None,
            environ={OPENROUTER_API_KEY_ENV: "sk-or-test"},
        )
        self.assertEqual(status, 400)
        self.assertEqual(payload["reason_code"], "audio_type_unsupported")

        def empty_text(_url, _body, _headers):
            return 200, json.dumps({"text": "   "}).encode()

        status, payload = transcribe_response(
            b"abcd",
            content_type="audio/webm",
            environ={OPENROUTER_API_KEY_ENV: "sk-or-test"},
            transport=empty_text,
        )
        self.assertEqual(status, 502)
        self.assertEqual(payload["reason_code"], "empty_transcript")


class VoiceHttpBoundaryTests(unittest.TestCase):
    def _server(self, root: Path):
        web = root / "web"
        web.mkdir(parents=True)
        (web / "index.html").write_text("<main>TraderCockpit</main>", encoding="utf-8")
        store = FileResearchCustodyStore(root / "data")
        server = ThreadingHTTPServer(("127.0.0.1", 0), make_handler(web, None, None, store))
        Thread(target=server.serve_forever, daemon=True).start()
        return server

    def test_transcribe_route_is_loopback_audio_only(self) -> None:
        with TemporaryDirectory() as tmp:
            server = self._server(Path(tmp))
            base = f"http://127.0.0.1:{server.server_address[1]}"
            try:
                with urlopen(f"{base}/api/assistant") as response:
                    status = json.loads(response.read())
                self.assertEqual(status["voice"]["status"], "unavailable")
                self.assertFalse(status["voice"]["native_mutation"])

                with self.assertRaises(HTTPError) as get_error:
                    urlopen(f"{base}/api/assistant/transcribe")
                self.assertEqual(get_error.exception.code, 405)

                with patch.dict("os.environ", {OPENROUTER_API_KEY_ENV: "sk-or-test"}, clear=False):
                    with patch(
                        "tradercockpit.assistant_voice._urllib_transport",
                        return_value=(200, json.dumps({"text": "Open Evolutionary Search."}).encode()),
                    ) as transport:
                        request = Request(
                            f"{base}/api/assistant/transcribe",
                            data=b"webm-audio",
                            headers={"content-type": "audio/webm"},
                            method="POST",
                        )
                        with urlopen(request) as response:
                            payload = json.loads(response.read())
                    self.assertEqual(payload["schema"], ASSISTANT_TRANSCRIPT_SCHEMA)
                    self.assertEqual(payload["transcript"], "Open Evolutionary Search.")
                    sent = json.loads(transport.call_args.args[1])
                    self.assertEqual(sent["input_audio"]["format"], "webm")

                    query = Request(
                        f"{base}/api/assistant/transcribe?path=C:/sqcli.exe",
                        data=b"webm-audio",
                        headers={"content-type": "audio/webm"},
                        method="POST",
                    )
                    with self.assertRaises(HTTPError) as query_error:
                        urlopen(query)
                    self.assertEqual(query_error.exception.code, 400)
            finally:
                server.shutdown()
                server.server_close()


if __name__ == "__main__":
    unittest.main()
