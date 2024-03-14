import asyncio
import unittest

from httpx import Client, BasicAuth


class APITest(unittest.TestCase):

    def setUp(self) -> None:
        with open("./bot_data_stuff/creds.txt") as f:
            self.username, self.password = f.read().splitlines()[:2]
        self.client = Client(
            follow_redirects=True,
            timeout=None,
            auth=BasicAuth(self.username, self.password),
        )
        return super().setUp()

    def tearDown(self) -> None:

        return super().tearDown()

    def test_moodle_no_cache(self):

        endpoints = ["calendar", "recent_courses", "notifications", "courses"]
        responses = [
            self.client.get("http://127.0.0.1:5000/moodle/{}".format(endpoint))
            for endpoint in endpoints
        ]

        for response in responses:
            response_json = response.json()
            assert response.status_code == 200
            assert response_json["error"] != True

        self.client.close()

    def test_moodle_with_cache(self): ...

    def test_others(self): ...

    def test_others_with_cache(self): ...


if __name__ == "__main__":
    unittest.main()
