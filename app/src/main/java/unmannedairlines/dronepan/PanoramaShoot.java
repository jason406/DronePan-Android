package unmannedairlines.dronepan;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;

import dji.common.camera.SystemState;
import dji.common.error.DJIError;
import dji.common.gimbal.Attitude;
import dji.common.gimbal.Rotation;
import dji.sdk.camera.Camera;
import dji.sdk.mission.MissionControl;
import dji.sdk.mission.timeline.TimelineElement;
import dji.sdk.mission.timeline.TimelineEvent;
import dji.sdk.mission.timeline.actions.AircraftYawAction;
import dji.sdk.mission.timeline.actions.GimbalAttitudeAction;
import dji.sdk.mission.timeline.actions.ShootPhotoAction;

public class PanoramaShoot implements MissionControl.Listener, SystemState.Callback {

    private static final String TAG = PanoramaShoot.class.getName();

    private Camera camera;
    private MissionControl missionControl;
    private Settings settings;

    private int numberOfPhotosTaken;
    private int totalNumberOfPhotos;

    private boolean inProgress;
    private Queue<TimelineElement> elements;
    private boolean isCameraReady;

    public interface Listener {
        void onPanoramaShootUpdate();
    }

    private Listener listener;

    public PanoramaShoot() {
        this.elements = new LinkedList<TimelineElement>();

        this.camera = DJIConnection.getInstance().getCamera();
        this.camera.setSystemStateCallback(this);

        this.missionControl = MissionControl.getInstance();
        this.missionControl.addListener(this);
    }

    public void setListener(Listener listener){
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
        boolean useGimbalYaw = false; //settings.getRelativeGimbalYaw();

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

    private void setupNadirShots()
    {
        int numberOfNadirShots = settings.getNumberOfNadirShots();
        boolean useGimbalYaw = settings.getRelativeGimbalYaw();

        if (numberOfNadirShots == 0)
        {
            return;
        }

        float nadirAngle = 360.0f / numberOfNadirShots;

        this.addGimbalPitchAction(90);
        this.addPhotoShootAction();

        for (int i = 1; i < numberOfNadirShots; i++)
        {
            if (useGimbalYaw)
            {
                this.addGimbalYawAction(nadirAngle * i);
            }
            else
            {
                this.addAircraftYawAction(nadirAngle);
            }

            this.addPhotoShootAction();
        }
    }

    private void addAircraftYawAction(float relativeYaw) {
        AircraftYawAction aircraftYawAction = new AircraftYawAction(relativeYaw, 40);
        this.elements.add(aircraftYawAction);
    }

    private void addGimbalYawAction(float absoluteYaw) {
        Attitude attitude = new Attitude(Rotation.NO_ROTATION, Rotation.NO_ROTATION, absoluteYaw);
        GimbalAttitudeAction gimbalAttitudeAction = new GimbalAttitudeAction(attitude);
        this.elements.add(gimbalAttitudeAction);
    }

    private void addGimbalPitchAction(float pitch) {
        Attitude attitude = new Attitude(pitch, Rotation.NO_ROTATION, Rotation.NO_ROTATION);
        GimbalAttitudeAction gimbalAttitudeAction = new GimbalAttitudeAction(attitude);
        this.elements.add(gimbalAttitudeAction);
    }

    private void addPhotoShootAction() {
        //int delayBeforeEachShot = 2;//(int)Math.round(settings.getDelayBeforeEachShot());
        ShootPhotoAction photoAction = new ShootPhotoAction();//, delayBeforeEachShot);
        //missionControl.scheduleElement(photoAction);
        this.elements.add(photoAction);
        this.totalNumberOfPhotos++;
    }

    public boolean isMissionRunning() {
        return missionControl.isTimelineRunning();
    }

    public void start() {
        this.continueWork();
    }

    public void stop() {
        if (this.missionControl.isTimelineRunning())
        {
            this.missionControl.stopTimeline();
        }
    }

    private void continueWork() {
        if (!this.inProgress) {
            return;
        }

        if (isPhotoShootNext()) {
            this.tryShootPhoto();

            return;
        }

        this.missionControl.unscheduleEverything();
        while (!isPhotoShootNext())
        {
            this.missionControl.scheduleElement(this.elements.remove());
        }

        this.missionControl.startTimeline();
    }

    private boolean isPhotoShootNext()
    {
        TimelineElement element = this.elements.peek();
        return (element instanceof ShootPhotoAction);
    }

    private void tryShootPhoto() {
        if (!this.isCameraReady) {
        }
    }


    @Override
    public void onEvent(@Nullable TimelineElement element, TimelineEvent event, @Nullable DJIError error) {
        if (error != null) {
            Log.e(TAG, error.toString());
        }

        if (event == TimelineEvent.FINISHED) {
            this.continueWork();
        }

        if (element instanceof ShootPhotoAction && event == TimelineEvent.ELEMENT_FINISHED) {
            this.numberOfPhotosTaken++;
            Log.e(TAG, "Picture taken: " + this.numberOfPhotosTaken);
            notifyListener();
        }
    }

    private void notifyListener()
    {
        if (this.listener != null)
        {
            this.listener.onPanoramaShootUpdate();
        }
    }
}
