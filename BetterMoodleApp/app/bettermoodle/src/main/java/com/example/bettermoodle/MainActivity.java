package com.example.bettermoodle;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.content.Intent;

public class MainActivity extends AppCompatActivity {

    EditText userid; EditText password; Button loginbutton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        userid = findViewById(R.id.userid);
        password = findViewById(R.id.password);
        loginbutton = findViewById(R.id.loginbutton); }

    public void loginbutton(View view)
    {
        Intent intent = new Intent(MainActivity.this, OptionPage2.class);
        startActivity(intent);
    }


        //this function goes to next page and outputs username and password to Logcat(for testing purposes)
        //loginbutton.setOnClickListener(new View.OnClickListener() {
            //@Override
            //public void onClick(View view) {
                //Intent intent = new Intent(MainActivity.this, OptionPage2.class);
                //startActivity(intent);

       // });


    }