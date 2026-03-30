package com.example.studybuddyapp.api.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.lang.reflect.Field;

public class ResponseDtoGetterTest {

    @Test
    public void loginResponse_gettersReturnStoredValues() throws Exception {
        LoginResponse response = new LoginResponse();
        setField(response, "token", "jwt-1");
        setField(response, "userId", 17L);
        setField(response, "username", "alice");
        setField(response, "isAdmin", true);

        assertEquals("jwt-1", response.getToken());
        assertEquals(Long.valueOf(17L), response.getUserId());
        assertEquals("alice", response.getUsername());
        assertTrue(response.isAdmin());
    }

    @Test
    public void sessionStatistics_gettersReturnStoredValues() throws Exception {
        SessionStatistics statistics = new SessionStatistics();
        setField(statistics, "rating", 4.8d);
        setField(statistics, "totalFocusTimeMinutes", 180L);
        setField(statistics, "completedCount", 5L);
        setField(statistics, "totalCount", 7L);

        assertEquals(4.8d, statistics.getRating(), 0.0);
        assertEquals(180L, statistics.getTotalFocusTimeMinutes());
        assertEquals(5L, statistics.getCompletedCount());
        assertEquals(7L, statistics.getTotalCount());
    }

    @Test
    public void joinMeetingResponse_gettersReturnStoredValues() throws Exception {
        JoinMeetingResponse response = new JoinMeetingResponse();
        setField(response, "meetingId", "m-1");
        setField(response, "channelName", "focus-room");
        setField(response, "duration", 30);

        assertEquals("m-1", response.getMeetingId());
        assertEquals("focus-room", response.getChannelName());
        assertEquals(30, response.getDuration());
    }

    @Test
    public void leaveMeetingResponse_gettersReturnStoredValues() throws Exception {
        LeaveMeetingResponse response = new LeaveMeetingResponse();
        setField(response, "success", true);
        setField(response, "message", "left");

        assertTrue(response.isSuccess());
        assertEquals("left", response.getMessage());
    }

    @Test
    public void startSessionResponse_getterReturnsStoredSessionId() throws Exception {
        StartSessionResponse response = new StartSessionResponse();
        setField(response, "sessionId", 91L);

        assertEquals(91L, response.getSessionId());
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
