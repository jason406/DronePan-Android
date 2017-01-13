package unmannedairlines.dronepan;

import android.content.Intent;
import android.graphics.Camera;
import android.graphics.SurfaceTexture;
import android.media.audiofx.BassBoost;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import dji.common.battery.DJIBatteryState;
import dji.common.camera.DJICameraSettingsDef;
import dji.common.error.DJIError;
import dji.common.flightcontroller.DJIVirtualStickYawControlMode;
import dji.common.gimbal.DJIGimbalAngleRotation;
import dji.common.gimbal.DJIGimbalRotateAngleMode;
import dji.common.gimbal.DJIGimbalRotateDirection;
import dji.common.product.Model;
import dji.common.util.DJICommonCallbacks;
import dji.sdk.base.DJIBaseProduct;
import dji.sdk.battery.DJIBattery;
import dji.sdk.camera.DJICamera;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.flightcontroller.DJIFlightController;

public class CameraActivity extends BaseActivity implements TextureView.SurfaceTextureListener, View.OnClickListener {

    private static final String TAG = CameraActivity.class.getName();
    protected DJICamera.CameraReceivedVideoDataCallback mReceivedVideoDataCallBack = null;

    // Codec for video live view
    protected DJICodecManager mCodecManager = null;
    protected TextureView mVideoSurface = null;

    private Button mSettingsBtn;

    private ImageButton panoButton;

    private TextView batteryLabel;

    private DJIFlightController flightController;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_camera);

        mReceivedVideoDataCallBack = new DJICamera.CameraReceivedVideoDataCallback() {

            @Override
            public void onResult(byte[] videoBuffer, int size) {
                if(mCodecManager != null){
                    // Send the raw H264 video data to codec manager for decoding
                    mCodecManager.sendDataToDecoder(videoBuffer, size);
                }else {
                    Log.e(TAG, "mCodecManager is null");
                }
            }
        };

        mVideoSurface = (TextureView)findViewById(R.id.video_previewer_surface);

        if (null != mVideoSurface) {
            mVideoSurface.setSurfaceTextureListener(this);
        }

        panoButton = (ImageButton) findViewById(R.id.panoButton);
        panoButton.setOnClickListener(this);

        mSettingsBtn = (Button) findViewById(R.id.btn_settings);
        mSettingsBtn.setOnClickListener(this);

        batteryLabel = (TextView)findViewById(R.id.batteryLabel);

        try {
            DJIConnection.getProductInstance().getBattery().setBatteryStateUpdateCallback(
                    new DJIBattery.DJIBatteryStateUpdateCallback() {
                        @Override
                        public void onResult(DJIBatteryState djiBatteryState) {

                            //batteryLabel.setText("Battery: " + djiBatteryState.getBatteryEnergyRemainingPercent() + "%");

                        }
                    }
            );
        } catch (Exception e) {

        }

    }

    @Override
    public void onResume() {
        super.onResume();
        initPreviewer();
    }

    @Override
    protected void onDestroy() {
        uninitPreviewer();
        super.onDestroy();
    }

    private void initPreviewer() {

        DJIBaseProduct product = DJIConnection.getProductInstance();

        if (product == null || !product.isConnected()) {
            showToast("Disconnected");
        } else {
            if (null != mVideoSurface) {
                mVideoSurface.setSurfaceTextureListener(this);
            }
            if (!product.getModel().equals(Model.UnknownAircraft)) {
                DJICamera camera = product.getCamera();
                if (camera != null){
                    // Set the callback
                    camera.setDJICameraReceivedVideoDataCallback(mReceivedVideoDataCallBack);
                }
            }
        }
    }

    private void uninitPreviewer() {
        DJICamera camera = DJIConnection.getCameraInstance();
        if (camera != null){
            // Reset the callback
            DJIConnection.getCameraInstance().setDJICameraReceivedVideoDataCallback(null);
        }
    }

    private void takePhoto(){

        //DJICameraSettingsDef.CameraMode cameraMode = DJICameraSettingsDef.CameraMode.ShootPhoto;

        final DJICamera camera = DJIConnection.getCameraInstance();
        if (camera != null) {

            DJICameraSettingsDef.CameraShootPhotoMode photoMode = DJICameraSettingsDef.CameraShootPhotoMode.Single; // Set the camera capture mode as Single mode
            camera.startShootPhoto(photoMode, new DJICommonCallbacks.DJICompletionCallback() {

                @Override
                public void onResult(DJIError error) {
                    if (error == null) {
                        Log.d(TAG, "takePhoto: success");
                    } else {
                        Log.d(TAG, error.getDescription());
                    }
                }

            });
        }
    }

    private void startPano() {

        flightController = DJIConnection.getAircraftInstance().getFlightController();

        /*// Let's enable virtual stick control mode so we can send commands to the flight controller
        flightController.enableVirtualStickControlMode(
                new DJICommonCallbacks.DJICompletionCallback() {
                    @Override
                    public void onResult(DJIError error) {
                        if (error == null) {

                            // Let's set the yaw mode to angle
                            // DJIConnection.getAircraftInstance().getFlightController().setYawControlMode(DJIVirtualStickYawControlMode.Angle);

                        } else {

                            showToast("Error enabling virtual stick mode");

                        }
                    }
                }
        );*/

        // We need to reset the gimbal first
        resetGimbal();

        // For I1 and I2 users
        shootPanoWithGimbal();

        Log.e(TAG, "Starting pano");

    }

    // Setup our gimbal pitch/yaw angles
    final float[] pitches = new float[]{0, -30, -60};
    final float[] yaws = new float[]{0, 60, 120, 180, -120, -60};

    private int pitchCount = 0;
    private int yawCount = 0;

    /*
    Shoot a pano with gimbal only
    The sequence is pitch gimbal, take photo, and repeat
    Once the column is complete we then yaw the gimbal and repeat above
    */
    private void shootPanoWithGimbal() {

        Log.d(TAG, "shootPanoWithGimbal called, pitch count: " + pitchCount + ", yaw count: " + yawCount);

        final Handler h = new Handler();

        // Begin take photo
        final Runnable photoThread = new Runnable() {

            @Override
            public void run() {

                Log.d(TAG, "Taking photo");

                takePhoto();

                // Increment the loop counter
                pitchCount++;

                // Move to next sequence
                shootPanoWithGimbal();

            }

        };
        // End take photo

        // Yaw gimbal
        final Runnable yawThread = new Runnable() {

            @Override
            public void run() {

                if (yawCount < yaws.length) {

                    // Yaw gimbal to next column
                    yawGimbal(yaws[yawCount]);

                    // Increment the yawCount
                    yawCount++;

                    // Move to next sequence
                    shootPanoWithGimbal();

                } else {

                    Log.d(TAG, "Done with pano sequence");

                }

            }

        };
        // End yaw gimbal

        // Pitch gimbal
        final Runnable pitchThread = new Runnable() {

            @Override
            public void run() {

                if (pitchCount < pitches.length) {

                    Log.d(TAG, "Pitching gimbal to: " + pitches[pitchCount]);

                    // Pitch with 0 yaw
                    pitchGimbal(pitches[pitchCount]);

                    // Delay and shoot photo
                    h.postDelayed(photoThread, 3000);

                } else if (yawCount < yaws.length) {

                    Log.d(TAG, "Yawing gimbal to: " + yaws[yawCount]);

                    // Column of photos is complete. Yaw the gimbal for the next column.
                    h.postDelayed(yawThread, 1000);

                    // Reset the pitch count before we begin the next column
                    pitchCount = 0;

                } else {

                    yawCount = 0;
                    pitchCount = 0;

                    // Rest the gimbal
                    resetGimbal();

                    Log.d(TAG, "We're done!!!");

                }

            }

        };
        // End pitch gimbal

        // This is the entry point for each loop
        h.postDelayed(pitchThread, 1000);


    }

    private void pitchGimbal(float pitch) {

        setGimbalAttitude(pitch, 0, true, false);

    }

    /*
    When we yaw we want to reset the pitch to
    zero as well. This is the start of a new column.
     */
    private void yawGimbal(float yaw) {

        setGimbalAttitude(0, yaw, true, true);

    }

    private void setGimbalAttitude(float pitch, float yaw, boolean pitchEnabled, boolean yawEnabled) {

        Log.d(TAG, "setGimbalAttitude called with pitch: " + pitch + " and yaw: " + yaw);

        DJIGimbalAngleRotation gimbalPitch = new DJIGimbalAngleRotation(pitchEnabled, pitch, DJIGimbalRotateDirection.Clockwise);
        DJIGimbalAngleRotation gimbalRoll = new DJIGimbalAngleRotation(false, 0, DJIGimbalRotateDirection.Clockwise);
        DJIGimbalAngleRotation gimbalYaw = new DJIGimbalAngleRotation(yawEnabled, yaw, DJIGimbalRotateDirection.Clockwise);

        DJIConnection.getProductInstance().getGimbal().rotateGimbalByAngle(DJIGimbalRotateAngleMode.AbsoluteAngle, gimbalPitch, gimbalRoll, gimbalYaw,
                new DJICommonCallbacks.DJICompletionCallback() {
                    @Override
                    public void onResult(DJIError error) {
                        if (error == null) {
                            Log.d(TAG, "rotateGimbalByAngle success");
                        } else {
                            Log.d(TAG, "rotateGimbalByAngle error");
                        }
                    }
                });

    }

    private void resetGimbal() {

        DJIGimbalAngleRotation gimbalPitch = new DJIGimbalAngleRotation(true, 0, DJIGimbalRotateDirection.Clockwise);
        DJIGimbalAngleRotation gimbalRoll = new DJIGimbalAngleRotation(true, 0, DJIGimbalRotateDirection.Clockwise);
        DJIGimbalAngleRotation gimbalYaw = new DJIGimbalAngleRotation(true, 0, DJIGimbalRotateDirection.Clockwise);

        DJIConnection.getProductInstance().getGimbal().rotateGimbalByAngle(DJIGimbalRotateAngleMode.AbsoluteAngle, gimbalPitch, gimbalRoll, gimbalYaw,
                new DJICommonCallbacks.DJICompletionCallback() {
                    @Override
                    public void onResult(DJIError error) {
                        if (error == null) {

                        }
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
                startPano();
                break;
            }
            default:
                break;
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if (mCodecManager == null) {
            mCodecManager = new DJICodecManager(this, surface, width, height);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {

        if (mCodecManager != null) {
            mCodecManager.cleanSurface();
            mCodecManager = null;
        }

        return false;
    }
}
