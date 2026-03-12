package com.example.studybuddyapp.api;

import android.content.Context;

import com.example.studybuddyapp.SessionManager;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public final class ApiClient {

    private static Retrofit retrofit;

    private ApiClient() {}

    public static Retrofit getInstance(Context context) {
        if (retrofit == null) {
            SessionManager session = new SessionManager(context.getApplicationContext());

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(chain -> {
                        Request original = chain.request();
                        Request.Builder builder = original.newBuilder();
                        String token = session.getToken();
                        if (token != null && !token.isEmpty()) {
                            builder.header("Authorization", "Bearer " + token);
                        }
                        return chain.proceed(builder.build());
                    })
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(ApiConfig.BASE_URL)
                    .client(client)
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    public static AuthApi getAuthApi(Context context) {
        return getInstance(context).create(AuthApi.class);
    }

    public static UserApi getUserApi(Context context) {
        return getInstance(context).create(UserApi.class);
    }

    public static void resetInstance() {
        retrofit = null;
    }
}
