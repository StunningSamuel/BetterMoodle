package com.example.bettermoodle;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;

import com.islandparadise14.mintable.MinTimeTableView;
import com.islandparadise14.mintable.model.ScheduleEntity;

import java.util.ArrayList;
import java.util.List;


public class ScheduleFragment extends Fragment {

    private Context context;
    private ScheduleModel model;

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
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Button refreshButton = view.findViewById(R.id.refreshButton);
        Button refreshColors = view.findViewById(R.id.refreshColors);
        refreshButton.setOnClickListener(v -> {
            this.model.setSchedule(true, false);
        });
        refreshColors.setOnClickListener(v -> {
            this.model.setSchedule(false, true);
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        ScheduleModel scheduleModel = new ScheduleModel(this.context);
        this.model = scheduleModel;
        View view = inflater.inflate(R.layout.fragment_schedule, container, false);
        MinTimeTableView table = view.findViewById(R.id.weekView);
        table.initTable(List.of("Mon", "Tue", "Wen", "Thu", "Fri").toArray(new String[5]));
        scheduleModel.setSchedule();
        LiveData<ArrayList<ScheduleEntity>> data = scheduleModel.getEvents();
        data.observe(getViewLifecycleOwner(), table::updateSchedules);
        return view;
    }


}