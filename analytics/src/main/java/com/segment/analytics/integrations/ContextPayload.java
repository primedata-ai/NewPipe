package com.segment.analytics.integrations;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.segment.analytics.internal.Private;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.segment.analytics.internal.Utils.assertNotNullOrEmpty;
import static com.segment.analytics.internal.Utils.isNullOrEmpty;

public class ContextPayload extends BasePayload {
    ContextPayload(@NonNull String event, @NonNull Date timestamp, @NonNull String sessionId, @Nullable Map<String, Object> target, @Nullable Map<String, Object> source, @Nullable String profileId) {
        super(event, timestamp, sessionId, target, source, profileId);
    }

    @NonNull
    @Override
    public Builder toBuilder() {
        return new Builder(this);
    }


    /**
     * Fluent API for creating {@link TrackPayload} instances.
     */
    public static class Builder extends BasePayload.Builder<ContextPayload, ContextPayload.Builder> {

        private String event;
        private Map<String, Object> properties;
        private Map<String, Object> traits;

        public Builder() {
            this.event = "open_app";
        }

        @Private
        Builder(ContextPayload track) {
            super(track);
        }

        @NonNull
        public ContextPayload.Builder event(@NonNull String event) {
            this.event = event;
            return this;
        }

        public ContextPayload.Builder traits(@NonNull Map<String, Object> traits) {
            this.traits = traits;
            return this;
        }

        @NonNull
        public ContextPayload.Builder properties(@NonNull Map<String, Object> properties) {
            this.properties = Collections.unmodifiableMap(new LinkedHashMap<>(properties));
            return this;
        }

        @Override
        protected ContextPayload realBuild(
                @NonNull String type,
                @NonNull String itemId,
                @NonNull Date timestamp,
                @NonNull String sessionId,
                @Nullable Map<String, Object> target,
                @Nullable Map<String, Object> source,
                @Nullable String profileId,
                boolean nanosecondTimestamps) {
            Map<String, Object> properties = this.properties;
            if (isNullOrEmpty(properties)) {
                properties = Collections.emptyMap();
            }
            ContextPayload payload = new ContextPayload(this.event, timestamp, sessionId, target, source, profileId);
            payload.put(PROPERTIES_KEY, properties);

            return payload;
        }

        @Override
        ContextPayload.Builder self() {
            return this;
        }

        public Map<String, Object> getTraits() {
            return traits;
        }
    }
}
