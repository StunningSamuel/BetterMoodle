package com.example.bettermoodle;

import static com.example.bettermoodle.UtilsKt.createLiveData;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SearchView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;


public class NotificationsFragment extends Fragment implements MenuProvider {

    ListView listView;
    SearchView searchView;
    ArrayAdapter<String> arrayAdapter;
    JsonInterface jsonInterface;
    String[] notificationslist = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};

    public class Data {

        @SerializedName("notifications")
        @Expose
        public List<Notification> notifications;

    }


    public class ResponseClass {

        @SerializedName("error")
        @Expose
        public Boolean error;
        @SerializedName("data")
        @Expose
        public Data data;

    }

    public class Notification {

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
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.jsonInterface = new JsonInterface(context);
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
        MutableLiveData<String> response = createLiveData();
        jsonInterface.postResponseToListener("moodle/notifications", response);
        response.observe(getViewLifecycleOwner(), s -> {
            Gson gson = new Gson();
            ResponseClass responseClass = gson.fromJson(s, ResponseClass.class);
            Notification[] notifications = responseClass.data.notifications.toArray(new Notification[0]);
            ArrayList<String> notificationNames = new ArrayList<>();
            for (Notification i : notifications) {
                notificationNames.add(i.subject);
            }
            arrayAdapter = new ArrayAdapter<>(requireActivity(), R.layout.list_custometext, notificationNames.toArray(new String[0]));
            listView.setAdapter(arrayAdapter);
        });
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


    @Override
    public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
        return false;
    }

}