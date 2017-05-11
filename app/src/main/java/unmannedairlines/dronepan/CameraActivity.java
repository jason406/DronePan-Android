package unmannedairlines.dronepan;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class CameraActivity extends BaseActivity implements View.OnClickListener, PanoramaShoot.Listener {

    private static final String TAG = CameraActivity.class.getName();

    protected BroadcastReceiver broadcastReceiver;

    private ImageButton settingsButton;
    private ImageButton panoButton;
//    private Button panoButton2;

    private TextView photosTakenStatus;
    private PanoramaShoot panoramaShoot;

    private boolean isPanoramaShootRunning;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        panoramaShoot = new PanoramaShoot();

        initUi();
        registerForDeviceChanges();
    }

    private void initUi() {
        panoButton = (ImageButton) findViewById(R.id.panoButton);
        panoButton.setOnClickListener(this);

//        panoButton2 = (Button) findViewById(R.id.panoButton2);
//        panoButton2.setOnClickListener(this);

        settingsButton = (ImageButton) findViewById(R.id.btn_settings);
        settingsButton.setOnClickListener(this);

        photosTakenStatus = (TextView)findViewById(R.id.photosTakenStatus);
    }

    private void registerForDeviceChanges() {
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                onProductChange();
            }
        };

        // Register the broadcast receiver for receiving the device connection's changes.
        IntentFilter filter = new IntentFilter();
        filter.addAction(DJIConnection.FLAG_CONNECTION_CHANGE);
        registerReceiver(broadcastReceiver, filter);
    }

    @Override
    public void onResume() {
        Log.e(TAG, "onResume");
        super.onResume();
        panoramaShoot.setListener(this);
        onProductChange();
    }

    @Override
    protected void onDestroy() {
        Log.e(TAG, "onDestroy");
        panoramaShoot.setListener(null);
        unregisterReceiver(broadcastReceiver);
        super.onDestroy();
    }

    protected void onProductChange() {

    }

    private void updatePhotoCountUi()
    {
        runOnUiThread(new Runnable() {
            public void run() {
                photosTakenStatus.setText(String.format("Photo: %o/%o", panoramaShoot.getNumberOfPhotosTaken(), panoramaShoot.getTotalNumberOfPhotos()));
            }
        });
    }

    public void showToast(final String msg) {
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(CameraActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.btn_settings:{
                Intent intent = new Intent(CameraActivity.this, SettingsActivity.class);
                startActivity(intent);
                break;
            }
            case R.id.panoButton: {
                startStopPanorama();
                break;
            }
            default:
                break;
        }
    }

    private void startStopPanorama()
    {
        if (this.panoramaShoot.isMissionRunning()) {
            this.panoramaShoot.stop();
            Log.i(TAG, "Panorama shoot stopped.");
        }
        else {
            Log.e(TAG, "Setting up panorama shoot ...");
            this.panoramaShoot.setup(SettingsManager.getInstance().getSettings(DJIConnection.getInstance().getModelSafely()));
            this.panoramaShoot.start();
            Log.i(TAG, "Panorama shoot started.");
        }
    }

    private void stopPanorama() {
        this.panoramaShoot.stop();
    }

    private void navigateToConnectionActivity() {

        if (DronePanApplication.isRunningOnEmulator()) {
            return;
        }

        final Handler h = new Handler();

        final Runnable begin = new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(CameraActivity.this, ConnectionActivity.class);
                startActivity(intent);
            }
        };

        // Let's delay for 1 second (we show the toast) and then navigate back to connection view.
        h.postDelayed(begin, 1000);
    }

    @Override
    public void onPanoramaShootUpdate() {
        this.updatePhotoCountUi();

        if (isPanoramaShootRunning != this.panoramaShoot.isMissionRunning())
        {
            isPanoramaShootRunning = this.panoramaShoot.isMissionRunning();
            if (isPanoramaShootRunning)
            {
                runOnUiThread(new Runnable() {
                    public void run() {
                        panoButton.setImageResource(R.mipmap.ic_stop);
                    }
                });
            }
            else
            {
                runOnUiThread(new Runnable() {
                    public void run() {
                        panoButton.setImageResource(R.mipmap.ic_start);
                    }
                });
            }
        }
    }
}
