package com.microtechmd.pda.ui.activity;


import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.microtechmd.pda.R;


public class ActivityTransform extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transform);
        startActivity(new Intent(this, ActivityMain.class));
        finish();
    }

}
