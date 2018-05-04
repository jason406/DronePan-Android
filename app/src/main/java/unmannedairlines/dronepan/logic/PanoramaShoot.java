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
import dji.sdk.mission.timeline.actions.AircraftYawAction;
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

    public void  initPanorama(Settings settings) {


        this.numberOfPhotosTaken = 0;
        this.totalNumberOfPhotos = 0;

        this.missionControl.unscheduleEverything();

        this.settings = settings;
        boolean useGimbalYaw = settings.getUseGimbalToYaw();
        int panoKind = settings.getNumberOfNadirShots();

        int[] pitchAngles;
        int[] numberOfPhotos;

        //panokind: 1-->72fov 15mm lens
        // 2--> 45mm lens

        switch (panoKind) {
            case 1://72fov 15mm lens
                pitchAngles=new int[]{0, 25, 54, 90};
                numberOfPhotos=new int[]{7,7,5,3};
            case 2://
                pitchAngles=new int[]{0,     8,    16,    26,    35,    45,    55,    66,    76,    90};
                numberOfPhotos=new int[]{23 ,23,22,21 ,19 , 16 ,13 ,  10 ,   6,   3};
            default:
                pitchAngles=new int[]{0,     8,    16,    26,    35,    45,    55,    66,    76,    90};
                numberOfPhotos=new int[]{23 ,23,22,21 ,19 , 16 ,13 ,  10 ,   6,   3};

                //30 overlap 187
//                pitchAngles=new int[]{0, 7,14,23,31,41,50,60,69,79,90};
//                numberOfPhotos=new int[]{25,25,25,23,22,19,16,13, 9, 6, 4};

        }
        Log.i(TAG, "initPanorama: case"+ panoKind);




        //inspire 2 60 fov
        //0    21    46    72    90
        //9     8     6     3     3
        //inspire 2 45mm lens


        float stepYaw;
        if (useGimbalYaw) {

            for (int i=0;i<pitchAngles.length;i++)
            {
                this.addGimbalPitchAction(-pitchAngles[i]);
                stepYaw=360/numberOfPhotos[i];
                for (int j=0;j<numberOfPhotos[i];j++)
                {
                    float yaw = stepYaw*j;
                    if (i%2==0) //偶数行-180~180
                    {
                        this.addGimbalAction(-pitchAngles[i],-179F+yaw);
//                        this.addGimbalYawAction(-179+yaw);
                        Log.i(TAG, "pitch:"+-pitchAngles[i]+" yaw:"+ (-180+yaw) );
                    }
                    else
                    {
                        this.addGimbalAction(-pitchAngles[i],180F-yaw);
//                        this.addGimbalYawAction(180-yaw);
                        Log.i(TAG, "pitch:"+-pitchAngles[i]+" yaw:"+ (180-yaw) );
                    }
                    this.addPhotoShootAction();

                }

            }
        }
        else {
            //// TODO: 2017/6/2 uav yaw
            for (int i=0;i<pitchAngles.length;i++)
            {
                this.addGimbalPitchAction(-pitchAngles[i]);
                stepYaw=360/numberOfPhotos[i];
                for (int j=0;j<numberOfPhotos[i];j++)
                {
                    this.addPhotoShootAction();
                    this.addAircraftYawActionv2(stepYaw);//raletive yaw
                }

            }
        }

        //this.setupNadirShots();
        this.notifyListener();
    }

    private void addAircraftYawActionv2(float relativeYaw) {
        AircraftYawAction aircraftYawAction = new AircraftYawAction(relativeYaw, 60);
        this.missionControl.scheduleElement(aircraftYawAction);
    }

    private void addGimbalAction(float pitch, float absoluteYaw) {
        // actually is relative YAW
        Attitude attitude = new Attitude(pitch, Rotation.NO_ROTATION, absoluteYaw);
        GimbalAttitudeAction gimbalAttitudeAction = new GimbalAttitudeAction(attitude);
//        gimbalAttitudeAction.setCompletionTime(2);
        this.missionControl.scheduleElement(gimbalAttitudeAction);
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
