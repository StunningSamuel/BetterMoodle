package com.example.bettermoodle;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    EditText userid;
    EditText password;
    Button loginbutton;
    OkHttpClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        userid = findViewById(R.id.userid);
        password = findViewById(R.id.password);
        loginbutton = findViewById(R.id.loginbutton);

        loginbutton.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, OptionPage2.class);
            String user = userid.getText().toString(), pass = password.getText().toString();
            client = new OkHttpClient.Builder().build();
            startActivity(intent);
            Request request = new Request.Builder()
                    // let's test the schedule endpoint for now
                    .url("https://httpbin.org/anything")
                    .build();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    // Do network action in this function
                    try (Response response = client.newCall(request).execute()) {
                        Log.d("HTTP Tag", response.isSuccessful() ? "200" : "401");
                        assert response.body() != null;
                        Log.d("HTTP Tag", "We got this response: " + response.body());
                    } catch (IOException e) {
                        throw new RuntimeException("Couldn't make network requests!");
                    }
                }
            }).start();

        });
    }}
