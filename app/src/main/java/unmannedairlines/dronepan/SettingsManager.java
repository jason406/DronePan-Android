package unmannedairlines.dronepan;

import android.content.Context;

import com.dd.plist.NSDictionary;
import com.dd.plist.NSNumber;
import com.dd.plist.NSObject;
import com.dd.plist.NSSet;
import com.dd.plist.NSString;
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
            Context c = DronePanApplication.getContext();

            InputStream stream = c.getResources().openRawResource(R.raw.models);
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
        loadFromDefaults(settings);
        settings.loadFromDisk();

        loadedSettings.put(modelName, settings);

        return settings;
    }

    public void revertSettings(Settings settings)
    {
        loadFromDefaults(settings);
    }

    private void loadFromDefaults(Settings settings)
    {
        String modelName = settings.getModelName();

        settings.setAllowsAboveHorizon(getValue(modelName, "allowsAboveHorizon", settings.getAllowsAboveHorizon()));
        settings.setPhotosPerRow(getValue(modelName, "photosPerRow", settings.getPhotosPerRow()));
        settings.setRelativeGimbalYaw(getValue(modelName, "relativeGimbalYaw", settings.getRelativeGimbalYaw()));
        settings.setSwitchPosition(getValue(modelName, "switchPostion", "position", settings.getSwitchPosition()));
        settings.setSwitchName(getValue(modelName, "switchPosition", "name", settings.getSwitchName()));
    }

    private <T> T getValue(String model, String propertyName, T fallbackValue)
    {
        T value = fallbackValue;
        if (this.defaults.containsKey(propertyName))
        {
            value = (T)getValue(defaults.get(propertyName));
        }

        if (this.modelOverrides.containsKey(propertyName))
        {
            NSDictionary propOverride = (NSDictionary)this.modelOverrides.get(propertyName);
            if (propOverride.containsKey(model))
            {
                value = (T)getValue(propOverride.get(model));
            }
        }

        return value;
    }

    private <T> T getValue(String model, String propertyName, String subPropertyName, T defaultValue)
    {
        NSDictionary dictionary = getValue(model, propertyName, null);
        if (dictionary != null && dictionary.containsKey(subPropertyName))
        {
            return (T)getValue(dictionary.get(subPropertyName));
        }

        return defaultValue;
    }

    private Object getValue(NSObject nsObject)
    {
        if (nsObject.getClass().equals(NSNumber.class))
        {
            NSNumber number = (NSNumber)nsObject;
            switch (number.type())
            {
                case NSNumber.BOOLEAN:
                    return number.boolValue();
                case NSNumber.INTEGER:
                    return number.intValue();
                case NSNumber.REAL:
                    return number.doubleValue();
            }
        }
        else if (nsObject.getClass().equals(NSString.class))
        {
            NSString string = (NSString)nsObject;
            return string.getContent();
        }
        else if (nsObject.getClass().equals(NSDictionary.class))
        {
            return nsObject;
        }

        return null;
    }
}
