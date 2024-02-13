import 'dart:convert';
import 'dart:io';
// class Notification {
//   bool? error;
//   Data? data;

//   Notification({this.error, this.data});

//   Notification.fromJson(Map<String, dynamic> json) {
//     error = json['error'];
//     data = json['data'] != null ? new Data.fromJson(json['data']) : null;
//   }

//   Map<String, dynamic> toJson() {
//     final Map<String, dynamic> data = new Map<String, dynamic>();
//     data['error'] = this.error;
//     if (this.data != null) {
//       data['data'] = this.data!.toJson();
//     }
//     return data;
//   }
// }

// class Data {
//   List<Notifications>? notifications;

//   Data({this.notifications});

//   Data.fromJson(Map<String, dynamic> json) {
//     if (json['notifications'] != null) {
//       notifications = <Notifications>[];
//       json['notifications'].forEach((v) {
//         notifications!.add(new Notifications.fromJson(v));
//       });
//     }
//   }

//   Map<String, dynamic> toJson() {
//     final Map<String, dynamic> data = new Map<String, dynamic>();
//     if (this.notifications != null) {
//       data['notifications'] =
//           this.notifications!.map((v) => v.toJson()).toList();
//     }
//     return data;
//   }
// }

class Notifications {
  int? id;
  int? useridfrom;
  int? useridto;
  String? subject;
  String? shortenedsubject;
  String? text;
  String? fullmessage;
  int? fullmessageformat;
  String? fullmessagehtml;
  String? smallmessage;
  String? contexturl;
  String? contexturlname;
  int? timecreated;
  String? timecreatedpretty;
  int? timeread;
  bool? read;
  bool? deleted;
  String? iconurl;
  String? component;
  String? eventtype;
  String? customdata;

  Notifications(
      {this.id,
      this.useridfrom,
      this.useridto,
      this.subject,
      this.shortenedsubject,
      this.text,
      this.fullmessage,
      this.fullmessageformat,
      this.fullmessagehtml,
      this.smallmessage,
      this.contexturl,
      this.contexturlname,
      this.timecreated,
      this.timecreatedpretty,
      this.timeread,
      this.read,
      this.deleted,
      this.iconurl,
      this.component,
      this.eventtype,
      this.customdata});

  Notifications.fromJson(Map<String, dynamic> json) {
    id = json['id'];
    useridfrom = json['useridfrom'];
    useridto = json['useridto'];
    subject = json['subject'];
    shortenedsubject = json['shortenedsubject'];
    text = json['text'];
    fullmessage = json['fullmessage'];
    fullmessageformat = json['fullmessageformat'];
    fullmessagehtml = json['fullmessagehtml'];
    smallmessage = json['smallmessage'];
    contexturl = json['contexturl'];
    contexturlname = json['contexturlname'];
    timecreated = json['timecreated'];
    timecreatedpretty = json['timecreatedpretty'];
    timeread = json['timeread'];
    read = json['read'];
    deleted = json['deleted'];
    iconurl = json['iconurl'];
    component = json['component'];
    eventtype = json['eventtype'];
    customdata = json['customdata'];
  }

  Map<String, dynamic> toJson() {
    final Map<String, dynamic> data = new Map<String, dynamic>();
    data['id'] = this.id;
    data['useridfrom'] = this.useridfrom;
    data['useridto'] = this.useridto;
    data['subject'] = this.subject;
    data['shortenedsubject'] = this.shortenedsubject;
    data['text'] = this.text;
    data['fullmessage'] = this.fullmessage;
    data['fullmessageformat'] = this.fullmessageformat;
    data['fullmessagehtml'] = this.fullmessagehtml;
    data['smallmessage'] = this.smallmessage;
    data['contexturl'] = this.contexturl;
    data['contexturlname'] = this.contexturlname;
    data['timecreated'] = this.timecreated;
    data['timecreatedpretty'] = this.timecreatedpretty;
    data['timeread'] = this.timeread;
    data['read'] = this.read;
    data['deleted'] = this.deleted;
    data['iconurl'] = this.iconurl;
    data['component'] = this.component;
    data['eventtype'] = this.eventtype;
    data['customdata'] = this.customdata;
    return data;
  }
}

void main(List<String> args) {
  var jsonData =
      '{ "subject" : "COMP364-20485.202420: Sheet 4", "fullmessage" : "Dear students,\n\nPlease complete Sheet 4, which is attached here, by Thursday."  }';
  var parsedJson = json.decode(jsonData);
  var notification = Notifications.fromJson(parsedJson);
  print('${notification.subject} is ${notification.fullmessage}');
}
