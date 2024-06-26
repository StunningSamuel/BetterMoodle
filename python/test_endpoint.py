from datetime import datetime, timedelta
from http import HTTPStatus
import os
import unittest
from dotenv import load_dotenv
from httpx import Client, BasicAuth


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

    @staticmethod
    def response_unsuccessful(code: int):
        return code >= 400 and code <= 500

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
                for endpoint in ["schedule", "login"]
            ]
        )
        return responses

    def test_wrong_creds(self):

        no_auth_client = Client(timeout=None)
        # test without basic auth, any endpoint is fine
        response = no_auth_client.get(self.url + "moodle/notifications")
        assert self.response_unsuccessful(response.status_code)
        # test with wrong creds (username is incorrect)
        no_auth_client.auth = BasicAuth(self.username + "dwqdq", self.password)
        response = no_auth_client.get(self.url + "moodle/notifications")
        assert self.response_unsuccessful(response.status_code)
        no_auth_client.auth = BasicAuth(self.username, "fqwfqfwqf")
        response = no_auth_client.get(self.url + "moodle/notifications")
        assert self.response_unsuccessful(response.status_code)
        # test with correct creds but malformed input json
        response = self.client.get(
            self.url + "moodle/notifications",
            headers={"Content-Type": "application/json"},
        )
        assert self.response_unsuccessful(response.status_code)
        no_auth_client.close()

    def test_quick_invalidation(self):
        # test expired sesskey quick invalidation
        endpoint = self.url + "moodle/notifications"
        response = self.client.get(endpoint)
        response_json = response.json()
        # dummy session key value
        response_json["expires"] = (
            datetime.now() - timedelta(hours=8)
        ).timestamp()  # moodle session keys last 8 hours
        response = self.client.post(endpoint, json=response_json)
        assert response.status_code == 400

    def test_without_cache(self):
        responses = self.all_tests("GET")
        for response in responses:
            response_json = response.json()
            assert response.status_code == HTTPStatus.OK
            assert response_json["cookies"]

    def test_with_cache(self):
        first_request = self.client.get(
            self.url + "moodle/notifications",
        )
        first_request.raise_for_status()
        request_json = first_request.json()
        responses = self.all_tests("POST", request_json=request_json)
        for response in responses:
            response_json = response.json()
            assert response.status_code == HTTPStatus.OK
            assert response_json["cookies"]

    def test_login(self):

        # test a dry run and a cached one
        first_response = self.client.get(self.url + "login")
        second_response = self.client.post(
            self.url + "login", json=first_response.json()
        )
        assert all(
            not self.response_unsuccessful(resp.status_code)
            for resp in (first_response, second_response)
        )


if __name__ == "__main__":
    load_dotenv()
    unittest.main()
