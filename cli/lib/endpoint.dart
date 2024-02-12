import 'dart:io';

import 'package:cli/utils.dart';

var client = HttpClient();

Future<void> main(List<String> args) async {
  try {
    var reply = await loginMoodle(client);
    print(reply);
  } finally {
    client.close();
  }
}
