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
import androidx.lifecycle.LiveData;
import androidx.security.crypto.EncryptedSharedPreferences;

import com.islandparadise14.mintable.MinTimeTableView;
import com.islandparadise14.mintable.model.ScheduleEntity;

import java.util.ArrayList;
import java.util.List;


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
        View view = inflater.inflate(R.layout.fragment_schedule, container, false);
        MinTimeTableView table = view.findViewById(R.id.weekView);
        table.initTable(List.of("Mon", "Tue", "Wen", "Thu", "Fri").toArray(new String[5]));
        scheduleModel.setSchedule(table);
        LiveData<ArrayList<ScheduleEntity>> data = scheduleModel.getEvents();
        data.observe(getViewLifecycleOwner(), table::updateSchedules);
        return view;
    }
}