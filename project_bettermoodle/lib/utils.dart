import 'dart:convert';
import 'dart:io';
import 'dart:math';

import 'package:dotenv/dotenv.dart';
import 'package:html/parser.dart';
import 'package:http_session/http_session.dart';

const dartVersion = 3.3;

const dartHeaders = {
  'Accept-Encoding': 'gzip',
  'Content-Type': 'application/json',
  'User-Agent': 'Dart/$dartVersion (dart:io)',
};

(String, String) getCredentials() {
  var env = DotEnv(includePlatformEnvironment: true);
  env.load();
  // ignore: invalid_use_of_visible_for_testing_member
  var envVars = env.map;
  var username = envVars["USERNAME"];
  var password = envVars["PASSWORD"];
  if (password == null || username == null) {
    throw Exception("Couldn't load credentials!");
  } else {
    return (
      username,
      password
    ); // Intellisense is dumb as hell so I had to put it inside the else
  }
}

// We will NOT close the connection until we execute all functions
Future<String> getLoginPageExecution(HttpSession client, Uri url) async {
  String execution;
  var html = (await client.get(url)).body;
  var document = parse(html);
  var attributes = document.querySelector("[name='execution']")!.attributes;
  execution = attributes["value"]!;
  return execution;
}

Future<String> login(HttpSession client, String site) async {
  var url = Uri.parse("https://icas.bau.edu.lb:8443/cas/login?service=$site");
  var execution = await getLoginPageExecution(client, url);

  List<dynamic> useragents = jsonDecode(
      File("lib/reference/browsers.json").readAsStringSync())["useragents"];

  String randomUseragent =
      useragents[Random().nextInt(useragents.length)]["useragent"];

  var pageHeaders = {
    "User-Agent": randomUseragent,
    "Accept":
        "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/jxl,image/webp,*/*;q=0.8",
    "Accept-Language": "en-US,en;q=0.5",
    "Accept-Encoding": "gzip, deflate, br",
    "Content-Type": "application/x-www-form-urlencoded",
    "Origin": "https://icas.bau.edu.lb:8443",
    "DNT": "1",
    "Connection": "keep-alive",
    "Referer": "https://icas.bau.edu.lb:8443/cas/login?service=$site",
    "Upgrade-Insecure-Requests": "1",
    "Sec-Fetch-Dest": "document",
    "Sec-Fetch-Mode": "navigate",
    "Sec-Fetch-Site": "same-origin",
    "Sec-Fetch-User": "?1",
    "Sec-GPC": "1"
  };

  assert(execution.isNotEmpty);

  var (username, password) = getCredentials();
  var reply = await client.post(url,
      body: {
        "username": username,
        "password": password,
        "execution": execution,
        "_eventId": "submit",
        "geolocation": ""
      },
      headers: pageHeaders);

  return reply.body;
}

Future<String> loginMoodle(HttpSession client) async {
  return await login(
      client, "https%3A%2F%2Fmoodle.bau.edu.lb%2Flogin%2Findex.php");
}

Future<String> loginIconnect(HttpSession client) async {
  return await login(
      client, "https://mis.bau.edu.lb/web/v12/iconnectv12/cas/sso.aspx");
}

Future<String> loginNotifications(HttpSession client) async {
  return await login(
      client, "https://mis.bau.edu.lb/web/v12/iconnectv12/cas/sso.aspx");
}
