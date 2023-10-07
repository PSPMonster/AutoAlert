package com.wikdev.autoalert;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.HashMap;
import java.util.Map;

public class ReminderBroadcast extends BroadcastReceiver {

    @SuppressLint("MissingPermission")
    @Override
    public void onReceive(Context context, Intent intent) {
        int notificationId = intent.getIntExtra("notificationId", 0);
        String notificationType = intent.getStringExtra("notificationType");
        String carName = intent.getStringExtra("carName");
        //String notificationDate = intent.getStringExtra("notificationDate");
        String notificationExpiryDate = intent.getStringExtra("notificationExpiryDate");

        Map<String, String> notificationMessages = new HashMap<>();
        notificationMessages.put("inspection_month", context.getString(R.string.inspection_month));
        notificationMessages.put("inspection_week", context.getString(R.string.inspection_week));
        notificationMessages.put("inspection_day", context.getString(R.string.inspection_day));
        notificationMessages.put("insurance_month", context.getString(R.string.insurance_month));
        notificationMessages.put("insurance_week", context.getString(R.string.insurance_week));
        notificationMessages.put("insurance_day", context.getString(R.string.insurance_day));

        String notificationMessage = notificationMessages.get(notificationType);
        String contentTitle;
        String contentText = "";

        if (notificationMessage != null) {
            contentTitle = String.format(notificationMessage, carName, notificationExpiryDate);
            contentText = contentTitle;
        }

        String defaultWarningTitle = context.getString(R.string.dialog_warn_title);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "notifyAutoAlert")
                .setSmallIcon(R.drawable.car_icon)
                .setContentTitle("🔔 " + defaultWarningTitle)
                .setContentText(contentText)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(notificationId, builder.build());
    }

    public static void cancelNotificationAlarm(Context context, String carId) {
        Intent intent = new Intent(context.getApplicationContext(), ReminderBroadcast.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context.getApplicationContext(), Integer.parseInt(carId), intent, PendingIntent.FLAG_IMMUTABLE);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
    }
}
