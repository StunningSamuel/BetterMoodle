from flask import Flask
app = Flask(__name__)

@app.route("/")
def home():
    return "-------------------------Welcome to Better Moodle--------------------"

@app.route("/notifications")
def test():
    return "Welcome to the notifications center!"

@app.route("/schedule")
def wel():
    return "Welcome to the schedule center!"

if __name__ == "__main__":
    app.run("0.0.0.0")