package com.example.bettermoodle;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

import okhttp3.Authenticator;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;

public class MainActivity extends AppCompatActivity {
    OkHttpClient client;
    EditText userid;
    EditText password;
    Button loginbutton;

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
            client = new OkHttpClient.Builder()
                    .authenticator(new Authenticator() {
                        @Override public Request authenticate(Route route, Response response) throws IOException {
                            if (response.request().header("Authorization") != null) {
                                return null; // Give up, we've already attempted to authenticate.
                            }

                            System.out.println("Authenticating for response: " + response);
                            System.out.println("Challenges: " + response.challenges());
                            String credential = Credentials.basic(user, pass);
                            return response.request().newBuilder()
                                    .header("Authorization", credential)
                                    .build();
                        }
                    })
                    .build();
            new Thread(() -> {
                Request request = new Request.Builder()
                        .url("http://172.33.143.127:5000/moodle/notifications")
                        .build();
                try (Response response = client.newCall(request).execute()) {
                    if(response.isSuccessful())
                    {
                        startActivity(intent);
                    }
                    else
                    {
                        Toast toast = Toast.makeText(this, "Oopsie", Toast.LENGTH_SHORT);
                        toast.show();
                    }
                    Log.d("HTTP Tag", "We got this response: " + response.body().string());
                    Log.d("HTTP Tag", "We got this code" + response.code());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).start();

        });
    }}
