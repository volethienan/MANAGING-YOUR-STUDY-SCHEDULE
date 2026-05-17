package com.example.cuoiky_qllichhoctap.model;

import org.json.JSONException;
import org.json.JSONObject;

public class UserProfile {
    private String name;
    private String email;
    private String goal;

    public UserProfile(String name, String email, String goal) {
        this.name = name;
        this.email = email;
        this.goal = goal;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getGoal() {
        return goal;
    }

    public void setGoal(String goal) {
        this.goal = goal;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("name", name);
        json.put("email", email);
        json.put("goal", goal);
        return json;
    }

    public static UserProfile fromJson(JSONObject json) {
        return new UserProfile(
                json.optString("name", "Minh Anh"),
                json.optString("email", "student@email.com"),
                json.optString("goal", "Quản lý lịch học và deadline")
        );
    }
}
