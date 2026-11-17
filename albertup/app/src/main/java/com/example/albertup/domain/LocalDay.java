package com.example.albertup.domain;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Local calendar-day helpers. Days are computed in the given time zone with {@link Calendar},
 * so 23/25-hour days around daylight-saving changes are handled correctly (minSdk 24 has no
 * java.time without desugaring).
 */
public final class LocalDay {

    private LocalDay() {
    }

    /** First instant of the local day containing {@code millis}. */
    public static long startOfDay(long millis, TimeZone timeZone) {
        Calendar calendar = Calendar.getInstance(timeZone);
        calendar.setTimeInMillis(millis);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    /** First instant of the local day after the one containing {@code millis}. */
    public static long startOfNextDay(long millis, TimeZone timeZone) {
        Calendar calendar = Calendar.getInstance(timeZone);
        calendar.setTimeInMillis(startOfDay(millis, timeZone));
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        return startOfDay(calendar.getTimeInMillis(), timeZone);
    }

    /** Local date encoded as yyyyMMdd, e.g. 20261117. Equal keys mean the same local day. */
    public static int dayKey(long millis, TimeZone timeZone) {
        Calendar calendar = Calendar.getInstance(timeZone);
        calendar.setTimeInMillis(millis);
        return dayKey(calendar);
    }

    static int dayKey(Calendar calendar) {
        return calendar.get(Calendar.YEAR) * 10000
                + (calendar.get(Calendar.MONTH) + 1) * 100
                + calendar.get(Calendar.DAY_OF_MONTH);
    }

    /** Day key of the local day before the one containing {@code millis}. */
    public static int previousDayKey(long millis, TimeZone timeZone) {
        Calendar calendar = Calendar.getInstance(timeZone);
        calendar.setTimeInMillis(millis);
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        return dayKey(calendar);
    }
}
