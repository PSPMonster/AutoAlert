package com.wikdev.autoalert;

import static android.content.ContentValues.TAG;
import static com.wikdev.autoalert.DateUtils.areDateAfterToday;
import static com.wikdev.autoalert.MainActivity.SHARED_APP_PREFS;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import smartdevelop.ir.eram.showcaseviewlib.GuideView;
import smartdevelop.ir.eram.showcaseviewlib.config.DismissType;


public class AddCarAlertActivity extends AppCompatActivity {

    private static final int IMAGE_PICKER_REQUEST = 1;

    TextView btnBack;
    EditText etCarName, etCarInspectionDate, etCarInsuranceDate;
    TextView txtCarName, txtInspectionDate, txtInsuranceDate;

    ImageView imgCarImage, imgInspectionDate, imgInsuranceDate;
    Button btnAddCarAlert, btnAddCarImage;
    CheckBox checkMonth, checkWeek, checkDay;

    String carImagePath;

    TextInputLayout ipCarDateLayout;

    AlertDialog mDialog;

    AdView mAdView;

    Uri selectedImageUri;

    Bitmap carImageBitmap;

    private InterstitialAd mInterstitialAd;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Odczytanie zapisanego języka z SharedPreferences
        SharedPreferences preferences = getSharedPreferences(SHARED_APP_PREFS, Context.MODE_PRIVATE);
        String selectedLanguage = preferences.getString("selected_language", "en");
        String launchTutorialAddCar = preferences.getString("launchTutorialAddCar", "yes");


        // Jeśli język jest zapisany, to zmień język aplikacji
        if (!selectedLanguage.isEmpty()) {
            MainActivity.checkLanguage(this, selectedLanguage);
        }

        setContentView(R.layout.activity_add_car_alert);

        createNotificationChannel();

        mAdView = findViewById(R.id.adView);
        AdRequest request = new AdRequest.Builder().build();
        mAdView.loadAd(request);


        InterstitialAd.load(this,"ca-app-pub-9446516549704282/9187719834", request,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        // The mInterstitialAd reference will be null until
                        // an ad is loaded.
                        mInterstitialAd = interstitialAd;
                        Log.i(TAG, "onAdLoaded");
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        // Handle the error
                        Log.d(TAG, loadAdError.toString());
                        mInterstitialAd = null;
                    }
                });


        // Przygotuj popup dla użytkownika
        if (!getDialogStatus()) {
            mDialog = createDialog();
        }

        ipCarDateLayout = findViewById(R.id.ip_car_date_layout);


        btnBack = findViewById(R.id.btnBack);
        btnAddCarAlert = findViewById(R.id.btnAddCarAlert);
        btnAddCarImage = findViewById(R.id.btnAddCarImage);

        imgCarImage = findViewById(R.id.imgCarImage);
        imgInspectionDate = findViewById(R.id.imgCarInspectionDate);
        imgInsuranceDate = findViewById(R.id.imgCarInsuranceDate);


        etCarName = findViewById(R.id.ip_car_name);
        etCarInspectionDate = findViewById(R.id.ip_car_date);
        etCarInsuranceDate = findViewById(R.id.ip_insurance_date);


        txtCarName = findViewById(R.id.txtCarName);
        txtInspectionDate = findViewById(R.id.txtCarInspectionDate);
        txtInsuranceDate = findViewById(R.id.txtCarInsuranceDate);

        checkMonth = findViewById(R.id.checkMonth);
        checkWeek = findViewById(R.id.checkWeek);
        checkDay = findViewById(R.id.checkDay);


        etCarName.addTextChangedListener(textWatcher);
        etCarInspectionDate.addTextChangedListener(textWatcher);
        etCarInsuranceDate.addTextChangedListener(textWatcher);

        updateCarInfo();

        btnBack.setOnClickListener(view -> finish());

        btnAddCarImage.setOnClickListener(view -> {
            if (getDialogStatus()) {
                openImagePicker();
                return;
            }
            mDialog.show();

        });

        // Ustaw nasłuchiwacze na polach EditText
        etCarInspectionDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker(etCarInspectionDate);
            }
        });

        etCarInsuranceDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker(etCarInsuranceDate);
            }
        });


        btnAddCarAlert.setOnClickListener(view -> {
            DatabaseHelper myDB = new DatabaseHelper(AddCarAlertActivity.this);
            long carId;
            String defaultWrongInsuranceDateText = getString(R.string.wrong_insurance_date_toast);
            String defaultWrongInspectionDateText = getString(R.string.wrong_inspection_date_toast);
            String defaultWrongBothDatesToast = getString(R.string.both_dates_wrong_toast);
            String defaultNoCarNameToast = getString(R.string.no_car_name_toast);
            String defaultNoDatesToast = getString(R.string.no_dates_toast);

            String defaultInspectionDateExpiredToast = getString(R.string.expired_inspection_date_toast);
            String defaultInsuranceDateExpiredToast = getString(R.string.expired_insurance_date_toast);
            String defaultNotificationNotSetToast = getString(R.string.notification_not_set_toast);


            String carName = etCarName.getText().toString().trim();
            String carInsuranceDate = DateUtils.validateAndParseDate(etCarInsuranceDate);
            String carInspectionDate = DateUtils.validateAndParseDate(etCarInspectionDate);

            boolean ifMonthClicked = checkMonth.isChecked();
            boolean ifWeekClicked = checkWeek.isChecked();
            boolean ifDayClicked = checkDay.isChecked();

            if (carName.isEmpty()) {
                Toast.makeText(getApplicationContext(), defaultNoCarNameToast, Toast.LENGTH_SHORT).show();
                etCarName.requestFocus();

                return;
            }

            // Check if user input at least one date
            if (etCarInspectionDate.getText().toString().trim().isEmpty() && etCarInsuranceDate.getText().toString().trim().isEmpty()) {
                Toast.makeText(getApplicationContext(), defaultNoDatesToast, Toast.LENGTH_SHORT).show();
                return;
            }


            if ((!etCarInspectionDate.getText().toString().isEmpty() && carInspectionDate == null) && (!etCarInsuranceDate.getText().toString().trim().isEmpty() && carInsuranceDate == null)) {
                Toast.makeText(getApplicationContext(), defaultWrongBothDatesToast, Toast.LENGTH_SHORT).show();
                return;
            } else if (carInspectionDate == null && !etCarInspectionDate.getText().toString().isEmpty()) {
                Toast.makeText(getApplicationContext(), defaultWrongInspectionDateText, Toast.LENGTH_SHORT).show();
                return;
            } else if (carInsuranceDate == null && !etCarInsuranceDate.getText().toString().isEmpty()) {
                Toast.makeText(getApplicationContext(), defaultWrongInsuranceDateText, Toast.LENGTH_SHORT).show();
                return;
            }

            boolean isCarInsuranceDateExpired = false;
            boolean isCarInspectionDateExpired = false;
            if (ifMonthClicked || ifWeekClicked || ifDayClicked) {
                if (carInsuranceDate != null) {
                    isCarInsuranceDateExpired = areDateAfterToday(carInsuranceDate, ifMonthClicked, ifWeekClicked, ifDayClicked);
                }

                if (carInspectionDate != null) {
                    isCarInspectionDateExpired = areDateAfterToday(carInspectionDate, ifMonthClicked, ifWeekClicked, ifDayClicked);
                }

                if (isCarInsuranceDateExpired) {
                    Toast.makeText(getApplicationContext(), defaultInsuranceDateExpiredToast, Toast.LENGTH_SHORT).show();
                    return;
                } else if (isCarInspectionDateExpired) {
                    Toast.makeText(getApplicationContext(), defaultInspectionDateExpiredToast, Toast.LENGTH_SHORT).show();
                    return;
                }

                saveImageLocally(carImageBitmap);
                carId = myDB.addCar(carName, carImagePath, carInspectionDate, carInsuranceDate);


                if (carInsuranceDate != null) {
                    if (ifMonthClicked) {
                        String carInsuranceDateMonth = DateUtils.subtractMonthsFromDate(carInsuranceDate, 1);
                        int notificationId = myDB.addNotification(carId, carInsuranceDateMonth, "insurance_month");
                        setAlarm(notificationId, carInsuranceDateMonth, DateUtils.parseDateToSlashFormat(carInsuranceDate), "insurance_month", carName);
                    }

                    if (ifWeekClicked) {
                        String carInsuranceDateWeek = DateUtils.subtractWeeksFromDate(carInsuranceDate, 1);
                        int notificationId = myDB.addNotification(carId, carInsuranceDateWeek, "insurance_week");
                        setAlarm(notificationId, carInsuranceDateWeek, DateUtils.parseDateToSlashFormat(carInsuranceDate), "insurance_week", carName);
                    }

                    if (ifDayClicked) {
                        String carInsuranceDateDay = DateUtils.subtractDaysFromDate(carInsuranceDate, 1);
                        int notificationId = myDB.addNotification(carId, carInsuranceDateDay, "insurance_day");
                        setAlarm(notificationId, carInsuranceDateDay, DateUtils.parseDateToSlashFormat(carInsuranceDate), "insurance_day", carName);
                    }
                }

                if (carInspectionDate != null) {
                    if (ifMonthClicked) {
                        String carInspectionDateMonth = DateUtils.subtractMonthsFromDate(carInspectionDate, 1);
                        int notificationId = myDB.addNotification(carId, carInspectionDateMonth, "inspection_month");
                        setAlarm(notificationId, carInspectionDateMonth, DateUtils.parseDateToSlashFormat(carInspectionDate), "inspection_month", carName);
                    }

                    if (ifWeekClicked) {
                        String carInspectionDateWeek = DateUtils.subtractWeeksFromDate(carInspectionDate, 1);
                        int notificationId = myDB.addNotification(carId, carInspectionDateWeek, "inspection_week");
                        setAlarm(notificationId, carInspectionDateWeek, DateUtils.parseDateToSlashFormat(carInspectionDate), "inspection_week", carName);
                    }

                    if (ifDayClicked) {
                        String carInspectionDateDay = DateUtils.subtractDaysFromDate(carInspectionDate, 1);
                        int notificationId = myDB.addNotification(carId, carInspectionDateDay, "inspection_day");
                        setAlarm(notificationId, carInspectionDateDay, DateUtils.parseDateToSlashFormat(carInspectionDate), "inspection_day", carName);
                    }
                }
            } else {
                Toast.makeText(getApplicationContext(), defaultNotificationNotSetToast, Toast.LENGTH_SHORT).show();
                return;
            }

            myDB.close();

            Intent intent = new Intent(AddCarAlertActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);

            if (mInterstitialAd != null) {
                mInterstitialAd.show(AddCarAlertActivity.this);
            } else {
                Log.d("TAG", "The interstitial ad wasn't ready yet.");
            }

        });

        if (launchTutorialAddCar.equals("yes")) {
            new GuideView.Builder(this)
                    .setContentText(getString(R.string.date_calendar_tutorial_text))
                    .setTargetView(etCarInspectionDate)
                    .setContentTextSize(16)//optional
                    .setDismissType(DismissType.outside) //optional - default dismissible by TargetView
                    .build()
                    .show();

            SharedPreferences.Editor editor = preferences.edit();
            editor.putString("launchTutorialAddCar", "no");
            editor.apply();
        }
    }

    private void setAlarm(int notificationId, String notificationDate, String notificationExpiryDate, String notificationType, String carName) {
        Intent intent = new Intent(getApplicationContext(), ReminderBroadcast.class);
        intent.putExtra("notificationId", notificationId);
        intent.putExtra("notificationType", notificationType);
        intent.putExtra("carName", carName);
        intent.putExtra("notificationDate", notificationDate);
        intent.putExtra("notificationExpiryDate", notificationExpiryDate);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), notificationId, intent, PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        // Ustawienie daty alarmu
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        try {
            Date date = dateFormat.parse(notificationDate);
            if (date != null) {
                Calendar notificationDateCalendar = Calendar.getInstance();
                notificationDateCalendar.setTime(date);
                calendar.set(Calendar.YEAR, notificationDateCalendar.get(Calendar.YEAR));
                calendar.set(Calendar.MONTH, notificationDateCalendar.get(Calendar.MONTH));
                calendar.set(Calendar.DAY_OF_MONTH, notificationDateCalendar.get(Calendar.DAY_OF_MONTH));
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
    }

    public void createNotificationChannel() {
        CharSequence name = "AutoAlertChannel";
        String description = "Channel for AutoAlert Reminders";
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel channel = new NotificationChannel("notifyAutoAlert", name, importance);
        channel.setDescription(description);

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }

    public void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, IMAGE_PICKER_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == IMAGE_PICKER_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            try {
                carImageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);
                imgCarImage.setImageBitmap(carImageBitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void saveImageLocally(Bitmap bitmap) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + ".jpg";

        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File imageFile = new File(storageDir, imageFileName);

        if (carImageBitmap != null) {
            try {
                FileOutputStream fos = new FileOutputStream(imageFile);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 60, fos);
                fos.close();
                carImagePath = imageFile.getAbsolutePath();
                Log.d(TAG, carImagePath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private final TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            updateCarInfo();
        }

        @Override
        public void afterTextChanged(Editable editable) {
        }
    };

    private void updateCarInfo() {
        String carName = etCarName.getText().toString().trim();
        String carInspectionDate = etCarInspectionDate.getText().toString();
        String carInsuranceDate = etCarInsuranceDate.getText().toString();

        String defaultCarNoDateText = getString(R.string.no_date_text);

        // Set default image for car card when image isn't set
        if (imgCarImage.getDrawable() == null) {
            imgCarImage.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.no_car_image, null));

        }

        if (carName.isEmpty()) {
            String defaultCarName = getString(R.string.car_name_text);
            txtCarName.setText(defaultCarName);
        } else txtCarName.setText(carName);


        if (carInspectionDate.isEmpty() && carInsuranceDate.isEmpty()) {
            imgInspectionDate.setVisibility(View.VISIBLE);
            imgInsuranceDate.setVisibility(View.VISIBLE);
            imgInspectionDate.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.car_inspection_icon, null));
            imgInsuranceDate.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.car_insurance_icon, null));

            txtInspectionDate.setText(defaultCarNoDateText);
            txtInsuranceDate.setText(defaultCarNoDateText);
        }


        if (carInspectionDate.isEmpty() && !carInsuranceDate.isEmpty()) {
            imgInspectionDate.setVisibility(View.VISIBLE);
            imgInspectionDate.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.car_inspection_icon, null));
            imgInsuranceDate.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.car_insurance_icon_green, null));

            txtInspectionDate.setText(defaultCarNoDateText);
            txtInsuranceDate.setText(carInsuranceDate);
        } else if (!carInspectionDate.isEmpty() && carInsuranceDate.isEmpty()) {
            imgInspectionDate.setVisibility(View.VISIBLE);
            imgInspectionDate.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.car_inspection_icon_green, null));
            imgInsuranceDate.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.car_insurance_icon, null));


            txtInspectionDate.setText(carInspectionDate);
            txtInsuranceDate.setText(defaultCarNoDateText);
        }

        if (!carInspectionDate.isEmpty() && !carInsuranceDate.isEmpty()) {
            txtInspectionDate.setText(carInspectionDate);
            txtInsuranceDate.setText(carInsuranceDate);

            imgInspectionDate.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.car_inspection_icon_green, null));
            imgInsuranceDate.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.car_insurance_icon_green, null));
        }
    }
    private void storeDialogStatus(boolean isChecked){
        SharedPreferences mSharedPreferences = getSharedPreferences("CheckItem", MODE_PRIVATE);
        SharedPreferences.Editor mEditor = mSharedPreferences.edit();
        mEditor.putBoolean("showAgain", isChecked);
        mEditor.apply();
    }
    private boolean getDialogStatus(){
        SharedPreferences mSharedPreferences = getSharedPreferences("CheckItem", MODE_PRIVATE);
        return mSharedPreferences.getBoolean("showAgain", false);
    }
    private AlertDialog createDialog() {
        AlertDialog.Builder mBuilder = new AlertDialog.Builder(this);
        View mView = getLayoutInflater().inflate(R.layout.dialog_image_info, null);
        CheckBox mCheckBox = mView.findViewById(R.id.checkBox);

        String defaultWarnTitle = getString(R.string.dialog_warn_title);
        String defaultWarnText = getString(R.string.dialog_image_info_text);

        mBuilder.setTitle(defaultWarnTitle);
        mBuilder.setMessage(defaultWarnText);
        mBuilder.setView(mView);

        mBuilder.setPositiveButton("OK", (dialogInterface, i) -> {
            dialogInterface.dismiss();
            openImagePicker();
        });

        AlertDialog mDialog = mBuilder.create();

        mDialog.setOnShowListener(dialogInterface -> {
            // Get the positive button from the dialog
            Button positiveButton = mDialog.getButton(AlertDialog.BUTTON_POSITIVE);

            // Change the text color of the positive button
            positiveButton.setTextColor(getResources().getColor(R.color.primary_blue)); // Zmień kolor tekstu tutaj
        });

        mCheckBox.setOnCheckedChangeListener((compoundButton, b) -> storeDialogStatus(compoundButton.isChecked()));

        return mDialog;
    }
    private void showDatePicker(final EditText editText) {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                R.style.MaterialCalendarTheme,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(android.widget.DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        // Tutaj możesz obsłużyć wybraną datę
                        String formattedDate = String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, monthOfYear  + 1, year);
                        editText.setText(formattedDate);
                    }
                },
                year, month, day
        );

        datePickerDialog.show();
    }
}