from datetime import datetime
import html
import logging
import re
from bs4 import BeautifulSoup
import nest_asyncio
import httpx
from fake_useragent import UserAgent

nest_asyncio.apply()

LOGIN_URL = r"https://icas.bau.edu.lb:8443/cas/login?service=https%3A%2F%2Fmoodle.bau.edu.lb%2Flogin%2Findex.php"
SECURE_URL = r"https://moodle.bau.edu.lb/my/"
SERIVCE_URL = r"https://moodle.bau.edu.lb/lib/ajax/service.php"
cookie_jar = dict()
ua = UserAgent(browsers=["chrome", "firefox"], os=["windows", "macos"])

### Utilities


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


# courses_querystring = {
#     "service": "https://moodle.bau.edu.lb/login/index.php"}

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


async def login(Session: httpx.AsyncClient, referer: str, username: str, password: str):
    url = "https://icas.bau.edu.lb:8443/cas/login"
    params = {"service": referer}
    page = await Session.get(url=url, headers=login_headers, params=params)
    my_format("HTML exists  ", f"{bool(page.text)}")
    execution = css_selector(page.text, "[name=execution]", "value")
    my_format("execution string: ", execution)
    l = await Session.post(
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


async def login_moodle(Session: httpx.AsyncClient, username: str, password: str):
    return await login(
        Session, "https://moodle.bau.edu.lb/login/index.php", username, password
    )


async def get_mappings(Session: httpx.AsyncClient, username: str, password: str):
    courses = await moodle_api(Session, username, password, "courses")
    mappings = {
        item["shortname"].split("-")[0]: " ".join(
            word
            for word in re.sub(r"[^a-zA-Z\s]", " ", html.unescape(item["fullname"]))
            .strip()
            .split()
            if not word.isspace()
        )
        for item in courses[0]["data"]["courses"]
    }
    return mappings


async def get_schedule(Session: httpx.AsyncClient, username: str, password: str):
    # TODO : also make a more efficient login mechanism because this takes way too long
    Session, response = await login(
        Session,
        "http://ban-prod-ssb2.bau.edu.lb:8010/ssomanager/c/SSB?pkg=bwskfshd.P_CrseSchd",
        username,
        password,
    )
    mappings = await get_mappings(Session, username, password)
    # we want to parse the html from it
    # uncomment this for testing
    # response = open("./scratch.html").read()
    json_response = []
    soop = soup_bowl(response)
    course_items = list(i.contents for i in soop.select(".ddlabel > a"))
    for course in course_items:
        subject_key = course[0].split("-")[0]  # type:ignore
        crn = course[2].split()[0]  # type:ignore
        time = course[4]  # type: ignore
        location = course[6]
        if not time:
            raise Exception("Couldn't find time for course!")
        subject = mappings[re.sub(r"\s", "", subject_key)]
        # subject = re.sub(r"\s", "", subject_key)
        json_response.append(
            {
                "CRN number": crn,
                "subject": subject,
                "time": time,
                "location": location,
            }
        )
    return {"Courses": json_response}


async def moodle_api(
    Session: httpx.AsyncClient,
    username: str,
    password: str,
    endpoint: str,
):
    """
    @param Session : the server's global session.
    @param username: the username from basic auth.
    @param password: the password from basic auth.
    @param api_querystring: URL encoded query for each moodle API.
    @param api_args : the arguments for the called moodle method.
    @param needs_userid : not all moodle APIs need the useridto argument .

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
    userid_field = request_json["id_field"]

    moodle_html = ""
    # check that we actually HAVE moodle cookies!
    # We may have logged in and obtained cookies for another service!
    moodle_cookies = next(
        filter(lambda cookie: "Moodle" in cookie.domain, Session.cookies.jar), None
    )
    # If we have NO cookies at ALL, just login to moodle.
    # Or if we do have cookies,we will just get sent back to login page if we have no moodle cookies
    if not Session.cookies.jar:
        Session, moodle_html = await login_moodle(Session, username, password)
    else:
        # we do have cookies, but they are not moodle cookies. We can bypass the login anyway
        # the moodle dashboard will be returned
        if not moodle_cookies:
            url = "https://icas.bau.edu.lb:8443/cas/login"
            params = {"service": "https://moodle.bau.edu.lb/login/index.php"}
            moodle_html = (
                await Session.get(url=url, headers=login_headers, params=params)
            ).text
        else:
            # we have moodle cookies, just get the page
            moodle_html = (await Session.get(SECURE_URL)).text

    sesskey, userid = get_user_info(moodle_html)
    api_payload = [
        {
            "index": 0,
            "methodname": request_json["method_name"],
            "args": request_json["args"],
        }
    ]
    if userid_field:
        api_payload[0]["args"][userid_field] = userid

    api_querystring = {"sesskey": sesskey, "info": request_json["method_name"]}
    api_response = await Session.post(
        url=SERIVCE_URL,
        headers=service_headers,
        json=api_payload,
        params=api_querystring,
    )
    return api_response.json()
