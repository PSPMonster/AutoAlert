package com.wikdev.autoalert;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CustomAdapter extends RecyclerView.Adapter<CustomAdapter.MyViewHolder> {

    Context context;

    private final ArrayList car_id;
    private final ArrayList car_name;
    private final ArrayList car_image_path;
    private final ArrayList car_inspection_date;
    private final ArrayList car_insurance_date;

    CustomAdapter(Context context, ArrayList car_id, ArrayList car_name, ArrayList car_image_path,
                  ArrayList car_inspection_date, ArrayList car_insurance_date) {
        this.context = context;
        this.car_id = car_id;
        this.car_name = car_name;
        this.car_image_path = car_image_path;
        this.car_inspection_date = car_inspection_date;
        this.car_insurance_date = car_insurance_date;
    }



    @NonNull
    @Override
    public CustomAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.car_row, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CustomAdapter.MyViewHolder holder, int position) {
        String defaultCarNoDateText = context.getString(R.string.no_date_text);

        String car_name_string = (String) car_name.get(position);
        String car_image_path_string = (String) car_image_path.get(position);

        Object car_inspection_date_object = car_inspection_date.get(position);
        Object car_insurance_date_object = car_insurance_date.get(position);

        Date todayDateObj = new Date();
        DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

        if (car_image_path_string == null) {
            holder.imgCarImage.setImageResource(R.drawable.no_car_image);
        } else {
            try {
                Bitmap bitmap = BitmapFactory.decodeFile(car_image_path_string);
                holder.imgCarImage.setImageBitmap(bitmap);
            } catch (Exception e) {
                holder.imgCarImage.setImageResource(R.drawable.no_car_image); // Ustawienie obrazu błędu, jeśli dekodowanie się nie powiedzie
            }
        }

        // Setting bottom padding for last item in recyclerView
        if (position == getItemCount()-1) {
            holder.carLayoutParent.setPadding(0, 0, 0, 190);
        }

        holder.txtCarName.setText(car_name_string);



        if (car_inspection_date_object == null) {
            holder.txtCarInspectionDate.setText(defaultCarNoDateText);
            holder.txtCarInspectionDate.setTextColor(Color.parseColor("#aaaaaa"));
            holder.imgCarInspectionDate.setImageResource(R.drawable.car_inspection_icon);
        } else {
            String parsedDate = DateUtils.parseDateToSlashFormat((String) car_inspection_date_object);
            Date parsedDateObj;

            try {
                parsedDateObj = dateFormat.parse(parsedDate);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }

            if (parsedDateObj != null && (parsedDateObj.before(todayDateObj) || parsedDateObj.equals(todayDateObj))) {
                holder.imgCarInspectionDate.setImageResource(R.drawable.car_inspection_icon_red);
                Animation blinkAnimation = AnimationUtils.loadAnimation(context, R.anim.blink);
                holder.imgCarInspectionDate.startAnimation(blinkAnimation);
            } else {
                holder.imgCarInspectionDate.setImageResource(R.drawable.car_inspection_icon_green);
            }

            holder.txtCarInspectionDate.setText(parsedDate);
        }

        if (car_insurance_date_object == null) {
            holder.txtCarInsuranceDate.setText(defaultCarNoDateText);
            holder.txtCarInsuranceDate.setTextColor(Color.parseColor("#aaaaaa"));
            holder.imgCarInsuranceDate.setImageResource(R.drawable.car_insurance_icon);
        } else {
            String parsedDate = DateUtils.parseDateToSlashFormat((String) car_insurance_date_object);
            Date parsedDateObj;

            try {
                parsedDateObj = dateFormat.parse(parsedDate);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }

            if (parsedDateObj != null && (parsedDateObj.before(todayDateObj) || parsedDateObj.equals(todayDateObj))) {
                holder.imgCarInsuranceDate.setImageResource(R.drawable.car_insurance_icon_red);
                Animation blinkAnimation = AnimationUtils.loadAnimation(context, R.anim.blink);
                holder.imgCarInsuranceDate.startAnimation(blinkAnimation);
            } else {
                holder.imgCarInsuranceDate.setImageResource(R.drawable.car_insurance_icon_green);
            }

            holder.txtCarInsuranceDate.setText(parsedDate);
        }


        holder.btnRemoveCar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int currentPosition = holder.getAdapterPosition(); // Pobranie aktualnej pozycji elementu
                // Usuwamy dany element z bazy danych
                DatabaseHelper myDB = new DatabaseHelper(context.getApplicationContext());
                boolean isCarDeleted = myDB.deleteCar(car_id.get(currentPosition).toString());

                List<Long> deletedIds = myDB.deleteNotificationsByCarId(Long.parseLong(car_id.get(currentPosition).toString()));
                if (isCarDeleted) {
                    for (long deletedId : deletedIds) {
                        ReminderBroadcast.cancelNotificationAlarm(context, String.valueOf(deletedId));
                    }


                    String imagePath = (String) car_image_path.get(currentPosition);
                    if (imagePath != null && !imagePath.isEmpty()) {
                        File imageFile = new File(imagePath);
                        if (imageFile.exists()) {
                            boolean isImageDeleted = imageFile.delete();
                            if (isImageDeleted) {
                                Log.d(TAG, "Obrazek usunięty pomyślnie: " + imagePath);
                            } else {
                                Log.e(TAG, "Błąd podczas usuwania obrazka: " + imagePath);
                            }
                        }
                    }

                    // Jeśli element został usunięty z bazy danych, to usuwamy go także z RecyclerView
                    car_id.remove(currentPosition);
                    car_name.remove(currentPosition);
                    car_image_path.remove(currentPosition);
                    car_inspection_date.remove(currentPosition);
                    car_insurance_date.remove(currentPosition);

                    notifyItemRemoved(currentPosition);
                    notifyItemRangeChanged(currentPosition, getItemCount());

                } else {
                    Toast.makeText(context, context.getString(R.string.car_deleted_unsuccessfully), Toast.LENGTH_SHORT).show();
                }
            }
        });


        holder.imgCarInspectionDate.setOnClickListener(view -> {
            String carInspectionText = context.getString(R.string.car_inspection_text);
            holder.imgCarInspectionDate.setTooltipText(carInspectionText);
        });

        holder.imgCarInsuranceDate.setOnClickListener(view -> {
            String carInsuranceText = context.getString(R.string.car_insurance_text);
            holder.imgCarInsuranceDate.setTooltipText(carInsuranceText);
        });

    }
    @Override
    public int getItemCount() {
        return car_id.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {

        ConstraintLayout carLayoutParent, carLayout;
        ImageView imgCarImage, btnRemoveCar;
        TextView txtCarName, txtCarInspectionDate, txtCarInsuranceDate;
        ImageView imgCarInspectionDate, imgCarInsuranceDate;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            carLayoutParent = itemView.findViewById(R.id.carLayoutParent);
            carLayout = itemView.findViewById(R.id.carLayout);

            imgCarImage = itemView.findViewById(R.id.imgCarImage);
            txtCarName = itemView.findViewById(R.id.txtCarName);
            txtCarInspectionDate = itemView.findViewById(R.id.txtCarInspectionDate);
            txtCarInsuranceDate = itemView.findViewById(R.id.txtCarInsuranceDate);

            imgCarInspectionDate = itemView.findViewById(R.id.imgCarInspectionDate);
            imgCarInsuranceDate = itemView.findViewById(R.id.imgCarInsuranceDate);

            btnRemoveCar = itemView.findViewById(R.id.btnRemoveCar);

        }
    }



}
