package com.wikdev.autoalert;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;

import androidx.annotation.Nullable;


import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private final Context context;
    private static final String DATABASE_NAME = "CarAlert.db";
    private static final int DATABASE_VERSION = 1;

    private static final String CARS_TABLE_NAME = "Cars";
    private static final String NOTIFICATIONS_TABLE_NAME = "Notifications";

    // Car Table
    private static final String COLUMN_ID = "_id";
    private static final String COLUMN_CAR_NAME = "car_name";
    private static final String COLUMN_CAR_IMAGE_PATH = "car_image_path";
    private static final String COLUMN_INSPECTION_DATE = "inspection_date";
    private static final String COLUMN_INSURANCE_DATE = "insurance_date";


    // Notification Table
    private static final String COLUMN_CAR_ID = "id_car";
    private static final String COLUMN_NOTIFICATION_DATE = "date";
    private static final String COLUMN_NOTIFICATION_TYPE = "type";

    public DatabaseHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String queryCarAlerts  =
                "CREATE TABLE " + CARS_TABLE_NAME +
                        " (" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COLUMN_CAR_NAME + " TEXT, " +
                        COLUMN_CAR_IMAGE_PATH + " TEXT, " +
                        COLUMN_INSPECTION_DATE + " TEXT, " +
                        COLUMN_INSURANCE_DATE + " TEXT);";

        db.execSQL(queryCarAlerts);

        String queryNotifications =
                "CREATE TABLE " + NOTIFICATIONS_TABLE_NAME +
                        " (" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COLUMN_CAR_ID + " INTEGER, " +
                        COLUMN_NOTIFICATION_DATE + " TEXT, " +
                        COLUMN_NOTIFICATION_TYPE + " TEXT, " +
                        "FOREIGN KEY(" + COLUMN_CAR_ID + ") REFERENCES " + CARS_TABLE_NAME + "(" + COLUMN_ID + "));";
        db.execSQL(queryNotifications);


    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
        db.execSQL("DROP TABLE IF EXISTS " + CARS_TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + NOTIFICATIONS_TABLE_NAME);
        onCreate(db);
    }

    long addCar(String carName, String carImagePath, String carInspectionDate, String carInsuranceDate) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();

        cv.put(COLUMN_CAR_NAME, carName);
        cv.put(COLUMN_CAR_IMAGE_PATH, carImagePath);
        cv.put(COLUMN_INSPECTION_DATE, carInspectionDate);
        cv.put(COLUMN_INSURANCE_DATE, carInsuranceDate);


        return db.insert(CARS_TABLE_NAME, null, cv);
    }

    int addNotification(long carId, String notificationDate, String notificationType) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();

        cv.put(COLUMN_CAR_ID, carId);
        cv.put(COLUMN_NOTIFICATION_DATE, notificationDate);
        cv.put(COLUMN_NOTIFICATION_TYPE, notificationType);

        int notificationId = (int) db.insert(NOTIFICATIONS_TABLE_NAME, null, cv);

        if (notificationId == -1) {
            Toast.makeText(context, "Ups! Dodawanie powiadomienia nie powiodło się.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "Gotowe! Powiadomienie zostało dodane.", Toast.LENGTH_SHORT).show();
        }

        return notificationId;
    }


    private SQLiteDatabase getReadableDatabaseInstance() {
        return this.getReadableDatabase();
    }

    private Cursor executeRawQuery(String query) {
        SQLiteDatabase db = getReadableDatabaseInstance();
        Cursor cursor = null;
        if (db != null) {
            cursor = db.rawQuery(query, null);
        }
        return cursor;
    }

    Cursor readAllCarData() {
        String query = "SELECT * FROM " + CARS_TABLE_NAME + " ORDER BY " + COLUMN_ID + " DESC";
        return executeRawQuery(query);
    }


    Cursor getCarNameById(long carId) {
        String query = "SELECT " + COLUMN_CAR_NAME +  " FROM " + CARS_TABLE_NAME + " WHERE " + COLUMN_CAR_ID+"="+ carId;
        return executeRawQuery(query);
    }


    public boolean deleteCar(String carId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int result = db.delete(CARS_TABLE_NAME, COLUMN_ID + "=?", new String[]{carId});
        return result > 0;
    }


    public List<Long> deleteNotificationsByCarId(long carId) {
        List<Long> deletedIds = new ArrayList<>();
        SQLiteDatabase database = this.getWritableDatabase();

        String[] columns = new String[]{DatabaseHelper.COLUMN_ID};
        Cursor cursor = database.query(DatabaseHelper.NOTIFICATIONS_TABLE_NAME, columns,
                DatabaseHelper.COLUMN_CAR_ID + " = ?", new String[]{String.valueOf(carId)},
                null, null, null);

        int idColumnIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_ID);

        if (idColumnIndex != -1 && cursor.moveToFirst()) {
            do {
                long deletedId = cursor.getLong(idColumnIndex);
                database.delete(DatabaseHelper.NOTIFICATIONS_TABLE_NAME,
                        DatabaseHelper.COLUMN_ID + " = ?", new String[]{String.valueOf(deletedId)});
                deletedIds.add(deletedId);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return deletedIds;
    }

}
