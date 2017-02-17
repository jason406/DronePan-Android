package unmannedairlines.dronepan;

import android.app.Application;
import android.content.Context;
import android.os.Build;

public class DronePanApplication extends DJIConnection {

    public static boolean isRunningOnEmulator()
    {
        return Build.PRODUCT.startsWith("sdk_google");
    }

    public static String getBuildVersion()
    {
        return BuildConfig.VERSION_NAME;
    }
}