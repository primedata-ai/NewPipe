package com.segment.analytics;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.segment.analytics.integrations.BasicItemPayload;
import com.segment.analytics.internal.Utils;

import java.time.Instant;
import java.util.UUID;

/**
 * Provide session persistence layer
 */
public class SessionPersistence implements Analytics.SessionManagerInterface, AnalyticsActivityLifecycleCallbacks.AppCycleListener {

    public static final String SESSION_ID = "session-id";

    private static final String TAG = "prime-data-session";

    public static final String PROFILE_ID = "profile-id";

    private final Application application;
    private String sessionId = "";
    private String profileId = "";
    private String sourceKey = "";

    private final AnalyticsContext context;

    public SessionPersistence(Application application, AnalyticsContext context, String sourceKey) {
        this.application = application;
        this.context = context;
        this.sourceKey = sourceKey;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void reOpen() {
        long lastOpen = getLastNano();
        long interval = Instant.now().getEpochSecond() - lastOpen;

        if (interval > 30 * 60) {
            sessionId = UUID.randomUUID().toString();
            setSessionID(sessionId);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void onClose() {
        updateLastClose();
    }

    @Override
    public String getSessionID() {
        if (sessionId.length() > 0) {
            return sessionId;
        }
        sessionId = getValue(SESSION_ID);

        if (sessionId.length() == 0) {
            sessionId = UUID.randomUUID().toString();
            setSessionID(sessionId);
        }
        return sessionId;
    }

    @Override
    public String getProfileID() {
        if (this.profileId.length() > 0) {
            return this.profileId;
        }
        profileId = getValue(PROFILE_ID);
        return profileId;
    }

    @Override
    public BasicItemPayload getSource(boolean includeContext) {
        BasicItemPayload.Builder builder = new BasicItemPayload.Builder();
        builder.itemId(this.application.getApplicationInfo().packageName).itemType("app").scope(this.sourceKey);
        if (includeContext) {
            builder.properties(context);
        }
        return builder.build();
    }

    @Override
    public void setProfileID(String profileID) {
        this.profileId = profileID;
        setValue(PROFILE_ID, profileID);
    }

    public String scope() {
        return this.sourceKey;
    }

    private void setSessionID(String sessionID) {
        setValue(SESSION_ID, sessionID);
    }

    private String getValue(String id) {
        SharedPreferences sharedPreferences = Utils.getSegmentSharedPreferences(application, TAG);
        return sharedPreferences.getString(id, "");
    }

    private void setValue(String id, String value) {
        SharedPreferences sharedPreferences = Utils.getSegmentSharedPreferences(application, TAG);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(id, value);
        editor.apply();
    }

    /**
     * Update last Nano
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void updateLastClose() {
        SharedPreferences sharedPreferences = Utils.getSegmentSharedPreferences(application, TAG);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Instant now = Instant.now();
        editor.putLong("cycle", now.getEpochSecond());
        editor.apply();
    }

    private long getLastNano() {
        SharedPreferences sharedPreferences = Utils.getSegmentSharedPreferences(application, TAG);
        return sharedPreferences.getLong("cycle", 0);
    }
}
