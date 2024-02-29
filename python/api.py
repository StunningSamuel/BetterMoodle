import os
from flask import Flask
from endpoint import get_notifications, login, main
import asyncio
from typing import Union
import httpx
from utilities.common import add_cookies_to_header, IO_DATA_DIR, my_format, css_selector, url_encode, soup_bowl, json, WebsiteMeta, mapping_init
app = Flask(__name__)

@app.route("/")
def home():
    return "-------------------------Welcome to Better Moodle--------------------"

@app.route("/notifications")
def test():
    print("Welcome to the notifications center!")
    Session,html = asyncio.run(login())
    asyncio.run(get_notifications(html,Session))

@app.route("/schedule")
def wel():
    print("Welcome to the schedule center!")
    main()

if __name__ == "__main__":
    app.run("0.0.0.0")