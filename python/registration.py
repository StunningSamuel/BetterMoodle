import re
import asyncio
import httpx
import nest_asyncio

nest_asyncio.apply()
from endpoint import soup_bowl, css_selector


def click_and_send(elem, text):
    elem.click()
    elem.send_keys(text)


async def main(username: str, password: str):
    url = "https://icas.bau.edu.lb:8443/cas/login"
    querystring = {
        "service": "http://ban-prod-ssb2.bau.edu.lb:8010/ssomanager/c/SSB?pkg=twbkwbis.P_GenMenu?name=bmenu.P_RegMnu"
    }
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

    Session = httpx.AsyncClient(follow_redirects=True, timeout=None)
    login_page = await Session.get(url, headers=headers, params=querystring)
    execution = css_selector(login_page.text, "[name=execution]", "value")
    response = await Session.request(
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

    return Session, response


async def register_courses(crn_numbers: str, username: str, _password: str):
    """
    @param crn_numbers : we will get the crn numbers in a comma delimited string like so : 1043414,4121414,452535 and so forth to make it easier to pass them from a POST request.
    """
    # login using normal http requests
    Session, _ = await main(username, _password)
    # the terms will be in a select element with the ID of each term
    terms = await Session.get(
        "http://ban-prod-ssb2.bau.edu.lb:7750/PROD/bwskflib.P_SelDefTerm"
    )
    most_recent_term = css_selector(terms.text, "#term_id > option", "value")
    # Tell the server we have chosen this term
    await Session.post(
        "http://ban-prod-ssb2.bau.edu.lb:7750/PROD/bwcklibs.P_StoreTerm",
        data={"name_var": "bmenu.P_RegMnu", "term_in": most_recent_term},
    )
    # Then go back to registration page
    fake_response = await Session.get(
        "http://ban-prod-ssb2.bau.edu.lb:7750/PROD/bwskfreg.P_AltPin"
    )
    response_bowl = soup_bowl(fake_response)
    crn_ids = response_bowl.find_all(re.compile(r"crn_id\d+"))
    asyncio.run(Session.aclose())
    # we don't have a time ticket, return the HTML of the registration page
    if not crn_ids:
        # Then close the session once we ge the response
        # For demonstration only
        return fake_response.text
    else:
        # actually register the user otherwise
        return "still not implemented"
