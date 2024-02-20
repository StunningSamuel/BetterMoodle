import 'dart:convert';
import 'dart:io'
    show
        Cookie,
        File,
        HttpClient,
        HttpClientRequest,
        HttpClientResponse,
        HttpHeaders;
import 'dart:math' show Random;
import 'package:dotenv/dotenv.dart';
import 'package:html/parser.dart';
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

class HTTPSession {
  // only one client per session
  static HttpClient client = HttpClient();
  List<Cookie> cookies = [];

  Future<HttpClientResponse> apiRequest(HTTPMethod method, String url,
      {Map<String, String>? jsonBody, Map<String, String>? headers}) async {
    // 1. Headers will work with both GET and POST requests.
    // 2. Headers will fall back to dart headers if not found.
    // 3. Body is required for POST but not for get. (If we give a GET request a body it'll just ignore it because that doesn't make sense)
    HttpClientRequest request;

    if (method == HTTPMethod.GET) {
      request = await client.getUrl(Uri.parse(url));
    } else {
      request = await client.postUrl(Uri.parse(url));
    }
    request.followRedirects = true;
    request.maxRedirects = 20;
    request.persistentConnection = true;
    // set headers first
    if (headers != null) {
      headers.forEach((key, value) {
        request.headers.set(key, value);
      });
    }
    var cookieHeaderValue = generateCookieHeader(cookies);
    if (cookieHeaderValue.isNotEmpty) {
      request.headers.set(HttpHeaders.cookieHeader, cookieHeaderValue);
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

    var requestCookies = response.cookies;
    // make sure to store cookies after each request
    if (requestCookies.isNotEmpty) {
      cookies += requestCookies;
    }
    return response;
  }

  static Future<String> returnResponse(HttpClientResponse response) async {
    return await response.transform(utf8.decoder).join();
  }

  Future<HttpClientResponse> get(String url,
      {Map<String, String> headers = dartHeaders}) async {
    var reply = await apiRequest(HTTPMethod.GET, url, headers: headers);
    return reply;
  }

  Future<HttpClientResponse> post(String url,
      {Map<String, String>? body,
      Map<String, String> headers = dartHeaders}) async {
    HttpClientResponse reply;
    if (body != null) {
      reply = await apiRequest(HTTPMethod.POST, url,
          jsonBody: body, headers: headers);
    } else {
      reply = await apiRequest(HTTPMethod.POST, url, headers: headers);
    }
    return reply;
  }

  static String generateCookieHeader(List<Cookie> cookieJar) {
    var cookies =
        List<Cookie>.of(cookieJar.where((element) => element.value.isNotEmpty));

    return cookies.map((e) => "${e.name}=${e.value}").join(";");
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
  var html =
      await HTTPSession.returnResponse(await client.get(url, headers: headers));
  var document = parse(html);
  var attributes = document.querySelector("[name='execution']")!.attributes;
  execution = attributes["value"]!;
  return execution;
}

Future<String> login(String site) async {
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
    currentUrl = loginResponse.headers.value("location")!;
    pageHeaders[HttpHeaders.cookieHeader] =
        HTTPSession.generateCookieHeader(cookies);
    pageHeaders.update(
      "Host",
      (value) => Uri.parse(currentUrl).host,
      ifAbsent: () => Uri.parse(currentUrl).host,
    );
    loginResponse = await client.get(currentUrl, headers: pageHeaders);
  }

  return await HTTPSession.returnResponse(loginResponse);
}

Future<String> loginMoodle() async {
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
