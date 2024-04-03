package com.example.bettermoodle;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SearchView;

import androidx.fragment.app.Fragment;

public class NotificationsFragment extends Fragment {

    ListView notifications;
    ArrayAdapter<String> arrayAdapter;
    String[] notificationslist = {"Notification 1", "Notification 2", "Notification 3", "Notification 4"};

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        notifications = findViewById(R.id.announcementplaceholder);
        arrayAdapter = new ArrayAdapter<String>(this, R.layout.notfi_list, notificationslist);
        notifications.setAdapter(arrayAdapter);
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_notifications, container, false);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.notifcations_menu);
        MenuItem menuItem = menu.findItem(R.id.Search);
        SearchView searchView = (SearchView) menuItem.getActionView();
        searchView.setQueryHint("Search for announcement:");

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

        return super.onCreateOptionsMenu(menu);
    }
}