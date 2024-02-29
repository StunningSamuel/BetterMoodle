from flask import Flask
from endpoint import get_notifications, get_schedule, login
import nest_asyncio
import asyncio
app = Flask(__name__)
nest_asyncio.apply()
@app.route("/")
def home():
    return "-------------------------Welcome to Better Moodle--------------------"

@app.route("/notifications")
def test():
    referer = r"https%3A%2F%2Fmoodle.bau.edu.lb%2Flogin%2Findex.php"
    Session,html = asyncio.run(login(referer))
    print("Welcome to the notifications center!")
    return asyncio.run(get_notifications(html,Session))

@app.route("/schedule")
def wel():
    return asyncio.run(get_schedule())
if __name__ == "__main__":
    app.run("0.0.0.0",debug=False)