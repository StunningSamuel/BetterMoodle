import 'dart:convert';
import 'dart:io' show Cookie, File, HttpHeaders;
import 'dart:math' show Random;
import 'package:dotenv/dotenv.dart';
import 'package:html/parser.dart';
import 'package:http/http.dart';
import 'package:http/retry.dart';
import 'package:sweet_cookie_jar/sweet_cookie_jar.dart';
// import 'package:requests/requests.dart';

// ignore: constant_identifier_names
enum HTTPMethod { GET, POST }

const dartVersion = 3.2;
const maxRedirects = 20;

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

String generateCookieHeader(List<Cookie> cookieJar) {
  var cookies =
      List<Cookie>.of(cookieJar.where((element) => element.value.isNotEmpty));

  return cookies.map((e) => "${e.name}=${e.value}").join(";");
}

Map<String, String> generateRandomUserAgent(Map<String, String> headers) {
  List<dynamic> useragents = jsonDecode(
      File("lib/reference/browsers.json").readAsStringSync())["useragents"];

  String randomUseragent =
      useragents[Random().nextInt(useragents.length)]["useragent"];

  headers[HttpHeaders.userAgentHeader] = randomUseragent;

  return headers;
}

class HTTPSession {
  // only one client per session
  Client client = RetryClient(
    Client(),
    when: (p0) {
      try {
        return p0.statusCode == 503;
        // retry if the net is being shit
      } on ClientException {
        return true;
      }
    },
  );
  List<Cookie> cookies = [];

  Future<Response> apiRequest(HTTPMethod method, String url,
      {Map<String, String>? jsonBody,
      Map<String, String> headers = dartHeaders}) async {
    // 1. Headers will work with both GET and POST requests.
    // 2. Headers will fall back to dart headers if not found.
    // 3. Body is required for POST but not for get. (If we give a GET request a body it'll just ignore it because that doesn't make sense)
    Response response;

    if (method == HTTPMethod.GET) {
      response = await client.get(Uri.parse(url), headers: headers);
    } else {
      response = await client.post(Uri.parse(url), body: jsonBody ?? {});
    }
    // set headers first
    headers.forEach((key, value) {
      response.headers[key] = value;
    });
    var cookieHeaderValue = generateCookieHeader(cookies);
    if (cookieHeaderValue.isNotEmpty) {
      response.headers[HttpHeaders.cookieHeader] = cookieHeaderValue;
    }
    if ((400 <= response.statusCode) && (response.statusCode < 600)) {
      throw Exception("Failed to get response!");
    }

    var requestCookies = SweetCookieJar.from(response: response);
    // make sure to store cookies after each request
    var requestCookiesList = requestCookies.nameSet
        .map((name) => requestCookies.find(name: name))
        .toList();
    if (requestCookies.isNotEmpty) {
      cookies += requestCookiesList;
    }
    return response;
  }

  Future<Response> get(String url,
      {Map<String, String> headers = dartHeaders}) async {
    var reply = await apiRequest(HTTPMethod.GET, url, headers: headers);
    return reply;
  }

  Future<Response> post(String url,
      {Map<String, String>? body,
      Map<String, String> headers = dartHeaders}) async {
    Response reply;
    if (body != null) {
      reply = await apiRequest(HTTPMethod.POST, url,
          jsonBody: body, headers: headers);
    } else {
      reply = await apiRequest(HTTPMethod.POST, url, headers: headers);
    }
    return reply;
  }

  void close() {
    client.close();
  }
}

final HTTPSession client = HTTPSession();
// We will NOT close the connection until we execute all functions
Future<String> getLoginPageExecution(
    String url, Map<String, String> headers) async {
  String execution;
  var html = (await client.get(url, headers: headers)).body;
  var document = parse(html);
  var attributes = document.querySelector("[name='execution']")!.attributes;
  execution = attributes["value"]!;
  return execution;
}

Future<Response> login(String site) async {
  var url = "https://icas.bau.edu.lb:8443/cas/login?service=$site";
  List<dynamic> useragents = jsonDecode(
      File("lib/reference/browsers.json").readAsStringSync())["useragents"];

  String randomUseragent =
      useragents[Random().nextInt(useragents.length)]["useragent"];
  var pageHeaders = {
    'User-Agent': randomUseragent,
    'Accept':
        "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8",
    'Accept-Language': "en-US,en;q=0.5",
    'Accept-Encoding': "gzip, deflate, br",
    'Content-Type': "application/x-www-form-urlencoded",
    'DNT': "1",
    'Connection': "keep-alive",
    'Upgrade-Insecure-Requests': "1",
    'Sec-Fetch-Dest': "document",
    'Sec-Fetch-Mode': "navigate",
    'Sec-Fetch-Site': "same-origin",
    'Sec-Fetch-User': "?1",
    'Sec-GPC': "1",
    'sec-ch-ua-platform': "Windows",
    'sec-ch-ua':
        "\"Google Chrome\";v=\"113\", \"Chromium\";v=\"113\", \"Not=A?Brand\";v=\"24\"",
    'sec-ch-ua-mobile': "?0",
    'Pragma': "no-cache",
    'Cache-Control': "no-cache"
  };
  var execution = await getLoginPageExecution(url, pageHeaders);
  var (username, password) = getCredentials();
  int redirects = 0;
  String currentUrl = url;
  var loginResponse = await client.post(currentUrl,
      body: {
        "username": username,
        "password": password,
        "execution": execution,
        "_eventId": "submit",
        "geolocation": ""
      },
      headers: pageHeaders);

  while (loginResponse.statusCode != 200) {
    if (redirects++ >= maxRedirects) {
      throw Exception("Exceeded a maximum of $maxRedirects redirects!");
    }
    if ((400 <= loginResponse.statusCode) && (loginResponse.statusCode < 600)) {
      throw Exception(
          "Encountered error status code ${loginResponse.statusCode}!");
    }
    var cookies = client.cookies;
    currentUrl = loginResponse.headers["location"]!;
    // pageHeaders[HttpHeaders.cookieHeader] = generateCookieHeader(cookies);
    // pageHeaders.update(
    //   "Host",
    //   (value) => Uri.parse(currentUrl).host,
    //   ifAbsent: () => Uri.parse(currentUrl).host,
    // );
    loginResponse = await client.get(currentUrl, headers: pageHeaders);
  }

  return loginResponse;
}

Future<Response> loginMoodle() async {
  // var moodleHeaders = {
  //   'User-Agent':
  //       "Mozilla/5.0 (Windows NT 10.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/99.0.7113.93 Safari/537.36",
  //   'Accept': "application/json, text/javascript, */*; q=0.01",
  //   'Accept-Language': "en-US,en;q=0.5",
  //   'Accept-Encoding': "gzip, deflate, br",
  //   'Content-Type': "application/json",
  //   'X-Requested-With': "XMLHttpRequest",
  //   'DNT': "1",
  //   'Connection': "keep-alive",
  //   'Sec-Fetch-Dest': "empty",
  //   'Sec-Fetch-Mode': "cors",
  //   'Sec-Fetch-Site': "same-origin",
  //   'Sec-GPC': "1"
  // };
  return await login(
    "https%3A%2F%2Fmoodle.bau.edu.lb%2Flogin%2Findex.php",
  );
}

// Future<String> loginIconnect(HTTPSession client) async {
//   return await login(
//       client, "https://mis.bau.edu.lb/web/v12/iconnectv12/cas/sso.aspx");
// }

// Future<String> loginNotifications(HTTPSession client) async {
//   var document = parse(await login(client,
//       "http%3A%2F%2Fban-prod-ssb2.bau.edu.lb%3A8010%2Fssomanager%2Fc%2FSSB%3Fpkg%3Dbwskfshd.P_CrseSchd"));

//   var elements = document.querySelectorAll(".ddlabel > a");

//   return elements.map((e) => e.text).toString();
// }
