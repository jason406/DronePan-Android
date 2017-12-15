package unmannedairlines.dronepan.logic;

import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.SeekBar;

import dji.common.product.Model;
import unmannedairlines.dronepan.BR;
import unmannedairlines.dronepan.R;

public class Settings extends BaseObservable {
    private static final String TAG = Settings.class.getName();

    private Model model;

    private int photosPerRow;
    private int numberOfRows;
    private int numberOfNadirShots;
    private int delayBeforeEachShotInMs = 1000;
    private boolean allowsAboveHorizon;

    private boolean canGimbalYaw;
    private boolean useGimbalToYaw;

    private boolean useImperial;
    private boolean aebPhotoMode;
    private boolean shootRowByRow;

    public Settings(Model model)
    {
        this.model = model;
        setDefaults();
    }

    public Model getModel()
    {
        return model;
    }

    @Bindable
    public String getModelDisplayName() {
        return model.getDisplayName();
    }

    @Bindable
    public int getNumberOfRows() {
        return numberOfRows;
    }

    @Bindable
    public int getPhotosPerRow() {
        return photosPerRow;
    }

    @Bindable
    public int getNumberOfNadirShots() {
        return numberOfNadirShots;
    }

    @Bindable
    public int getDelayBeforeEachShotInMs() {
        return delayBeforeEachShotInMs;
    }

    @Bindable
    public boolean getAllowsAboveHorizon()
    {
        return allowsAboveHorizon;
    }

    @Bindable
    public boolean getUseGimbalToYaw()
    {
        return useGimbalToYaw;
    }

    @Bindable
    public boolean getCanGimbalYaw()
    {
        return canGimbalYaw;
    }

    @Bindable
    public boolean getUseImperial() {
        return useImperial;
    }

    @Bindable
    public boolean getAebPhotoMode() {
        return aebPhotoMode;
    }

    @Bindable
    public boolean getShootRowByRow() {
        return shootRowByRow;
    }

    @Bindable
    public int getNumberOfPhotos() {
        return numberOfRows * photosPerRow + numberOfNadirShots;
    }

    @Bindable
    public float getYawAngle() {
        float yawAngle = 360.0f / getPhotosPerRow();
        return yawAngle;
    }

    @Bindable
    public float getPitchAngle() {
        // TODO: adjust max pitch angle based on settings.
        //float maxPitchAngle = this.allowsAboveHorizon ? 90.0f : 0.0f;
        float maxPitchAngle = 0.0f;
        float minPitchAngle = -90.0f;

        float totalPitchAngle = maxPitchAngle - minPitchAngle;
        float pitchAngle = totalPitchAngle / getNumberOfRows();
        return pitchAngle;
    }

    public void setNumberOfRows(int numberOfRows) {
        this.numberOfRows = numberOfRows;
        notifyPropertyChanged(BR.numberOfRows);
        notifyPropertyChanged(BR.numberOfPhotos);
        notifyPropertyChanged(BR.pitchAngle);
    }

    public void setPhotosPerRow(int photosPerRow) {
        this.photosPerRow = photosPerRow;
        notifyPropertyChanged(BR.photosPerRow);
        notifyPropertyChanged(BR.numberOfPhotos);
        notifyPropertyChanged(BR.yawAngle);
    }

    public void setNumberOfNadirShots(int numberOfNadirShots) {
        this.numberOfNadirShots = numberOfNadirShots;
        notifyPropertyChanged(BR.numberOfNadirShots);
        notifyPropertyChanged(BR.numberOfPhotos);
    }

    public void setDelayBeforeEachShotInMs(int delayBeforeEachShotInMs) {
        this.delayBeforeEachShotInMs = delayBeforeEachShotInMs;
        notifyPropertyChanged(BR.delayBeforeEachShotInMs);
    }

    public void setAllowsAboveHorizon(boolean allowsAboveHorizon) {
        this.allowsAboveHorizon = allowsAboveHorizon;
        notifyPropertyChanged(BR.allowsAboveHorizon);
    }

    public void setUseGimbalToYaw(boolean useGimbalToYaw) {
        this.useGimbalToYaw = useGimbalToYaw;
        notifyPropertyChanged(BR.useGimbalToYaw);
    }

    public void setCanGimbalYaw(boolean canGimbalYaw) {
        this.canGimbalYaw = canGimbalYaw;
        notifyPropertyChanged(BR.canGimbalYaw);
    }

    public void setAebPhotoMode(boolean aebPhotoMode) {
        this.aebPhotoMode = aebPhotoMode;
        notifyPropertyChanged(BR.aebPhotoMode);
    }

    public void setShootRowByRow(boolean shootRowByRow) {
        this.shootRowByRow = shootRowByRow;
        notifyPropertyChanged(BR.shootRowByRow);
    }

    public void setUseImperial(boolean useImperial) {
        this.useImperial = useImperial;
        notifyPropertyChanged(BR.useImperial);
    }

    public void valueChanged(SeekBar s, int progressValue, boolean fromUser) {
        if (fromUser)
        {
            switch (s.getId())
            {
                case R.id.picturesPerRowSeekBar:
                    setPhotosPerRow(progressValue);
                    break;

                case R.id.numberOfRowsSeekBar:
                    setNumberOfRows(progressValue);
                    break;

                case R.id.numberOfNadirShotsSeekBar:
                    setNumberOfNadirShots(progressValue);
                    break;

                case R.id.delayBeforeEachShotSeekBar:
                    setDelayBeforeEachShotInMs(progressValue / 10000);
                    break;
            }
        }
    }

    public void checkedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.imperialMetricSwitch:
                setUseImperial(isChecked);
                break;

            case R.id.aebSwitch:
                setAebPhotoMode(isChecked);
                break;

            case R.id.shootBySwitch:
                setShootRowByRow(isChecked);
                break;
        }
    }

    public void onClicked(View v) {
        switch (v.getId()) {
            case R.id.saveSettingsButton:
                saveToDisk();
                break;

            case R.id.cancelSettingsButton:
                revertSettings();
                break;
        }
    }

    public void saveToDisk() {
        SettingsFactory factory = new SettingsFactory(this);
        factory.saveUserSettings();
    }

    public void revertSettings() {
        SettingsFactory factory = new SettingsFactory(this);
        factory.revertToDefaultSettings();
    }

    private void setDefaults() {
        photosPerRow = 1;
        numberOfRows = 1;
        numberOfNadirShots = 0;
        delayBeforeEachShotInMs = 0;
        allowsAboveHorizon = false;
        canGimbalYaw = false;
        useGimbalToYaw = false;
        useImperial = false;
        aebPhotoMode = false;
        shootRowByRow = false;
    }
}
