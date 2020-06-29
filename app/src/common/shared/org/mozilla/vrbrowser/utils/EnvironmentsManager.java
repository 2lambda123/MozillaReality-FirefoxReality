package org.mozilla.vrbrowser.utils;

import android.app.DownloadManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mozilla.speechlibrary.utils.zip.UnzipCallback;
import com.mozilla.speechlibrary.utils.zip.UnzipTask;

import org.mozilla.vrbrowser.R;
import org.mozilla.vrbrowser.browser.SettingsStore;
import org.mozilla.vrbrowser.downloads.Download;
import org.mozilla.vrbrowser.downloads.DownloadJob;
import org.mozilla.vrbrowser.downloads.DownloadsManager;
import org.mozilla.vrbrowser.ui.widgets.WidgetManagerDelegate;

import java.io.File;

public class EnvironmentsManager implements DownloadsManager.DownloadsListener, SharedPreferences.OnSharedPreferenceChangeListener {

    private WidgetManagerDelegate mApplicationDelegate;
    private Context mContext;
    private DownloadsManager mDownloadManager;
    private SharedPreferences mPrefs;

    public EnvironmentsManager(@NonNull Context context) {
        mContext = context;
        mApplicationDelegate = ((WidgetManagerDelegate)context);
        mDownloadManager = mApplicationDelegate.getServicesProvider().getDownloadsManager();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
    }

    public void start() {
        mDownloadManager.addListener(this);
        mPrefs.registerOnSharedPreferenceChangeListener(this);
    }

    public void stop() {
        mDownloadManager.removeListener(this);
        mPrefs.unregisterOnSharedPreferenceChangeListener(this);
    }

    public void setOrDownloadEnvironment(@NonNull String envId) {
        if (EnvironmentUtils.isBuiltinEnvironment(mContext, envId)) {
            mApplicationDelegate.updateEnvironment();

        } else {
            if (EnvironmentUtils.isExternalEnvReady(mContext, envId)) {
                // If the environment is ready, call native to update
                mApplicationDelegate.updateEnvironment();

            } else {
                downloadEnvironment(envId);
            }
        }
    }

    @Nullable
    public String getOrDownloadEnvironment() {
        return getOrDownloadEnvironment(SettingsStore.getInstance(mContext).getEnvironment());
    }

    @Nullable
    public String getOrDownloadEnvironment(@NonNull String envId) {
        String environmentPath = null;
        if (EnvironmentUtils.isBuiltinEnvironment(mContext, envId)) {
            environmentPath = EnvironmentUtils.getBuiltinEnvPath(envId);

        } else {
            if (EnvironmentUtils.isExternalEnvReady(mContext, envId)) {
                // If the environment is ready, return the path
                environmentPath = EnvironmentUtils.getExternalEnvPath(mContext, envId);

            } else {
                downloadEnvironment(envId);
            }
        }

        return environmentPath;
    }

    private void downloadEnvironment(@NonNull String envId) {
        final Environment environment = EnvironmentUtils.getExternalEnvironmentById(mContext, envId);
        if (environment != null) {
            // Check if the env is being downloaded
            boolean isDownloading = mDownloadManager.getDownloads().stream()
                    .filter(item ->
                            item.getStatus() == DownloadManager.STATUS_RUNNING &&
                                    item.getStatus() == DownloadManager.STATUS_PAUSED &&
                                    item.getStatus() == DownloadManager.STATUS_PENDING &&
                                    item.getUri().equals(environment.getPayload()))
                    .findFirst().orElse(null) != null;

            if (!isDownloading) {
                // If the env is not being downloaded, start downloading it
                DownloadJob job = DownloadJob.create(environment.getPayload());
                mDownloadManager.startDownload(job);
            }
        }
    }

    // DownloadsManager

    @Override
    public void onDownloadCompleted(@NonNull Download download) {
        Environment env = EnvironmentUtils.getExternalEnvironmentByPayload(mContext, download.getUri());
        if (env != null) {
            // We don't want the download to be left in the downloads list, so we just remove it when the download is done.
            mDownloadManager.removeDownload(download.getId(), false);
            UnzipTask unzip = new UnzipTask(mContext);
            unzip.addListener(new UnzipCallback() {
                @Override
                public void onUnzipStart(@NonNull String zipFile) {

                }

                @Override
                public void onUnzipProgress(@NonNull String zipFile, double progress) {

                }

                @Override
                public void onUnzipFinish(@NonNull String zipFile, @NonNull String outputPath) {
                    // Delete the zip file when the unzipping is done.
                    File file = new File(zipFile);
                    file.delete();

                    // the environment is ready, call native to update the current env.
                    mApplicationDelegate.updateEnvironment();
                }

                @Override
                public void onUnzipCancelled(@NonNull String zipFile) {

                }

                @Override
                public void onUnzipError(@NonNull String zipFile, @Nullable String error) {

                }
            });
            String zipOutputPath = EnvironmentUtils.getEnvPath(mContext, env.getValue());
            if (zipOutputPath != null) {
                unzip.start(download.getOutputFilePath(), zipOutputPath);
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        if (s.equals(mContext.getString(R.string.settings_key_remote_props))) {
            mApplicationDelegate.updateEnvironment();
        }
    }
}
