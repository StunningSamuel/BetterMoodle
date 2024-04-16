package com.example.bettermoodle;

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


public class NotificationsFragment extends Fragment implements MenuProvider {

    ListView listView;
    SearchView searchView;
    ArrayAdapter<String> arrayAdapter;
    String[] notificationslist = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};

     @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_notifications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        listView = requireView().findViewById(R.id.listView);
        searchView = requireView().findViewById(R.id.SearchView);
        arrayAdapter = new ArrayAdapter<>(requireActivity(), R.layout.list_custometext, notificationslist);
        listView.setAdapter(arrayAdapter);
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