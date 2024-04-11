package com.example.bettermoodle;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    OkHttpClient client;
    EditText userid;
    EditText password;
    EditText ipaddress;
    Button loginbutton;

    ProgressBar progressBar;

    public void logStackTrace(String Tag, @NonNull Exception e) {
        Log.e(Tag, String.valueOf(e.getCause()));
    }

    public void showToast(final String toast) {
        runOnUiThread(() -> Toast.makeText(this, toast, Toast.LENGTH_SHORT).show());
    }

    public EncryptedSharedPreferences getPrefs() {
        String masterKeyAlias = null;
        try {
            masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
        } catch (Exception e) {
            logStackTrace("Security Tag", e);
        }

        // Initialize/open an instance of
        // EncryptedSharedPreferences on below line.
        // on below line initializing our encrypted
        // shared preferences and passing our key to it.
        assert masterKeyAlias != null;
        EncryptedSharedPreferences sharedPreferences = null;
        try {
            sharedPreferences = (EncryptedSharedPreferences) EncryptedSharedPreferences.create(
                    // passing a file name to share a preferences
                    "preferences",
                    masterKeyAlias,
                    getApplicationContext(),
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            logStackTrace("Security Tag", e);
        }
        return sharedPreferences;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        progressBar = findViewById(R.id.progress_bar);
        userid = findViewById(R.id.userid);
        password = findViewById(R.id.password);
        ipaddress = findViewById(R.id.userip);
        loginbutton = findViewById(R.id.loginbutton);
        EncryptedSharedPreferences prefs = getPrefs();
        String[] keys = {"username", "password", "ip"};
        EditText[] texts = {userid, password, ipaddress};
        boolean hasCredentials = true;
        for (String key : keys) {
            boolean contains = prefs.contains(key);
            if (!contains) {
                hasCredentials = false;
            }
        }
        if (hasCredentials) {
            // we already have credentials, replace text with the values
            for (int i = 0; i < 3; i++) {
                String value = prefs.getString(keys[i], "");
                texts[i].setText(value);
            }
        }

        boolean finalHasCredentials = hasCredentials;
        loginbutton.setOnClickListener(view -> {
            // store information for future logins
            if (!finalHasCredentials) {
                for (int i = 0; i < 3; i++) {
                    prefs.edit().putString(keys[i], texts[i].getText().toString()).apply();
                }
            }
            Intent intent = new Intent(MainActivity.this, OptionPage2.class);
            String user = userid.getText().toString(), pass = password.getText().toString();
            BasicAuthInterceptor interceptor = new BasicAuthInterceptor(user, pass);
            // net in here slow as balls so we gotta up the timeout seconds
            client = new OkHttpClient.Builder()
                    .addInterceptor(interceptor)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build()
            ;
            Request request = new Request.Builder()
                    .url(String.format("http://%s:5000/", ipaddress.getText().toString()))
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
                    } else {
                        this.showToast("Invalid Credentials!");
                    }
                });
                return response;
            });

            new Thread(() -> {
                try {
                    responseFuture.get();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } catch (ExecutionException e) {
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "Failed to connect to moodle API!", Toast.LENGTH_SHORT).show();
                        logStackTrace("HTTP Tag", e);
                        progressBar.setVisibility(View.GONE);
                    });
                }
            }).start();
        });


    }


}
