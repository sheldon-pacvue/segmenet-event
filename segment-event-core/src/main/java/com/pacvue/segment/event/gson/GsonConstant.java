package com.pacvue.segment.event.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.segment.analytics.gson.AutoValueAdapterFactory;
import com.segment.analytics.gson.ISO8601DateAdapter;

import java.util.Date;

public interface GsonConstant {
    Gson gson = new GsonBuilder()
            .registerTypeAdapterFactory(new AutoValueAdapterFactory())
            .registerTypeAdapter(Date.class, new ISO8601DateAdapter())
            .create();
}
