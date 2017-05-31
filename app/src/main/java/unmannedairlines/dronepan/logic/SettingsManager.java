package unmannedairlines.dronepan.logic;

import java.util.HashMap;

import dji.common.product.Model;

public class SettingsManager {
    private static final String TAG = SettingsManager.class.getName();

    private static SettingsManager instance;
    public static SettingsManager getInstance() {
        if (instance == null) {
            instance = new SettingsManager();
        }

        return instance;
    }

    HashMap<String, Settings> loadedSettings;

    private SettingsManager() {
        loadedSettings = new HashMap<String, Settings>();
    }

    public Settings getSettings(Model model) {
        if (loadedSettings.containsKey(model.name())) {
            return loadedSettings.get(model.name());
        }

        SettingsFactory factory = new SettingsFactory(model);
        Settings settings = factory.create();

        loadedSettings.put(model.name(), settings);

        return settings;
    }
}
