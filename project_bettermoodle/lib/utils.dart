import 'dart:convert';
import 'dart:io';
import 'package:dotenv/dotenv.dart';
import 'package:html/parser.dart';

// ignore: constant_identifier_names
enum HTTPMethod { GET, POST }

enum Sites { moodle, iconnect }

const dartHeaders = {
  'Accept-Encoding': 'gzip',
  'Content-Type': 'application/json',
  'User-Agent': 'Dart/3.2 (dart:io)',
};

class HTTPSession {
  // only one client per session
  static HttpClient client = HttpClient();
  final List<Cookie> cookies = [];

  Future<String> apiRequest(HTTPMethod method, Uri url,
      {Map<String, String>? jsonBody, Map<String, String>? headers}) async {
    // 1. Headers will work with both GET and POST requests.
    // 2. Headers will fall back to dart headers if not found.
    // 3. Body is required for POST but not for get. (If we give a GET request a body it'll just ignore it because that doesn't make sense)
    HttpClientRequest request;

    if (method == HTTPMethod.GET) {
      request = await client.getUrl(url);
    } else {
      request = await client.postUrl(url);
    }
    request.maxRedirects = 10;
    // set headers first
    if (headers != null) {
      headers.forEach((key, value) {
        request.headers.set(key, value);
      });
    }
    // request with body
    if (method != HTTPMethod.GET && jsonBody != null) {
      // GET requests usually don't need to send a JSON body
      request.add(utf8.encode(json.encode(jsonBody)));
    }
    HttpClientResponse response = await request.close();
    if (response.statusCode != 200) {
      throw Exception("Failed to get response!");
    }

    String reply = await response.transform(utf8.decoder).join();
    var requestCookies = response.cookies;
    // make sure to store cookies after each request
    if (requestCookies.isNotEmpty) {
      cookies.addAll(requestCookies);
    }
    return reply;
  }

  Future<String> getRequest(Uri url,
      {Map<String, String>? headers = dartHeaders}) async {
    var reply = await apiRequest(HTTPMethod.GET, url, headers: headers);
    return reply;
  }

  Future<String> postRequest(Uri url, Map<String, String> jsonBody,
      {Map<String, String>? headers = dartHeaders}) async {
    var reply = await apiRequest(HTTPMethod.POST, url,
        jsonBody: jsonBody, headers: headers);
    return reply;
  }

  void close() {
    client.close();
  }
}

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
Future<String> getLoginPageExecution(HTTPSession client, Uri url) async {
  String execution;
  var html = await client.getRequest(url);
  var document = parse(html);
  var attributes = document.querySelector("[name='execution']")!.attributes;
  execution = attributes["value"]!;
  return execution;
}

Future<String> login(HTTPSession client, Sites site) async {
  String serviceParam;
  if (site == Sites.moodle) {
    serviceParam = "https%3A%2F%2Fmoodle.bau.edu.lb%2Flogin%2Findex.php";
  } else {
    serviceParam = "https://mis.bau.edu.lb/web/v12/iconnectv12/cas/sso.aspx";
  }

  try {
    var url = Uri.parse(
        "https://icas.bau.edu.lb:8443/cas/login?service=$serviceParam");
    var execution = await getLoginPageExecution(client, url);

    var pageHeaders = {
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
      "Referer": "https://icas.bau.edu.lb:8443/cas/login?service=$serviceParam",
      "Upgrade-Insecure-Requests": "1",
      "Sec-Fetch-Dest": "document",
      "Sec-Fetch-Mode": "navigate",
      "Sec-Fetch-Site": "same-origin",
      "Sec-Fetch-User": "?1",
      "Sec-GPC": "1"
    };

    assert(execution.isNotEmpty);

    var (username, password) = getCredentials();
    var reply = await client.postRequest(
        url,
        {
          "username": username,
          "password": password,
          "execution": execution,
          "_eventId": "submit",
          "geolocation": ""
        },
        headers: pageHeaders);

    return reply;
  } finally {
    client.close();
  }
}

Future<String> loginMoodle(HTTPSession client) async {
  return await login(client, Sites.moodle);
}

Future<String> loginIconnect(HTTPSession client) async {
  return await login(client, Sites.iconnect);
}
