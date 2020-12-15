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
package com.segment.analytics;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import com.segment.analytics.integrations.Logger;
import com.segment.analytics.internal.Private;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import static android.Manifest.permission.ACCESS_NETWORK_STATE;
import static android.content.Context.CONNECTIVITY_SERVICE;
import static android.content.Context.TELEPHONY_SERVICE;
import static android.net.ConnectivityManager.TYPE_BLUETOOTH;
import static android.net.ConnectivityManager.TYPE_MOBILE;
import static com.segment.analytics.internal.Utils.getDeviceId;
import static com.segment.analytics.internal.Utils.getSystemService;
import static com.segment.analytics.internal.Utils.hasPermission;
import static com.segment.analytics.internal.Utils.isNullOrEmpty;
import static com.segment.analytics.internal.Utils.isOnClassPath;
import static java.util.Collections.unmodifiableMap;

/**
 * Context is a dictionary of free-form information about the state of the device. Context is
 * attached to every outgoing call. You can add any custom data to the context dictionary that you'd
 * like to have access to in the raw logs.
 *
 * <p>This is renamed to AnalyticsContext on Android to avoid confusion with {@link Context} in the
 * Android framework. Any documentation for Context on our website is referring to AnalyticsContext
 * on Android.
 *
 * <p>Some keys in the context dictionary have semantic meaning and will be collected for you
 * automatically, depending on the library you send data from. Some keys, such as IP address, and
 * speed need to be manually entered, such as IP Address, speed, etc.
 *
 * <p>AnalyticsContext is not persisted to disk, and is filled each time the app starts.
 *
 * <p>On the topic of the thread safety of traits, whilst this class utilises LinkedHashMap, writes
 * to this map only occur from within the handler thread. Meanwhile reads are served immutable
 * copies of traits mitigating the risk of data races with the exception of clients of this library
 * modifying nested data structures after passing them to this library. This concern could be
 * mitigated by deep rather than shallow copying (e.g. via de-serialiation and re-serialisation),
 * however this would contribute a performance penalty.
 */
public class AnalyticsContext extends ValueMap {

    private static final String LOCALE_KEY = "locale";
    private static final String TRAITS_KEY = "traits";
    private static final String USER_AGENT_KEY = "userAgent";
    private static final String TIMEZONE_KEY = "timezone";
    // App
    private static final String APP_NAME_KEY = "name";
    private static final String APP_VERSION_KEY = "version";
    private static final String APP_NAMESPACE_KEY = "namespace";
    private static final String APP_BUILD_KEY = "build";
    // Campaign
    private static final String CAMPAIGN_KEY = "campaign";
    // Device
    private static final String DEVICE_KEY = "device";
    // Library
    private static final String LIBRARY_NAME_KEY = "library_name";
    private static final String LIBRARY_VERSION_KEY = "library_version";
    // Location
    private static final String LOCATION_KEY = "location";
    // Network
    private static final String NETWORK_BLUETOOTH_KEY = "network_bluetooth";
    private static final String NETWORK_CARRIER_KEY = "network_carrier";
    private static final String NETWORK_CELLULAR_KEY = "network_cellular";
    private static final String NETWORK_WIFI_KEY = "network_wifi";
    // OS
    private static final String OS_KEY = "os";
    private static final String OS_NAME_KEY = "os_name";
    private static final String OS_VERSION_KEY = "os_version";
    // Referrer
    private static final String REFERRER_KEY = "referrer";
    // Screen
    private static final String SCREEN_DENSITY_KEY = "screen_density";
    private static final String SCREEN_HEIGHT_KEY = "screen_height";
    private static final String SCREEN_WIDTH_KEY = "screen_width";

    /**
     * Create a new {@link AnalyticsContext} instance filled in with information from the given
     * {@link Context}. The {@link Analytics} client can be called from anywhere, so the returned
     * instances is thread safe.
     */
    static synchronized AnalyticsContext create(
            Context context, Traits traits, boolean collectDeviceId) {
        AnalyticsContext analyticsContext =
                new AnalyticsContext(new ConcurrentHashMap<>());
        analyticsContext.putApp(context);
        analyticsContext.putDevice(context, collectDeviceId);
        analyticsContext.putLibrary();
        analyticsContext.put(
                LOCALE_KEY,
                Locale.getDefault().getLanguage() + "-" + Locale.getDefault().getCountry());
        analyticsContext.putNetwork(context);
        analyticsContext.putOs();
        analyticsContext.putScreen(context);
        putUndefinedIfNull(analyticsContext, USER_AGENT_KEY, System.getProperty("http.agent"));
        putUndefinedIfNull(analyticsContext, TIMEZONE_KEY, TimeZone.getDefault().getID());
        return analyticsContext;
    }

    static void putUndefinedIfNull(Map<String, Object> target, String key, CharSequence value) {
        if (isNullOrEmpty(value)) {
            target.put(key, "undefined");
        } else {
            target.put(key, value);
        }
    }

    // For deserialization and wrapping
    AnalyticsContext(Map<String, Object> delegate) {
        super(delegate);
    }

    void attachAdvertisingId(Context context, CountDownLatch latch, Logger logger) {
        // This is done as an extra step so we don't run into errors like this for testing
        // http://pastebin.com/gyWJKWiu.
        if (isOnClassPath("com.google.android.gms.ads.identifier.AdvertisingIdClient")) {
            // This needs to be done each time since the settings may have been updated.
            new GetAdvertisingIdTask(this, latch, logger).execute(context);
        } else {
            logger.debug(
                    "Not collecting advertising ID because "
                            + "com.google.android.gms.ads.identifier.AdvertisingIdClient "
                            + "was not found on the classpath.");
            latch.countDown();
        }
    }

    @Override
    public AnalyticsContext putValue(String key, Object value) {
        super.putValue(key, value);
        return this;
    }

    /**
     * Returns an unmodifiable shallow copy of the values in this map.
     */
    public AnalyticsContext unmodifiableCopy() {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>(this);
        return new AnalyticsContext(unmodifiableMap(map));
    }

    /**
     * Attach a copy of the given {@link Traits} to this instance. This creates a copy of the given
     * {@code traits}, so exposing {@link #traits()} to the public API is acceptable.
     */
    void setTraits(Traits traits) {
        put(TRAITS_KEY, traits.unmodifiableCopy());
    }

    /**
     * Note: Not for public use. Clients should modify the user's traits with {@link
     * Analytics#identify(String, Traits, Options)}. Modifying this instance will not reflect
     * changes to the user's information that is passed onto bundled integrations.
     *
     * <p>Return the {@link Traits} attached to this instance.
     */
    public Traits traits() {
        return getValueMap(TRAITS_KEY, Traits.class);
    }

    /**
     * Fill this instance with application info from the provided {@link Context}. No need to expose
     * a getter for this for bundled integrations (they'll automatically fill what they need
     * themselves).
     */
    void putApp(Context context) {
        try {
            PackageManager packageManager = context.getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
//            put(APP_NAME_KEY, packageInfo.applicationInfo.loadLabel(packageManager));
//            put(APP_VERSION_KEY, packageInfo.versionName);
//            put(APP_NAMESPACE_KEY, packageInfo.packageName);
            put(APP_BUILD_KEY, String.valueOf(packageInfo.versionCode));
        } catch (PackageManager.NameNotFoundException e) {
            // ignore
        }
    }

    /**
     * Set information about the campaign that resulted in the API call.
     */
    public AnalyticsContext putCampaign(Campaign campaign) {
        return putValue(CAMPAIGN_KEY, campaign);
    }

    public Campaign campaign() {
        return getValueMap(CAMPAIGN_KEY, Campaign.class);
    }

    /**
     * Fill this instance with device info from the provided {@link Context}.
     */
    void putDevice(Context context, boolean collectDeviceID) {
        String identifier = collectDeviceID ? getDeviceId(context) : traits().anonymousId();
        put(Device.DEVICE_ID_KEY, identifier);
        put(Device.DEVICE_MANUFACTURER_KEY, Build.MANUFACTURER);
//        put(Device.DEVICE_MODEL_KEY, Build.MODEL);
        put(Device.DEVICE_NAME_KEY, Build.DEVICE);
    }

    public Device device() {
        return getValueMap(DEVICE_KEY, Device.class);
    }

    /**
     * Set a device token. Convenience method for {@link Device#putDeviceToken(String)}
     */
    public AnalyticsContext putDeviceToken(String token) {
        device().putDeviceToken(token);
        return this;
    }

    /**
     * Fill this instance with library information.
     */
    void putLibrary() {
//        put(LIBRARY_NAME_KEY, "analytics-android");
//        put(LIBRARY_VERSION_KEY, BuildConfig.VERSION_NAME);
    }

    /**
     * Set location information about the device.
     */
    public AnalyticsContext putLocation(Location location) {
        return putValue(LOCATION_KEY, location);
    }

    public Location location() {
        return getValueMap(LOCATION_KEY, Location.class);
    }

    /**
     * Fill this instance with network information. No need to expose a getter for this for bundled
     * integrations (they'll automatically fill what they need themselves).
     */
    @SuppressLint("MissingPermission")
    void putNetwork(Context context) {
        if (hasPermission(context, ACCESS_NETWORK_STATE)) {
            ConnectivityManager connectivityManager =
                    getSystemService(context, CONNECTIVITY_SERVICE);
            if (connectivityManager != null) {
//                NetworkInfo wifiInfo = connectivityManager.getNetworkInfo(TYPE_WIFI);
//                put(NETWORK_WIFI_KEY, wifiInfo != null && wifiInfo.isConnected());
                NetworkInfo bluetoothInfo = connectivityManager.getNetworkInfo(TYPE_BLUETOOTH);
                put(
                        NETWORK_BLUETOOTH_KEY,
                        bluetoothInfo != null && bluetoothInfo.isConnected());
                NetworkInfo cellularInfo = connectivityManager.getNetworkInfo(TYPE_MOBILE);
                put(
                        NETWORK_CELLULAR_KEY, cellularInfo != null && cellularInfo.isConnected());
            }
        }

        TelephonyManager telephonyManager = getSystemService(context, TELEPHONY_SERVICE);
        if (telephonyManager != null) {
            put(NETWORK_CARRIER_KEY, telephonyManager.getNetworkOperatorName());
        } else {
            put(NETWORK_CARRIER_KEY, "unknown");
        }
    }

    /**
     * Fill this instance with operating system information.
     */
    void putOs() {
//        put(OS_NAME_KEY, "Android");
        put(OS_VERSION_KEY, Build.VERSION.RELEASE);
    }

    /**
     * Set the referrer for this session.
     */
    public AnalyticsContext putReferrer(Referrer referrer) {
        return putValue(REFERRER_KEY, referrer);
    }

    /**
     * Fill this instance with application info from the provided {@link Context}. No need to expose
     * a getter for this for bundled integrations (they'll automatically fill what they need
     * themselves).
     */
    void putScreen(Context context) {
        WindowManager manager = getSystemService(context, Context.WINDOW_SERVICE);
        Display display = manager.getDefaultDisplay();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        display.getMetrics(displayMetrics);
        put(SCREEN_DENSITY_KEY, displayMetrics.density);
        put(SCREEN_HEIGHT_KEY, displayMetrics.heightPixels);
        put(SCREEN_WIDTH_KEY, displayMetrics.widthPixels);
    }

    /**
     * Information about the campaign that resulted in the API call, containing name, source,
     * medium, term and content. This maps directly to the common UTM campaign parameters.
     *
     * @see <a href="https://support.google.com/analytics/answer/1033867?hl=en">UTM parameters</a>
     */
    public static class Campaign extends ValueMap {

        private static final String CAMPAIGN_NAME_KEY = "name";
        private static final String CAMPAIGN_SOURCE_KEY = "source";
        private static final String CAMPAIGN_MEDIUM_KEY = "medium";
        private static final String CAMPAIGN_TERM_KEY = "term";
        private static final String CAMPAIGN_CONTENT_KEY = "content";

        // Public Constructor
        public Campaign() {
        }

        // For deserialization
        private Campaign(Map<String, Object> map) {
            super(map);
        }

        @Override
        public Campaign putValue(String key, Object value) {
            super.putValue(key, value);
            return this;
        }

        /**
         * Set the UTM campaign name.
         */
        public Campaign putName(String name) {
            return putValue(CAMPAIGN_NAME_KEY, name);
        }

        public String name() {
            return getString(CAMPAIGN_NAME_KEY);
        }

        /**
         * Set the UTM campaign source.
         */
        public Campaign putSource(String source) {
            return putValue(CAMPAIGN_SOURCE_KEY, source);
        }

        public String source() {
            return getString(CAMPAIGN_SOURCE_KEY);
        }

        /**
         * Set the UTM campaign medium.
         */
        public Campaign putMedium(String medium) {
            return putValue(CAMPAIGN_MEDIUM_KEY, medium);
        }

        public String medium() {
            return getString(CAMPAIGN_MEDIUM_KEY);
        }

        /**
         * Set the UTM campaign term.
         */
        public Campaign putTerm(String term) {
            return putValue(CAMPAIGN_TERM_KEY, term);
        }

        /**
         * @deprecated Use {@link #term()} instead.
         */
        public String tern() {
            return term();
        }

        public String term() {
            return getString(CAMPAIGN_TERM_KEY);
        }

        /**
         * Set the UTM campaign content.
         */
        public Campaign putContent(String content) {
            return putValue(CAMPAIGN_CONTENT_KEY, content);
        }

        public String content() {
            return getString(CAMPAIGN_CONTENT_KEY);
        }
    }

    /**
     * Information about the device.
     */
    public static class Device extends ValueMap {

        @Private
        public static final String DEVICE_ID_KEY = "device_id";
        @Private
        static final String DEVICE_MANUFACTURER_KEY = "deviceBrand";
        @Private
        static final String DEVICE_MODEL_KEY = "deviceCategory";
        @Private
        static final String DEVICE_NAME_KEY = "deviceName";
        @Private
        static final String DEVICE_TYPE_KEY = "deviceType";
        @Private
        static final String DEVICE_TOKEN_KEY = "token";
        @Private
        static final String DEVICE_ADVERTISING_ID_KEY = "advertisingId";
        @Private
        static final String DEVICE_AD_TRACKING_ENABLED_KEY = "adTrackingEnabled";

        @Private
        Device() {
        }

        // For deserialization
        private Device(Map<String, Object> map) {
            super(map);
        }

        @Override
        public Device putValue(String key, Object value) {
            super.putValue(key, value);
            return this;
        }

        /**
         * Set the advertising information for this device.
         */
        void putAdvertisingInfo(String advertisingId, boolean adTrackingEnabled) {
            if (adTrackingEnabled && !isNullOrEmpty(advertisingId)) {
                put(DEVICE_ADVERTISING_ID_KEY, advertisingId);
            }
            put(DEVICE_AD_TRACKING_ENABLED_KEY, adTrackingEnabled);
        }

        /**
         * Set a device token.
         */
        public Device putDeviceToken(String token) {
            return putValue(DEVICE_TOKEN_KEY, token);
        }
    }

    /**
     * Information about the location of the device.
     */
    public static class Location extends ValueMap {

        private static final String LOCATION_LATITUDE_KEY = "latitude";
        private static final String LOCATION_LONGITUDE_KEY = "longitude";
        private static final String LOCATION_SPEED_KEY = "speed";

        // Public constructor
        public Location() {
        }

        // For deserialization
        private Location(Map<String, Object> map) {
            super(map);
        }

        @Override
        public Location putValue(String key, Object value) {
            super.putValue(key, value);
            return this;
        }

        /**
         * Set the latitude for the location of the device.
         */
        public Location putLatitude(double latitude) {
            return putValue(LOCATION_LATITUDE_KEY, latitude);
        }

        public double latitude() {
            return getDouble(LOCATION_LATITUDE_KEY, 0);
        }

        /**
         * Set the longitude for the location of the device.
         */
        public Location putLongitude(double longitude) {
            return putValue(LOCATION_LONGITUDE_KEY, longitude);
        }

        public double longitude() {
            return getDouble(LOCATION_LONGITUDE_KEY, 0);
        }

        /**
         * Set the speed of the device.
         */
        public Location putSpeed(double speed) {
            return putValue(LOCATION_SPEED_KEY, speed);
        }

        public double speed() {
            return getDouble(LOCATION_SPEED_KEY, 0);
        }
    }

    /**
     * Information about the referrer that resulted in the API call.
     */
    public static class Referrer extends ValueMap {

        private static final String REFERRER_ID_KEY = "id";
        private static final String REFERRER_LINK_KEY = "link";
        private static final String REFERRER_NAME_KEY = "name";
        private static final String REFERRER_TYPE_KEY = "type";
        private static final String REFERRER_URL_KEY = "url";

        // Public constructor
        public Referrer() {
        }

        // For deserialization
        public Referrer(Map<String, Object> map) {
            super(map);
        }

        @Override
        public Referrer putValue(String key, Object value) {
            super.putValue(key, value);
            return this;
        }

        /**
         * Set the referrer ID.
         */
        public Referrer putId(String id) {
            return putValue(REFERRER_ID_KEY, id);
        }

        public String id() {
            return getString(REFERRER_ID_KEY);
        }

        /**
         * Set the referrer link.
         */
        public Referrer putLink(String link) {
            return putValue(REFERRER_LINK_KEY, link);
        }

        public String link() {
            return getString(REFERRER_LINK_KEY);
        }

        /**
         * Set the referrer name.
         */
        public Referrer putName(String name) {
            return putValue(REFERRER_NAME_KEY, name);
        }

        public String name() {
            return getString(REFERRER_NAME_KEY);
        }

        /**
         * Set the referrer type.
         */
        public Referrer putType(String type) {
            return putValue(REFERRER_TYPE_KEY, type);
        }

        public String type() {
            return getString(REFERRER_TYPE_KEY);
        }

        /**
         * @deprecated Use {@link #putUrl(String)}
         */
        public Referrer putTerm(String url) {
            return putValue(REFERRER_URL_KEY, url);
        }

        /**
         * Set the referrer url.
         */
        public Referrer putUrl(String url) {
            return putValue(REFERRER_URL_KEY, url);
        }

        public String url() {
            return getString(REFERRER_URL_KEY);
        }
    }
}
