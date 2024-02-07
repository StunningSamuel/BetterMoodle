import 'dart:convert';
import 'dart:io';

var client = HttpClient();
Future<void> main(List<String> args) async {
  try {
    var url = Uri.parse(
        'https://icas.bau.edu.lb:8443/cas/login?service=https%3A%2F%2Fmoodle.bau.edu.lb%2Flogin%2Findex.php');
    HttpClientRequest request = await client.getUrl(url);
    // Optionally set up headers...
    // Optionally write to the request object...
    HttpClientResponse response = await request.close();
    // Process the response
    final stringData = await response.transform(utf8.decoder).join();
  } finally {
    client.close();
  }
}
