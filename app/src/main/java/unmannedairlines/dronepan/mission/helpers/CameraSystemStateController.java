package unmannedairlines.dronepan.mission.helpers;

import android.support.annotation.NonNull;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import dji.common.camera.SystemState;
import dji.sdk.camera.Camera;

public class CameraSystemStateController implements SystemState.Callback {
    private static final String TAG = CameraSystemStateController.class.getName();

    Camera camera;
    SystemState currentSystemState;

    public interface Listener {
        void onCameraStateChanged();
    }

    private List<Listener> listeners = new ArrayList<Listener>();

    public CameraSystemStateController(Camera camera) {
        this.camera = camera;
        this.camera.setSystemStateCallback(this);
    }

    @Override
    protected void finalize() {
        this.camera.setSystemStateCallback(null);
    }

    public void registerListener(Listener listener) {
        this.listeners.add(listener);
    }

    public void unregisterListener(Listener listener) {
        this.listeners.remove(listener);
    }

    @Override
    public void onUpdate(@NonNull SystemState systemState) {
        this.currentSystemState = systemState;
        //Log.i(TAG, "isBusy? " + this.isBusy() + " isStoring? " + this.isStoringPhoto() + " isShooting? " + this.isShootingPhoto());

        this.notifyListener();
    }
    private void notifyListener() {
        for (Listener l : this.listeners) {
            l.onCameraStateChanged();
        }
    }

    public boolean isReady() {
        return !isBusy();
    }

    public boolean isBusy() {
        return //isRecordingVideo() ||
            //isShootingPhoto() ||
            isStoringPhoto() ||
            hasError();
    }

    public boolean hasError() {
        if (this.currentSystemState == null) return false;
        return this.currentSystemState.hasError() || this.currentSystemState.isOverheating();
    }

    public boolean isRecordingVideo() {
        if (this.currentSystemState == null) return false;
        return this.currentSystemState.isRecording();
    }

    public boolean isShootingPhoto() {
        if (this.currentSystemState == null) return false;
        return this.currentSystemState.isShootingBurstPhoto() ||
            this.currentSystemState.isShootingIntervalPhoto() ||
            this.currentSystemState.isShootingRAWBurstPhoto() ||
            this.currentSystemState.isShootingSinglePhoto() ||
            this.currentSystemState.isShootingSinglePhotoInRAWFormat();
    }

    public boolean isStoringPhoto() {
        if (this.currentSystemState == null) return false;
        return this.currentSystemState.isStoringPhoto();
    }
}
