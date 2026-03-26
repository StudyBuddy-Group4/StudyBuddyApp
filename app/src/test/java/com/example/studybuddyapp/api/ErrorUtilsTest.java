package com.example.studybuddyapp.api;

import org.junit.Test;

import okhttp3.MediaType;
import okhttp3.ResponseBody;
import retrofit2.Response;

import static org.junit.Assert.assertEquals;

public class ErrorUtilsTest {

    @Test
    public void parseError_returnsFallbackWhenErrorBodyIsMissing() {
        Response<String> response = Response.error(
                400,
                ResponseBody.create(MediaType.get("text/plain"), "")
        );

        String message = ErrorUtils.parseError(response, "Fallback message");

        assertEquals("Fallback message", message);
    }

    @Test
    public void parseError_extractsMessageInsideDefaultMessageWrapper() {
        String errorBody = "{\"message\":\"default message [Email is already in use]\"}";
        Response<String> response = Response.error(
                400,
                ResponseBody.create(MediaType.get("application/json"), errorBody)
        );

        String message = ErrorUtils.parseError(response, "Fallback message");

        assertEquals("Email is already in use", message);
    }

    @Test
    public void parseError_returnsShortPlainTextBodyWhenReadable() {
        Response<String> response = Response.error(
                401,
                ResponseBody.create(MediaType.get("text/plain"), "Invalid password")
        );

        String message = ErrorUtils.parseError(response, "Fallback message");

        assertEquals("Invalid password", message);
    }

    @Test
    public void parseError_returnsFallbackWhenBodyIsTooLong() {
        String longBody = "x".repeat(220);
        Response<String> response = Response.error(
                500,
                ResponseBody.create(MediaType.get("text/plain"), longBody)
        );

        String message = ErrorUtils.parseError(response, "Fallback message");

        assertEquals("Fallback message", message);
    }
}
