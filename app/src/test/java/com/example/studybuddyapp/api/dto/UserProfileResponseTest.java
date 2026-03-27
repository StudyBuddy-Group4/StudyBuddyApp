package com.example.studybuddyapp.api.dto;

import org.junit.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UserProfileResponseTest {

    @Test
    public void isCurrentlyRestricted_returnsTrueWhenUserIsPermanentlyBanned() throws Exception {
        UserProfileResponse response = new UserProfileResponse();
        setField(response, "isBanned", true);
        setField(response, "bannedUntil", null);

        assertTrue(response.isCurrentlyRestricted());
    }

    @Test
    public void isCurrentlyRestricted_returnsFalseWhenNoBanInformationExists() throws Exception {
        UserProfileResponse response = new UserProfileResponse();
        setField(response, "isBanned", false);
        setField(response, "bannedUntil", null);

        assertFalse(response.isCurrentlyRestricted());
    }

    @Test
    public void isCurrentlyRestricted_returnsFalseWhenBanEndIsEmpty() throws Exception {
        UserProfileResponse response = new UserProfileResponse();
        setField(response, "isBanned", false);
        setField(response, "bannedUntil", "");

        assertFalse(response.isCurrentlyRestricted());
    }

    @Test
    public void isCurrentlyRestricted_returnsTrueWhenTemporaryBanEndsInFuture() throws Exception {
        UserProfileResponse response = new UserProfileResponse();
        setField(response, "isBanned", false);
        setField(response, "bannedUntil", LocalDateTime.now().plusDays(1).toString());

        assertTrue(response.isCurrentlyRestricted());
    }

    @Test
    public void isCurrentlyRestricted_returnsFalseWhenTemporaryBanAlreadyExpired() throws Exception {
        UserProfileResponse response = new UserProfileResponse();
        setField(response, "isBanned", false);
        setField(response, "bannedUntil", LocalDateTime.now().minusDays(1).toString());

        assertFalse(response.isCurrentlyRestricted());
    }

    @Test
    public void isCurrentlyRestricted_returnsFalseWhenBanDateCannotBeParsed() throws Exception {
        UserProfileResponse response = new UserProfileResponse();
        setField(response, "isBanned", false);
        setField(response, "bannedUntil", "not-a-date");

        assertFalse(response.isCurrentlyRestricted());
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
