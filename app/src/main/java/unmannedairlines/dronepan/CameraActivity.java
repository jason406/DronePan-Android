package unmannedairlines.dronepan;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import dji.common.product.Model;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;

public class CameraActivity extends BaseActivity implements TextureView.SurfaceTextureListener, View.OnClickListener {

    private static final String TAG = CameraActivity.class.getName();

    protected BroadcastReceiver broadcastReceiver;
    protected VideoFeeder.VideoDataCallback videoDataCallback;

    // Codec for video live view
    protected DJICodecManager codecManager;
    protected TextureView videoSurface;

    private ImageButton settingsButton;
    private ImageButton panoButton;

    private TextView sequenceLabel;
    private TextView satelliteLabel;
    private TextView distanceLabel;
    private TextView altitudeLabel;

    //private PanoramaShoot panoramaShoot;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        initUi();
        createVideoCallback();
        registerForDeviceChanges();
    }

    private void initUi() {
        videoSurface = (TextureView)findViewById(R.id.video_previewer_surface);

        if (videoSurface != null) {
            videoSurface.setSurfaceTextureListener(this);
        }

        panoButton = (ImageButton) findViewById(R.id.panoButton);
        panoButton.setOnClickListener(this);

        settingsButton = (ImageButton) findViewById(R.id.btn_settings);
        settingsButton.setOnClickListener(this);

        sequenceLabel = (TextView)findViewById(R.id.sequenceLabel);
        satelliteLabel = (TextView)findViewById(R.id.satelliteLabel);
        distanceLabel = (TextView)findViewById(R.id.distanceLabel);
        altitudeLabel = (TextView)findViewById(R.id.altitudeLabel);
    }

    private void createVideoCallback() {
        // The callback for receiving the raw H264 video data for camera live view
        videoDataCallback = new VideoFeeder.VideoDataCallback() {
            @Override
            public void onReceive(byte[] videoBuffer, int size) {
                if(codecManager != null){
                    // Send the raw H264 video data to codec manager for decoding
                    codecManager.sendDataToDecoder(videoBuffer, size);
                }
            }
        };
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

        initPreviewer();
        onProductChange();

        if(videoSurface == null) {
            Log.e(TAG, "videoSurface is null");
        }
    }

    @Override
    protected void onDestroy() {
        Log.e(TAG, "onDestroy");

        unregisterReceiver(broadcastReceiver);
        uninitPreviewer();

        super.onDestroy();
    }

    protected void onProductChange() {
        initPreviewer();
    }

    private void initPreviewer() {
        BaseProduct product = DJIConnection.getInstance().getProduct();
        if (product == null || !product.isConnected()) {
            showToast(getString(R.string.disconnected));
            navigateToConnectionActivity();
        } else {
            if (videoSurface != null) {
                videoSurface.setSurfaceTextureListener(this);
            }
            if (!product.getModel().equals(Model.UNKNOWN_AIRCRAFT)) {
                if (VideoFeeder.getInstance().getVideoFeeds() != null
                        && VideoFeeder.getInstance().getVideoFeeds().size() > 0) {
                    VideoFeeder.getInstance().getVideoFeeds().get(0).setCallback(videoDataCallback);
                }
            }
        }
    }

    private void uninitPreviewer() {
        Camera camera = DJIConnection.getInstance().getCamera();
        if (camera != null){
            // Reset the callback
            VideoFeeder.getInstance().getVideoFeeds().get(0).setCallback(null);
        }
    }

    private void updatePhotoCountUi()
    {
        // Update UI.
        runOnUiThread(new Runnable() {
            public void run() {
                //sequenceLabel.setText("Photo: " + photos_taken_count + "/" + settings.getNumberOfPhotos());
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
                //startPano();
                break;
            }
            default:
                break;
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.e(TAG, "onSurfaceTextureAvailable");
        if (codecManager == null) {
            codecManager = new DJICodecManager(this, surface, width, height);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        Log.e(TAG, "onSurfaceTextureSizeChanged");
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.e(TAG,"onSurfaceTextureDestroyed");
        if (codecManager != null) {
            codecManager.cleanSurface();
            codecManager = null;
        }
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
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
}
