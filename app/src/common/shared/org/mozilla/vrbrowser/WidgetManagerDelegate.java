package org.mozilla.vrbrowser;

public interface WidgetManagerDelegate {
    interface Listener {
        void onWidgetUpdate(Widget aWidget);
    }
    interface PermissionListener {
        void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults);
    }
    int newWidgetHandle();
    void addWidget(Widget aWidget);
    void updateWidget(Widget aWidget);
    void removeWidget(Widget aWidget);
    void startWidgetResize(Widget aWidget);
    void finishWidgetResize(Widget aWidget);
    void addListener(WidgetManagerDelegate.Listener aListener);
    void removeListener(WidgetManagerDelegate.Listener aListener);
    void pushBackHandler(Runnable aRunnable);
    void popBackHandler(Runnable aRunnable);
    void fadeOutWorld();
    void fadeInWorld();
    void setTrayVisible(boolean visible);
    void setBrowserSize(float targetWidth, float targetHeight);
    void keyboardDismissed();
    void updateEnvironment();
    void updatePointerColor();
    void addPermissionListener(PermissionListener aListener);
    void removePermissionListener(PermissionListener aListener);
}
