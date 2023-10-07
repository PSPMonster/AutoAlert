package com.wikdev.autoalert;

import android.annotation.SuppressLint;
import android.widget.EditText;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DateUtils {

    public static String validateAndParseDate(EditText date) {
        String dateInput = date.getText().toString();
        String datePattern = "\\d{2}/\\d{2}/\\d{4}";

        Pattern pattern = Pattern.compile(datePattern);
        Matcher matcher = pattern.matcher(dateInput);

        if (!matcher.matches()) {
            return null;
        }

        @SuppressLint("SimpleDateFormat") SimpleDateFormat inputFormat = new SimpleDateFormat("dd/MM/yyyy");
        @SuppressLint("SimpleDateFormat") SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd");
        inputFormat.setLenient(false);

        try {
            Date parsedDate = inputFormat.parse(dateInput);
            assert parsedDate != null;
            return outputFormat.format(parsedDate);
        } catch (ParseException e) {
            return null;
        }

    }

    public static boolean areDateAfterToday(String date, boolean monthCheck, boolean weekCheck, boolean dayCheck) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date currentDate = Calendar.getInstance().getTime();
            Date inputDate = sdf.parse(date);

            if (monthCheck && inputDate != null) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(inputDate);
                cal.add(Calendar.MONTH, -1);
                inputDate = cal.getTime();
            }

            if (weekCheck && inputDate != null) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(inputDate);
                cal.add(Calendar.WEEK_OF_YEAR, -1);
                inputDate = cal.getTime();
            }

            if (dayCheck && inputDate != null) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(inputDate);
                cal.add(Calendar.DAY_OF_MONTH, -1);
                inputDate = cal.getTime();
            }

            return currentDate.after(inputDate);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }



    /**
     * Parses a date string into a LocalDate object using the ISO_DATE format (yyyy-MM-dd).
     *
     * @param date the date string to parse
     * @return the parsed LocalDate object
     */
    public static LocalDate parseDate(String date) {
        return LocalDate.parse(date, DateTimeFormatter.ISO_DATE);
    }

    /**
     * Parses a date string from the database into a formatted string of the form "dd/MM/yyyy".
     *
     * @param date the date string to parse
     * @return the formatted date string
     */
    public static String parseDateToSlashFormat(String date) {
        LocalDate parsedDate = LocalDate.parse(date, DateTimeFormatter.ISO_DATE);
        return parsedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    /**
     * Subtracts the specified number of months from a given date string and returns the result in ISO_DATE format.
     *
     * @param date   the date string to subtract months from
     * @param months the number of months to subtract
     * @return the resulting date string
     */
    public static String subtractMonthsFromDate(String date, int months) {
        LocalDate localDate = parseDate(date);
        LocalDate newDate = localDate.minusMonths(months);
        return newDate.format(DateTimeFormatter.ISO_DATE);
    }

    /**
     * Subtracts the specified number of weeks from a given date string and returns the result in ISO_DATE format.
     *
     * @param date  the date string to subtract weeks from
     * @param weeks the number of weeks to subtract
     * @return the resulting date string
     */
    public static String subtractWeeksFromDate(String date, int weeks) {
        LocalDate localDate = parseDate(date);
        LocalDate newDate = localDate.minusWeeks(weeks);
        return newDate.format(DateTimeFormatter.ISO_DATE);
    }

    /**
     * Subtracts the specified number of days from a given date string and returns the result in ISO_DATE format.
     *
     * @param date the date string to subtract days from
     * @param days the number of days to subtract
     * @return the resulting date string
     */
    public static String subtractDaysFromDate(String date, int days) {
        LocalDate localDate = parseDate(date);
        LocalDate newDate = localDate.minusDays(days);
        return newDate.format(DateTimeFormatter.ISO_DATE);
    }


}