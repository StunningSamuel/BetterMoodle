import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:http_session/http_session.dart';
import 'package:project_bettermoodle/utils.dart';
import 'package:test/test.dart';

void main() {
  test('sanity checks', () async {
    // check that the basic credentials fetcher as well as the HTTP functions (GET, POST) work as expected
    // 1. Headers must work with both GET and POST requests.
    // 2. Headers will fall back to dart headers if not found.
    // 3. Body is required for POST but not for get. (If we give a GET request a body it'll just ignore it because that doesn't make sense)
    var client = HttpSession();
    var getUrl = Uri.parse("https://httpbin.org/headers");
    var postUrl = Uri.parse("https://httpbin.org/post");
    var jsonBody = {"foo": "bar", "pussy": "juice"};
    var (user, pass) = getCredentials();

    // make sure we have no empty env variables
    assert(user.isNotEmpty && pass.isNotEmpty);

    var testHeaders = {
      "User-Agent":
          "Mozilla/5.0 (Windows NT 10.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/99.0.7113.93 Safari/537.36",
      "Accept":
          "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/jxl,image/webp,*/*;q=0.8",
      "Accept-Language": "en-US,en;q=0.5",
      "Accept-Encoding": "gzip, deflate, br",
      "Content-Type": "application/x-www-form-urlencoded",
      "Origin": "https://icas.bau.edu.lb:8443",
      "DNT": "1",
      "Connection": "keep-alive",
      "Referer":
          r"https://icas.bau.edu.lb:8443/cas/login?service=https%3A%2F%2Fmoodle.bau.edu.lb%2Flogin%2Findex.php",
      "Upgrade-Insecure-Requests": "1",
      "Sec-Fetch-Dest": "document",
      "Sec-Fetch-Mode": "navigate",
      "Sec-Fetch-Site": "same-origin",
      "Sec-Fetch-User": "?1",
      "Sec-GPC": "1"
    };

    var getReply = (await client.get(getUrl, headers: testHeaders)).body;
    var postReply =
        (await client.post(postUrl, body: jsonBody, headers: testHeaders)).body;
    var getNoHeadersReply = (await client.get(getUrl)).body;
    var postNoHeadersReply = (await client.post(postUrl, body: jsonBody)).body;
    // for no headers, should have the user agent be dart io
    expect(json.decode(getNoHeadersReply)["headers"]["User-Agent"],
        dartHeaders["User-Agent"]);
    expect((json.decode(postNoHeadersReply) as Map)["headers"]["User-Agent"],
        dartHeaders["User-Agent"]);
    expect(json.decode(postNoHeadersReply)["form"], jsonBody);
    // otherwise spoof with desired user agent
    expect(json.decode(getReply)["headers"]["User-Agent"],
        testHeaders["User-Agent"]);
    expect(json.decode(postReply)["form"], jsonBody);

    client.close();
  });

  test('intermediate functions', () async {
    // var loginPageHTML = File("lib/reference/sample.html").readAsStringSync();
    // var reqHTML = File("lib/reference/request.html");
    var client = HttpSession();
    var req = await loginMoodle(client);
    debugPrint(req);
    // reqHTML.writeAsStringSync(req);
    // assert(req != loginPageHTML);
  });
}
