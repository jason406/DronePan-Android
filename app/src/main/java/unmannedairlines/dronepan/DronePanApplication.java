package unmannedairlines.dronepan;

import android.app.Application;
import android.content.Context;

public class DronePanApplication extends Application {

    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext(); // Grab the Context you want.
    }

    public static Context getContext() { return context; }
}