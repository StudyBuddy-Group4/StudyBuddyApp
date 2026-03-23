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

public interface TaskApi {

    @POST("api/tasks")
    Call<TaskItem> createTask(@Body CreateTaskRequest request);

    @GET("api/tasks")
    Call<List<TaskItem>> getPendingTasks();

    @GET("api/tasks/session/{sessionId}")
    Call<List<TaskItem>> getTasksForSession(@Path("sessionId") long sessionId);

    @PUT("api/tasks/assign")
    Call<String> assignTasksToSession(@Body AssignTasksRequest request);

    @PUT("api/tasks/{id}/complete")
    Call<String> markComplete(@Path("id") long taskId);

    @PUT("api/tasks/{id}/incomplete")
    Call<String> markIncomplete(@Path("id") long taskId);

    @DELETE("api/tasks/{id}")
    Call<String> deleteTask(@Path("id") long taskId);
}
