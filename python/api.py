import http
import os
from flask import Flask, abort, request
from endpoint import get_courses, get_notifications, get_schedule, login_moodle
from functools import wraps
from dotenv import load_dotenv
import nest_asyncio
import asyncio

from registration import register_courses
app = Flask(__name__)
load_dotenv()
app.secret_key = os.environ.get("SECRET_KEY")
nest_asyncio.apply()

def requires_basic_auth(func):
    @wraps(func)

    def decorated_function(*args, **kwargs):
        if not request.authorization:
            return abort(401) # Not authorized
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
    
    return username,password
    


@app.route("/")
def home():
    return "-------------------------Welcome to Better Moodle--------------------"

@app.route("/notifications")
@requires_basic_auth
def test():
    username,password = get_creds()
    Session,html = asyncio.run(login_moodle(username,password))
    try:
        print("Welcome to the notifications center!")
        return asyncio.run(get_notifications(html,Session))
    finally:
        asyncio.run(Session.aclose())

@app.route("/schedule")
@requires_basic_auth
def wel():
    username,password = get_creds()
    schedule,client = asyncio.run(get_schedule(username,password))
    try:
        return schedule
    finally:
        asyncio.run(client.aclose())

@app.route("/courses")
@requires_basic_auth
def courses():
    username,password = get_creds()
    Session,moodle_html = asyncio.run(login_moodle(username,password))
    try:
        return asyncio.run(get_courses(moodle_html,Session))
    finally:
        asyncio.run(Session.aclose())

@app.route("/register",methods=["POST"])
@requires_basic_auth
def register():
    courses = request.form["courses"]
    username,password = get_creds()
    register_courses(courses,username,password)
    return "OK",200

if __name__ == "__main__":
    app.run("0.0.0.0",debug=False)