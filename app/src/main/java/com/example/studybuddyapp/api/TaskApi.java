package com.example.studybuddyapp.api;

import com.example.studybuddyapp.api.dto.AssignTasksRequest;
import com.example.studybuddyapp.api.dto.CreateTaskRequest;
import com.example.studybuddyapp.api.dto.TaskItem;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

/**
 * Task-management endpoints.
 */
public interface TaskApi {

    // Create a task
    @POST("api/tasks")
    Call<TaskItem> createTask(@Body CreateTaskRequest request);

    // Load pending tasks
    @GET("api/tasks")
    Call<List<TaskItem>> getPendingTasks();

    // Load tasks for one session
    @GET("api/tasks/session/{sessionId}")
    Call<List<TaskItem>> getTasksForSession(@Path("sessionId") long sessionId);

    // Link pending tasks to a session
    @PUT("api/tasks/assign")
    Call<String> assignTasksToSession(@Body AssignTasksRequest request);

    // Mark a task complete
    @PUT("api/tasks/{id}/complete")
    Call<String> markComplete(@Path("id") long taskId);

    // Mark a task incomplete
    @PUT("api/tasks/{id}/incomplete")
    Call<String> markIncomplete(@Path("id") long taskId);

    // Delete a task
    @DELETE("api/tasks/{id}")
    Call<String> deleteTask(@Path("id") long taskId);
}
