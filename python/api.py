import http
import json
import os
from flask import Flask, Response, abort, request
import httpx
from functools import wraps
from dotenv import load_dotenv
import asyncio
from endpoint import get_mappings, get_schedule, moodle_api
from registration import register_courses

app = Flask(__name__)
load_dotenv()
app.secret_key = os.environ.get("SECRET_KEY")
Session = httpx.AsyncClient(timeout=None, follow_redirects=True)


def return_error_json(code: int, reason: str):
    return abort(
        Response(
            status=code,
            content_type="application/json",
            response=json.dumps({"error_reason": reason}),
        )
    )


def requires_basic_auth(func):
    @wraps(func)
    def decorated_function(*args, **kwargs):
        auth = request.authorization
        get_or_none = lambda key: auth.get(key) if auth else None
        username, password = get_or_none("username"), get_or_none("password")
        # if any of these are None, abort
        if not auth or not username or not password:
            return abort(
                Response(
                    status=http.HTTPStatus.BAD_REQUEST, content_type="application/json"
                )
            )  # Not authorized
        # university IDs cannot have letters
        elif not username.isnumeric():
            return abort(400)

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


@app.route("/schedule")
def get_schedule_endpoint():
    username, password = get_creds()
    return asyncio.run(get_schedule(Session, username, password))


@app.route("/mappings")
def get_mappings_endpoint():
    username, password = get_creds()
    return asyncio.run(get_mappings(Session, username, password))


@app.route("/moodle/<endpoint>")
@requires_basic_auth
def moodle_route_variable(endpoint: str):
    username, password = get_creds()
    try:
        return asyncio.run(
            moodle_api(
                Session,
                username,
                password,
                endpoint,
            )
        )

    except AssertionError:
        abort(
            return_error_json(
                http.HTTPStatus.BAD_REQUEST, "Moodle has no such endpoint!"
            )
        )
    except AttributeError:
        return abort(
            return_error_json(
                http.HTTPStatus.INTERNAL_SERVER_ERROR,
                "Sesskey expired! Please try again",
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
