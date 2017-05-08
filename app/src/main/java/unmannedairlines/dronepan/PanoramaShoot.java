package unmannedairlines.dronepan;

import android.support.annotation.Nullable;

import dji.common.error.DJIError;
import dji.common.gimbal.Attitude;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.mission.MissionControl;
import dji.sdk.mission.timeline.TimelineElement;
import dji.sdk.mission.timeline.TimelineEvent;
import dji.sdk.mission.timeline.actions.AircraftYawAction;
import dji.sdk.mission.timeline.actions.GimbalAttitudeAction;
import dji.sdk.mission.timeline.actions.ShootPhotoAction;

public class PanoramaShoot implements MissionControl.Listener {
    private DJIConnection connection;
    private FlightController flightController;

    private MissionControl missionControl;
    private Settings settings;

    private int numberOfPhotosTaken;
    private int totalNumberOfPhotos;

    public PanoramaShoot(DJIConnection connection) {
        this.connection = connection;
        missionControl = MissionControl.getInstance();
        missionControl.addListener(this);
    }

    public void setup(Settings settings)
    {
        missionControl.unscheduleEverything();

        settings = SettingsManager.getInstance().getSettings(connection.getModelSafely());

        if (settings.getShootRowByRow()) {
            setupRowByRow();
        }
        else {
            setupColumnByColumn();
        }
    }

    private void setupRowByRow() {
        int photosPerRow = settings.getPhotosPerRow();
        int numberOfRows = settings.getNumberOfRows();
        boolean useGimbalYaw = settings.getRelativeGimbalYaw();

        for (int r = 0; r < numberOfRows; r++) {
            addGimbalPitchAction(settings.getPitchAngle() * r);

            for (int i = 0; i < photosPerRow; i++) {
                if (useGimbalYaw) {
                    addGimbalYawAction(settings.getYawAngle() * i);
                }
                else {
                    addAircraftYawAction(settings.getYawAngle());
                }

                addPhotoShootAction();
            }
        }
    }


    private void setupColumnByColumn() {
        int photosPerRow = settings.getPhotosPerRow();
        int numberOfRows = settings.getNumberOfRows();
        boolean useGimbalYaw = settings.getRelativeGimbalYaw();

        for (int c = 0; c < photosPerRow; c++) {
            if (useGimbalYaw) {
                addGimbalYawAction(settings.getYawAngle() * c);
            }
            else {
                addAircraftYawAction(settings.getYawAngle());
            }

            for (int i = 0; i < numberOfRows; i++) {
                addGimbalPitchAction(settings.getPitchAngle() * i);
                addPhotoShootAction();
            }
        }
    }

    private void addAircraftYawAction(float relativeYaw) {
        AircraftYawAction aircraftYawAction = new AircraftYawAction(relativeYaw, 40);
        missionControl.scheduleElement(aircraftYawAction);
    }


    private void addGimbalYawAction(float absoluteYaw) {
        Attitude attitude = new Attitude(absoluteYaw, 0, 0);
        GimbalAttitudeAction gimbalAttitudeAction = new GimbalAttitudeAction(attitude);
        missionControl.scheduleElement(gimbalAttitudeAction);
    }

    private void addGimbalPitchAction(float pitch) {
        Attitude attitude = new Attitude(0, 0, pitch);
        GimbalAttitudeAction gimbalAttitudeAction = new GimbalAttitudeAction(attitude);
        missionControl.scheduleElement(gimbalAttitudeAction);
    }

    private void addPhotoShootAction() {
        int delayBeforeEachShot = (int)settings.getDelayBeforeEachShot();
        ShootPhotoAction photoAction = new ShootPhotoAction(1, delayBeforeEachShot);
        missionControl.scheduleElement(photoAction);
    }

    public boolean isMissionRunning() {
        return missionControl.isTimelineRunning();
    }

    public void start() {
        missionControl.startTimeline();
    }

    public void stop() {
        missionControl.stopTimeline();
    }

//    private void setup()
//    {
//        // Setup for old/previous method?
//        pitchCount = 0;
//        yawCount = 0;
//
//        float pitchAngle = settings.getPitchAngle();
//        float yawAngle = settings.getYawAngle();
//
//        pitches = new float[settings.getNumberOfRows()];
//        for (int i = 0; i < pitches.length; i++)
//        {
//            pitches[i] = i * pitchAngle;
//            if (pitches[i] > 90)
//            {
//                pitches[i] -= 180;
//            }
//        }
//
//        yaws = new float[settings.getPhotosPerRow()];
//        for (int i = 0; i < yaws.length; i++)
//        {
//            yaws[i] = i * yawAngle;
//            if (yaws[i] > 180)
//            {
//                yaws[i] -= 360;
//            }
//        }
//    }

    @Override
    public void onEvent(@Nullable TimelineElement previousElement, TimelineEvent nextElement, @Nullable DJIError djiError) {

    }

//    private void startPano() {
//
//        // User has started the pano
//        if (!pano_in_progress) {
//
//            // We need to reset the gimbal first
//            resetGimbal();
//
//            // Precalcuate panorama parameters.
//            setupPanoramaShoot();
//
//
//            // Set the pano state and change the button icon
//            pano_in_progress = true;
//            panoButton.setImageResource(R.drawable.stop_icon);
//
//            // Inspire
//            //shootPanoWithGimbalAndCustomMission();
//
//            shootColumn();
//
//            showToast("Starting panorama");
//
//        } else {
//            // User has stopped the pano
//
//            pano_in_progress = false;
//            showToast("Stopping panorama. Please wait...");
//
//        }
//    }
//
//    // Setup our gimbal pitch/yaw angles
//    float[] pitches;
//    float[] yaws;
//
//    private int pitchCount = 0;
//    private int yawCount = 0;
//
//    private void setupPanoramaShoot()
//    {
//        settings = SettingsManager.getInstance().getSettings(DJIConnectionOld.getModelSafely());
//
//        // Setup for old/previous method?
//        pitchCount = 0;
//        yawCount = 0;
//
//        float pitchAngle = settings.getPitchAngle();
//        float yawAngle = settings.getYawAngle();
//
//        pitches = new float[settings.getNumberOfRows()];
//        for (int i = 0; i < pitches.length; i++)
//        {
//            pitches[i] = i * pitchAngle;
//            if (pitches[i] > 90)
//            {
//                pitches[i] -= 180;
//            }
//        }
//
//        yaws = new float[settings.getPhotosPerRow()];
//        for (int i = 0; i < yaws.length; i++)
//        {
//            yaws[i] = i * yawAngle;
//            if (yaws[i] > 180)
//            {
//                yaws[i] -= 360;
//            }
//        }
//
//        // Setup for custom mission / shoot column
//        column_counter = 0;
//        photos_taken_count = 0;
//
//        updatePhotoCountUi();
//    }
//
//    /*
//    Shoot a pano with gimbal only
//    The sequence is pitch gimbal, take photo, and repeat
//    Once the column is complete we then yaw the gimbal and repeat above
//    */
//    private void shootPanoWithGimbalOnly() {
//
//        Log.d(TAG, "shootPanoWithGimbal called, pitch count: " + pitchCount + ", yaw count: " + yawCount);
//
//        final Handler h = new Handler();
//
//        // Begin take photo
//        final Runnable photoThread = new Runnable() {
//
//            @Override
//            public void run() {
//
//                Log.d(TAG, "Taking photo");
//
//                takePhotoWithDelay(1000);
//
//                // Increment the loop counter
//                pitchCount++;
//
//                // Move to next sequence
//                shootPanoWithGimbalOnly();
//
//            }
//
//        };
//        // End take photo
//
//        // Yaw gimbal
//        final Runnable yawThread = new Runnable() {
//
//            @Override
//            public void run() {
//
//                if (yawCount < yaws.length) {
//
//                    // Yaw gimbal to next column
//                    yawGimbal(yaws[yawCount]);
//
//                    // Increment the yawCount
//                    yawCount++;
//
//                    // Move to next sequence
//                    shootPanoWithGimbalOnly();
//
//                } else {
//
//                    Log.d(TAG, "Done with pano sequence");
//
//                }
//
//            }
//
//        };
//        // End yaw gimbal
//
//        // Pitch gimbal
//        final Runnable pitchThread = new Runnable() {
//
//            @Override
//            public void run() {
//
//                if (pitchCount < pitches.length) {
//
//                    Log.d(TAG, "Pitching gimbal to: " + pitches[pitchCount]);
//
//                    // Pitch with 0 yaw
//                    pitchGimbal(pitches[pitchCount]);
//
//                    // Delay and shoot photo
//                    h.postDelayed(photoThread, 3000);
//
//                } else if (yawCount < yaws.length) {
//
//                    Log.d(TAG, "Yawing gimbal to: " + yaws[yawCount]);
//
//                    // Column of photos is complete. Yaw the gimbal for the next column.
//                    h.postDelayed(yawThread, 1000);
//
//                    // Reset the pitch count before we begin the next column
//                    pitchCount = 0;
//
//                } else {
//
//                    yawCount = 0;
//                    pitchCount = 0;
//
//                    // Rest the gimbal
//                    resetGimbal();
//
//                    Log.d(TAG, "We're done!!!");
//
//                }
//
//            }
//
//        };
//        // End pitch gimbal
//
//        // This is the entry point for each loop
//        h.postDelayed(pitchThread, 1000);
//    }
//
//    private void prepareAndStartCustomMission(LinkedList<DJIMissionStep> steps) {
//
//        Log.d(TAG, "shootPanoWithGimbalAndCustomMission");
//
//        final DJIMissionManager missionManager = DJIMissionManager.getInstance();
//
//        // Load the steps into a cusstom mission
//        DJICustomMission customMission = new DJICustomMission(steps);
//
//        // Prepare the mission
//        missionManager.prepareMission(customMission, new DJIMission.DJIMissionProgressHandler() {
//
//            @Override
//            public void onProgress(DJIMission.DJIProgressType type, float progress) {
//                //setProgressBar((int)(progress * 100f));
//            }
//
//        }, new DJICommonCallbacks.DJICompletionCallback() {
//            @Override
//            public void onResult(DJIError error) {
//                if (error == null) {
//
//                    // Success preparing mission, let's start the mission
//                    missionManager.startMissionExecution(new DJICommonCallbacks.DJICompletionCallback() {
//
//                        @Override
//                        public void onResult(DJIError mError) {
//
//                            if (mError == null) {
//
//                                // Success starting mission
//                                Log.d(TAG, "Starting mission");
//
//                            } else {
//
//                                // Error starting mission
//                                Log.d(TAG, "Error starting mission");
//
//                            }
//                        }
//                    });
//
//                } else {
//                    // Error preparing mission
//                    Log.d(TAG, "Error preparing mission");
//                }
//            }
//        });
//
//    }
//
//    private void yawAircraftCustomMission() {
//
//        Log.d(TAG, "shootPanoWithAircraftYawCutomMission");
//
//        LinkedList<DJIMissionStep> steps = new LinkedList<DJIMissionStep>();
//
//        steps.add(yawAircraftStep(settings.getYawAngle()));
//        prepareAndStartCustomMission(steps);
//
//        // This should work but doesn't - bug in DJI SDK 3.5.
//        /*steps.add(pitchYawGimbalStep(0, 0));
//        steps.add(photoStep());
//        steps.add(pitchGimbalStep(10f));
//        steps.add(photoStep());
//        steps.add(pitchGimbalStep(20f));
//        steps.add(photoStep());
//        steps.add(pitchYawGimbalStep(0, 60));*/
//
//
//    }
//
//    private DJIAircraftYawStep yawAircraftStep(float angle) {
//
//        return new DJIAircraftYawStep(angle, 40,
//
//                new DJICommonCallbacks.DJICompletionCallback() {
//
//                    @Override
//                    public void onResult(DJIError error) {
//
//                        if (error == null) {
//
//                            Log.d(TAG, "Yaw step success");
//
//                        } else {
//
//                            Log.d(TAG, "Yaw step error");
//
//                        }
//                    }
//                });
//
//    }
//
//
//    private DJIShootPhotoStep photoStep() {
//
//        return new DJIShootPhotoStep(new DJICommonCallbacks.DJICompletionCallback() {
//
//            @Override
//            public void onResult(DJIError error) {
//
//                if (error == null) {
//
//                    Log.d(TAG, "Shoot photo successful");
//
//                } else {
//
//                    Log.d(TAG, "Error shooting photo");
//
//                }
//            }
//        });
//    }
//
//    boolean pano_in_progress = false;
//    // The goal here is to shoot a column of photos regardless of aircraft or gimbal yaw approach
//    // The sequence is pitch, shoot, pitch, shoot, etc
//    // Let's not shoot the nadir photo here
//    private void shootColumn() {
//
//        final Handler h = new Handler();
//
//        final Runnable photoThread = new Runnable() {
//
//            @Override
//            public void run() {
//
//                // Check if the user has stopped the pano
//                if (!pano_in_progress) {
//
//                    cancelPano();
//                    return;
//
//                }
//
//                Log.d(TAG, "Taking photo");
//
//                // Take the photo with no delay
//                takePhotoWithDelay(0);
//
//                // Increment the column counter
//                column_counter++;
//
//                // Loop again
//                shootColumn();
//
//            }
//
//        };
//
//        final Runnable pitchThread = new Runnable() {
//
//            @Override
//            public void run() {
//
//                // Check if the user has stopped the pano
//                if (!pano_in_progress) {
//
//                    cancelPano();
//                    return;
//
//                }
//
//                if (column_counter < settings.getNumberOfRows()) {
//
//                    float angle = settings.getPitchAngle() * column_counter * -1;
//
//                    Log.d(TAG, "Pitching gimbal to: " + angle);
//
//                    pitchGimbal(angle);
//
//                    // Give the gimbal 1.5 seconds to pitch before we take the photo
//                    h.postDelayed(photoThread, 1500);
//
//                    // We've done a full column so let's yaw the gimbal or aircraft
//                } else {
//
//                    column_counter = 0;
//
//                    missionYawCount++;
//
//                    // Now yaw
//                    yawAircraftCustomMission();
//
//                    Log.d(TAG, "Yawing to new position");
//                }
//
//
//            }
//
//        };
//
//        // Delay three seconds because this will get called after a photo and we want to delay
//        // This logic will need to be cleaned up to pitch only after we've written a file to the
//        // SD card
//        h.postDelayed(pitchThread, 3000);
//
//    }
//
//    // Shoot the final nadir shot
//    // Make this configurable to support more than one
//    private void shootNadir() {
//
//        pitchGimbal(-90);
//
//        final Handler h = new Handler();
//
//        final Runnable photoThread = new Runnable() {
//
//            @Override
//            public void run() {
//
//                takePhotoWithDelay(0);
//
//                finishPano();
//
//            }
//
//        };
//
//        h.postDelayed(photoThread, 1500);
//
//    }
//
//    // Last step of the pano and give the user feedback
//    private void finishPano() {
//
//        final Handler h = new Handler();
//
//        final Runnable gimbalThread = new Runnable() {
//
//            @Override
//            public void run() {
//
//                runOnUiThread(new Runnable() {
//                    public void run() {
//                        panoButton.setImageResource(R.drawable.start_icon);
//                    }
//                });
//
//                showToast("Pano completed successfully");
//
//                resetGimbal();
//
//                // Reset the counters
//                photos_taken_count = 0;
//                missionYawCount = 0;
//                column_counter = 0;
//
//                // So the user can shoot another pano
//                pano_in_progress = false;
//
//            }
//
//        };
//
//        h.postDelayed(gimbalThread, 3000);
//
//    }
//
//    // If a user stops the pano during the process
//    private void cancelPano() {
//
//        runOnUiThread(new Runnable() {
//
//            @Override
//            public void run() {
//
//                panoButton.setImageResource(R.drawable.start_icon);
//                sequenceLabel.setText("Photo: -");
//
//            }
//        });
//
//        // Reset the counters
//        photos_taken_count = 0;
//        missionYawCount = 0;
//        column_counter = 0;
//
//        resetGimbal();
//
//        showToast("Pano stopped successfully");
//
//    }
//
//
//    // Pitch gimbal to specific angle
//    private void pitchGimbal(float pitch) {
//
//        setGimbalAttitude(pitch, 0, true, false);
//
//    }
//
//    /*
//    When we yaw we want to reset the pitch to
//    zero as well. This is the start of a new column.
//     */
//    private void yawGimbal(float yaw) {
//
//        setGimbalAttitude(0, yaw, true, true);
//
//    }
//
//    private void setGimbalAttitude(float pitch, float yaw, boolean pitchEnabled, boolean yawEnabled) {
//
//        Log.d(TAG, "setGimbalAttitude called with pitch: " + pitch + " and yaw: " + yaw);
//
//        DJIGimbalAngleRotation gimbalPitch = new DJIGimbalAngleRotation(pitchEnabled, pitch, DJIGimbalRotateDirection.Clockwise);
//        DJIGimbalAngleRotation gimbalRoll = new DJIGimbalAngleRotation(false, 0, DJIGimbalRotateDirection.Clockwise);
//        DJIGimbalAngleRotation gimbalYaw = new DJIGimbalAngleRotation(yawEnabled, yaw, DJIGimbalRotateDirection.Clockwise);
//
//        DJIConnectionOld.getProductInstance().getGimbal().rotateGimbalByAngle(DJIGimbalRotateAngleMode.AbsoluteAngle, gimbalPitch, gimbalRoll, gimbalYaw,
//                new DJICommonCallbacks.DJICompletionCallback() {
//                    @Override
//                    public void onResult(DJIError error) {
//                        if (error == null) {
//                            Log.d(TAG, "rotateGimbalByAngle success");
//                        } else {
//                            Log.d(TAG, "rotateGimbalByAngle error");
//                        }
//                    }
//                });
//
//    }
//
//    private void resetGimbal() {
//
//        DJIGimbalAngleRotation gimbalPitch = new DJIGimbalAngleRotation(true, 0, DJIGimbalRotateDirection.Clockwise);
//        DJIGimbalAngleRotation gimbalRoll = new DJIGimbalAngleRotation(true, 0, DJIGimbalRotateDirection.Clockwise);
//        DJIGimbalAngleRotation gimbalYaw = new DJIGimbalAngleRotation(true, 0, DJIGimbalRotateDirection.Clockwise);
//
//        ///DJIConnectionOld.getProductInstance().getGimbal().rotateGimbalByAngle(DJIGimbalRotateAngleMode.AbsoluteAngle, gimbalPitch, gimbalRoll, gimbalYaw,
//                new DJICommonCallbacks.DJICompletionCallback() {
//                    @Override
//                    public void onResult(DJIError error) {
//                        if (error == null) {
//
//                        }
//                    }
//                });
//    }

}
