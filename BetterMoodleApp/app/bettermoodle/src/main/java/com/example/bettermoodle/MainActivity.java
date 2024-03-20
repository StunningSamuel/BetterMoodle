package com.example.bettermoodle;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    OkHttpClient client;
    EditText userid;
    EditText password;
    Button loginbutton;

    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        progressBar = findViewById(R.id.progress_bar);
        userid = findViewById(R.id.userid);
        password = findViewById(R.id.password);
        loginbutton = findViewById(R.id.loginbutton);
        loginbutton.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, OptionPage2.class);
            String user = userid.getText().toString(), pass = password.getText().toString();
            BasicAuthInterceptor interceptor = new BasicAuthInterceptor(user, pass);
            client = new OkHttpClient.Builder()
                    .addInterceptor(interceptor)
                    .build();
            Request request = new Request.Builder()
                    .url("http://192.168.1.14:5000/moodle/notifications")
                    .build();


            // set the bar to visible before request
            progressBar.setVisibility(View.VISIBLE);
            CompletableFuture<Response> responseFuture = CompletableFuture.supplyAsync(() -> {
                try (Response response = client.newCall(request).execute()) {
                    Log.d("HTTP Tag", "We got this response: " + (response.body() != null ? response.body().string() : null));
                    Log.d("HTTP Tag", "We got this code" + response.code());
                    return response;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).thenApplyAsync(response -> {
                    // once the request is done, remove loading spinner
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        if (response.isSuccessful()) {
                            startActivity(intent);
                        }
                        else {
                            Toast.makeText(this,"Invalid Credentials!", Toast.LENGTH_SHORT).show();
                        }
                    });
                return response;
            });

            new Thread(() -> {
                try {
                    responseFuture.get();
                } catch (ExecutionException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }).start();
        });





    }


}
