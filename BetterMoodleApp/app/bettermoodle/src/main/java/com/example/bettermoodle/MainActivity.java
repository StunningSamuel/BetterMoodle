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

import okhttp3.OkHttpClient;
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
            String url = String.format("http://%s:5000/", ipaddress.getText().toString());
            JsonInterface jsonInterface = new JsonInterface(user, pass, this);
            // set the bar to visible before request
            progressBar.setVisibility(View.VISIBLE);
            CompletableFuture<Response> responseFuture = jsonInterface.connectToApi(url);
            new Thread(() -> {
                try {
                    Response response = responseFuture.get();
                    runOnUiThread(() -> {
                        if (response.isSuccessful()) {
                            startActivity(intent);
                        } else {
                            this.showToast("Invalid Credentials!");
                        }
                    });

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
