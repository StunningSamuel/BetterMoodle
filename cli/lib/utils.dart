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

Future<(String, List<Cookie>)> apiRequest(
    HttpClient httpClient, HTTPMethod method, Uri url,
    {Map<String, String>? jsonBody, Map<String, String>? headers}) async {
  // 1. Headers will work with both GET and POST requests.
  // 2. Headers will fall back to dart headers if not found.
  // 3. Body is required for POST but not for get. (If we give a GET request a body it'll just ignore it because that doesn't make sense)
  HttpClientRequest request;

  if (method == HTTPMethod.GET) {
    request = await httpClient.getUrl(url);
  } else {
    request = await httpClient.postUrl(url);
  }
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
  return (reply, request.cookies);
}

Future<(String, List<Cookie>)> getRequest(HttpClient httpClient, Uri url,
    {Map<String, String>? headers = dartHeaders}) async {
  return await apiRequest(httpClient, HTTPMethod.GET, url, headers: headers);
}

Future<(String, List<Cookie>)> postRequest(
    HttpClient httpClient, Uri url, Map<String, String> jsonBody,
    {Map<String, String>? headers = dartHeaders}) async {
  return await apiRequest(httpClient, HTTPMethod.POST, url,
      jsonBody: jsonBody, headers: headers);
}

// We will NOT close the connection until we execute all functions
Future<String> getLoginPageExecution(HttpClient client, Uri url) async {
  String execution;
  var (html, _) = await getRequest(client, url);
  var document = parse(html);
  var attributes = document.querySelector("[name='execution']")!.attributes;
  execution = attributes["value"]!;
  return execution;
}

Future<String> login(HttpClient client, Sites site) async {
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

    var (username, password) = getCredentials();
    var (reply, _) = await postRequest(client, url, {
      "username": username,
      "password": password,
      "execution": execution,
      "_eventId": "submit",
      "geolocation": ""
    });

    return reply;
  } finally {
    client.close();
  }
}

Future<String> loginMoodle(HttpClient client) async {
  return await login(client, Sites.moodle);
}

Future<String> loginIconnect(HttpClient client) async {
  return await login(client, Sites.iconnect);
}
