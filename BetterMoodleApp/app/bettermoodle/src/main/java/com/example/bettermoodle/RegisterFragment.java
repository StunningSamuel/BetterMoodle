package com.example.bettermoodle;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class RegisterFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_register, container, false);
        TextView dateText = view.findViewById(R.id.datevar);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MMMM dd, yyyy HH:mm a");
        ZonedDateTime dateTime = ZonedDateTime.now();
        String text = dateTime.format(dtf);
        dateText.setText(text);
        return view;
    }


}