package com.example.albertup.domain;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

public class WorkoutStatsTest {

    private static final TimeZone BUENOS_AIRES =
            TimeZone.getTimeZone("America/Argentina/Buenos_Aires");
    private static final TimeZone NEW_YORK = TimeZone.getTimeZone("America/New_York");

    private static long at(TimeZone zone, int year, int month, int day, int hour, int minute) {
        Calendar calendar = Calendar.getInstance(zone);
        calendar.clear();
        calendar.set(year, month, day, hour, minute, 0);
        return calendar.getTimeInMillis();
    }

    @Test
    public void lastMillisecondBeforeMidnightBelongsToThatDay() {
        long midnight = at(BUENOS_AIRES, 2026, Calendar.NOVEMBER, 18, 0, 0);

        assertEquals(20261117, LocalDay.dayKey(midnight - 1, BUENOS_AIRES));
        assertEquals(20261118, LocalDay.dayKey(midnight, BUENOS_AIRES));
        assertEquals(midnight, LocalDay.startOfNextDay(midnight - 1, BUENOS_AIRES));
    }

    @Test
    public void dayRangeHandlesShortDaylightSavingDay() {
        long noon = at(NEW_YORK, 2026, Calendar.MARCH, 8, 12, 0);

        long start = LocalDay.startOfDay(noon, NEW_YORK);
        long next = LocalDay.startOfNextDay(noon, NEW_YORK);

        assertEquals(at(NEW_YORK, 2026, Calendar.MARCH, 8, 0, 0), start);
        assertEquals(at(NEW_YORK, 2026, Calendar.MARCH, 9, 0, 0), next);
        assertEquals(23L * 60 * 60 * 1000, next - start);
    }

    @Test
    public void streakCountsConsecutiveDaysOnceEach() {
        long now = at(BUENOS_AIRES, 2026, Calendar.NOVEMBER, 17, 20, 0);
        List<Long> timestamps = Arrays.asList(
                at(BUENOS_AIRES, 2026, Calendar.NOVEMBER, 17, 8, 0),
                at(BUENOS_AIRES, 2026, Calendar.NOVEMBER, 17, 9, 0),
                at(BUENOS_AIRES, 2026, Calendar.NOVEMBER, 16, 23, 59),
                at(BUENOS_AIRES, 2026, Calendar.NOVEMBER, 15, 0, 1),
                // The missing 14th ends the streak, so the 13th is not counted.
                at(BUENOS_AIRES, 2026, Calendar.NOVEMBER, 13, 10, 0));

        assertEquals(3, WorkoutStats.currentStreak(timestamps, now, BUENOS_AIRES));
    }

    @Test
    public void streakStaysAliveWhenNothingLoggedTodayYet() {
        long now = at(BUENOS_AIRES, 2026, Calendar.NOVEMBER, 1, 23, 0);
        List<Long> timestamps = Arrays.asList(
                at(BUENOS_AIRES, 2026, Calendar.OCTOBER, 31, 7, 0),
                at(BUENOS_AIRES, 2026, Calendar.OCTOBER, 30, 7, 0));

        assertEquals(2, WorkoutStats.currentStreak(timestamps, now, BUENOS_AIRES));
    }

    @Test
    public void streakIsBrokenAfterAMissedDay() {
        long now = at(BUENOS_AIRES, 2026, Calendar.NOVEMBER, 17, 9, 0);

        assertEquals(0, WorkoutStats.currentStreak(
                Collections.singletonList(at(BUENOS_AIRES, 2026, Calendar.NOVEMBER, 15, 7, 0)),
                now, BUENOS_AIRES));
        assertEquals(0, WorkoutStats.currentStreak(
                Collections.<Long>emptyList(), now, BUENOS_AIRES));
    }

    @Test
    public void goalProgressIsClampedAndRemainingNeverNegative() {
        assertEquals(33, WorkoutStats.goalPercent(50, 150));
        assertEquals(100, WorkoutStats.goalPercent(250, 100));
        assertEquals(55, WorkoutStats.remainingToGoal(45, 100));
        assertEquals(0, WorkoutStats.remainingToGoal(130, 100));
    }
}
