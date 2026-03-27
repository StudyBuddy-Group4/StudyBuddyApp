package com.example.studybuddyapp;

import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;

public class StatisticsFragmentTest {

    @Test
    public void formatDate_returnsFormattedDateForValidIsoString() throws Exception {
        StatisticsFragment fragment = new StatisticsFragment();

        String formattedDate = invokeFormatDate(fragment, "2026-03-27T10:15:30");

        assertEquals("Mar 27 - 2026", formattedDate);
    }

    @Test
    public void formatDate_returnsEmptyStringForNullDate() throws Exception {
        StatisticsFragment fragment = new StatisticsFragment();

        String formattedDate = invokeFormatDate(fragment, null);

        assertEquals("", formattedDate);
    }

    @Test
    public void formatDate_returnsEmptyStringForTooShortDate() throws Exception {
        StatisticsFragment fragment = new StatisticsFragment();

        String formattedDate = invokeFormatDate(fragment, "2026-03");

        assertEquals("", formattedDate);
    }

    @Test
    public void formatDate_returnsOriginalDatePartWhenParsingFails() throws Exception {
        StatisticsFragment fragment = new StatisticsFragment();

        String formattedDate = invokeFormatDate(fragment, "2026-aa-27T10:15:30");

        assertEquals("2026-aa-27", formattedDate);
    }

    private String invokeFormatDate(StatisticsFragment fragment, String isoDate) throws Exception {
        Method method = StatisticsFragment.class.getDeclaredMethod("formatDate", String.class);
        method.setAccessible(true);
        return (String) method.invoke(fragment, isoDate);
    }
}
