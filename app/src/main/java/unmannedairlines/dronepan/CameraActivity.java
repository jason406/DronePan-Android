package unmannedairlines.dronepan;


import android.content.Intent;
import android.graphics.Camera;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

import dji.common.battery.DJIBatteryState;
import dji.common.camera.DJICameraSettingsDef;
import dji.common.error.DJIError;
import dji.common.flightcontroller.DJIFlightControllerCurrentState;
import dji.common.flightcontroller.DJIFlightControllerDataType;
import dji.common.flightcontroller.DJIVirtualStickFlightControlData;
import dji.common.flightcontroller.DJIVirtualStickVerticalControlMode;
import dji.common.flightcontroller.DJIVirtualStickYawControlMode;
import dji.common.gimbal.DJIGimbalAngleRotation;
import dji.common.gimbal.DJIGimbalRotateAngleMode;
import dji.common.gimbal.DJIGimbalRotateDirection;
import dji.common.product.Model;
import dji.common.util.DJICommonCallbacks;
import dji.sdk.base.DJIBaseProduct;
import dji.sdk.battery.DJIBattery;
import dji.sdk.camera.DJICamera;
import dji.sdk.camera.DJIMedia;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.flightcontroller.DJICompass;
import dji.sdk.flightcontroller.DJIFlightController;
import dji.sdk.flightcontroller.DJIFlightControllerDelegate;
import dji.sdk.missionmanager.DJICustomMission;
import dji.sdk.missionmanager.DJIMission;
import dji.sdk.missionmanager.DJIMissionManager;
import dji.sdk.missionmanager.DJIPanoramaMission;
import dji.sdk.missionmanager.missionstep.DJIAircraftYawStep;
import dji.sdk.missionmanager.missionstep.DJIGimbalAttitudeStep;
import dji.sdk.missionmanager.missionstep.DJIMissionStep;
import dji.sdk.missionmanager.missionstep.DJIShootPhotoStep;
import dji.sdk.products.DJIAircraft;

public class CameraActivity extends BaseActivity implements TextureView.SurfaceTextureListener, View.OnClickListener {

    private static final String TAG = CameraActivity.class.getName();
    protected DJICamera.CameraReceivedVideoDataCallback mReceivedVideoDataCallBack = null;

    // Codec for video live view
    protected DJICodecManager mCodecManager = null;
    protected TextureView mVideoSurface = null;

    private Button mSettingsBtn;

    private ImageButton panoButton;

    private TextView batteryLabel;

    private TextView sequenceLabel;

    private DJIFlightController flightController;

    private DJICompass compass;

    private Double aircraftHeading;

    private Settings settings;

    private Timer yawAircraftTimer;

    private YawAircraftTask yawAircraftTask;


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
        sequenceLabel = (TextView)findViewById(R.id.sequenceLabel);

    }

    // Putting these callbacks in here because that's what DJI does in their sample code
    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        // Setup the battery listener
        try {
            DJIBaseProduct product = DJIConnection.getProductInstance();
            //settings = SettingsManager.getInstance().getSettings(product.getModel().name());

            product.getBattery().setBatteryStateUpdateCallback(
                    new DJIBattery.DJIBatteryStateUpdateCallback() {
                        @Override
                        public void onResult(DJIBatteryState djiBatteryState) {

                            final DJIBatteryState batteryState = djiBatteryState;

                            runOnUiThread(new Thread(new Runnable() {
                                public void run() {
                                    batteryLabel.setText("Battery: " + batteryState.getBatteryEnergyRemainingPercent() + "%");
                                }
                            }));

                        }
                    }
            );
        } catch (Exception e) {

            showToast("Error setting up battery listener");

        }

        // Setup the flight controller listener
        flightController = DJIConnection.getAircraftInstance().getFlightController();

        if (flightController != null) {

            flightController.setUpdateSystemStateCallback(
                    new DJIFlightControllerDelegate.FlightControllerUpdateSystemStateCallback() {
                        @Override
                        public void onResult(DJIFlightControllerCurrentState
                                                     djiFlightControllerCurrentState) {
                            if (compass != null) {

                                aircraftHeading = compass.getHeading();

                                runOnUiThread(new Thread(new Runnable() {
                                    public void run() {
                                        sequenceLabel.setText("Heading  : " + aircraftHeading);
                                    }
                                }));

                            }
                        }
                    });

            if (DJIConnection.isCompassAvailable()) {

                compass = flightController.getCompass();

            }
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

        // We need to reset the gimbal first
        //resetGimbal();

        // For I1 and I2 users
        //shootPanoWithGimbal();

        shootPanoWithGimbalAndCustomMission();

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

    private void shootPanoWithAircraft() {

        Settings settings = new Settings("Inspire 1");

        settings.getNumberOfRows();

        flightController = DJIConnection.getAircraftInstance().getFlightController();

        // Let's enable virtual stick control mode so we can send commands to the flight controller
        flightController.enableVirtualStickControlMode(
                new DJICommonCallbacks.DJICompletionCallback() {
                    @Override
                    public void onResult(DJIError error) {
                        if (error == null) {

                            // Set vertical control mode to velocity so that when we send a value of 0 it won't descend
                            flightController.setVerticalControlMode(DJIVirtualStickVerticalControlMode.Velocity);

                            // Let's set the yaw mode to angle - angles are relative to the front of the aircraft
                            flightController.setYawControlMode(DJIVirtualStickYawControlMode.Angle);

                            if (yawAircraftTimer == null) {
                                yawAircraftTask = new YawAircraftTask();
                                yawAircraftTimer = new Timer();
                                yawAircraftTimer.schedule(yawAircraftTask, 0, 200);
                            }

                        } else {

                            showToast("Error enabling virtual stick mode");

                        }
                    }
                }
        );


    }

    private void shootPanoWithAircraftAndCustomMission() {

        final DJIMissionManager missionManager = DJIMissionManager.getInstance();

        LinkedList<DJIMissionStep> steps = new LinkedList<DJIMissionStep>();

        // Pitch gimbal to 0
        //steps.add();

        // Take a photo
        steps.add(new DJIShootPhotoStep(new DJICommonCallbacks.DJICompletionCallback() {

            @Override
            public void onResult(DJIError error) {
                // Handle photo error here
            }
        }));

        // Pitch gimbal to -30
        steps.add(new DJIGimbalAttitudeStep(DJIGimbalRotateAngleMode.AbsoluteAngle,
                new DJIGimbalAngleRotation(true, -30f, DJIGimbalRotateDirection.Clockwise), null, null,
                new DJICommonCallbacks.DJICompletionCallback() {
                    @Override
                    public void onResult(DJIError error) {
                        // Handle pitch gimbal error here
                    }
                }));

        // Take a photo
        steps.add(new DJIShootPhotoStep(new DJICommonCallbacks.DJICompletionCallback() {

            @Override
            public void onResult(DJIError error) {
                // Handle photo error here
            }
        }));

        // Pitch gimbal to -60
        steps.add(new DJIGimbalAttitudeStep(DJIGimbalRotateAngleMode.AbsoluteAngle,
                new DJIGimbalAngleRotation(true, -60f, DJIGimbalRotateDirection.Clockwise), null, null,
                new DJICommonCallbacks.DJICompletionCallback() {
                    @Override
                    public void onResult(DJIError error) {
                        // Handle pitch gimbal error here
                    }
                }));

        // Take a photo
        steps.add(new DJIShootPhotoStep(new DJICommonCallbacks.DJICompletionCallback() {

            @Override
            public void onResult(DJIError error) {
                // Handle photo error here
            }
        }));

        // Yaw aircraft with angle and angular velocity
        steps.add(new DJIAircraftYawStep(60, 20, new DJICommonCallbacks.DJICompletionCallback() {

            @Override
            public void onResult(DJIError error) {
                // Handle yaw aircraft error here
            }

        }));

        // Load the steps into a cusstom mission
        DJICustomMission customMission = new DJICustomMission(steps);

        // Prepare the mission
        missionManager.prepareMission(customMission, new DJIMission.DJIMissionProgressHandler() {

            @Override
            public void onProgress(DJIMission.DJIProgressType type, float progress) {
                //setProgressBar((int)(progress * 100f));
            }

        }, new DJICommonCallbacks.DJICompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                if (error == null) {

                    // Success preparing mission, let's start the mission
                    missionManager.startMissionExecution(new DJICommonCallbacks.DJICompletionCallback() {

                        @Override
                        public void onResult(DJIError mError) {

                            if (mError == null) {

                                // Success starting mission
                                Log.d(TAG, "Starting mission");

                            } else {

                                // Error starting mission
                                Log.d(TAG, "Error starting mission");

                            }
                        }
                    });

                } else {
                    // Error preparing mission
                    Log.d(TAG, "Error preparing mission");
                }
            }
        });

    }

    private void prepareAndStartCustomMission(LinkedList<DJIMissionStep> steps) {

        Log.d(TAG, "shootPanoWithGimbalAndCustomMission");

        final DJIMissionManager missionManager = DJIMissionManager.getInstance();

        // Load the steps into a cusstom mission
        DJICustomMission customMission = new DJICustomMission(steps);

        // Prepare the mission
        missionManager.prepareMission(customMission, new DJIMission.DJIMissionProgressHandler() {

            @Override
            public void onProgress(DJIMission.DJIProgressType type, float progress) {
                //setProgressBar((int)(progress * 100f));
            }

        }, new DJICommonCallbacks.DJICompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                if (error == null) {

                    // Success preparing mission, let's start the mission
                    missionManager.startMissionExecution(new DJICommonCallbacks.DJICompletionCallback() {

                        @Override
                        public void onResult(DJIError mError) {

                            if (mError == null) {

                                // Success starting mission
                                Log.d(TAG, "Starting mission");

                            } else {

                                // Error starting mission
                                Log.d(TAG, "Error starting mission");

                            }
                        }
                    });

                } else {
                    // Error preparing mission
                    Log.d(TAG, "Error preparing mission");
                }
            }
        });

    }

    private void shootPanoWithGimbalAndCustomMission() {

        Log.d(TAG, "shootPanoWithGimbalAndCustomMission");

        LinkedList<DJIMissionStep> steps = new LinkedList<DJIMissionStep>();

        steps.add(pitchYawGimbalStep(0, 0));
        steps.add(photoStep());
        steps.add(pitchGimbalStep(10f));
        steps.add(photoStep());
        steps.add(pitchGimbalStep(20f));
        steps.add(photoStep());
        steps.add(pitchYawGimbalStep(0, 60));


        prepareAndStartCustomMission(steps);

    }

    private DJIGimbalAttitudeStep pitchYawGimbalStep(float pitch, float yaw) {

        return new DJIGimbalAttitudeStep(DJIGimbalRotateAngleMode.AbsoluteAngle,
                new DJIGimbalAngleRotation(true, pitch, DJIGimbalRotateDirection.Clockwise),
                null,
                new DJIGimbalAngleRotation(true, yaw, DJIGimbalRotateDirection.Clockwise),
                new DJICommonCallbacks.DJICompletionCallback() {
                    @Override
                    public void onResult(DJIError error) {
                        // Handle pitch gimbal error here
                    }
                });

    }

    private DJIGimbalAttitudeStep pitchGimbalStep(float pitch) {

        return new DJIGimbalAttitudeStep(DJIGimbalRotateAngleMode.AbsoluteAngle,
                new DJIGimbalAngleRotation(true, pitch, DJIGimbalRotateDirection.Clockwise),
                null,
                null,
                new DJICommonCallbacks.DJICompletionCallback() {
                    @Override
                    public void onResult(DJIError error) {

                        if (error == null) {

                            Log.d(TAG, "Pitch gimbal successful");

                        } else {

                            Log.d(TAG, "Error pitching gimbal");

                        }

                    }
                });
    }

    private DJIShootPhotoStep photoStep() {

        return new DJIShootPhotoStep(new DJICommonCallbacks.DJICompletionCallback() {

            @Override
            public void onResult(DJIError error) {

                if (error == null) {

                    Log.d(TAG, "Shoot photo successful");

                } else {

                    Log.d(TAG, "Error shooting photo");

                }
            }
        });
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
                intent.putExtra("modelName", getModelName());
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

    private String getModelName()
    {
        return "DJIAircraftModelNameMavicPro";

        /*
        DJIBaseProduct product = DJIConnection.getProductInstance();
        if (product != null)
        {
            return product.getModel().name();
        }

        return "Default";
        */
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



    class YawAircraftTask extends TimerTask {

        @Override
        public void run() {

            Log.d(TAG, "task is running");

            DJIFlightController fc = DJIConnection.getAircraftInstance().getFlightController();
            if (fc != null) {
                fc.sendVirtualStickFlightControlData(
                        new DJIVirtualStickFlightControlData(
                                0, 0, 60, 0
                        ), new DJICommonCallbacks.DJICompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {

                            }
                        }
                );
            }
        }
    }
}
