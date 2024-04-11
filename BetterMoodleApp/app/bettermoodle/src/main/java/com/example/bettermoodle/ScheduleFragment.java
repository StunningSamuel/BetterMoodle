package com.example.bettermoodle;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.alamkanak.weekview.WeekView;


public class ScheduleFragment extends Fragment {


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        ScheduleModel scheduleModel = new ScheduleModel();
        View view = inflater.inflate(R.layout.fragment_schedule, container, false);
        WeekView weekView = view.findViewById(R.id.weekView);
        ScheduleAdapter adapter = new ScheduleAdapter();
        weekView.setAdapter(adapter);
        scheduleModel.getEventsView().observe(getViewLifecycleOwner(), adapter::submitList);
        return weekView;
    }
}