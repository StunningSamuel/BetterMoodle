import 'dart:io';

import 'package:cli/utils.dart';

var client = HttpClient();

Future<void> main(List<String> args) async {
  try {
    var url = Uri.parse("https://httpbin.org/headers");
    var loginHeaders = {
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
          "https://icas.bau.edu.lb:8443/cas/login?service=https%3A%2F%2Fmoodle.bau.edu.lb%2Flogin%2Findex.php",
      "Upgrade-Insecure-Requests": "1",
      "Sec-Fetch-Dest": "document",
      "Sec-Fetch-Mode": "navigate",
      "Sec-Fetch-Site": "same-origin",
      "Sec-Fetch-User": "?1",
      "Sec-GPC": "1"
    };
    var reply = await getRequest(
      client,
      url,
    );
    print(reply);
  } finally {
    client.close();
  }
}
