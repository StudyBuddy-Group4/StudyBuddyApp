package com.example.studybuddyapp.api.dto;

import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DtoConstructionTest {

    @Test
    public void assignTasksRequest_storesSessionId() throws Exception {
        AssignTasksRequest request = new AssignTasksRequest(55L);

        assertEquals(55L, request.getSessionId());
    }

    @Test
    public void createTaskRequest_storesTitleAndNote() {
        CreateTaskRequest request = new CreateTaskRequest("Write summary", "Use chapter 4");

        assertEquals("Write summary", request.getTitle());
        assertEquals("Use chapter 4", request.getNote());
    }

    @Test
    public void startSessionRequest_storesDurationAndChannelName() {
        StartSessionRequest request = new StartSessionRequest(30, "study_30");

        assertEquals(30, request.getDurationMinutes());
        assertEquals("study_30", request.getChannelName());
    }

    @Test
    public void deleteAccountRequest_storesPassword() throws Exception {
        DeleteAccountRequest request = new DeleteAccountRequest("secret");

        assertEquals("secret", getField(request, "password"));
    }

    @Test
    public void loginRequest_storesUsernameOrEmailAndPassword() throws Exception {
        LoginRequest request = new LoginRequest("alice@example.com", "pw123");

        assertEquals("alice@example.com", getField(request, "usernameOrEmail"));
        assertEquals("pw123", getField(request, "password"));
    }

    @Test
    public void registerRequest_storesProvidedValues() throws Exception {
        RegisterRequest request = new RegisterRequest("alice", "alice@example.com", "pw123");

        assertEquals("alice", getField(request, "username"));
        assertEquals("alice@example.com", getField(request, "email"));
        assertEquals("pw123", getField(request, "password"));
    }

    @Test
    public void updateProfileRequest_storesUsernameAndEmail() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest("new-name", "new@example.com");

        assertEquals("new-name", getField(request, "username"));
        assertEquals("new@example.com", getField(request, "email"));
    }

    @Test
    public void reportRequest_storesAllFields() throws Exception {
        ReportRequest request = new ReportRequest(1L, 2L, "meeting-1", "Noise", 123456789L);

        assertEquals(1L, getField(request, "reportingUserId"));
        assertEquals(2L, getField(request, "reportedUserId"));
        assertEquals("meeting-1", getField(request, "meetingId"));
        assertEquals("Noise", getField(request, "reason"));
        assertEquals(123456789L, getField(request, "timestamp"));
    }

    @Test
    public void taskItem_setCompleted_updatesCompletedFlag() throws Exception {
        TaskItem taskItem = new TaskItem();

        taskItem.setCompleted(true);

        assertEquals(Boolean.TRUE, getField(taskItem, "completed"));
    }

    @Test
    public void createTaskRequest_allowsNullNote() {
        CreateTaskRequest request = new CreateTaskRequest("Read notes", null);

        assertEquals("Read notes", request.getTitle());
        assertNull(request.getNote());
    }

    private Object getField(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }
}
