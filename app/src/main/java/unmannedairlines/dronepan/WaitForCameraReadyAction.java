package unmannedairlines.dronepan;

import android.support.annotation.NonNull;

import dji.common.camera.SystemState;
import dji.common.error.DJIError;
import dji.sdk.camera.Camera;
import dji.sdk.mission.MissionControl;
import dji.sdk.mission.timeline.TimelineElement;
import dji.sdk.mission.timeline.TimelineElementFeedback;


public class WaitForCameraReadyAction extends TimelineElement implements SystemState.Callback {

    Camera camera;
    TimelineElementFeedback feedback;

    public WaitForCameraReadyAction() {
        this.camera = DJIConnection.getInstance().getCamera();
        this.feedback = MissionControl.getInstance();
    }

    @Override
    public void run() {
        this.feedback.onStart(this);
        this.camera.setSystemStateCallback(this);
    }

    @Override
    public boolean isPausable() {
        return false;
    }

    @Override
    public void stop() {
        this.camera.setSystemStateCallback(null);
        this.feedback.onStopWithError(this, null);
    }

    @Override
    public DJIError checkValidity() {
        return null;
    }

    @Override
    public void onUpdate(@NonNull SystemState systemState) {
        if (CameraSystemStateExtensions.isReady(systemState)) {
            this.feedback.onFinishWithError(this, null);
        }
    }
}
