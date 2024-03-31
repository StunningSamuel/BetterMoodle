from dataclasses import dataclass, field
from datetime import datetime
import html
import http
import json
import logging
import re
from typing import Callable
from bs4 import BeautifulSoup
from flask import Response, abort, request
import httpx
from fake_useragent import UserAgent


LOGIN_URL = r"https://icas.bau.edu.lb:8443/cas/login?service=https%3A%2F%2Fmoodle.bau.edu.lb%2Flogin%2Findex.php"
SECURE_URL = r"https://moodle.bau.edu.lb/my/"
SERIVCE_URL = r"https://moodle.bau.edu.lb/lib/ajax/service.php"
cookie_jar = dict()
ua = UserAgent(browsers=["chrome", "firefox"], os=["windows"])


### Utilities


def return_error_json(code: int, reason: str):
    return Response(
        status=code,
        content_type="application/json",
        response=json.dumps({"error_reason": reason}),
    )


def my_format(item, description=None, level=logging.info):
    if description is not None:
        return level(f"{description}: {item}")
    return level(f"{item}")


def soup_bowl(html):
    return BeautifulSoup(html, "lxml")


def css_selector(html: str, selector: str, attribute: str | None = None):
    soup = BeautifulSoup(html, "lxml")
    result = soup.select_one(selector)
    if not result:
        raise AttributeError("Failed to get value from css selector!")
    if attribute:
        return result[attribute]

    return result


def get_user_info(moodle_html: str):

    sesskey = css_selector(moodle_html, "[name=sesskey]", "value")
    userid = css_selector(moodle_html, "[data-userid]", "data-userid")

    assert type(sesskey) == str
    assert type(userid) == str

    return sesskey, userid


def serialize_session_cookies(dict_to_modify: dict, Session: httpx.Client):
    cookie_jar = Session.cookies.jar

    dict_to_modify["cookies"] = [
        {
            "name": cookie.name,
            "value": cookie.value,
            "domain": cookie.domain,
            "path": cookie.path,
        }
        for cookie in cookie_jar
    ]


class Notifications:
    POSSIBLE_KEYWORDS = ["exam", "homework", "lecture"]

    @dataclass
    class Notification:
        source_obj: dict

        @property
        def subject(self):
            subject_code = self.source_obj["fullmessage"].split("-")[0]
            return subject_code

        @property
        def time_created(self) -> datetime:
            return datetime.fromtimestamp(self.source_obj["timecreated"])

        @property
        def message(self) -> str:
            return (
                self.source_obj["fullmessage"]
                .strip("\n")
                .split(
                    "---------------------------------------------------------------------"
                )[1]
                .lower()
            )

        @property
        def title(self) -> str:
            return self.source_obj["subject"]

        @property
        def keywords(self):
            types = (
                dict.fromkeys(
                    ("quiz", "test", "exam", "exams", "midterm", "final"), "exam"
                )
                | dict.fromkeys(("homework", "assignment", "hw", "solve"), "homework")
                | dict.fromkeys(("grade", "mark"), "grades")
                | dict.fromkeys(("lecture", "location", "session"), "lecture")
            )
            keywords_set = set()
            whole_words_regex = re.compile("|".join(keyword for keyword in types))
            # search title as well because there might be missing keywords
            for result in whole_words_regex.findall(self.title + "\n\n" + self.message):
                keywords_set.add(types[result])

            return keywords_set

        @property
        def deadline(self):
            return

        def set_mappings(self, mappings: dict[str, str]):
            self.mappings = mappings

        def __str__(self) -> str:
            """
            Builds a string incrementally from object until it reaches a breakpoint (a field is missing)
            """

            def built_strings():
                attributes = [
                    name
                    for name in dir(self)
                    if not name.startswith("_") and name != "source_obj"
                ][::-1]

                for attribute in attributes:
                    if getattr(self, attribute):
                        yield f"{attribute} : {getattr(self,attribute)}"

            padding = "=" * 30
            return "\n".join(filter(None, built_strings())) + "\n" + padding + "\n"

    def __init__(
        self, json_objects: str, mappings: dict[str, str] | None = None
    ) -> None:
        self.notifications = [
            self.Notification(obj)
            for obj in json.loads(json_objects)["data"]["notifications"]
        ]
        if mappings:
            for notification in self.notifications:
                notification.set_mappings(mappings)

    def search(self, query: str):

        return list(
            notification
            for notification in self.notifications
            if (notification.title + "\n\n" + notification.message).find(query) != -1
        )

    def filter_notifications(self, func: Callable[[Notification], bool]):

        return list(filter(func, self.notifications))

    def get_important(self):
        return self.filter_notifications(lambda n: bool(n.keywords))

    def get_keywords(self, keywords: list[str]):
        return self.filter_notifications(
            lambda notification: any(
                keyword in keywords for keyword in notification.keywords
            )
        )

    @staticmethod
    def pretty_print(notifications: list[Notification]):
        for notification in notifications:
            print(notification)


### End utilities

service_headers = {
    "User-Agent": ua.random,
    "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/jxl,image/webp,*/*;q=0.8",
    "Accept-Language": "en-US,en;q=0.5",
    "Accept-Encoding": "gzip, deflate, br",
    "DNT": "1",
    "Connection": "keep-alive",
    "Upgrade-Insecure-Requests": "1",
    "Sec-Fetch-Dest": "document",
    "Sec-Fetch-Mode": "navigate",
    "Sec-Fetch-Site": "same-origin",
    "Sec-Fetch-User": "?1",
    "Sec-GPC": "1",
}


login_headers = {
    "User-Agent": ua.random,
    "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/jxl,image/webp,*/*;q=0.8",
    "Accept-Language": "en-US,en;q=0.5",
    "Accept-Encoding": "gzip, deflate, br",
    "Content-Type": "application/x-www-form-urlencoded",
    "DNT": "1",
    "Connection": "keep-alive",
    "Upgrade-Insecure-Requests": "1",
    "Sec-Fetch-Dest": "document",
    "Sec-Fetch-Mode": "navigate",
    "Sec-Fetch-Site": "same-origin",
    "Sec-Fetch-User": "?1",
    "Sec-GPC": "1",
}

api_headers = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/99.0.7113.93 Safari/537.36",
    "Accept": "application/json, text/javascript, */*; q=0.01",
    "Accept-Language": "en-US,en;q=0.5",
    "Accept-Encoding": "gzip, deflate, br",
    "Content-Type": "application/json",
    "X-Requested-With": "XMLHttpRequest",
    "Origin": "https://moodle.bau.edu.lb",
    "DNT": "1",
    "Connection": "keep-alive",
    "Referer": "https://moodle.bau.edu.lb/my/",
    "Sec-Fetch-Dest": "empty",
    "Sec-Fetch-Mode": "cors",
    "Sec-Fetch-Site": "same-origin",
    "Sec-GPC": "1",
}


def login(Session: httpx.Client, referer: str, username: str, password: str):
    url = "https://icas.bau.edu.lb:8443/cas/login"
    params = {"service": referer}
    page = Session.get(url=url, headers=login_headers, params=params)
    my_format("HTML exists  ", f"{bool(page.text)}")
    execution = css_selector(page.text, "[name=execution]", "value")
    my_format("execution string: ", execution)
    l = Session.post(
        "https://icas.bau.edu.lb:8443/cas/login",
        data={
            "username": username,
            "password": password,
            "execution": rf"{execution}",
            "_eventId": "submit",
            "geolocation": "",
        },
        headers=login_headers,
        params={"service": referer},
        timeout=None,
    )
    my_format(l.url, "We are on")
    Session.cookies.extract_cookies(l)
    return Session, l.text


def login_moodle(Session: httpx.Client, username: str, password: str):
    return login(
        Session, "https://moodle.bau.edu.lb/login/index.php", username, password
    )


def get_mappings(Session: httpx.Client, username: str, password: str):
    mappings = {}
    courses = moodle_api(Session, username, password, "courses", mappings)
    # we can't catch it in the after request processor because we're using this as an internal function
    if courses["error"] == True:
        return abort(
            return_error_json(http.HTTPStatus.BAD_REQUEST, "Sesskey is logged out!")
        )

    # assuming sesskey not expired, continue
    mappings.update(
        {
            item["shortname"].split("-")[0]: " ".join(
                word
                for word in re.sub(r"[^a-zA-Z\s]", " ", html.unescape(item["fullname"]))
                .strip()
                .split()
                if not word.isspace()
            )
            for item in courses["data"]["courses"]
        }
    )
    return mappings


def get_schedule(
    Session: httpx.Client,
    username: str,
    password: str,
):
    # get cookies from moodle on dry run because it is faster
    mappings = get_mappings(Session, username, password)
    url = "https://icas.bau.edu.lb:8443/cas/login"
    params = {
        "service": "http://ban-prod-ssb2.bau.edu.lb:8010/ssomanager/c/SSB?pkg=bwskfshd.P_CrseSchd"
    }
    response = Session.get(url, params=params, headers=login_headers)
    # we want to parse the html from it
    # uncomment this for testing
    # response = open("./scratch.html").read()
    json_response = []
    soop = soup_bowl(response.text)
    course_items = list(i for i in soop.select(".ddlabel > a"))
    weekday_row = soop.select_one(".datadisplaytable > tbody > tr")
    assert weekday_row
    weekday_row = weekday_row.select("th")
    for course in course_items:
        parent_td = course.parent
        assert parent_td
        parent_tr = parent_td.parent
        assert parent_tr
        day = weekday_row[parent_tr.select(".ddlabel").index(parent_td)].text.strip()
        contents = course.contents
        subject_key = contents[0].split("-")[0]  # type:ignore
        crn = contents[2].split()[0]  # type:ignore
        time = contents[4]  # type: ignore
        location = contents[6]
        if not time:
            raise Exception("Couldn't find time for course!")
        subject = mappings[re.sub(r"\s", "", subject_key)]
        # subject = re.sub(r"\s", "", subject_key)
        json_response.append(
            {
                "CRN number": crn,
                "subject": subject,
                "time": time,
                "day": day,
                "location": location,
            }
        )

    mappings.update({"Courses": json_response})
    return mappings


def moodle_api(
    Session: httpx.Client,
    username: str,
    password: str,
    endpoint: str,
    dict_to_modify: dict | None = None,
):
    """
    @param Session : the server's global session.
    @param username: the username from basic auth.
    @param password: the password from basic auth.
    @param dict_to_modify : the dictionary to mutate in order to propagate the sesskey up the call chain. That is, if a function calls this function it will modify the top function's response.

    """
    now = datetime.now()
    endpoint_info = {
        "calendar": {
            "method_name": "core_calendar_get_calendar_monthly_view",
            "args": {
                "year": now.year,
                "month": now.month,
                "courseid": 1,
                "categoryid": 0,
                "includenavigation": False,
                "mini": True,
                "day": now.day,
            },
            "id_field": None,
        },
        "recent_courses": {
            "method_name": "core_course_get_recent_courses",
            "args": {
                "limit": 10,
            },
            "id_field": "userid",
        },
        "notifications": {
            "method_name": "message_popup_get_popup_notifications",
            "args": {
                "offset": 0,
                "limit": 30,
            },
            "id_field": "useridto",
        },
        "courses": {
            "method_name": "core_course_get_enrolled_courses_by_timeline_classification",
            "args": {
                "offset": 0,
                "limit": 0,
                "classification": "all",
                "sort": "fullname",
                "customfieldname": "",
                "customfieldvalue": "",
            },
            "id_field": None,
        },
    }

    request_json = endpoint_info.get(endpoint)
    assert request_json, "Request JSON is empty!"

    def make_final_request(sesskey: str, userid: str):
        api_payload = [
            {
                "index": 0,
                "methodname": request_json["method_name"],
                "args": request_json["args"],
            }
        ]
        userid_field = request_json["id_field"]
        if userid_field:
            api_payload[0]["args"][userid_field] = userid

        api_querystring = {"sesskey": sesskey, "info": request_json["method_name"]}
        api_response = Session.post(
            url=SERIVCE_URL,
            headers=service_headers,
            json=api_payload,
            params=api_querystring,
        )
        # at the end of every request, return the current cookies
        response_json: dict = api_response.json()[0]
        response_json.update(sesskey=sesskey, userid=userid)
        return response_json

    moodle_html = ""
    # check that we actually HAVE moodle cookies!
    # We may have logged in and obtained cookies for another service!
    moodle_cookies = next(
        filter(lambda cookie: "Moodle" in cookie.domain, Session.cookies.jar), None
    )

    # if we already have a sesskey, just use it
    if request.content_type == "application/json":
        creds_json = request.json
        assert creds_json
        session_key = creds_json["sesskey"]
        userid = creds_json["userid"]
        return make_final_request(session_key, userid)

    # otherwise we have to get it from moodle
    # If we have NO cookies at ALL, just login to moodle.
    if not Session.cookies.jar:
        Session, moodle_html = login_moodle(Session, username, password)
    else:
        # we do have cookies, but they are not moodle cookies. We can bypass the login anyway
        # the moodle dashboard will be returned
        if not moodle_cookies:
            url = "https://icas.bau.edu.lb:8443/cas/login"
            params = {"service": "https://moodle.bau.edu.lb/login/index.php"}
            moodle_html = (
                Session.get(url=url, headers=login_headers, params=params)
            ).text
        else:
            # we have moodle cookies, just get the page
            moodle_html = Session.get(SECURE_URL).text

    try:
        sesskey, userid = get_user_info(moodle_html)

    # we have been redirected to login page, credentials are wrong.
    except AttributeError:
        return abort(
            return_error_json(
                http.HTTPStatus.BAD_REQUEST,
                "Provided credentials are wrong or server overloaded, please try again with correct ones.",
            )
        )

    final_json = make_final_request(sesskey, userid)
    if dict_to_modify != None:
        dict_to_modify.update(
            sesskey=final_json["sesskey"], userid=final_json["userid"]
        )
    return final_json
