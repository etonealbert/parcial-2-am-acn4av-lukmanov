package com.example.albertup.ui;

import android.content.Context;
import android.text.format.DateFormat;

import androidx.annotation.NonNull;
import androidx.core.os.ConfigurationCompat;

import com.example.albertup.R;
import com.example.albertup.data.Exercise;
import com.example.albertup.domain.LocalDay;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** Locale-aware formatting that follows the language chosen in the app. */
public final class Formats {

    private Formats() {
    }

    @NonNull
    public static Locale locale(@NonNull Context context) {
        Locale locale = ConfigurationCompat.getLocales(
                context.getResources().getConfiguration()).get(0);
        return locale != null ? locale : Locale.getDefault();
    }

    /** "12 flexiones", "1 pull-up"... */
    @NonNull
    public static String exerciseCount(@NonNull Context context, @NonNull Exercise exercise,
                                       int reps) {
        return context.getResources().getQuantityString(
                exercise.getCountPluralsRes(), reps, reps);
    }

    @NonNull
    public static String reps(@NonNull Context context, int reps) {
        return context.getResources().getQuantityString(R.plurals.reps_count, reps, reps);
    }

    @NonNull
    public static String sets(@NonNull Context context, int sets) {
        return context.getResources().getQuantityString(R.plurals.sets_count, sets, sets);
    }

    /** Integer with locale grouping, e.g. "1.240" (es) or "1,240" (en). */
    @NonNull
    public static String number(@NonNull Context context, int value) {
        return NumberFormat.getIntegerInstance(locale(context)).format(value);
    }

    /** Time of day honoring the device 12/24-hour preference. */
    @NonNull
    public static String time(@NonNull Context context, long millis) {
        return format(context, DateFormat.is24HourFormat(context) ? "Hm" : "hma", millis);
    }

    /** "Martes, 17 de noviembre"; the year is added when it differs from {@code now}. */
    @NonNull
    public static String longDate(@NonNull Context context, long millis, long now) {
        String skeleton = year(millis) == year(now) ? "EEEEdMMMM" : "EEEEdMMMMy";
        return capitalize(format(context, skeleton, millis), locale(context));
    }

    @NonNull
    public static String dateTime(@NonNull Context context, long millis) {
        return context.getString(R.string.format_date_time,
                longDate(context, millis, System.currentTimeMillis()), time(context, millis));
    }

    /** "Hoy", "Ayer" or the long date. */
    @NonNull
    public static String dayLabel(@NonNull Context context, long millis, long now) {
        TimeZone timeZone = TimeZone.getDefault();
        int day = LocalDay.dayKey(millis, timeZone);
        if (day == LocalDay.dayKey(now, timeZone)) {
            return context.getString(R.string.day_today);
        }
        if (day == LocalDay.previousDayKey(now, timeZone)) {
            return context.getString(R.string.day_yesterday);
        }
        return longDate(context, millis, now);
    }

    @NonNull
    private static String format(Context context, String skeleton, long millis) {
        Locale locale = locale(context);
        String pattern = DateFormat.getBestDateTimePattern(locale, skeleton);
        return new SimpleDateFormat(pattern, locale).format(new Date(millis));
    }

    private static int year(long millis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        return calendar.get(Calendar.YEAR);
    }

    @NonNull
    private static String capitalize(String text, Locale locale) {
        if (text.isEmpty()) {
            return text;
        }
        return text.substring(0, 1).toUpperCase(locale) + text.substring(1);
    }
}
