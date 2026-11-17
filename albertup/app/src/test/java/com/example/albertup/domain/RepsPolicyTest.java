package com.example.albertup.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RepsPolicyTest {

    @Test
    public void parseAcceptsTrimmedWholeNumbers() {
        assertEquals(Integer.valueOf(12), RepsPolicy.parse(" 12 "));
        assertEquals(Integer.valueOf(7), RepsPolicy.parse("007"));
    }

    @Test
    public void parseRejectsEmptyOrNonNumericInput() {
        assertNull(RepsPolicy.parse(null));
        assertNull(RepsPolicy.parse("   "));
        assertNull(RepsPolicy.parse("12a"));
        assertNull(RepsPolicy.parse("-5"));
        assertNull(RepsPolicy.parse("1.5"));
        assertNull(RepsPolicy.parse("99999999999"));
    }

    @Test
    public void validRangeIsInclusive() {
        assertFalse(RepsPolicy.isValid(RepsPolicy.MIN_REPS - 1));
        assertTrue(RepsPolicy.isValid(RepsPolicy.MIN_REPS));
        assertTrue(RepsPolicy.isValid(RepsPolicy.MAX_REPS));
        assertFalse(RepsPolicy.isValid(RepsPolicy.MAX_REPS + 1));
    }
}
