package com.pacvue.segment.event.entity;

import com.google.auto.value.AutoValue;
import com.segment.analytics.gson.AutoGson;
import com.segment.analytics.messages.*;


@AutoValue
@AutoGson
public abstract class SegmentPersistingMessage implements Message {
    public final static int LOG_OPERATION_SEND_TO_SQS = 1;
    public final static int LOG_OPERATION_SEND_TO_SEGMENT = 2;

    public abstract Message message();

    public abstract boolean result();

    public abstract int operation();

    public abstract String secret();

    public static Builder builder() {
        return new AutoValue_SegmentDataBaseMessage.Builder();
    }

    public Builder toBuilder() {
        return builder().message(message()).result(result()).operation(operation());
    }


    public static class Builder {
        private Message message;
        private boolean result;
        private String secret;
        private int operation = LOG_OPERATION_SEND_TO_SEGMENT;

        public Builder message(Message message) {
            this.message = message;
            return this;
        }

        public Builder result(boolean result) {
            this.result = result;
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

        public SegmentPersistingMessage build() {
            return new AutoValue_SegmentDataBaseMessage(message, result, operation, secret);
        }
    }
}