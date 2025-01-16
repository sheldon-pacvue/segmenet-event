package com.pacvue.segment.event.entity;

import com.google.auto.value.AutoValue;
import com.segment.analytics.gson.AutoGson;
import com.segment.analytics.messages.*;

import java.io.Serializable;


@AutoValue
@AutoGson
public abstract class SegmentLogMessage implements Message, Serializable {
    public final static int LOG_OPERATION_SEND_TO_SQS = 1;
    public final static int LOG_OPERATION_SEND_TO_SEGMENT = 2;

    public abstract Message message();

    public abstract boolean result();

    public abstract String reportApp();

    public abstract int operation();

    public abstract String secret();

    public static Builder builder() {
        return new AutoValue_SegmentLogMessage.Builder();
    }

    public Builder toBuilder() {
        return builder().message(message()).result(result()).operation(operation()).reportApp(reportApp()).secret(secret());
    }


    public static class Builder {
        private Message message;
        private boolean result;
        private String reportApp;
        private int operation = LOG_OPERATION_SEND_TO_SEGMENT;
        private String secret;

        public Builder message(Message message) {
            this.message = message;
            return this;
        }

        public Builder result(boolean result) {
            this.result = result;
            return this;
        }

        public Builder reportApp(String reportApp) {
            this.reportApp = reportApp;
            return this;
        }

        public Builder operation(int operation) {
            this.operation = operation;
            return this;
        }

        public Builder secret(String secret) {
            this.secret = secret;
            return this;
        }

        public SegmentLogMessage build() {
            return new AutoValue_SegmentLogMessage(message, result, reportApp, operation, secret);
        }
    }
}