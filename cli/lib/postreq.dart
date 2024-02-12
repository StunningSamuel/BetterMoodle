import 'package:http/http.dart' as http;

void main() async {
  var url = Uri.parse('https://icas.bau.edu.lb:8443/cas/login');
  // make http post request
  var response = await http.post(url);
  // check the status code for the result
  if (response.statusCode == 200) {
    print(response.body);
  } else {
    print('Request failed with status: ${response.statusCode}.');
  }
}
