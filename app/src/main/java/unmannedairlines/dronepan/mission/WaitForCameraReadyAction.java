package unmannedairlines.dronepan.mission;

import android.support.annotation.Nullable;

import dji.common.error.DJIError;
import dji.sdk.mission.MissionControl;
import dji.sdk.mission.timeline.TimelineElement;
import dji.sdk.mission.timeline.TimelineElementFeedback;
import unmannedairlines.dronepan.mission.helpers.CameraSystemStateController;

public class WaitForCameraReadyAction extends TimelineElement implements CameraSystemStateController.Listener {

    CameraSystemStateController stateController;
    TimelineElementFeedback feedback;

    public WaitForCameraReadyAction(CameraSystemStateController stateController) {
        this.stateController = stateController;
        this.feedback = MissionControl.getInstance();
    }

    @Override
    public void run() {
        this.feedback.onStart(this);
        this.stateController.registerListener(this);
        this.checkIfCameraIsReadyAndNotify();
    }

    @Override
    public boolean isPausable() {
        return false;
    }

    @Override
    public void stop() {
        this.stateController.unregisterListener(this);
        this.feedback.onStopWithError(this, null);
    }

    @Override
    public DJIError checkValidity() {
        return null;
    }
    @Override
    public void finishRun(@Nullable DJIError var1) {return;}

    @Override
    public void onCameraStateChanged() {
        this.checkIfCameraIsReadyAndNotify();
    }

    private void checkIfCameraIsReadyAndNotify() {
        if (this.stateController.isReady()) {
            this.feedback.onFinishWithError(this, null);
            this.stateController.unregisterListener(this);
        }
    }
}
