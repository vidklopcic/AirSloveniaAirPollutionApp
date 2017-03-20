package com.vidklopcic.airsense.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vidklopcic.airsense.data.Gson.Measurement;
import com.vidklopcic.airsense.data.Gson.MeasurementRangeParams;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Created by vidklopcic on 19/03/2017.
 */

public class API {
    public static API.AirSense initApi() {
        Gson gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
                .create();


        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Constants.API.base_url)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
        return retrofit.create(API.AirSense.class);
    }

    public interface AirSense {
        // Request method and URL specified in the annotation
        // Callback for the parsed response is the last parameter
        @GET("station/{id}/range")
        Call<Measurement[]> getMeasurementsRange(@Path("id") String station_id, @Query("start") String start, @Query("end") String end);

        @GET("station/{id}/last")
        Call<Measurement> getLastMeasurement(@Path("id") String station_id);
    }
}
