package unmannedairlines.dronepan.logic;

import android.support.annotation.Nullable;
import android.util.Log;

import dji.common.error.DJIError;
import dji.common.gimbal.Attitude;
import dji.common.gimbal.Rotation;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.mission.MissionControl;
import dji.sdk.mission.timeline.TimelineElement;
import dji.sdk.mission.timeline.TimelineEvent;
import dji.sdk.mission.timeline.actions.GimbalAttitudeAction;
import dji.sdk.mission.timeline.actions.ShootPhotoAction;
import unmannedairlines.dronepan.mission.CustomAircraftYawAction;
import unmannedairlines.dronepan.mission.DelayAction;
import unmannedairlines.dronepan.mission.WaitForCameraReadyAction;
import unmannedairlines.dronepan.mission.helpers.CameraSystemStateController;

public class PanoramaShoot implements MissionControl.Listener {
    private static final String TAG = PanoramaShoot.class.getName();

    private FlightController flightController;
    private MissionControl missionControl;
    private Settings settings;
    private CameraSystemStateController cameraStateController;

    private int numberOfPhotosTaken;
    private int totalNumberOfPhotos;

    public interface Listener {
        void onPanoramaShootUpdate();
    }

    private Listener listener;

    public PanoramaShoot(DJIConnection djiConnection) {
        this.missionControl = MissionControl.getInstance();
        this.missionControl.addListener(this);

        if (djiConnection.getCamera() != null) {
            this.cameraStateController = new CameraSystemStateController(djiConnection.getCamera());
        }
    }

    public void setListener(Listener listener) {
        this.listener = listener;
        this.notifyListener();
    }

    public int getNumberOfPhotosTaken() {
        return this.numberOfPhotosTaken;
    }

    public int getTotalNumberOfPhotos() {
        return this.totalNumberOfPhotos;
    }

    public void setup(Settings settings) {
        this.numberOfPhotosTaken = 0;
        this.totalNumberOfPhotos = 0;

        this.missionControl.unscheduleEverything();

        this.settings = settings;

        if (this.settings.getShootRowByRow()) {
            this.setupRowByRow();
        }
        else {
            this.setupColumnByColumn();
        }

        this.setupNadirShots();
        this.notifyListener();
    }

    private void setupRowByRow() {
        int photosPerRow = settings.getPhotosPerRow();
        int numberOfRows = settings.getNumberOfRows();
        boolean useGimbalYaw = settings.getUseGimbalToYaw();

        for (int r = 0; r < numberOfRows; r++) {
            this.addGimbalPitchAction(settings.getPitchAngle() * -r);

            for (int i = 0; i < photosPerRow; i++) {
                this.addPhotoShootAction();

                if (useGimbalYaw) {
                    this.addGimbalYawAction(settings.getYawAngle() * (i+1));
                }
                else {
                    this.addAircraftYawAction(settings.getYawAngle());
                }
            }
        }
    }

    private void setupColumnByColumn() {
        int photosPerRow = settings.getPhotosPerRow();
        int numberOfRows = settings.getNumberOfRows();
        boolean useGimbalYaw = settings.getUseGimbalToYaw();

        for (int c = 0; c < photosPerRow; c++) {
            for (int i = 0; i < numberOfRows; i++) {
                this.addGimbalPitchAction(settings.getPitchAngle() * -i);
                this.addPhotoShootAction();
            }

            if (useGimbalYaw) {
                this.addGimbalYawAction(settings.getYawAngle() * (c + 1));
            }
            else {
                this.addAircraftYawAction(settings.getYawAngle());
            }
        }
    }

    private void setupNadirShots() {
        int numberOfNadirShots = settings.getNumberOfNadirShots();
        boolean useGimbalYaw = settings.getUseGimbalToYaw();

        if (numberOfNadirShots == 0) {
            return;
        }

        float nadirAngle = 360.0f / numberOfNadirShots;

        this.addGimbalPitchAction(-90.0f);
        this.addPhotoShootAction();

        for (int i = 1; i < numberOfNadirShots; i++) {
            if (useGimbalYaw) {
                this.addGimbalYawAction(nadirAngle * i);
            }
            else {
                this.addAircraftYawAction(nadirAngle);
            }

            this.addPhotoShootAction();
        }
    }

    private void addAircraftYawAction(float relativeYaw) {
        CustomAircraftYawAction aircraftYawAction = new CustomAircraftYawAction(relativeYaw, 20);
        this.missionControl.scheduleElement(aircraftYawAction);
    }

    private void addGimbalYawAction(float absoluteYaw) {
        Attitude attitude = new Attitude(Rotation.NO_ROTATION, Rotation.NO_ROTATION, absoluteYaw);
        GimbalAttitudeAction gimbalAttitudeAction = new GimbalAttitudeAction(attitude);
        this.missionControl.scheduleElement(gimbalAttitudeAction);
    }

    private void addGimbalPitchAction(float pitch) {
        Attitude attitude = new Attitude(pitch, Rotation.NO_ROTATION, Rotation.NO_ROTATION);
        GimbalAttitudeAction gimbalAttitudeAction = new GimbalAttitudeAction(attitude);
        this.missionControl.scheduleElement(gimbalAttitudeAction);
    }

    private void addPhotoShootAction() {
        WaitForCameraReadyAction waitForCameraReadyAction = new WaitForCameraReadyAction(this.cameraStateController);
        this.missionControl.scheduleElement(waitForCameraReadyAction);

        int delayInMilliseconds = this.settings.getDelayBeforeEachShotInMs();
        if (delayInMilliseconds > 0) {
            DelayAction delayAction = new DelayAction(delayInMilliseconds);
            this.missionControl.scheduleElement(delayAction);
        }

        ShootPhotoAction photoAction = new ShootPhotoAction();
        this.missionControl.scheduleElement(photoAction);

        this.totalNumberOfPhotos++;
    }

    public boolean isMissionRunning() {
        return missionControl.isTimelineRunning();
    }

    public void start() {
        this.missionControl.startTimeline();
    }

    public void stop() {
        if (this.missionControl.isTimelineRunning()) {
            this.missionControl.stopTimeline();
        }
    }

    @Override
    public void onEvent(@Nullable TimelineElement element, TimelineEvent event, @Nullable DJIError error) {
        if (error != null) {
            Log.e(TAG, error.toString());
        }

        String message = "";
        if (event != null) {
            message += "Mission Event: " + event.toString();
        }
        if (element != null) {
            message += " Element: " + element.toString();
        }

        Log.i(TAG, message);

        if (event == TimelineEvent.STARTED ||
            event == TimelineEvent.FINISHED ||
            event == TimelineEvent.STOPPED ||
            event == TimelineEvent.STOP_ERROR) {
            this.notifyListener();
        }

        if (element instanceof ShootPhotoAction && event == TimelineEvent.FINISHED) {
            this.numberOfPhotosTaken++;
            Log.e(TAG, "Picture taken: " + this.numberOfPhotosTaken);
            this.notifyListener();
        }
    }

    private void notifyListener() {
        if (this.listener != null) {
            this.listener.onPanoramaShootUpdate();
        }
    }
}
