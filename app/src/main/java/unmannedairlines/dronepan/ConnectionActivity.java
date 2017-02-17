package unmannedairlines.dronepan;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import dji.sdk.base.DJIBaseProduct;

public class ConnectionActivity extends BaseActivity {

    private static final String TAG = ConnectionActivity.class.getName();

    private TextView mTextConnectionStatus;

    protected BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            updateUI();

        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.VIBRATE,
                            Manifest.permission.INTERNET, Manifest.permission.ACCESS_WIFI_STATE,
                            Manifest.permission.WAKE_LOCK, Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.CHANGE_WIFI_STATE, Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
                            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.SYSTEM_ALERT_WINDOW,
                            Manifest.permission.READ_PHONE_STATE,
                    }
                    , 1);
        }

        setContentView(R.layout.activity_connection);

        // Get notified of the device connection change
        IntentFilter filter = new IntentFilter();
        filter.addAction(DJIConnection.FLAG_CONNECTION_CHANGE);
        registerReceiver(mReceiver, filter);

        mTextConnectionStatus = (TextView) findViewById(R.id.text_connection_status);

        // For testing in emulator
        if (DronePanApplication.isRunningOnEmulator()) {
            launchCameraActivity();
        }
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    private void updateUI() {
        DJIBaseProduct mProduct = DJIConnection.getProductInstance();

        if (null != mProduct && mProduct.isConnected()) {

            if (null != mProduct.getModel()) {
                mTextConnectionStatus.setText(mProduct.getModel().getDisplayName() + " connected");
            } else {
                mTextConnectionStatus.setText("Model unavailable");
            }

            // Let's take them to the camera view
            launchCameraActivity();

        } else {

            mTextConnectionStatus.setText("No product connected");

        }
    }

    private void launchCameraActivity() {

        final Handler h = new Handler();

        final Runnable begin = new Runnable() {

            @Override
            public void run() {

                Intent intent = new Intent(ConnectionActivity.this, CameraActivity.class);
                startActivity(intent);

            }
        };

        // Let's delay for 1 second and then we'll display the camera view
        h.postDelayed(begin, 1000);

    }
}
