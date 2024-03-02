from os import path
from pathlib import Path
import random
from selenium import webdriver
from selenium.webdriver.support.wait import WebDriverWait
from selenium.webdriver.firefox.service import Service
from selenium.webdriver.common.by import By
from selenium.common.exceptions import NoSuchElementException


service = Service(str(Path("./bot_data_stuff/geckodriver").resolve()),log_output=path.devnull)
options = webdriver.FirefoxOptions()
options.binary_location = "/usr/bin/firefox-trunk"


def click_and_send(elem, text):
    elem.click()
    elem.send_keys(text)




def register_courses(crn_numbers : str, username:str, _password:str):
    """
    @param crn_numbers : we will get the crn numbers in a comma delimited string like so : 1043414,4121414,452535 and so forth to make it easier to pass them from a POST request.
    """
    driver = webdriver.Firefox(options=options,service=service)
    def click_and_select(strategy, selector):
        elem = driver.find_element(strategy, selector)
        elem.click()


    def click_and_select_css(selector):
        click_and_select(By.CSS_SELECTOR, selector)


    def click_and_select_id(selector):
        click_and_select(By.ID, selector)

    def click_and_select_linktext(selector):
        click_and_select(By.LINK_TEXT, selector)

    driver.implicitly_wait(random.randint(2, 6))
    wait_obj = WebDriverWait(driver, 5)
    driver.get("https://iconnect.bau.edu.lb")
    user = driver.find_element(By.ID, "username")
    click_and_send(user,username)
    password = driver.find_element(By.ID, "password")
    click_and_send(password,_password)
    click_and_select(By.NAME, "submit_form")
    # switch to online registration
    driver.get(
        "http://ban-prod-ssb2.bau.edu.lb:8010/ssomanager/c/SSB?pkg=twbkwbis.P_GenMenu?name=bmenu.P_RegMnu")
    click_and_select_linktext("Select Term")
    click_and_select_id("term_id")
    click_and_select_css("option:nth-child(1)")
    click_and_select_css(".pagebodydiv input:nth-child(3)")
    click_and_select_linktext("Add or Drop Classes")
    # add courses to registration boxes
    crn_input_boxes = [driver.find_element(
        By.ID, f"crn_id{num}") for num in range(1, 11)]
    crn_numbers_int = list(map(
        int, crn_numbers.split(",")))

    try:
        for idx, box in enumerate(crn_input_boxes):
            box.send_keys(crn_numbers_int[idx])
        click_and_select_css("input:nth-child(28)")
    except NoSuchElementException:
        return
    finally:
        driver.close()
