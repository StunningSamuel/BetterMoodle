package com.example.bettermoodle;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SearchView;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class NotificationsFragment extends Fragment implements MenuProvider {

    ListView listView;
    SearchView searchView;
    ArrayAdapter<String> arrayAdapter;
    JsonInterface jsonInterface;
//    String[] notificationslist = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};

    public static class Data {

        @SerializedName("notifications")
        @Expose
        public List<Notification> notifications;

    }


    public static class ResponseClass {

        @SerializedName("error")
        @Expose
        public Boolean error;
        @SerializedName("data")
        @Expose
        public Data data;

    }

    public static class Notification {

        @SerializedName("id")
        @Expose
        public Integer id;
        @SerializedName("useridfrom")
        @Expose
        public Integer useridfrom;
        @SerializedName("useridto")
        @Expose
        public Integer useridto;
        @SerializedName("subject")
        @Expose
        public String subject;
        @SerializedName("shortenedsubject")
        @Expose
        public String shortenedsubject;
        @SerializedName("text")
        @Expose
        public String text;
        @SerializedName("fullmessage")
        @Expose
        public String fullmessage;
        @SerializedName("fullmessageformat")
        @Expose
        public Integer fullmessageformat;
        @SerializedName("fullmessagehtml")
        @Expose
        public String fullmessagehtml;
        @SerializedName("smallmessage")
        @Expose
        public String smallmessage;
        @SerializedName("contexturl")
        @Expose
        public String contexturl;
        @SerializedName("contexturlname")
        @Expose
        public String contexturlname;
        @SerializedName("timecreated")
        @Expose
        public Integer timecreated;
        @SerializedName("timecreatedpretty")
        @Expose
        public String timecreatedpretty;
        @SerializedName("timeread")
        @Expose
        public Integer timeread;
        @SerializedName("read")
        @Expose
        public Boolean read;
        @SerializedName("deleted")
        @Expose
        public Boolean deleted;
        @SerializedName("iconurl")
        @Expose
        public String iconurl;
        @SerializedName("component")
        @Expose
        public String component;
        @SerializedName("eventtype")
        @Expose
        public String eventtype;
        @SerializedName("customdata")
        @Expose
        public String customdata;

    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.jsonInterface = new JsonInterface(context, "moodle/notifications");
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

//         this.jsonInterface = new JsonInterface();
        return inflater.inflate(R.layout.fragment_notifications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        listView = requireView().findViewById(R.id.listView);
        searchView = requireView().findViewById(R.id.SearchView);
        //placeholder notifications:
        String[] notificationNames = new String[11];
        // dummy notifications for now
        String[] notificationArray = new String[11];
        for (int i = 0; i < 11 ; i++) {
            notificationArray[i] = "Assignment: MQTT and Blynk\n" +
                "by Hafez Al Khatib - Thursday, 27 February 2025, 7:50 PM" + "\n\n" + "Dear COMP525 students,\n" +
                    "\n" +
                    "Since we have completed chapters concerning MQTT and Blynk, you are requested to work on a group-based assignment where you prepare a prototype implementation that involves MQTT and Blynk in your final project, outlining why each learned technology is applicable for your project, how it works, and what are drawbacks and potential advancements towards the final submission. This submission aims at streamlining your project work by helping you begin implementation of a chunk of the final project for a modular approach.\n" +
                    "\n" +
                    "You will submit:\n" +
                    "\n" +
                    "- A prototype implementation that aligns with your project idea.\n" +
                    "\n" +
                    "- A PowerPoint presentation highlighting the details of your implementation, the reason you chose these implementations, future ambitions, and possible limitations.\n" +
                    "\n" +
                    "- Your submitted presentation should include audio recordings on top of the slides. Each group member shall record no more than 3 minutes in the presentation. All group members must participate in this submission.\n" +
                    "\n" +
                    "The deadline for this submission is next Sunday, March 9th, 2025 at 11:59 PM.";
        }
//        notificationNames[0] = "Assignment: MQTT and Blynk\n" +
//                "by Hafez Al Khatib - Thursday, 27 February 2025, 7:50 PM";
//
//        notificationNames[1] = "Project Phase 2 Submissions\n" +
//                "by Hafez Al Khatib - Sunday, 23 February 2025, 10:23 PM";
//
//        notificationNames[2] = "Monday's Lecture\n" +
//                "by Imane Haidar - Friday, 21 February 2025, 2:35 PM";
//
//        notificationArray[0] = "Dear COMP525 students,\n" +
//                "\n" +
//                "Since we have completed chapters concerning MQTT and Blynk, you are requested to work on a group-based assignment where you prepare a prototype implementation that involves MQTT and Blynk in your final project, outlining why each learned technology is applicable for your project, how it works, and what are drawbacks and potential advancements towards the final submission. This submission aims at streamlining your project work by helping you begin implementation of a chunk of the final project for a modular approach.\n" +
//                "\n" +
//                "You will submit:\n" +
//                "\n" +
//                "- A prototype implementation that aligns with your project idea.\n" +
//                "\n" +
//                "- A PowerPoint presentation highlighting the details of your implementation, the reason you chose these implementations, future ambitions, and possible limitations.\n" +
//                "\n" +
//                "- Your submitted presentation should include audio recordings on top of the slides. Each group member shall record no more than 3 minutes in the presentation. All group members must participate in this submission.\n" +
//                "\n" +
//                "The deadline for this submission is next Sunday, March 9th, 2025 at 11:59 PM.";
//
//        notificationArray[1] = "Dear COMP525 students,\n" +
//                "\n" +
//                "This is a reminder to submit your Phase 2 progress as a PDF file for the final project. For easy access, I have added a separate upload link for you on Moodle for that submission. Those who have uploaded to the previous submission link do not need to re-upload.\n" +
//                "\n" +
//                "Best regards";
//
//        notificationArray[2] = "Salam\n" +
//                "\n" +
//                "We have already taken ch 1 and 2, I have uploaded them along with the new ch3.\n" +
//                "\n" +
//                "You are asked to review ch1, 2 and have a general look at ch3.\n" +
//                "\n" +
//                "I will make a 10 min mcq quiz on Monday 24th\n" +
//                "\n" +
//                "\n" +
//                "\n" +
//                "(Prediction1: You are shocked! How ch3 is included? \uD83D\uDE2E\n" +
//                "\n" +
//                "Prediction2:  You will try to contact me to inquiry about the issue. (DO NOT)\n" +
//                "\n" +
//                "Prediction3: You will do great if you just read it, DO NOT PANIC! \uD83D\uDE01)";


            listView.setOnItemClickListener((parent, view1, position, id) -> {
                    Intent intent = new Intent(getActivity(), IndividualAnnouncement.class);
                    String notificationMessage = notificationArray[position];
//                    String notificationHeader = notificationNames[position];
//                    intent.putExtra("header", notificationHeader);
                    intent.putExtra("message", notificationMessage);
                startActivity(intent);
            });
            arrayAdapter = new ArrayAdapter<>(requireActivity(), R.layout.list_custometext, notificationArray);
            listView.setAdapter(arrayAdapter);


//        jsonInterface.postResponseToListener().observe(getViewLifecycleOwner(), s -> {
//            Gson gson = new Gson();
//            ResponseClass responseClass = gson.fromJson(s, ResponseClass.class);
//            Notification[] notifications = responseClass.data.notifications.toArray(new Notification[0]);
//            ArrayList<String> notificationNames = new ArrayList<>();
//            for (Notification i : notifications) {
//                notificationNames.add(i.subject);
//            }
    //            arrayAdapter = new ArrayAdapter<>(requireActivity(), R.layout.list_custometext, notificationNames.toArray(new String[0]));
//            listView.setAdapter(arrayAdapter);
//        });
    }


    @Override
    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.notifcations_menu, menu);
        MenuItem menuItem = menu.findItem(R.id.Search);
        SearchView searchView = (SearchView) menuItem.getActionView();
        assert searchView != null;

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                arrayAdapter.getFilter().filter(newText);
                return false;
            }
        });

    }

//Function that takes you to the individual notification
    @Override
    public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
        return false;
    }

}