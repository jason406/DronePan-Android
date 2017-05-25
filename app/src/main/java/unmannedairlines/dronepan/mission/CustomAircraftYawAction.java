package unmannedairlines.dronepan.mission;

import android.util.Log;

import java.lang.ref.WeakReference;

import dji.common.error.DJIError;
import dji.common.error.DJIMissionError;
import dji.common.error.DJISDKError;
import dji.common.flightcontroller.virtualstick.FlightControlData;
import dji.common.flightcontroller.virtualstick.RollPitchControlMode;
import dji.common.flightcontroller.virtualstick.VerticalControlMode;
import dji.common.flightcontroller.virtualstick.YawControlMode;
import dji.common.util.CommonCallbacks;
import dji.midware.data.model.P3.DataOsdGetPushCommon;
import dji.sdk.base.BaseProduct;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.mission.MissionControl;
import dji.sdk.mission.error.AircraftYawActionError;
import dji.sdk.mission.timeline.actions.MissionAction;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.thirdparty.eventbus.EventBus;
import unmannedairlines.dronepan.mission.helpers.CameraSystemStateController;

public class CustomAircraftYawAction extends MissionAction {
    private static final String TAG = CustomAircraftYawAction.class.getName();
    private static final double AllowedErrorInDegrees = 0.5;


    private float angle;
    private float targetAngle;

    private boolean targetAngleCalulationNeeded;

    private float currentAngle;
    private float lastRotateAngle;
    private boolean isStop;
    private int stopCounter;

    private RollPitchControlMode oldRollPitchControlMode;
    private YawControlMode oldYawControlMode;
    private VerticalControlMode oldVerticalControlMode;

    private FlightController flightController;

    private WeakReference<CustomAircraftYawAction> target;
    private InnerEventBus innerEventBus;

    public CustomAircraftYawAction(float relativeAngle, float angularVelocity) {
        this.angle = relativeAngle;
        this.targetAngle = relativeAngle;
    }

    private FlightController getFlightController() {
        BaseProduct product = DJISDKManager.getInstance().getProduct();
        return product != null && product instanceof Aircraft ?((Aircraft)product).getFlightController() : null;
    }

    private void startExecution() {
        Log.i(TAG, "startExecution()");
        this.targetAngleCalulationNeeded = true;

        this.flightController = this.getFlightController();
        if(this.flightController == null) {
            MissionControl.getInstance().onStartWithError(this, DJISDKError.DEVICE_NOT_FOUND);
        }
        else {
            this.target = new WeakReference(this);
            this.backupControlMode();
            this.flightController.setVirtualStickAdvancedModeEnabled(true);
            this.flightController.setVirtualStickModeEnabled(true, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError == null) {
                        (CustomAircraftYawAction.this.target.get()).startRun();
                    }
                    else {
                        MissionControl.getInstance().onStartWithError((CustomAircraftYawAction.this.target.get()), djiError);
                    }
                }
            });
        }
    }

    private void rotate() {
        Log.i(TAG, "rotate() to target=" + this.targetAngle);

        FlightControlData controlData = new FlightControlData(0.0F, 0.0F, 0.0F, 0.0F);
        this.flightController.setRollPitchControlMode(RollPitchControlMode.VELOCITY);
        this.flightController.setVerticalControlMode(VerticalControlMode.VELOCITY);
        this.flightController.setYawControlMode(YawControlMode.ANGLE);
        controlData.setYaw(this.targetAngle);

        this.flightController.sendVirtualStickFlightControlData(controlData, new CommonCallbacks.CompletionCallback() {
            public void onResult(DJIError error) {
                if(error != null && CustomAircraftYawAction.this.target.get() != null) {
                    (CustomAircraftYawAction.this.target.get()).finishRun(error);
                }
            }
        });
    }

    private void stopRotate() {
        if(this.flightController.isVirtualStickControlModeAvailable()) {
            FlightControlData controlData = new FlightControlData(0.0F, 0.0F, 0.0F, 0.0F);
            this.flightController.setRollPitchControlMode(RollPitchControlMode.VELOCITY);
            this.flightController.setYawControlMode(YawControlMode.ANGULAR_VELOCITY);
            this.flightController.sendVirtualStickFlightControlData(controlData, null);
        }

        this.restoreControlMode();
        this.finishRun(null);
    }

    protected void startListen() {
        this.innerEventBus = new InnerEventBus();
    }

    protected void stopListen() {
        this.innerEventBus.destroy();
    }

    private void backupControlMode() {
        this.oldRollPitchControlMode = this.getFlightController().getRollPitchControlMode();
        this.oldYawControlMode = this.getFlightController().getYawControlMode();
        this.oldVerticalControlMode = this.getFlightController().getVerticalControlMode();
    }

    private void restoreControlMode() {
        this.getFlightController().setRollPitchControlMode(this.oldRollPitchControlMode);
        this.getFlightController().setYawControlMode(this.oldYawControlMode);
        this.getFlightController().setVerticalControlMode(this.oldVerticalControlMode);
    }

    protected void onReceivedOSDData(DataOsdGetPushCommon osdData) {
        if(this.isRunning()) {
            if(osdData.groundOrSky() != 2) {
                this.finishRun(DJIMissionError.AIRCRAFT_NOT_IN_THE_AIR);
                return;
            }

            float currentYaw = (float)osdData.getYaw() * 0.1F;
            Log.i(TAG, "currentYaw= " + currentYaw);

            this.calculateTargetAngleIfNeeded(currentYaw);

            if (this.checkYawAngle(currentYaw)) {
                this.stopRotate();
            }
            else {
                this.rotate();
            }
        }
    }

    private void calculateTargetAngleIfNeeded(float currentYaw) {
        if (!this.targetAngleCalulationNeeded) {
            return;
        }

        this.targetAngleCalulationNeeded = false;

        this.lastRotateAngle = currentYaw;
        this.targetAngle = this.angle + this.lastRotateAngle;

        if (this.targetAngle > 180.0) {
            this.targetAngle -= 360.0;
        }

        if (this.targetAngle < -180.0) {
            this.targetAngle += 360.0;
        }
    }

    private boolean checkYawAngle(float currentAngle) {
        double distance = Math.abs((double)currentAngle - (double)this.targetAngle);
        Log.i(TAG, "checkYawAngle, distance=" + distance);

        if (distance <= AllowedErrorInDegrees || Math.abs(distance - 360.0) <= AllowedErrorInDegrees) {
            return true;
        }

        return false;
    }

    public void run() {
        this.startExecution();
    }

    public boolean isPausable() {
        return false;
    }

    public void stop() {
        this.finishRun(null);
    }

    public DJIError checkValidity() {
        if(this.angle > 180.0F || this.angle < -180.0F) {
            return AircraftYawActionError.INVALID_ANGLE_VALUE;
        }

        return null;
    }

    private class InnerEventBus {
        public InnerEventBus() {
            EventBus.getDefault().register(this);
        }

        public void destroy() {
            EventBus.getDefault().unregister(this);
        }

        public void onEventBackgroundThread(DataOsdGetPushCommon var1) {
            CustomAircraftYawAction action = CustomAircraftYawAction.this.target.get();
            if (action != null) {
                action.onReceivedOSDData(var1);
            }
        }
    }
}
