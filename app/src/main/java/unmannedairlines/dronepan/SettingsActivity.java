package unmannedairlines.dronepan;

import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class SettingsActivity extends BaseActivity implements View.OnClickListener {

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

        Button button = (Button)findViewById(R.id.goBackButton);
        button.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId())
        {
            case R.id.goBackButton:
                super.onBackPressed();
                break;
        }
    }
}
