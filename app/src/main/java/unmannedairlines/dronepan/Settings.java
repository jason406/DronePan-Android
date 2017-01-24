package unmannedairlines.dronepan;

import android.content.Context;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import dji.common.product.Model;

public class Settings extends BaseObservable {
    private static final String TAG = Settings.class.getName();

    private Model model;

    private int photosPerRow;
    private int numberOfRows;
    private int numberOfNadirShots;
    private double delayBeforeEachShot;
    private boolean allowsAboveHorizon;
    private boolean relativeGimbalYaw;
    private boolean useImperial;
    private boolean aebPhotoMode;
    private String switchName;
    private int switchPosition;

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
    public double getDelayBeforeEachShot() {
        return delayBeforeEachShot;
    }

    @Bindable
    public boolean getAllowsAboveHorizon()
    {
        return allowsAboveHorizon;
    }

    @Bindable
    public boolean getRelativeGimbalYaw()
    {
        return relativeGimbalYaw;
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
        float maxPitchAngle = this.allowsAboveHorizon ? 180.0f : 90.0f;
        float pitchAngle = maxPitchAngle / getNumberOfRows();
        return pitchAngle;
    }

    @Bindable
    public String getSwitchName()
    {
        return switchName;
    }

    @Bindable
    public int getSwitchPosition()
    {
        return switchPosition;
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

    public void setDelayBeforeEachShot(double delayBeforeEachShot) {
        this.delayBeforeEachShot = delayBeforeEachShot;
        notifyPropertyChanged(BR.delayBeforeEachShot);
    }

    public void setAllowsAboveHorizon(boolean allowsAboveHorizon) {
        this.allowsAboveHorizon = allowsAboveHorizon;
        notifyPropertyChanged(BR.allowsAboveHorizon);
    }

    public void setRelativeGimbalYaw(boolean relativeGimbalYaw) {
        this.relativeGimbalYaw = relativeGimbalYaw;
        notifyPropertyChanged(BR.relativeGimbalYaw);
    }

    public void setAebPhotoMode(boolean aebPhotoMode) {
        this.aebPhotoMode = aebPhotoMode;
        notifyPropertyChanged(BR.aebPhotoMode);
    }

    public void setUseImperial(boolean useImperial) {
        this.useImperial = useImperial;
        notifyPropertyChanged(BR.useImperial);
    }

    public void setSwitchName(String switchName)
    {
        this.switchName = switchName;
        notifyPropertyChanged(BR.switchName);
    }

    public void setSwitchPosition(int switchPosition)
    {
        this.switchPosition = switchPosition;
        notifyPropertyChanged(BR.switchPosition);
    }

    public void valueChanged(SeekBar s, int progressValue, boolean fromUser)
    {
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
                    setDelayBeforeEachShot(progressValue / 10.0);
                    break;
            }
        }
    }

    public void checkedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId())
        {
            case R.id.imperialMetricSwitch:
                setUseImperial(isChecked);
                break;

            case R.id.aebSwitch:
                setAebPhotoMode(isChecked);
                break;
        }
    }

    public void onClicked(View v)
    {
        switch (v.getId())
        {
            case R.id.saveSettingsButton:
                saveToDisk();
                break;

            case R.id.cancelSettingsButton:
                revertSettings();
                break;
        }
    }

    public void loadFromDisk()
    {
        String jsonString = readJson();
        if (jsonString == null)
        {
            return;
        }

        try {
            JSONObject json = new JSONObject(jsonString);

            setPhotosPerRow(json.getInt("photosPerRow"));
            setNumberOfRows(json.getInt("numberOfRows"));
            setNumberOfNadirShots(json.getInt("numberOfNadirShots"));
            setDelayBeforeEachShot(json.getDouble("delayBeforeEachShot"));
            setAllowsAboveHorizon(json.getBoolean("allowsAboveHorizon"));
            setUseImperial(json.getBoolean("useImperial"));
            setAebPhotoMode(json.getBoolean("aebPhotoMode"));
        }
        catch (JSONException e)
        {
            Log.e(TAG, "Could not parse JSON.", e);
        }
    }

    public void saveToDisk()
    {
        try {
            JSONObject json = new JSONObject();

            json.put("photosPerRow", photosPerRow);
            json.put("numberOfRows", numberOfRows);
            json.put("numberOfNadirShots", numberOfNadirShots);
            json.put("delayBeforeEachShot", delayBeforeEachShot);
            json.put("allowsAboveHorizon", allowsAboveHorizon);
            json.put("useImperial", useImperial);
            json.put("aebPhotoMode", aebPhotoMode);

            saveJson(json.toString());
        }
        catch (JSONException e)
        {
            Log.e(TAG, "Could not create JSON.", e);
        }
    }

    public void revertSettings()
    {
        setDefaults();
        SettingsManager.getInstance().revertSettings(this);
    }

    private String readJson()
    {
        File file = getFile();
        if (file.exists()) {
            try {
                FileInputStream inputStream = DronePanApplication.getContext().openFileInput(file.getName());
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                StringBuilder stringBuilder = new StringBuilder();

                String receiveString = "";
                while ((receiveString = bufferedReader.readLine()) != null )
                {
                    stringBuilder.append(receiveString);
                }

                inputStream.close();

                return stringBuilder.toString();
            }
            catch (FileNotFoundException e)
            {
                Log.e(TAG, "Could not open file.", e);
            }
            catch (IOException e)
            {
                Log.e(TAG, "Unexpected IO error.", e);
            }
        }

        return "{}";
    }

    private void saveJson(String json)
    {
        try
        {
            File file = getFile();
            FileOutputStream outputStream = DronePanApplication.getContext().openFileOutput(file.getName(), Context.MODE_PRIVATE);
            outputStream.write(json.getBytes());
            outputStream.close();

            Toast.makeText(DronePanApplication.getContext(), "Settings saved successfully.", Toast.LENGTH_LONG).show();
        }
        catch (Exception e) {
            Toast.makeText(DronePanApplication.getContext(), "Could not save settings.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Saving to JSON failed.", e);
        }
    }

    private File getFile()
    {
        String filename = this.model.name() + ".settings";
        File file = new File(DronePanApplication.getContext().getFilesDir(), filename);
        return file;
    }

    private void setDefaults()
    {
        photosPerRow = 10;
        numberOfRows = 3;
        numberOfNadirShots = 2;
        delayBeforeEachShot = 1.5;
        allowsAboveHorizon = false;
        relativeGimbalYaw = false;
        switchName = "F";
        switchPosition = 3;
        useImperial = false;
        aebPhotoMode = false;
    }
}
