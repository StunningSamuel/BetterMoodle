package com.example.bettermoodle;

import static com.example.bettermoodle.UtilsKt.getPrefs;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.security.crypto.EncryptedSharedPreferences;

import com.alamkanak.weekview.WeekView;


public class ScheduleFragment extends Fragment {

    private Context context;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        EncryptedSharedPreferences preferences = getPrefs(this.context);
        assert preferences != null;
        ScheduleModel scheduleModel = new ScheduleModel(preferences);
        scheduleModel.populateEvents();
        View view = inflater.inflate(R.layout.fragment_schedule, container, false);
        WeekView weekView = view.findViewById(R.id.weekView);
        ScheduleAdapter adapter = new ScheduleAdapter();
        weekView.setAdapter(adapter);
        scheduleModel.getEventsView().observe(getViewLifecycleOwner(), adapter::submitList);
        return view;
    }
}