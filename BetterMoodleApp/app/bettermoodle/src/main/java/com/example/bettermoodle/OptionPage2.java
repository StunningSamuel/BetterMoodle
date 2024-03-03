package com.example.bettermoodle;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class OptionPage2 extends AppCompatActivity {
    Button btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.optionpage);
        btn = findViewById(R.id.backbtn);
    }
    public void Button(View view){
        setContentView(R.layout.activity_main);
    }

}