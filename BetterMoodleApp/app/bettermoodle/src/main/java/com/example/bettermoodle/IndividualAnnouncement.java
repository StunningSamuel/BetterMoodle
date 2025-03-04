package com.example.bettermoodle;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;


public class IndividualAnnouncement extends AppCompatActivity {

    private String message;
    TextView notification_body;
    TextView notification_header;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        setContentView(R.layout.individual_announcment);
//        notification_header = findViewById(R.id.announcement_header);
        notification_body = findViewById(R.id.announcement_body);
//        String header = intent.getStringExtra("header");
        String message = intent.getStringExtra("message");
//        notification_header.setText(header);
        notification_body.setText(message);
    }


}
