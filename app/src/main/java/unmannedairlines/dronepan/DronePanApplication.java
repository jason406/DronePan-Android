package unmannedairlines.dronepan;

import android.app.Application;
import android.content.Context;
import android.os.Build;
import dji.sdk.sdkmanager.DJISDKManager;

public class DronePanApplication extends Application {

    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();

        context = getApplicationContext();
        DJIConnection.getInstance().initialize(context);
    }

    public static boolean isRunningOnEmulator()
    {
        return Build.PRODUCT.startsWith("sdk_google");
    }

    public static String getBuildVersion()
    {
        return BuildConfig.VERSION_NAME;
    }

    public static Context getContext() { return context; }
}