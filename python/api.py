from datetime import datetime, timedelta
import http
import json
import os
from flask import Flask, Response, abort, redirect, request
import httpx
from functools import wraps
from dotenv import load_dotenv
from endpoint import (
    login_moodle,
    moodle_api,
    return_error_json,
    serialize_session_cookies,
)
from registration import register_courses, schedule

app = Flask(__name__)
load_dotenv()
app.secret_key = os.environ.get("SECRET_KEY")


def add_cookies(Session: httpx.Client):
    if request.content_type == "application/json":
        # we got cookies from the user, add them to the session object
        assert request.json != None, "Json is empty!"
        for cookie in request.json["cookies"]:
            Session.cookies.set(**cookie)

    return Session


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
    # session.setdefault("session", httpx.Client(timeout=None, follow_redirects=True))
    return "-------------------------Welcome to Better Moodle--------------------", 200


@app.after_request
def add_metadata(response: Response):

    try:
        response_json = json.loads(response.get_data().decode())
        if response_json.get("error"):
            return return_error_json(
                http.HTTPStatus.BAD_REQUEST, "Invalid or expired sesskey!"
            )

        return response

    except json.decoder.JSONDecodeError:
        # we didn't get a json file
        return response


@app.route("/login", methods=["POST", "GET"])
@requires_basic_auth
def login():
    username, password = get_creds()
    with httpx.Client(timeout=None, follow_redirects=True) as Session:
        Session = add_cookies(Session)
        login_moodle(Session, username, password)
        expires = (datetime.now() + timedelta(hours=8)).timestamp()
        return serialize_session_cookies({"expires": expires}, Session)


@app.route("/schedule", methods=["GET", "POST"])
@requires_basic_auth
def get_schedule_endpoint():
    with httpx.Client(timeout=None, follow_redirects=True) as Session:
        Session = add_cookies(Session)
        username, password = get_creds()
        return serialize_session_cookies(schedule(Session, username, password), Session)


@app.route("/moodle/<endpoint>", methods=["GET", "POST"])
@requires_basic_auth
def moodle_route_variable(endpoint: str):
    username, password = get_creds()
    with httpx.Client(timeout=None, follow_redirects=True) as Session:
        Session = add_cookies(Session)
        try:
            response_json = moodle_api(Session, username, password, endpoint)
            return serialize_session_cookies(response_json, Session)

        except AssertionError:
            abort(
                return_error_json(
                    http.HTTPStatus.BAD_REQUEST, "Moodle has no such endpoint!"
                )
            )


@app.route("/register", methods=["POST"])
@requires_basic_auth
def register():
    with httpx.Client(timeout=None, follow_redirects=True) as Session:
        Session = add_cookies(Session)
        username, password = get_creds()
        courses = request.form["courses"]
        registration_page = register_courses(Session, courses, username, password)
        return registration_page, 200


if __name__ == "__main__":
    app.run("0.0.0.0", debug=False)
