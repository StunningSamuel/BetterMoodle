import http
import json
import os
from flask import Flask, Response, abort, request
import httpx
from functools import wraps
from dotenv import load_dotenv
import asyncio
from endpoint import (
    get_mappings,
    get_schedule,
    moodle_api,
    return_error_json,
    serialize_session_cookies,
)
from registration import register_courses

app = Flask(__name__)
load_dotenv()
app.secret_key = os.environ.get("SECRET_KEY")
Session = httpx.Client(timeout=None, follow_redirects=True)


def requires_basic_auth(func):
    @wraps(func)
    def decorated_function(*args, **kwargs):
        auth = request.authorization
        get_or_none = lambda key: auth.get(key) if auth else None
        username, password = get_or_none("username"), get_or_none("password")
        # if any of these are None, abort
        if not auth or not username or not password:
            return abort(
                return_error_json(
                    http.HTTPStatus.UNAUTHORIZED, "Username or password is missing!"
                )
            )  # Not authorized
        # university IDs cannot have letters
        elif not username.isnumeric():
            return abort(
                return_error_json(
                    http.HTTPStatus.BAD_REQUEST,
                    "University id {} is not valid.".format(username),
                )
            )

        # assuming all data is correct
        return func(*args, **kwargs)

    return decorated_function


def get_creds():
    auth = request.authorization
    if not auth:
        abort(http.HTTPStatus.BAD_REQUEST)
    username = auth.get("username")
    password = auth.get("password")
    if not username or not password:
        abort(http.HTTPStatus.BAD_REQUEST)

    return username, password


@app.route("/")
def home():
    return "-------------------------Welcome to Better Moodle--------------------"


@app.after_request
def add_metadata(response: Response):

    try:
        response_json = json.loads(response.get_data().decode())
        if response_json.get("error"):
            return return_error_json(
                http.HTTPStatus.BAD_REQUEST, "Invalid or expired sesskey!"
            )

        # after every request, we simply add the session cookies and a time stamp
        # If the endpoint wants to add more metadata it can do so.
        serialize_session_cookies(
            response_json,
            Session,
        )
        response.set_data(json.dumps(response_json).encode())
        return response

    except json.decoder.JSONDecodeError:
        # we didn't get a json file
        return response


@app.route("/schedule", methods=["GET", "POST"])
def get_schedule_endpoint():
    username, password = get_creds()
    return get_schedule(Session, username, password)


@app.route("/mappings", methods=["GET", "POST"])
def get_mappings_endpoint():
    username, password = get_creds()
    return get_mappings(Session, username, password)


@app.route("/moodle/<endpoint>", methods=["GET", "POST"])
@requires_basic_auth
def moodle_route_variable(endpoint: str):
    username, password = get_creds()
    try:
        if request.content_type == "application/json":
            # we got cookies from the user, add them to the session object
            assert request.json != None, "Json is empty!"
            for cookie in request.json["cookies"]:
                Session.cookies.set(**cookie)
        return moodle_api(Session, username, password, endpoint)

    except AssertionError:
        abort(
            return_error_json(
                http.HTTPStatus.BAD_REQUEST, "Moodle has no such endpoint!"
            )
        )


@app.route("/register", methods=["POST"])
@requires_basic_auth
def register():
    username, password = get_creds()
    courses = request.form["courses"]
    registration_page = asyncio.run(register_courses(courses, username, password))
    return registration_page, 200


if __name__ == "__main__":
    app.run("0.0.0.0", debug=False)
