package com.wikdev.autoalert;
import android.Manifest;


import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

import smartdevelop.ir.eram.showcaseviewlib.GuideView;
import smartdevelop.ir.eram.showcaseviewlib.config.DismissType;


public class MainActivity extends AppCompatActivity {
    RecyclerView recyclerView;
    FloatingActionButton btnAddCarAlert;
    TextView btnSettings, txtNoData;
    ImageView imgNoData;

    DatabaseHelper myDB;
    public CustomAdapter customAdapter;

    ArrayList<String> car_id, car_name, car_image_path, car_inspection_date, car_insurance_date;

    ImageView btnChangeToPolish, btnChangeToEnglish;

    AdView mAdView;


    public static final String SHARED_APP_PREFS = "app_language_prefs";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String systemLanguage = Locale.getDefault().getLanguage();
        SharedPreferences preferences = getSharedPreferences(SHARED_APP_PREFS, Context.MODE_PRIVATE);

        String selectedLanguage = preferences.getString("selected_language", "");
        String launchTutorialMain = preferences.getString("launchTutorialMain", "yes");

        NotificationManager notificationManager = this.getSystemService(NotificationManager.class);


        if (selectedLanguage.isEmpty()) {
            // Sprawdź, czy język systemowy jest obsługiwany
            if (systemLanguage.equals("pl")) {
                setAppLanguage(this, "pl");
            } else if (systemLanguage.equals("en")) {
                setAppLanguage(this, "en");
            } else {
                setAppLanguage(this, "en");
            }
        } else {
            checkLanguage(this, selectedLanguage);
        }

        setContentView(R.layout.activity_main);


        MobileAds.initialize(this, initializationStatus -> {
            mAdView = findViewById(R.id.adView);
            mAdView.setVisibility(View.VISIBLE);
            AdRequest adRequest = new AdRequest.Builder().build();
            mAdView.loadAd(adRequest);
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this,Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[] {Manifest.permission.POST_NOTIFICATIONS}, 1);
            }

        }

        recyclerView = findViewById(R.id.recyclerView);

        myDB = new DatabaseHelper(MainActivity.this);
        car_id = new ArrayList<>();
        car_name = new ArrayList<>();
        car_image_path = new ArrayList<>();
        car_inspection_date = new ArrayList<>();
        car_insurance_date = new ArrayList<>();

        btnAddCarAlert = findViewById(R.id.btnAddCarAlert);
        btnSettings = findViewById(R.id.btnSettings);

        imgNoData = findViewById(R.id.imgNoData);
        txtNoData = findViewById(R.id.txtNoData);

        customAdapter = new CustomAdapter(this, car_id, car_name, car_image_path, car_inspection_date, car_insurance_date);
        recyclerView.setAdapter(customAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));

        storeCarsInfoInArrays();

        Animation fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        recyclerView.startAnimation(fadeInAnimation);

        btnSettings.setOnClickListener(view -> showBottomSheetDialog());

        btnAddCarAlert.setOnClickListener(view -> {
            Intent myIntent = new Intent(MainActivity.this, AddCarAlertActivity.class);
            startActivity(myIntent);
        });




        // Handle the RecyclerView's scroll behavior with OnScrollListener
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                // The delay of the extension of the FAB is set for 12 items
                if (dy > 12 && btnAddCarAlert.isShown()) {
                    btnAddCarAlert.hide();
                }

                // The delay of the extension of the FAB is set for 12 items
                if (dy < -12 && !btnAddCarAlert.isShown()) {
                    btnAddCarAlert.show();
                }

                // If the RecyclerView is at the first item of the list then the
                // floating action should be in the show state
                if (!recyclerView.canScrollVertically(-1)) {
                    btnAddCarAlert.show();
                }
            }
        });

        if (launchTutorialMain.equals("yes")) {
//            new GuideView.Builder(this)
//                    .setContentText(getString(R.string.settings_tutorial_text))
//                    .setTargetView(btnSettings)
//                    .setContentTextSize(16)//optional
//                    .setDismissType(DismissType.outside) //optional - default dismissible by TargetView
//                    .build()
//                    .show();

            new GuideView.Builder(this)
                    .setContentText(getString(R.string.add_car_tutorial_text))
                    .setTargetView(btnAddCarAlert)
                    .setContentTextSize(16)//optional
                    .setDismissType(DismissType.targetView) //optional - default dismissible by TargetView
                    .build()
                    .show();

            SharedPreferences.Editor editor = preferences.edit();
            editor.putString("launchTutorialMain", "no");
            editor.apply();
        }

    }


    private void showBottomSheetDialog() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        bottomSheetDialog.setContentView(R.layout.bottom_sheet);

        BottomSheetBehavior<View> bottomSheetBehavior = BottomSheetBehavior.from(Objects.requireNonNull(bottomSheetDialog.getDelegate().findViewById(com.google.android.material.R.id.design_bottom_sheet)));
        bottomSheetBehavior.setPeekHeight((int) (getResources().getDisplayMetrics().heightPixels * 0.8));

        btnChangeToPolish =  bottomSheetDialog.findViewById(R.id.btnChangeToPolishLanguage);
        btnChangeToEnglish = bottomSheetDialog.findViewById(R.id.btnChangeToEnglishLanguage);

        btnChangeToPolish.setOnClickListener(view -> changeLanguage(MainActivity.this, "pl"));

        btnChangeToEnglish.setOnClickListener(view -> changeLanguage(MainActivity.this, "en"));

        TextView mainIconsText = bottomSheetDialog.findViewById(R.id.txtIconsAuthor1);
        TextView uiIconsText = bottomSheetDialog.findViewById(R.id.txtIconsAuthor2);
        TextView flagsIconsText = bottomSheetDialog.findViewById(R.id.txtIconsAuthor3);
        TextView iconsSiteText = bottomSheetDialog.findViewById(R.id.txtIconsSite);
        TextView appGithubText = bottomSheetDialog.findViewById(R.id.textView4);


        for (TextView textView : Arrays.asList(mainIconsText, uiIconsText, flagsIconsText, iconsSiteText, appGithubText)) {
            textView.setMovementMethod(LinkMovementMethod.getInstance());
        }

        bottomSheetDialog.show();
    }


    void storeCarsInfoInArrays() {
        Cursor cursor = myDB.readAllCarData();
        if (cursor.getCount() == 0) {
            imgNoData.setVisibility(View.VISIBLE);
            txtNoData.setVisibility(View.VISIBLE);
        } else {
            while (cursor.moveToNext()) {
                car_id.add(cursor.getString(0));
                car_name.add(cursor.getString(1));
                car_image_path.add(cursor.getString(2));
                car_inspection_date.add(cursor.getString(3));
                car_insurance_date.add(cursor.getString(4));
            }
            imgNoData.setVisibility(View.GONE);
            txtNoData.setVisibility(View.GONE);
        }

        myDB.close();
    }

    private void changeLanguage(Context context, String language) {
        Locale locale = new Locale(language);
        Locale.setDefault(locale);

        Resources resources = context.getResources();

        Configuration configuration = resources.getConfiguration();

        configuration.setLocale(locale);

        resources.updateConfiguration(configuration, resources.getDisplayMetrics());

        SharedPreferences preferences = getSharedPreferences(SHARED_APP_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("selected_language", language);
        editor.apply();


        Intent refresh = new Intent(this, MainActivity.class);
        startActivity(refresh);
        finish();
    }
    public static void setAppLanguage(Context context, String language) {
        Locale locale = new Locale(language);
        Locale.setDefault(locale);

        Resources resources = context.getResources();

        Configuration configuration = resources.getConfiguration();

        configuration.setLocale(locale);

        resources.updateConfiguration(configuration, resources.getDisplayMetrics());

        // Zapisz wybrany język do preferencji, aby go pamiętać
        SharedPreferences preferences = context.getSharedPreferences(SHARED_APP_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("selected_language", language);
        editor.apply();
    }


    public static void checkLanguage(Context context, String language) {
        Locale locale = new Locale(language);
        Locale.setDefault(locale);

        Resources resources = context.getResources();

        Configuration configuration = resources.getConfiguration();

        configuration.setLocale(locale);

        resources.updateConfiguration(configuration, resources.getDisplayMetrics());

    }

}