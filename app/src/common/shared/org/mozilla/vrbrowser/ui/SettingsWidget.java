/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.vrbrowser.ui;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;

import org.mozilla.geckoview.GeckoRuntimeSettings;
import org.mozilla.vrbrowser.R;
import org.mozilla.vrbrowser.SessionStore;
import org.mozilla.vrbrowser.WidgetPlacement;
import org.mozilla.vrbrowser.audio.AudioEngine;

public class SettingsWidget extends UIWidget {
    private static final String LOGTAG = "VRB";

    private AudioEngine mAudio;
    private CrashReportingWidget mCrashReportingWidget;
    private Runnable mBackHandler;

    public SettingsWidget(Context aContext) {
        super(aContext);
        initialize(aContext);
    }

    public SettingsWidget(Context aContext, AttributeSet aAttrs) {
        super(aContext, aAttrs);
        initialize(aContext);
    }

    public SettingsWidget(Context aContext, AttributeSet aAttrs, int aDefStyle) {
        super(aContext, aAttrs, aDefStyle);
        initialize(aContext);
    }

    private void initialize(Context aContext) {
        inflate(aContext, R.layout.settings, this);

        ImageButton cancelButton = findViewById(R.id.settingsCancelButton);

        cancelButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mAudio != null) {
                    mAudio.playSound(AudioEngine.Sound.CLICK);
                }

                toggle();
            }
        });

        SettingsButton privacyButton = findViewById(R.id.privacyButton);
        privacyButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mAudio != null) {
                    mAudio.playSound(AudioEngine.Sound.CLICK);
                }

                onSettingsPrivacyClick();
            }
        });

        Switch crashReportingSwitch  = findViewById(R.id.crash_reporting_switch);
        crashReportingSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (mAudio != null) {
                    mAudio.playSound(AudioEngine.Sound.CLICK);
                }

                onSettingsCrashReportingChange(b);
            }
        });
        crashReportingSwitch.setChecked(SettingsStore.getInstance(getContext()).isCrashReportingEnabled());

        Switch telemetrySwitch  = findViewById(R.id.telemetry_switch);
        telemetrySwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (mAudio != null) {
                    mAudio.playSound(AudioEngine.Sound.CLICK);
                }

                onSettingsTelemetryChange(b);
            }
        });
        telemetrySwitch.setChecked(SettingsStore.getInstance(getContext()).isTelemetryEnabled());

        TextView versionText = findViewById(R.id.versionText);
        try {
            PackageInfo pInfo = getContext().getPackageManager().getPackageInfo(getContext().getPackageName(), 0);
            versionText.setText(String.format(getResources().getString(R.string.settings_version), pInfo.versionName));

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        SettingsButton reportButton = findViewById(R.id.reportButton);
        reportButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mAudio != null) {
                    mAudio.playSound(AudioEngine.Sound.CLICK);
                }

                onSettingsReportClick();
            }
        });

        mAudio = AudioEngine.fromContext(aContext);

        mBackHandler = new Runnable() {
            @Override
            public void run() {
                toggle();
            }
        };
    }

    @Override
    void initializeWidgetPlacement(WidgetPlacement aPlacement) {
        aPlacement.visible = false;
        aPlacement.width =  WidgetPlacement.dpDimension(getContext(), R.dimen.settings_width);
        aPlacement.height = WidgetPlacement.dpDimension(getContext(), R.dimen.settings_height);
        aPlacement.parentAnchorX = 0.5f;
        aPlacement.parentAnchorY = 0.5f;
        aPlacement.anchorX = 0.5f;
        aPlacement.anchorY = 0.5f;
        aPlacement.translationY = WidgetPlacement.unitFromMeters(getContext(), R.dimen.settings_world_y);
        aPlacement.translationZ = WidgetPlacement.unitFromMeters(getContext(), R.dimen.settings_world_z);
    }

    public void toggle() {
        if (getPlacement().visible) {
            getPlacement().visible = false;
            mWidgetManager.removeWidget(this);
            mWidgetManager.popBackHandler(mBackHandler);

            if (mCrashReportingWidget != null) {
                mCrashReportingWidget.hide();
            }

        } else {
            getPlacement().visible = true;
            mWidgetManager.addWidget(this);
            mWidgetManager.pushBackHandler(mBackHandler);
        }
    }

    private void onSettingsCrashReportingChange(boolean isEnabled) {
        SettingsStore.getInstance(getContext()).setCrashReportingEnabled(isEnabled);

        if (mCrashReportingWidget == null) {
            mCrashReportingWidget = new CrashReportingWidget(getContext());
            mCrashReportingWidget.getPlacement().parentHandle = getHandle();
        }

        mCrashReportingWidget.show();
    }

    private void onSettingsTelemetryChange(boolean isEnabled) {
        SettingsStore.getInstance(getContext()).setTelemetryEnabled(isEnabled);
        // TODO: Waiting for Telemetry to be merged
    }

    private void onSettingsPrivacyClick() {
        SessionStore.SessionSettings settings = new SessionStore.SessionSettings();
        int sessionId = SessionStore.get().createSession(settings);
        SessionStore.get().setCurrentSession(sessionId);
        SessionStore.get().loadUri(getContext().getString(R.string.private_policy_url));

        toggle();
    }

    private void onSettingsReportClick() {
        SessionStore.SessionSettings settings = new SessionStore.SessionSettings();
        int sessionId = SessionStore.get().createSession(settings);
        SessionStore.get().setCurrentSession(sessionId);
        SessionStore.get().loadUri(getContext().getString(R.string.private_report_url));

        toggle();
    }

}
