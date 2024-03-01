import html
import json
import logging
import re
from typing import Union
from bs4 import BeautifulSoup
import httpx

LOGIN_URL = r"https://icas.bau.edu.lb:8443/cas/login?service=https%3A%2F%2Fmoodle.bau.edu.lb%2Flogin%2Findex.php"
SECURE_URL = r"https://moodle.bau.edu.lb/my/"
SERIVCE_URL = r"https://moodle.bau.edu.lb/lib/ajax/service.php"
cookie_jar = dict()


### Utilities

def my_format(item, description=None, level=logging.info):
    if description is not None:
        return level(f"{description}: {item}")
    return level(f"{item}")

# def url_encode(vals : dict):
#     temp : list[str] = []
#     for i, j in enumerate(list(zip(vals, vals.values()))):
#         temp[i] = "=".join(j)
#     return "&".join(temp)


def soup_bowl(html): return BeautifulSoup(html, "lxml")

def css_selector(html: str, selector="", value=None):
    soup = BeautifulSoup(html, "lxml").select(selector)
    soup = soup[0][value] if value else soup
    return soup


def raise_if_none(soup : BeautifulSoup,selector: str, attribute : str | None = None):
    result = soup.select_one(selector)
    if not result :
        raise Exception("Failed to get value from css selector!")
    if attribute:
        return getattr(result,attribute)
    
    return result


def get_user_info(moodle_html : str):

    soup = soup_bowl(moodle_html)
    sesskey = raise_if_none(soup,"[name=sesskey]","attrs")["value"]
    userid = raise_if_none(soup,"[data-userid]","attrs")["data-userid"]

    return sesskey,userid




# set up the data needed on data fetch
# mapping_init()

### End utilities

api_payload = [
    {
        "index": 0,
        "methodname": "message_popup_get_popup_notifications",
        "args": {
            "limit": 20,
            "offset": 0,
            "useridto": ""
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
    # "Referer": r"https://icas.bau.edu.lb:8443/cas/login?service=https%3A%2F%2Fmoodle.bau.edu.lb%2Flogin%2Findex.php",
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



async def login(referer : str, username : str, password : str):
    unencoded_url = fr"https://icas.bau.edu.lb:8443/cas/login?service={referer}"
    login_headers["Referer"] = unencoded_url
    my_format(unencoded_url, "Login URL is")
    Session = httpx.AsyncClient(follow_redirects=True)
    page = await Session.get(url=unencoded_url)
    my_format("HTML exists  ", f"{bool(page.text)}")
    execution = css_selector(page.text, "[name=execution]", "value")
    my_format("execution string: ", execution)
    l = await Session.post(unencoded_url,
                            data={"username": username,
                                "password": password,
                                "execution": fr"{execution}", "_eventId": "submit",
                                "geolocation": ""}, 
                            headers=login_headers,
                            timeout=None)
    my_format(l.url, "We are on")
    Session.cookies.extract_cookies(l)
    return Session, l.text

async def login_moodle(username : str, password : str):
    return await login("https%3A%2F%2Fmoodle.bau.edu.lb%2Flogin%2Findex.php",username,password)


    
async def get_schedule(username:str,password:str):
    #TODO : also make a more efficient login mechanism because this takes way too long
    course_session, course_html = await login_moodle(username,password) 
    courses = await get_courses(course_html,course_session)
    mappings = {item["shortname"].split("-")[0]: " ".join(word for word in re.sub(r'[^a-zA-Z\s]'," ",  html.unescape(
        item["fullname"])).strip().split() if not word.isspace())  for item in courses[0]["data"]["courses"]}
    await course_session.aclose() # we don't need this session anymore, make a new one
    _, response = await login("http://ban-prod-ssb2.bau.edu.lb:8010/ssomanager/c/SSB?pkg=bwskfshd.P_CrseSchd",username,password)
    # we want to parse the html from it
    # uncomment this for testing
    # response = open("./scratch.html").read()
    json_response = []
    soop = soup_bowl(response)
    course_items = list(i.text for i in soop.select(".ddlabel > a"))
    for course in course_items:
        subject_key = course.split("-")[0]
        crn = course.split("-")[1][:course.split("-")[1].find(" ")]
        crn = re.sub(r"\s","",crn)
        subject_key = re.sub(r"\s","",subject_key)
        subject = mappings[subject_key]
        time = re.search(r"\d+:\d+\s[ampm]+-\d+:\d+\s[ampm]+",course)
        location = re.search(r"[^ampm]+ E\w+\d+",course)  
        if not time or not location:
            raise Exception("Couldn't find time or location for course!")
        
        json_response.append({
        "subject" : subject,
        "time" : time.group(),
        "location" : location.group(),
        })
    return {"Courses" : json_response}, _
    # return course_items,_


async def get_notifications(moodle_html : str, Session : httpx.AsyncClient):
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
    return required_json

