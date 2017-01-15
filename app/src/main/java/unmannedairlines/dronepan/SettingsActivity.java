package unmannedairlines.dronepan;

import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.os.Bundle;

public class SettingsActivity extends BaseActivity {

    String modelName;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        this.modelName = getIntent().getExtras().getString("modelName");
        if (this.modelName == null || this.modelName.isEmpty())
        {
            this.modelName = "Default";
        }

        ViewDataBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_settings);

        Settings settings = SettingsManager.getInstance().getSettings(modelName);
        binding.setVariable(BR.settings, settings);
    }
}
