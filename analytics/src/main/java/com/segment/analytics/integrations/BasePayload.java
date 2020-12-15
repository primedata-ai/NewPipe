/**
 * The MIT License (MIT)
 * <p>
 * Copyright (c) 2014 Segment.io, Inc.
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.segment.analytics.integrations;

import static com.segment.analytics.internal.Utils.assertNotNull;
import static com.segment.analytics.internal.Utils.assertNotNullOrEmpty;
import static com.segment.analytics.internal.Utils.immutableCopyOf;
import static com.segment.analytics.internal.Utils.isNullOrEmpty;
import static com.segment.analytics.internal.Utils.parseISO8601DateWithNanos;
import static com.segment.analytics.internal.Utils.toISO8601String;

import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.segment.analytics.AnalyticsContext;
import com.segment.analytics.Properties;
import com.segment.analytics.ValueMap;
import com.segment.analytics.internal.NanoDate;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A payload object that will be sent to the server. Clients will not decode instances of this
 * directly, but through one if it's subclasses.
 */
// This ignores projectId, receivedAt and version that are set by the server.
// sentAt is set on SegmentClient#BatchPayload
public abstract class BasePayload extends ValueMap {

    static final String TYPE_KEY = "eventType";
    static final String ITEM_TYPE_KEY = "itemType";
    public static final String USER_ID_KEY = "profileId";
    public static final String SESSION_ID_KEY = "sessionId";
    static final String CHANNEL_KEY = "channel";
    static final String ITEM_ID = "itemId";
    static final String CONTEXT_KEY = "context";
    static final String INTEGRATIONS_KEY = "integrations";
    static final String TIMESTAMP_KEY = "timeStamp";
    static final String TARGET_KEY = "target";
    static final String SOURCE_KEY = "source";
    static final String PROPERTIES_KEY = "properties";

    private String sessionId = "";
    private String profileId = "";

    BasePayload(
            @NonNull String event,
            @Nullable Date timestamp,
            @NonNull String sessionId,
            @Nullable Map<String, Object> target,
            @Nullable Map<String, Object> source,
            @Nullable String profileId) {
        put(TYPE_KEY, event);
        if (timestamp != null) {
            put(TIMESTAMP_KEY, toISO8601String(timestamp));
        }
        if (target != null) {
            put(TARGET_KEY, target);
        }
        if (source != null) {
            put(SOURCE_KEY, source);
        }
        this.profileId = profileId;
        this.sessionId = sessionId;
    }

    BasePayload(
            @NonNull String type,
            @NonNull String itemId,
            @Nullable Date timestamp,
            @Nullable Map<String, Object> prop
    ) {
        put(ITEM_TYPE_KEY, type);
        put(ITEM_ID, itemId);
        if (timestamp != null) {
            put(TIMESTAMP_KEY, toISO8601String(timestamp));
        }
        if (prop != null) {
            put(PROPERTIES_KEY, prop);
        }
    }

    /**
     * The type of message.
     */
    @NonNull
    public Type type() {
        return getEnum(Type.class, TYPE_KEY);
    }

    /**
     * Event Type
     *
     * @return
     */
    public String eventType() {
        String val = getString(TYPE_KEY);
        if (val == null) {
            return "";
        }
        return val;
    }

    /**
     * The user ID is an identifier that unique identifies the user in your database. Ideally it
     * should not be an email address, because emails can change, whereas a database ID can't.
     */
    @Nullable
    public String profileId() {
        return this.profileId;
    }

    /**
     * The anonymous ID is an identifier that uniquely (or close enough) identifies the user, but
     * isn't from your database. This is useful in cases where you are able to uniquely identifier
     * the user between visits before they sign up thanks to a cookie, or session ID or device ID.
     * In our mobile and browser libraries we will automatically handle sending the anonymous ID.
     */
    @NonNull
    public String sessionId() {
        return this.sessionId;
    }

    /**
     * A randomly generated unique id for this message.
     */
    @NonNull
    public String itemId() {
        return getString(ITEM_ID);
    }

    /**
     * Set a timestamp the event occurred.
     *
     * <p>This library will automatically create and attach a timestamp to all events.
     *
     * @see <a href="https://segment.com/docs/spec/common/#timestamps">Timestamp</a>
     */
    @Nullable
    public Date timestamp() {
        // It's unclear if this will ever be null. So we're being safe.
        String timestamp = getString(TIMESTAMP_KEY);
        if (isNullOrEmpty(timestamp)) {
            return null;
        }
        return parseISO8601DateWithNanos(timestamp);
    }

    /**
     * A dictionary of integration names that the message should be proxied to. 'All' is a special
     * name that applies when no key for a specific integration is found, and is case-insensitive.
     */
    public ValueMap integrations() {
        return getValueMap(INTEGRATIONS_KEY);
    }

    /**
     * Source Object
     *
     * @return ValueMap
     */
    public ValueMap source() {
        return getValueMap(SOURCE_KEY);
    }

    /**
     * Target object
     *
     * @return ValueMap
     */
    public ValueMap target() {
        return getValueMap(TARGET_KEY);
    }

    /**
     * The context is a dictionary of extra information that provides useful context about a
     * message, for example ip address or locale.
     *
     * @see <a href="https://segment.com/docs/spec/common/#context">Context fields</a>
     */
    public AnalyticsContext context() {
        return getValueMap(CONTEXT_KEY, AnalyticsContext.class);
    }

    @Override
    public BasePayload putValue(String key, Object value) {
        super.putValue(key, value);
        return this;
    }

    @NonNull
    public abstract Builder toBuilder();

    public String getItemType() {
        return getString(ITEM_TYPE_KEY);
    }

    /**
     * @see #TYPE_KEY
     */
    public enum Type {
        alias,
        group,
        identify,
        screen,
        track
    }

    /**
     * The channel where the request originated from: server, browser or mobile. In the future we
     * may add additional channels as we add libraries, for example console.
     *
     * <p>This is always {@link Channel#mobile} for us.
     */
    public enum Channel {
        browser,
        mobile,
        server
    }

    public abstract static class Builder<P extends BasePayload, B extends Builder> {

        private String itemId;
        private Date timestamp;
        private Map<String, Object> properties;
        private Map<String, Object> target;
        private Map<String, Object> source;
        private String profileId;
        private String sessionId;
        private boolean nanosecondTimestamps = false;
        private String itemType;
        private String scope = "";

        Builder() {
            // Empty constructor.
        }

        Builder(BasePayload payload) {
            String tsStr = payload.getString(TIMESTAMP_KEY);
            if (tsStr != null
                    && tsStr.length() > 24) { // [yyyy-MM-ddThh:mm:ss.sssZ] format without nanos
                nanosecondTimestamps = true;
            }
            itemId = payload.itemId();
            timestamp = payload.timestamp();
            target = payload.target();
            source = payload.source();
            profileId = payload.profileId();
            sessionId = payload.sessionId();
            itemType = payload.getItemType();
        }

        /**
         * The Message ID is a unique identifier for each message. If not provided, one will be
         * generated for you. This ID is typically used for deduping - messages with the same IDs as
         * previous events may be dropped.
         *
         * @see <a href="https://segment.com/docs/spec/common/">Common Fields</a>
         */
        @NonNull
        public B itemId(@NonNull String itemId) {
            assertNotNullOrEmpty(itemId, "itemId");
            this.itemId = itemId;
            return self();
        }

        /**
         * The Item Type
         */
        @NonNull
        public B itemType(@NonNull String itemType) {
            assertNotNullOrEmpty(itemType, "itemType");
            this.itemType = itemType;
            return self();
        }

        /**
         * The Item Type
         */
        @NonNull
        public B scope(@NonNull String scope) {
            assertNotNullOrEmpty(scope, "scope");
            this.scope = scope;
            return self();
        }

        /**
         * Set a timestamp for the event. By default, the current timestamp is used, but you may
         * override it for historical import.
         *
         * <p>This library will automatically create and attach a timestamp to all events.
         *
         * @see <a href="https://segment.com/docs/spec/common/#timestamps">Timestamp</a>
         */
        @NonNull
        public B timestamp(@NonNull Date timestamp) {
            assertNotNull(timestamp, "timeStamp");
            this.timestamp = timestamp;
            return self();
        }

        /**
         * Set a map of information about the state of the device. You can add any custom data to
         * the context dictionary that you'd like to have access to in the raw logs.
         *
         * <p>Some keys in the context dictionary have semantic meaning and will be collected for
         * you automatically, depending on the library you send data from. Some keys, such as
         * location and speed need to be manually entered.
         *
         * @see <a href="https://segment.com/docs/spec/common/#context">Context</a>
         */
        @NonNull
        public B properties(@NonNull Map<String, Object> properties) {
            this.properties = assertNotNullOrEmpty(properties, "properties");
            return self();
        }

        /**
         * Set a map of information about the state of the device. You can add any custom data to
         * the context dictionary that you'd like to have access to in the raw logs.
         *
         * <p>Some keys in the context dictionary have semantic meaning and will be collected for
         * you automatically, depending on the library you send data from. Some keys, such as
         * location and speed need to be manually entered.
         *
         * @see <a href="https://segment.com/docs/spec/common/#context">Context</a>
         */
        @NonNull
        public B source(@NonNull Map<String, Object> source) {
            this.source = assertNotNullOrEmpty(source, "source");
            return self();
        }

        /**
         * Set whether this message is sent to the specified integration or not. 'All' is a special
         * key that applies when no key for a specific integration is found.
         *
         * @see <a href="https://segment.com/docs/spec/common/#integrations">Integrations</a>
         */
        @NonNull
        public B integration(@NonNull String key, boolean enable) {
            return self();
        }

        /**
         * Pass in some options that will only be used by the target integration. This will
         * implicitly mark the integration as enabled.
         *
         * @see <a href="https://segment.com/docs/spec/common/#integrations">Integrations</a>
         */
        @NonNull
        public B integration(@NonNull String key, @NonNull Map<String, Object> options) {
            return self();
        }

        /**
         * Specify a dictionary of options for integrations.
         *
         * @see <a href="https://segment.com/docs/spec/common/#integrations">Integrations</a>
         */
        @NonNull
        public B integrations(@Nullable Map<String, ?> integrations) {
            return self();
        }

        /**
         * The Anonymous ID is a pseudo-unique substitute for a User ID, for cases when you don't
         * have an absolutely unique identifier.
         *
         * @see <a href="https://segment.com/docs/spec/identify/#identities">Identities</a>
         * @see <a href="https://segment.com/docs/spec/identify/#anonymous-id">Anonymous ID</a>
         */
        @NonNull
        public B sessionId(@NonNull String sessionId) {
            this.sessionId = assertNotNullOrEmpty(sessionId, "sessionId");
            return self();
        }

        @NonNull
        public B target(@NonNull Map<String, Object> target) {
            this.target = assertNotNullOrEmpty(target, "target");
            return self();
        }

        /**
         * The User ID is a persistent unique identifier for a user (such as a database ID).
         *
         * @see <a href="https://segment.com/docs/spec/identify/#identities">Identities</a>
         * @see <a href="https://segment.com/docs/spec/identify/#user-id">User ID</a>
         */
        @NonNull
        public B profileId(@Nullable String profileId) {
            if (profileId != null && profileId.length() > 0) {
                this.profileId = assertNotNullOrEmpty(profileId, "profileId");
            }
            return self();
        }

        /**
         * Returns true if profileId is not-null or non-empty, false otherwise
         */
        public boolean isprofileIdSet() {
            return !isNullOrEmpty(profileId);
        }

        public B nanosecondTimestamps(boolean enabled) {
            this.nanosecondTimestamps = enabled;
            return self();
        }

        abstract P realBuild(
                @NonNull String type,
                @NonNull String itemId,
                @Nullable Date timestamp,
                @NonNull String sessionId,
                @Nullable Map<String, Object> target,
                @Nullable Map<String, Object> source,
                @Nullable String profileId,
                boolean nanosecondTimestamps);

        abstract B self();

        /**
         * Create a {@link BasePayload} instance.
         */
        @CheckResult
        @NonNull
        public P build() {
            if (isNullOrEmpty(itemId)) {
                itemId = UUID.randomUUID().toString();
            }

            P p = realBuild(
                    itemType,
                    itemId,
                    timestamp,
                    sessionId,
                    target,
                    source,
                    profileId,
                    nanosecondTimestamps);
            if (this.properties != null) {
                p.put(PROPERTIES_KEY, properties);
            }

            if (this.scope.length() > 0) {
                p.put("scope", this.scope);
            }
            return p;
        }
    }
}
