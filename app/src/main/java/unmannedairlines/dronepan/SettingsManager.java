package unmannedairlines.dronepan;

import com.dd.plist.NSDictionary;
import com.dd.plist.PropertyListParser;

import java.io.InputStream;
import java.util.HashMap;

public class SettingsManager {

    private static SettingsManager instance;
    public static SettingsManager getInstance()
    {
        if (instance == null)
        {
            instance = new SettingsManager();
        }

        return instance;
    }

    NSDictionary defaults;
    NSDictionary modelOverrides;

    HashMap<String, Settings> loadedSettings;

    private SettingsManager()
    {
        loadConfiguration();

        loadedSettings = new HashMap<String, Settings>();
    }


    private void loadConfiguration()
    {
        try {
            InputStream stream = DronePanApplication.getContext().getResources().openRawResource(R.raw.models);
            modelOverrides = (NSDictionary) PropertyListParser.parse(stream);
            defaults = (NSDictionary) modelOverrides.get("defaults");
        } catch (Exception ex) {
        }
    }

    public Settings getSettings(String modelName)
    {
        if (loadedSettings.containsKey(modelName))
        {
            return loadedSettings.get(modelName);
        }

        Settings settings = new Settings(modelName);

        settings.setAllowsAboveHorizon(getValue(modelName, "allowsAboveHorizon", settings.getAllowsAboveHorizon()));
        settings.setPhotosPerRow(getValue(modelName, "photosPerRow", settings.getPhotosPerRow()));
        settings.setRelativeGimbalYaw(getValue(modelName, "relativeGimbalYaw", settings.getRelativeGimbalYaw()));
        settings.setSwitchPosition(getValue(modelName, "switchPostion", "position", settings.getSwitchPosition()));
        settings.setSwitchName(getValue(modelName, "switchPosition", "name", settings.getSwitchName()));

        settings.loadFromDisk();

        loadedSettings.put(modelName, settings);

        return settings;
    }

    private <T> T getValue(String model, String propertyName, T defaultValue)
    {
        T value = defaultValue;
        if (this.defaults.containsKey(propertyName))
        {
            value = (T)defaults.get(propertyName);
        }

        if (this.modelOverrides.containsKey(propertyName))
        {
            NSDictionary propOverride = (NSDictionary)this.modelOverrides.get(propertyName);
            if (propOverride.containsKey((model)))
            {
                value = (T)propOverride.get(model);
            }
        }

        return value;
    }

    private <T> T getValue(String model, String propertyName, String subPropertyName, T defaultValue)
    {
        NSDictionary dictionary = getValue(model, propertyName, null);
        if (dictionary != null && dictionary.containsKey(subPropertyName))
        {
            return (T)dictionary.get(subPropertyName);
        }

        return defaultValue;
    }
}
