package org.schabi.newpipe.settings;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.preference.Preference;

import com.segment.analytics.Analytics;
import com.segment.analytics.Traits;

import org.schabi.newpipe.R;

public class LoginSettingsFragment extends BasePreferenceFragment {
    private final Preference.OnPreferenceChangeListener loginPreferenceChange
            = (preference, newValue) -> {
        Analytics.with(null).identify((String) newValue, new Traits().putEmail((String) newValue), null);
        defaultPreferences.edit()
                .putString(getString(R.string.login_email), (String) newValue).apply();
        return true;
    };

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final String loginToggleKey = getString(R.string.login_email);
        findPreference(loginToggleKey).setOnPreferenceChangeListener(loginPreferenceChange);
    }

    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        addPreferencesFromResource(R.xml.login_settings);
    }
}
