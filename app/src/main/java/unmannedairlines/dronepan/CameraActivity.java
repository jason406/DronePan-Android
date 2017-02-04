package unmannedairlines.dronepan;

import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.LinkedList;
import java.util.Timer;

import dji.common.battery.DJIBatteryState;
import dji.common.camera.DJICameraSettingsDef;
import dji.common.error.DJIError;
import dji.common.flightcontroller.DJIFlightControllerCurrentState;
import dji.common.gimbal.DJIGimbalAngleRotation;
import dji.common.gimbal.DJIGimbalRotateAngleMode;
import dji.common.gimbal.DJIGimbalRotateDirection;
import dji.common.product.Model;
import dji.common.util.DJICommonCallbacks;
import dji.sdk.base.DJIBaseProduct;
import dji.sdk.battery.DJIBattery;
import dji.sdk.camera.DJICamera;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.flightcontroller.DJICompass;
import dji.sdk.flightcontroller.DJIFlightController;
import dji.sdk.flightcontroller.DJIFlightControllerDelegate;
import dji.sdk.missionmanager.DJICustomMission;
import dji.sdk.missionmanager.DJIMission;
import dji.sdk.missionmanager.DJIMissionManager;
import dji.sdk.missionmanager.missionstep.DJIAircraftYawStep;
import dji.sdk.missionmanager.missionstep.DJIMissionStep;
import dji.sdk.missionmanager.missionstep.DJIShootPhotoStep;

public class CameraActivity extends BaseActivity implements TextureView.SurfaceTextureListener, View.OnClickListener {

    private static final String TAG = CameraActivity.class.getName();
    protected DJICamera.CameraReceivedVideoDataCallback mReceivedVideoDataCallBack = null;

    // Codec for video live view
    protected DJICodecManager mCodecManager = null;
    protected TextureView mVideoSurface = null;

    private ImageButton mSettingsBtn;
    private ImageButton panoButton;

    private TextView batteryLabel;
    private TextView sequenceLabel;
    private TextView satelliteLabel;
    private TextView distanceLabel;
    private TextView altitudeLabel;

    private DJIFlightController flightController;

    private DJIMissionManager missionManager;

    private int missionYawCount = 0;

    private DJICompass compass;

    private Double aircraftHeading;

    private Settings settings;

    private Timer yawAircraftTimer;

    // shootColumn variables
    int column_counter = 0;
    int photos_taken_count = 0;

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

        mSettingsBtn = (ImageButton) findViewById(R.id.btn_settings);
        mSettingsBtn.setOnClickListener(this);

        batteryLabel = (TextView)findViewById(R.id.batteryLabel);
        sequenceLabel = (TextView)findViewById(R.id.sequenceLabel);
        satelliteLabel = (TextView)findViewById(R.id.satelliteLabel);
        distanceLabel = (TextView)findViewById(R.id.distanceLabel);
        altitudeLabel = (TextView)findViewById(R.id.altitudeLabel);
    }

    // Putting these callbacks in here because that's what DJI does in their sample code
    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        DJIBaseProduct product = DJIConnection.getProductInstance();

        // Setup the battery listener
        try {
            product.getBattery().setBatteryStateUpdateCallback(
                    new DJIBattery.DJIBatteryStateUpdateCallback() {
                        @Override
                        public void onResult(DJIBatteryState djiBatteryState) {

                            final DJIBatteryState batteryState = djiBatteryState;

                            runOnUiThread(new Thread(new Runnable() {
                                public void run() {
                                    batteryLabel.setText("Batt: " + batteryState.getBatteryEnergyRemainingPercent() + "%");
                                }
                            }));

                        }
                    }
            );
        } catch (Exception e) {

            showToast("Error setting up battery listener");

        }

        // If we are testing on the emulator, just return here an don't execute the code below.
        if (DronePanApplication.isRunningOnEmulator()) {
            return;
        }

        // Setup the flight controller listener
        flightController = DJIConnection.getAircraftInstance().getFlightController();

        if (flightController != null) {

            flightController.setUpdateSystemStateCallback(
                    new DJIFlightControllerDelegate.FlightControllerUpdateSystemStateCallback() {
                        @Override
                        public void onResult(DJIFlightControllerCurrentState
                                                     djiFlightControllerCurrentState) {

                            final DJIFlightControllerCurrentState state = djiFlightControllerCurrentState;

                            if (compass != null) {

                                aircraftHeading = compass.getHeading();

                            }

                            // Update some telemetry labels
                            runOnUiThread(new Runnable() {
                                public void run() {

                                    satelliteLabel.setText("Sats: " + state.getSatelliteCount());
                                    altitudeLabel.setText("Alt: " + state.getAircraftLocation().getAltitude() + "m");

                                }
                            });

                        }
                    });

            if (DJIConnection.isCompassAvailable()) {

                compass = flightController.getCompass();

            }
        }

        // Setup the mission manager listener
        missionManager = product.getMissionManager();

        missionManager.setMissionExecutionFinishedCallback(new DJICommonCallbacks.DJICompletionCallback() {

            @Override
            public void onResult(DJIError error) {

                // Yaw aircraft is complete. Let's take photo
                if(error == null) {

                    Log.d(TAG, "Mission finished successfully");


                    // This will loop 6 times and take 6 shots hopefully
                    if (missionYawCount < settings.getPhotosPerRow()) {

                        shootColumn();

                    } else {

                        shootNadir();

                    }

                } else {

                    Log.d(TAG, "Error finishing mission");

                }

            }
        });
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

    private void takePhotoWithDelay(long delay){

        //DJICameraSettingsDef.CameraMode cameraMode = DJICameraSettingsDef.CameraMode.ShootPhoto;

        final Handler h = new Handler();

        final Runnable photoThread = new Runnable() {

            @Override
            public void run() {

                final DJICamera camera = DJIConnection.getCameraInstance();

                if (camera != null) {

                    DJICameraSettingsDef.CameraShootPhotoMode photoMode = DJICameraSettingsDef.CameraShootPhotoMode.Single; // Set the camera capture mode as Single mode
                    camera.startShootPhoto(photoMode, new DJICommonCallbacks.DJICompletionCallback() {

                        @Override
                        public void onResult(DJIError error) {
                            if (error == null) {

                                photos_taken_count++;

                                updatePhotoCountUi();

                                Log.d(TAG, "takePhotoWithDelay: success");

                            } else {
                                Log.d(TAG, error.getDescription());
                            }
                        }

                    });
                }
            }

        };

        h.postDelayed(photoThread, delay);
    }

    private void startPano() {

        // User has started the pano
        if (!pano_in_progress) {

            // We need to reset the gimbal first
            resetGimbal();

            // Precalcuate panorama parameters.
            setupPanoramaShoot();

        	            
            // Set the pano state and change the button icon
            pano_in_progress = true;
            panoButton.setImageResource(R.drawable.stop_icon);

			// Inspire
            //shootPanoWithGimbalAndCustomMission();

			shootColumn();

            showToast("Starting panorama");

        } else {
			// User has stopped the pano
        
            pano_in_progress = false;
            showToast("Stopping panorama. Please wait...");

        }
    }

    // Setup our gimbal pitch/yaw angles
    float[] pitches;
    float[] yaws;

    private int pitchCount = 0;
    private int yawCount = 0;

    private void setupPanoramaShoot()
    {
        settings = SettingsManager.getInstance().getSettings(DJIConnection.getModelSafely());

        // Setup for old/previous method?
        pitchCount = 0;
        yawCount = 0;

        float pitchAngle = settings.getPitchAngle();
        float yawAngle = settings.getYawAngle();

        pitches = new float[settings.getNumberOfRows()];
        for (int i = 0; i < pitches.length; i++)
        {
            pitches[i] = i * pitchAngle;
            if (pitches[i] > 90)
            {
                pitches[i] -= 180;
            }
        }

        yaws = new float[settings.getPhotosPerRow()];
        for (int i = 0; i < yaws.length; i++)
        {
            yaws[i] = i * yawAngle;
            if (yaws[i] > 180)
            {
                yaws[i] -= 360;
            }
        }

        // Setup for custom mission / shoot column
        column_counter = 0;
        photos_taken_count = 0;

        updatePhotoCountUi();
    }

    /*
    Shoot a pano with gimbal only
    The sequence is pitch gimbal, take photo, and repeat
    Once the column is complete we then yaw the gimbal and repeat above
    */
    private void shootPanoWithGimbalOnly() {

        Log.d(TAG, "shootPanoWithGimbal called, pitch count: " + pitchCount + ", yaw count: " + yawCount);

        final Handler h = new Handler();

        // Begin take photo
        final Runnable photoThread = new Runnable() {

            @Override
            public void run() {

                Log.d(TAG, "Taking photo");

                takePhotoWithDelay(1000);

                // Increment the loop counter
                pitchCount++;

                // Move to next sequence
                shootPanoWithGimbalOnly();

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
                    shootPanoWithGimbalOnly();

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

    private void yawAircraftCustomMission() {

        Log.d(TAG, "shootPanoWithAircraftYawCutomMission");

        LinkedList<DJIMissionStep> steps = new LinkedList<DJIMissionStep>();

        steps.add(yawAircraftStep(settings.getYawAngle()));
        prepareAndStartCustomMission(steps);

        // This should work but doesn't - bug in DJI SDK 3.5.
        /*steps.add(pitchYawGimbalStep(0, 0));
        steps.add(photoStep());
        steps.add(pitchGimbalStep(10f));
        steps.add(photoStep());
        steps.add(pitchGimbalStep(20f));
        steps.add(photoStep());
        steps.add(pitchYawGimbalStep(0, 60));*/


    }

    private DJIAircraftYawStep yawAircraftStep(float angle) {

        return new DJIAircraftYawStep(angle, 40,

                new DJICommonCallbacks.DJICompletionCallback() {

                    @Override
                    public void onResult(DJIError error) {

                        if (error == null) {

                            Log.d(TAG, "Yaw step success");

                        } else {

                            Log.d(TAG, "Yaw step error");

                        }
                    }
                });

    }


    /*
    @Override
    public void missionProgressStatus(DJIMission.DJIMissionProgressStatus progressStatus) {

        if (progressStatus == null) {
            return;
        }

        if (progressStatus instanceof DJICustomMission.DJICustomMissionProgressStatus) {

            String currentStep = ((DJICustomMission.DJICustomMissionProgressStatus) progressStatus).getCurrentExecutingStep() == null
                    ? "Null" : ((DJICustomMission.DJICustomMissionProgressStatus) progressStatus)
                    .getCurrentExecutingStep().getClass().getSimpleName();



        }
        else if (progressStatus instanceof DJIWaypointMission.DJIWaypointMissionStatus || progressStatus instanceof DJIHotPointMission.DJIHotPointMissionStatus || progressStatus instanceof DJIPanoramaMission.DJIPanoramaMissionStatus ||
                        progressStatus instanceof DJIFollowMeMission.DJIFollowMeMissionStatus
                ) {

            // Do nothing for now

        } else {

            DJIError error = progressStatus.getError();

            if(error == null) {


            } else {

                Log.d(TAG, "Mission progress error: " + error.getDescription());

            }

        }

    }
    */

    /* GIMBAL STEP IS BROKEN IN DJI ANDROID SDK 3.5.1
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

        return new DJIGimbalAttitudeStep(DJIGimbalRotateAngleMode.RelativeAngle,
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
*/

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

    boolean pano_in_progress = false;
    // The goal here is to shoot a column of photos regardless of aircraft or gimbal yaw approach
    // The sequence is pitch, shoot, pitch, shoot, etc
    // Let's not shoot the nadir photo here
    private void shootColumn() {

        final Handler h = new Handler();

        final Runnable photoThread = new Runnable() {

            @Override
            public void run() {

                // Check if the user has stopped the pano
                if (!pano_in_progress) {

                    cancelPano();
                    return;

                }

                Log.d(TAG, "Taking photo");

                // Take the photo with no delay
                takePhotoWithDelay(0);

                // Increment the column counter
                column_counter++;

                // Loop again
                shootColumn();

            }

        };

        final Runnable pitchThread = new Runnable() {

            @Override
            public void run() {

                // Check if the user has stopped the pano
                if (!pano_in_progress) {

                    cancelPano();
                    return;

                }

                if (column_counter < settings.getNumberOfRows()) {

                    float angle = settings.getPitchAngle() * column_counter * -1;

                    Log.d(TAG, "Pitching gimbal to: " + angle);

                    pitchGimbal(angle);

                    // Give the gimbal 1.5 seconds to pitch before we take the photo
                    h.postDelayed(photoThread, 1500);

                // We've done a full column so let's yaw the gimbal or aircraft
                } else {

                    column_counter = 0;

                    missionYawCount++;

                    // Now yaw
                    yawAircraftCustomMission();

                    Log.d(TAG, "Yawing to new position");
                }


            }

        };

        // Delay three seconds because this will get called after a photo and we want to delay
        // This logic will need to be cleaned up to pitch only after we've written a file to the
        // SD card
        h.postDelayed(pitchThread, 3000);

    }

    // Shoot the final nadir shot
    // Make this configurable to support more than one
    private void shootNadir() {

        pitchGimbal(-90);

        final Handler h = new Handler();

        final Runnable photoThread = new Runnable() {

            @Override
            public void run() {

                takePhotoWithDelay(0);

                finishPano();

            }

        };

        h.postDelayed(photoThread, 1500);

    }

    // Last step of the pano and give the user feedback
    private void finishPano() {

        final Handler h = new Handler();

        final Runnable gimbalThread = new Runnable() {

            @Override
            public void run() {

                runOnUiThread(new Runnable() {
                    public void run() {
                        panoButton.setImageResource(R.drawable.start_icon);
                    }
                });

                showToast("Pano completed successfully");

                resetGimbal();

                // Reset the counters
                photos_taken_count = 0;
                missionYawCount = 0;
                column_counter = 0;

                // So the user can shoot another pano
                pano_in_progress = false;

            }

        };

        h.postDelayed(gimbalThread, 3000);

    }

    // If a user stops the pano during the process
    private void cancelPano() {

        runOnUiThread(new Runnable() {

            @Override
            public void run() {

                panoButton.setImageResource(R.drawable.start_icon);
                sequenceLabel.setText("Photo: -");

            }
        });

        // Reset the counters
        photos_taken_count = 0;
        missionYawCount = 0;
        column_counter = 0;

        resetGimbal();

        showToast("Pano stopped successfully");

    }


    // Pitch gimbal to specific angle
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

    private void updatePhotoCountUi()
    {
        // Update UI.
        runOnUiThread(new Runnable() {
            public void run() {
                sequenceLabel.setText("Photo: " + photos_taken_count + "/" + settings.getNumberOfPhotos());
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
