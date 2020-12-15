package com.segment.analytics.integrations;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class BasicItemPayload extends BasePayload {
    BasicItemPayload(@NonNull String type, @NonNull String itemId, @NonNull Date timestamp, @Nullable Map<String, Object> target) {
        super(type, itemId, timestamp, target);
    }

    public Map<?, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        for (Map.Entry<String, Object> entry : entrySet()) {
            map.put(entry.getKey(), entry.getValue());
        }
        return map;
    }
    @NonNull
    @Override
    public Builder toBuilder() {
        return null;
    }

    public static class Builder extends BasePayload.Builder<BasicItemPayload, BasicItemPayload.Builder> {

        @Override
        BasicItemPayload realBuild(@NonNull String type, @NonNull String itemId, @NonNull Date timestamp, @NonNull String sessionId, @Nullable Map<String, Object> target, @Nullable Map<String, Object> source, @Nullable String profileId, boolean nanosecondTimestamps) {
            return new BasicItemPayload(type, itemId, timestamp, target);
        }

        @Override
        Builder self() {
            return this;
        }
    }
}
