from http import HTTPStatus
import json
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
                for endpoint in ["schedule"]
            ]
        )
        return responses

    def test_wrong_creds(self):
        def response_unsuccessful(code: int):
            return code >= 400 and code <= 500

        no_auth_client = Client(timeout=None)
        # test without basic auth, any endpoint is fine
        response = no_auth_client.get(self.url + "moodle/notifications")
        assert response_unsuccessful(response.status_code)
        # test with wrong creds (username is incorrect)
        no_auth_client.auth = auth = BasicAuth(self.username + "dwqdq", self.password)
        response = no_auth_client.get(self.url + "moodle/notifications")
        assert response_unsuccessful(response.status_code)
        # test with wrong creds (username is correct but password isn't)
        # no_auth_client = Client(
        #     auth=BasicAuth(self.username, "fqwfqfwqf"), timeout=None
        # )
        no_auth_client.auth = BasicAuth(self.username, "fqwfqfwqf")
        response = no_auth_client.get(self.url + "moodle/notifications")
        assert response_unsuccessful(response.status_code)
        # test with correct creds but malformed input json
        response = self.client.get(
            self.url + "moodle/notifications",
            headers={"Content-Type": "application/json"},
        )
        assert response_unsuccessful(response.status_code)
        no_auth_client.close()

    def test_without_cache(self):
        responses = self.all_tests("GET")
        for response in responses:
            response_json = response.json()
            assert response.status_code == HTTPStatus.OK
            assert all(
                response_json[feature] for feature in ["sesskey", "userid", "cookies"]
            )

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


if __name__ == "__main__":
    load_dotenv()
    suite = unittest.TestSuite()
    suite.addTest(APITest("test_with_cache"))
    runner = unittest.TextTestRunner()
    runner.run(suite)
