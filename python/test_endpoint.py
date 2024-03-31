from http import HTTPStatus
import json
import os
import unittest
from dotenv import load_dotenv
from httpx import Client, BasicAuth

load_dotenv()


class APITest(unittest.TestCase):

    def setUp(self) -> None:
        username, password = os.environ.get("USERNAME"), os.environ.get("PASSWORD")
        if not username or not password:
            raise Exception("Cannot find username or password!")
        self.username, self.password = username, password
        self.client = Client(
            follow_redirects=True,
            timeout=None,
            auth=BasicAuth(self.username, self.password),
        )
        self.url = "http://127.0.0.1:5000/"
        return super().setUp()

    def tearDown(self) -> None:
        self.client.close()
        return super().tearDown()

    def all_tests(self, method: str, request_json=None):

        endpoints = ["calendar", "recent_courses", "notifications", "courses"]
        responses = [
            self.client.request(
                method, "{}/moodle/{}".format(self.url, endpoint), json=request_json
            )
            for endpoint in endpoints
        ]

        responses.extend(
            [
                self.client.request(
                    method, "{}/{}".format(self.url, endpoint), json=request_json
                )
                for endpoint in ["schedule", "mappings"]
            ]
        )
        return responses

    def test_wrong_creds(self):
        no_auth_client = Client()
        # test without basic auth, any endpoint is fine
        response = no_auth_client.get(self.url + "moodle/notifications")
        assert response.status_code == HTTPStatus.UNAUTHORIZED
        # test with wrong creds (username is incorrect)
        no_auth_client = Client(auth=BasicAuth(self.username + "dwqdq", self.password))
        response = no_auth_client.get(self.url + "moodle/notifications")
        assert response.status_code == HTTPStatus.BAD_REQUEST
        # test with wrong creds (username is correct but password isn't)
        no_auth_client = Client(auth=BasicAuth(self.username, "fqwfqfwqf"))
        response = no_auth_client.get(self.url + "moodle/notifications")
        assert response.status_code == HTTPStatus.BAD_REQUEST
        # test with correct creds but malformed input json
        response = self.client.get(
            self.url + "moodle/notifications",
            headers={"Content-Type": "application/json"},
        )
        assert response.status_code == HTTPStatus.BAD_REQUEST

    def test_without_cache(self):
        responses = self.all_tests("GET")
        for response in responses:
            response_json = response.json()
            assert response.status_code == HTTPStatus.OK
            assert all(
                response_json[feature] for feature in ["sesskey", "userid", "cookies"]
            )

    def test_with_cache(self):
        request_json = self.client.get(
            self.url + "moodle/notifications",
            headers={"Content-Type": "application/json"},
        ).json()
        responses = self.all_tests("POST", request_json=request_json)
        for response in responses:
            response_json = response.json()
            assert response.status_code == HTTPStatus.OK
            assert all(
                response_json[feature] for feature in ["sesskey", "userid", "cookies"]
            )


if __name__ == "__main__":
    unittest.main()
