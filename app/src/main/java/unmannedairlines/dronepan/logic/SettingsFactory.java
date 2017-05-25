package unmannedairlines.dronepan.logic;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;

import dji.common.product.Model;
import unmannedairlines.dronepan.DronePanApplication;
import unmannedairlines.dronepan.R;

public class SettingsFactory {
    private static final String TAG = SettingsFactory.class.getName();

    private static JSONObject defaults;
    private static JSONObject defaultSettings;

    public static void loadDefaults() {
        if (defaults != null) {
            return;
        }

        try {
            Context context = DronePanApplication.getContext();
            InputStream stream = context.getResources().openRawResource(R.raw.models);
            defaults = StringReader.ReadJson(stream);
            defaultSettings = defaults.getJSONObject("default");
        }
        catch (JSONException e) {
            Log.e(TAG, "Can't load default settings.", e);
        }
    }

    Settings settings;

    JSONObject modelSettings;
    JSONObject userSettings;

    private Model model;
    private String modelName;
    private boolean useUserSettings;

    private abstract class SetterGetter {
        public String setting;
        public boolean saveAsUserSetting;

        public SetterGetter(String setting) {
            this.setting = setting;
            this.saveAsUserSetting = true;
        }

        public SetterGetter(String setting, boolean saveAsUserSetting) {
            this.saveAsUserSetting = true;
        }

        abstract void set(Object value);
        abstract Object get();
    }

    ArrayList<SetterGetter> setterGetters = new ArrayList<SetterGetter>();

    public SettingsFactory(Model model)
    {
        this.settings = new Settings(model);
        this.initialize();
    }

    public SettingsFactory(Settings settings) {
        this.settings = settings;
        this.initialize();
    }

    private void initialize() {
        loadDefaults();
        this.createSetterGetters();

        this.model = this.settings.getModel();
        this.modelName = this.model.name();

        try {
            this.modelSettings = defaults.getJSONObject(modelName);
        }
        catch (JSONException e) {
            Log.e(TAG, "Can't load model specific settings.", e);
        }
    }

    private void createSetterGetters() {
        setterGetters.add(new SetterGetter(SettingsConstants.PhotosPerRow) {
            @Override
            void set(Object value) { settings.setPhotosPerRow((int)value); }

            @Override
            Object get() { return settings.getPhotosPerRow(); }
        });
        setterGetters.add(new SetterGetter(SettingsConstants.NumberOfRows) {
            @Override
            void set(Object value) { settings.setNumberOfRows((int)value); }

            @Override
            Object get() { return settings.getNumberOfRows(); }
        });
        setterGetters.add(new SetterGetter(SettingsConstants.NumberOfNadirShots) {
            @Override
            void set(Object value) { settings.setNumberOfNadirShots((int)value); }

            @Override
            Object get() { return settings.getNumberOfNadirShots(); }
        });
        setterGetters.add(new SetterGetter(SettingsConstants.DelayBeforeEachShot) {
            @Override
            void set(Object value) { settings.setDelayBeforeEachShotInMs((int)value); }

            @Override
            Object get() { return settings.getDelayBeforeEachShotInMs(); }
        });
        setterGetters.add(new SetterGetter(SettingsConstants.AllowsAboveHorizon) {
            @Override
            void set(Object value) { settings.setAllowsAboveHorizon((boolean)value); }

            @Override
            Object get() { return settings.getAllowsAboveHorizon(); }
        });
        setterGetters.add(new SetterGetter(SettingsConstants.UseImperial) {
            @Override
            void set(Object value) { settings.setUseImperial((boolean)value); }

            @Override
            Object get() { return settings.getUseImperial(); }
        });
        setterGetters.add(new SetterGetter(SettingsConstants.AebPhotoMode) {
            @Override
            void set(Object value) { settings.setAebPhotoMode((boolean)value); }

            @Override
            Object get() { return settings.getAebPhotoMode(); }
        });
        setterGetters.add(new SetterGetter(SettingsConstants.ShootRowByRow) {
            @Override
            void set(Object value) { settings.setShootRowByRow((boolean)value); }

            @Override
            Object get() { return settings.getShootRowByRow(); }
        });
        setterGetters.add(new SetterGetter(SettingsConstants.RelativeGimbalYaw) {
            @Override
            void set(Object value) { settings.setRelativeGimbalYaw((boolean)value); }

            @Override
            Object get() { return settings.getRelativeGimbalYaw(); }
        });
    }

    public Settings create() {
        this.userSettings = this.readUserSettings();
        this.useUserSettings = true;

        for (SetterGetter setterGetter : setterGetters) {
            this.loadAndSetValue(setterGetter);
        }

        return settings;
    }

    private void loadAndSetValue(SetterGetter setterGetter) {
        Object value = setterGetter.get();

        try {
            if (this.defaultSettings.has(setterGetter.setting)) {
                value = this.defaultSettings.get(setterGetter.setting);
            }
        }
        catch (JSONException e) {}

        try {
            if (this.modelSettings.has(setterGetter.setting)) {
                value = this.modelSettings.get(setterGetter.setting);
            }
        }
        catch (JSONException e) {}

        if (userSettings != null && this.useUserSettings) {
            try {
                if (this.userSettings.has(setterGetter.setting)) {
                    value = this.userSettings.get(setterGetter.setting);
                }
            } catch (JSONException e) { }
        }

        setterGetter.set(value);
    }

    public void saveUserSettings() {
        this.useUserSettings = false;

        try {
            JSONObject json = new JSONObject();

            for (SetterGetter setterGetter : this.setterGetters) {
                if (!setterGetter.saveAsUserSetting) {
                    continue;
                }

                json.put(setterGetter.setting, setterGetter.get());
            }

            this.saveToUserSettingsFile(json.toString());
        }
        catch (JSONException e) {
            Log.e(TAG, "Could not create JSON.", e);
        }
    }

    public void revertToDefaultSettings() {
        this.useUserSettings = false;

        for (SetterGetter setterGetter : setterGetters) {
            this.loadAndSetValue(setterGetter);
        }
    }

    private File getUserSettingsFile() {
        String filename = this.model.name() + ".settings";
        File file = new File(DronePanApplication.getContext().getFilesDir(), filename);
        return file;
    }

    private JSONObject readUserSettings() {
        File file = getUserSettingsFile();
        if (file.exists()) {
            try {
                FileInputStream inputStream = DronePanApplication.getContext().openFileInput(file.getName());
                return StringReader.ReadJson(inputStream);
            }
            catch (FileNotFoundException e) {
                Log.e(TAG, "Could not open file.", e);
            }
            catch (JSONException e) {
                Log.e(TAG, "Could not parse JSON.", e);
            }
        }

        return null;
    }

    private void saveToUserSettingsFile(String json) {
        try {
            File file = getUserSettingsFile();
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
}
