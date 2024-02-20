import asyncio
from typing import Union
import httpx
from utilities.common import IO_DATA_DIR, my_format, css_selector, url_encode, soup_bowl, json, WebsiteMeta, mapping_init

LOGIN_URL = r"https://icas.bau.edu.lb:8443/cas/login?service=https%3A%2F%2Fmoodle.bau.edu.lb%2Flogin%2Findex.php"
SECURE_URL = r"https://moodle.bau.edu.lb/my/"
SERIVCE_URL = r"https://moodle.bau.edu.lb/lib/ajax/service.php"
cookie_jar = dict()

api_payload = [
    {
        "index": 0,
        "methodname": "message_popup_get_popup_notifications",
        "args": {
            "limit": 20,
            "offset": 0,
            "useridto": "20663"
        }
    }
]

courses_payload = [
    {
        "index": 0,
        "methodname": "core_course_get_enrolled_courses_by_timeline_classification",
        "args": {
            "offset": 0,
            "limit": 0,
            "classification": "all",
            "sort": "fullname",
            "customfieldname": "",
            "customfieldvalue": ""
        }
    }
]

service_headers = {
    'User-Agent': "Mozilla/5.0 (Windows NT 10.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/99.0.7113.93 Safari/537.36",
    'Accept': "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/jxl,image/webp,*/*;q=0.8",
    'Accept-Language': "en-US,en;q=0.5",
    'Accept-Encoding': "gzip, deflate, br",
    'Referer': "https://moodle.bau.edu.lb/message/output/popup/notifications.php",
    'DNT': "1",
    'Connection': "keep-alive",
    'Upgrade-Insecure-Requests': "1",
    'Sec-Fetch-Dest': "document",
    'Sec-Fetch-Mode': "navigate",
    'Sec-Fetch-Site': "same-origin",
    'Sec-Fetch-User': "?1",
    'Sec-GPC': "1"
}
courses_querystring = {
    "service": "https://moodle.bau.edu.lb/login/index.php"}
login_headers = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/99.0.7113.93 Safari/537.36",
    "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/jxl,image/webp,*/*;q=0.8",
    "Accept-Language": "en-US,en;q=0.5",
    "Accept-Encoding": "gzip, deflate, br",
    "Content-Type": "application/x-www-form-urlencoded",
    "Origin": "https://icas.bau.edu.lb:8443",
    "DNT": "1",
    "Connection": "keep-alive",
    "Referer": r"https://icas.bau.edu.lb:8443/cas/login?service=https%3A%2F%2Fmoodle.bau.edu.lb%2Flogin%2Findex.php",
    "Upgrade-Insecure-Requests": "1",
    "Sec-Fetch-Dest": "document",
    "Sec-Fetch-Mode": "navigate",
    "Sec-Fetch-Site": "same-origin",
    "Sec-Fetch-User": "?1",
    "Sec-GPC": "1"}

api_headers = {
    'User-Agent': "Mozilla/5.0 (Windows NT 10.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/99.0.7113.93 Safari/537.36",
    'Accept': "application/json, text/javascript, */*; q=0.01",
    'Accept-Language': "en-US,en;q=0.5",
    'Accept-Encoding': "gzip, deflate, br",
    'Content-Type': "application/json",
    'X-Requested-With': "XMLHttpRequest",
    'Origin': "https://moodle.bau.edu.lb",
    'DNT': "1",
    'Connection': "keep-alive",
    'Referer': "https://moodle.bau.edu.lb/my/",
    # 'Cookie': "MoodleSession=bnmceob122qsilkle4iqj2nsvu; BNES_MoodleSession=eA7cedJkx2sqRLiU1yZBleUUCLVzGKRLv+uNNazIb25Y7AmDI8GOHXXADsmVbmSzHi2Cm3kXHmgAUxO1NfVrFT9jvdY+Lp1Yfc81DRIOiJadqJT0nTgwOA==",
    'Sec-Fetch-Dest': "empty",
    'Sec-Fetch-Mode': "cors",
    'Sec-Fetch-Site': "same-origin",
    'Sec-GPC': "1"

}


async def login():
    global cookie_jar
    Session = httpx.AsyncClient(follow_redirects=True)
    global service_headers, api_headers
    page = await Session.get(url=LOGIN_URL)
    my_format("HTML exists  ", f"{bool(page.text)}")
    execution = css_selector(page.text, "[name=execution]", "value")
    my_format("execution string: ", execution)
    l = await Session.post(LOGIN_URL,
                            data=url_encode(
                                {"username": WebsiteMeta.username,
                                "password": WebsiteMeta.password,
                                "execution": fr"{execution}", "_eventId": "submit",
                                "geolocation": ""}),
                            headers=login_headers, params=courses_querystring)
    Session.cookies.extract_cookies(l)
    cookie_jar = dict(Session.cookies.items())
    my_format(
        f"{cookie_jar['MoodleSession'], cookie_jar['BNES_MoodleSession' ]}", "Required Items")
    # service_headers, api_headers = tuple(add_cookies_to_header(
    #     header, cookie_jar) for header in (service_headers, api_headers))
    # r = await Session.get(SECURE_URL, headers=service_headers)
    # assert r.url == SECURE_URL
    # assert 'notifications' in r.text
    # my_format("Cookies in service headers: ",
    #             f"{json.dumps(service_headers,indent=4)}")

    return Session, l.text

def get_user_info(moodle_html : str):
    soup : str = soup_bowl(moodle_html)
    sesskey: str = soup.select_one("[name=sesskey]")["value"]
    userid = soup.select_one("[data-userid]").attrs["data-userid"]
    return sesskey,userid

    

async def get_notifications(moodle_html : str, Session : httpx.AsyncClient) -> Union[dict, list]:
    sesskey,userid = get_user_info(moodle_html)
    my_format("Payload json object: ", api_payload)
    api_querystring = {"sesskey": sesskey,
                        "info": "message_popup_get_popup_notifications"}
    api_payload[0]["args"]["useridto"] = userid
    my_format("User ID: ", userid)
    api_response = await Session.post(url=SERIVCE_URL,
                                        headers=service_headers,
                                        json=api_payload,
                                        params=api_querystring
                                        )
    IO_DATA_DIR("results.json", "w", text=json.dumps(
        api_response.json(), indent=4))
    return api_response.json()

async def get_courses(moodle_html : str, Session : httpx.AsyncClient) -> Union[dict, list]:
    sesskey, _ = get_user_info(moodle_html)
    courses_querystring = {
        "sesskey": sesskey, "info": "core_course_get_enrolled_courses_by_timeline_classification"}
    courses = await Session.post(url=SERIVCE_URL,
                                    headers=service_headers,
                                    json=courses_payload,
                                    params=courses_querystring)
    required_json = courses.json()
    if required_json:
        IO_DATA_DIR("courses.json", "w",
                        json.dumps(required_json, indent=4))
    return required_json


async def main():
    Session, moodle_html = await login()
    notifications, courses = await asyncio.gather(get_notifications(moodle_html,Session),get_courses(moodle_html,Session))
    for i in (notifications, courses):
        assert type(i) != dict

asyncio.run(main())
# set up the data needed on data fetch
mapping_init()