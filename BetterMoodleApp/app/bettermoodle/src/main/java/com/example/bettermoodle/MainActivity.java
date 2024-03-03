package com.example.bettermoodle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.util.Base64;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.Request;

public class MainActivity extends AppCompatActivity {
    OkHttpClient client;
    String getURL = "http://127.0.0.1:5000/schedule";

    EditText userid;
    EditText password;
    Button loginbutton;
    TextView loginoutput;
    String USERNAME, PASSWORD;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        userid = findViewById(R.id.userid);
        password = findViewById(R.id.password);
        loginbutton = findViewById(R.id.loginbutton);
        loginoutput = findViewById(R.id.loginoutput);
        USERNAME = userid.toString();
        PASSWORD = password.toString();
        client = new OkHttpClient();

        loginbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                get();
            }
        });
    }
        public void get(){
            Request req = new Request.Builder().url(getURL).build();
            client.newCall(req).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    e.printStackTrace();
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //                                loginoutput.setText(response.body().string());
                            Log.d("ommak", "hello");
                        }
                    });
                }
            });
        }

    }
