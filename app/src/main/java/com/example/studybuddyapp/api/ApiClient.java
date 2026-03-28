package com.example.studybuddyapp.api;

import android.content.Context;

import com.example.studybuddyapp.SessionManager;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

/**
 * Creates and exposes the shared Retrofit client used by the app.
 */
public final class ApiClient {

    // Shared Retrofit instance
    private static Retrofit retrofit;

    // No instances
    private ApiClient() {}

    /**
     * Returns the shared Retrofit instance.
     */
    public static Retrofit getInstance(Context context) {
        if (retrofit == null) {
            // Read the current stored token
            SessionManager session = new SessionManager(context.getApplicationContext());

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(chain -> {
                        // Start from the original request
                        Request original = chain.request();
                        Request.Builder builder = original.newBuilder();
                        // Add the auth header only when a token exists
                        String token = session.getToken();
                        if (token != null && !token.isEmpty()) {
                            builder.header("Authorization", "Bearer " + token);
                        }
                        // Continue with the final request
                        return chain.proceed(builder.build());
                    })
                    .build();

            // Scalars are registered before Gson so plain-string responses still work
            retrofit = new Retrofit.Builder()
                    .baseUrl(ApiConfig.BASE_URL)
                    .client(client)
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    /**
     * Returns the auth API service.
     */
    public static AuthApi getAuthApi(Context context) {
        return getInstance(context).create(AuthApi.class);
    }

    /**
     * Returns the user API service.
     */
    public static UserApi getUserApi(Context context) {
        return getInstance(context).create(UserApi.class);
    }

    /**
     * Returns the moderation API service.
     */
    public static ModerationApi getModerationApi(Context context) {
        return getInstance(context).create(ModerationApi.class);
    }

    /**
     * Returns the session API service.
     */
    public static SessionApi getSessionApi(Context context) {
        return getInstance(context).create(SessionApi.class);
    }

    /**
     * Returns the task API service.
     */
    public static TaskApi getTaskApi(Context context) {
        return getInstance(context).create(TaskApi.class);
    }

    /**
     * Returns the matching API service.
     */
    public static MatchingApi getMatchingApi(Context context) {
        return getInstance(context).create(MatchingApi.class);
    }

    /**
     * Clears the cached Retrofit instance.
     */
    public static void resetInstance() {
        retrofit = null;
    }
}
