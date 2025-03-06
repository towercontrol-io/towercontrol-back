package com.disk91.common.interfaces.chirpstack;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiErrorResponse {
    /**
     *
     * {
     *   "code": 13,
     *   "message": "duplicate key value violates unique constraint \"device_pkey\"",
     *   "details": []
     *  }
     *
     */

    @JsonProperty("code")
    private int code;

    @JsonProperty("message")
    private String message;

    // Getters and Setters


    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
