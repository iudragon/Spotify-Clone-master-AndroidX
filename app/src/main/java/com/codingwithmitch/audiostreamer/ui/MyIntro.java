package com.codingwithmitch.audiostreamer.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Toast;

import com.codingwithmitch.audiostreamer.R;
import com.github.paolorotolo.appintro.AppIntro;
import com.github.paolorotolo.appintro.AppIntroFragment;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class MyIntro extends AppIntro {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addSlide(AppIntroFragment.newInstance("HEY!!!!!", "Music", R.drawable.ic_audiotrack_white_24dp, Color.MAGENTA));
        addSlide(AppIntroFragment.newInstance("What's Up?????", "Podcast", R.drawable.ic_mic_white_24dp, Color.GREEN));
        addSlide(AppIntroFragment.newInstance("WE are", "Baka", R.drawable.ic_appintro_done_white, Color.RED));
        setBarColor(Color.parseColor("#000000"));
        showStatusBar(false);
    }

    @Override
    public void onDonePressed(Fragment currentFragment) {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    @Override
    public void onSkipPressed(Fragment currentFragment) {
        startActivity(new Intent(this, MainActivity.class));
        finish();
        Toast.makeText(this, "Alright then we will keep our secrets!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSlideChanged(@Nullable Fragment oldFragment, @Nullable Fragment newFragment) {
        Toast.makeText(this, "Running late?", Toast.LENGTH_SHORT).show();
    }
}