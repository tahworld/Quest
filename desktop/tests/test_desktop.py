import json
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from ai import compact_context, generate, validate
from storage import Store


CONDITIONS = {"minutes": 15, "energy": 3, "resources": ["手机"]}


class DesktopTest(unittest.TestCase):
    def test_store_flow_and_reopen(self):
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder) / "quest.db"
            store = Store(path)
            store.save_direction("每天阅读")
            draft = generate("每天阅读", "", CONDITIONS, [], [])
            first = store.create_quest("每天阅读", "", CONDITIONS, draft)
            self.assertTrue(store.transition(first, "PENDING", "ACTIVE"))
            self.assertFalse(store.transition(first, "PENDING", "ACTIVE"))
            self.assertEqual(Store(path).latest_active()["id"], first)
            self.assertTrue(store.transition(first, "ACTIVE", "COMPLETED", "读完一段，记了一个例子"))
            self.assertEqual(Store(path).direction(), "每天阅读")
            self.assertEqual(Store(path).recent()[0]["result_text"], "读完一段，记了一个例子")
            second = store.create_quest("每天阅读", "", CONDITIONS, draft)
            self.assertTrue(store.reject(second, "现在做不了"))
            self.assertEqual(store.recent_rejections()[0]["reason"], "现在做不了")

    def test_validation_rejects_generic_and_oversized(self):
        good = generate("跑步", "", CONDITIONS, [], [])
        with self.assertRaises(ValueError):
            validate({**good, "title": "学习一下"}, 15)
        with self.assertRaises(ValueError):
            validate({**good, "estimatedMinutes": 30}, 15)
        with self.assertRaises(ValueError):
            validate({**good, "steps": []}, 15)

    def test_bounded_context_and_continuity(self):
        with tempfile.TemporaryDirectory() as folder:
            store = Store(Path(folder) / "test.db")
            first = store.create_quest("每天阅读", "", CONDITIONS,
                                       generate("每天阅读", "", CONDITIONS, [], []))
            store.transition(first, "PENDING", "ACTIVE")
            store.transition(first, "ACTIVE", "COMPLETED", "找到了一个可复述的例子")
            draft = generate("每天阅读", "", CONDITIONS, store.recent(), [])
            self.assertIn("上次", " ".join(draft["steps"]))
            context = compact_context("X" * 300, "Y" * 600, CONDITIONS,
                                      store.recent() * 12, [{"reason": "换一个"}] * 20)
            self.assertEqual(len(context["recentQuests"]), 5)
            self.assertEqual(len(context["recentRejections"]), 3)
            self.assertEqual(len(context["direction"]), 200)
            self.assertEqual(len(context["currentIntention"]), 500)

    def test_empty_api_response_retries_once(self):
        draft = generate("跑步", "", CONDITIONS, [], [])
        with patch("ai._request", side_effect=[("", "length"), (json.dumps(draft), "stop")]) as request:
            self.assertEqual(generate("跑步", "", CONDITIONS, [], [], api_key="test")["title"], draft["title"])
            self.assertEqual(request.call_count, 2)
            self.assertEqual(request.call_args.args[-2:], (4096, "none"))

    def test_no_key_never_calls_network(self):
        with patch("ai._request") as request:
            self.assertTrue(generate("每天阅读", "", CONDITIONS, [], [])["steps"])
            request.assert_not_called()


if __name__ == "__main__":
    unittest.main()
