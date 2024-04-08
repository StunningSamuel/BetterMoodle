import asyncio
from datetime import datetime
import itertools
import re
from typing import Any
from flask import request
import httpx
from endpoint import soup_bowl, css_selector


def batched(iterable, n: int):
    # batched('ABCDEFG', 3) → ABC DEF G
    if n < 1:
        raise ValueError("n must be at least one")
    it = iter(iterable)
    while batch := tuple(itertools.islice(it, n)):
        yield batch


def get_tag_text(tag):
    return tag.get_text(strip=True)


def index_or(iterable, index, default):
    try:
        return iterable[index]

    except IndexError:
        return default


headers = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/99.0.7113.93 Safari/537.36",
    "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8",
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


def registration_main_menu(Session: httpx.Client, username: str, password: str):
    url = "https://icas.bau.edu.lb:8443/cas/login"
    querystring = {
        "service": "http://ban-prod-ssb2.bau.edu.lb:8010/ssomanager/c/SSB?pkg=twbkwbis.P_GenMenu?name=bmenu.P_RegMnu"
    }

    login_page = Session.get(url, headers=headers, params=querystring)

    execution = css_selector(login_page.text, "[name=execution]", "value")

    Session.request(
        "POST",
        url,
        data={
            "username": username,
            "password": password,
            "execution": execution,
            "_eventId": "submit",
            "geolocation": "",
        },
        headers=headers,
        params=querystring,
    )

    terms = Session.get(
        "http://ban-prod-ssb2.bau.edu.lb:7750/PROD/bwskflib.P_SelDefTerm",
        headers=headers,
    )
    most_recent_term = css_selector(terms.text, "#term_id > option", "value")
    # Tell the server we have chosen this term
    Session.post(
        "http://ban-prod-ssb2.bau.edu.lb:7750/PROD/bwcklibs.P_StoreTerm",
        headers=headers,
        data={"name_var": "bmenu.P_RegMnu", "term_in": most_recent_term},
    )

    return Session


def schedule(Session: httpx.Client, username: str, password: str):

    Session = registration_main_menu(Session, username, password)

    schedule_page = Session.get(
        "http://ban-prod-ssb2.bau.edu.lb:7750/PROD/bwskfshd.P_CrseSchdDetl",
        headers=headers,
    )
    course_tables = soup_bowl(schedule_page.text).select(".datadisplaytable")
    json_schedule = {
        "M": [],
        "T": [],
        "W": [],
        "R": [],
    }

    def get_instructors(elem: Any | str) -> str:

        if type(elem) != str:
            href: str = elem.attrs["href"]  # type: ignore
            return href
        else:
            return elem  # type: ignore

    # for every course, we have two tables
    for course_pair in batched(course_tables, 2):
        first_table, second_table = (
            course_pair[0],
            course_pair[1],
        )

        first_rows, second_rows = (
            first_table.select("td"),
            second_table.select("tr")[1:],
        )
        course_name = first_table.select_one("caption")
        assert course_name
        course_names = get_tag_text(course_name).split("-")
        course_name, course_code = course_names[0].strip(), course_names[1].strip()
        crn = first_rows[1].text
        batched_instructors = list(
            batched(filter(lambda x: x != "\n", first_rows[3].contents), 2)
        )
        instructors = ",".join(
            [
                "{} ({})".format(
                    pair[0].strip(",\n").strip(),
                    get_instructors(index_or(pair, 1, "")),
                )
                for pair in batched_instructors
            ]
        )
        campus = get_tag_text(first_rows[-1])

        for tr in second_rows:
            columns = tr.select("td")
            course_time = get_tag_text(columns[1])
            days = get_tag_text(columns[2])
            location = get_tag_text(columns[3])
            course_type = get_tag_text(columns[5])
            # WOAHH TRIPLE FOR LOOP
            for day in days:
                json_schedule[day].append(
                    {
                        "name": course_name,
                        "code": course_code,
                        "crn": crn,
                        "instructors": instructors,
                        "campus": campus,
                        "time": course_time,
                        "type": course_type,
                        "location": location,
                    }
                )

    # finally, sort each day by starting time
    def sort_by_day(obj: dict):
        date = datetime.strptime(obj["time"].split("-")[0], "%I:%M %p ")
        return date

    for i in json_schedule:
        json_schedule[i].sort(key=sort_by_day)

    return json_schedule


def register_courses(
    Session: httpx.Client, crn_numbers: str, username: str, _password: str
):
    """
    @param crn_numbers : we will get the crn numbers in a comma delimited string like so : 1043414,4121414,452535 and so forth to make it easier to pass them from a POST request.
    """
    # login using normal http requests
    Session = registration_main_menu(Session, username, _password)
    # the terms will be in a select element with the ID of each term
    # Then go back to registration page
    fake_response = Session.get(
        "http://ban-prod-ssb2.bau.edu.lb:7750/PROD/bwskfreg.P_AltPin"
    )
    response_bowl = soup_bowl(fake_response)
    crn_ids = response_bowl.find_all(re.compile(r"crn_id\d+"))
    Session.close()
    # we don't have a time ticket, return the HTML of the registration page
    if not crn_ids:
        # Then close the session once we ge the response
        # For demonstration only
        return fake_response.text
    else:
        # actually register the user otherwise
        return "still not implemented"
