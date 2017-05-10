package unmannedairlines.dronepan;

import dji.common.camera.SystemState;

public class CameraSystemStateExtensions {
    static boolean isReady(SystemState state) {
        return !isBusy(state);
    }

    static boolean isBusy(SystemState state) {
        return isRecordingVideo(state) ||
            isShootingPhoto(state) ||
            isStoringPhoto(state) ||
            hasError(state);
    }

    static boolean hasError(SystemState state) {
        return state.hasError() ||
            state.isOverheating();
    }

    static boolean isRecordingVideo(SystemState state) {
        return state.isRecording();
    }

    static boolean isShootingPhoto(SystemState state) {
        return state.isShootingBurstPhoto() ||
            state.isShootingIntervalPhoto() ||
            state.isShootingRAWBurstPhoto() ||
            state.isShootingSinglePhoto() ||
            state.isShootingSinglePhotoInRAWFormat();
    }

    static boolean isStoringPhoto(SystemState state) {
        return state.isStoringPhoto();
    }
}
