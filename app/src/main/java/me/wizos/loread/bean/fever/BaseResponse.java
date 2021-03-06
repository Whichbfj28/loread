package me.wizos.loread.bean.fever;

import com.google.gson.annotations.SerializedName;

public class BaseResponse {
    @SerializedName("api_version")
    private int apiVersion;
    @SerializedName("auth")
    private int auth; // 为 1 时，代表验证/授权成功
    @SerializedName("last_refreshed_on_time")
    private long lastRefreshedOnTime;
    @SerializedName("error")
    private String error; // NOT_LOGGED_IN

    public int getApiVersion() {
        return apiVersion;
    }

    public int getAuth() {
        return auth;
    }

    public long getLastRefreshedOnTime() {
        return lastRefreshedOnTime;
    }


    public boolean isSuccessful(){
        return auth == 1;
    }
}
