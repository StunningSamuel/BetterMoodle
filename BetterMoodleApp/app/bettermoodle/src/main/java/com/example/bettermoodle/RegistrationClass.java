package com.example.bettermoodle;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class RegistrationClass extends AppCompatActivity {

    TextView Total_Hours, Billing_Hours, MinHours, MaxHours, DateVar;
    EditText crn1, crn2, crn3, crn4, crn5, crn6, crn7, crn8, crn9;
    Button sumbit_button, reset_button, class_search_button;

    protected void OnCreate(Bundle savedInstance){
        super.onCreate(savedInstance);
        setContentView(R.layout.fragment_register);
        Total_Hours = findViewById(R.id.totalhours);
        Billing_Hours = findViewById(R.id.billinghours);
        MinHours = findViewById(R.id.minhours);
        MaxHours = findViewById(R.id.maxhours);
        DateVar = findViewById(R.id.datevar);
        crn1 = findViewById(R.id.CRN1);
        crn2 = findViewById(R.id.CRN2);
        crn3 = findViewById(R.id.CRN3);
        crn4 = findViewById(R.id.CRN4);
        crn5 = findViewById(R.id.CRN5);
        crn6 = findViewById(R.id.CRN6);
        crn7 = findViewById(R.id.CRN7);
        crn8 = findViewById(R.id.CRN8);
        crn9 = findViewById(R.id.CRN9);
        sumbit_button = findViewById(R.id.submitbutton);
        reset_button = findViewById(R.id.resetbutton);
        class_search_button = findViewById(R.id.class_search);
    }

    //to do: actually making this functional


}
