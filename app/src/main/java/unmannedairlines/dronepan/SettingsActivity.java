package unmannedairlines.dronepan;

import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import dji.common.product.Model;
import unmannedairlines.dronepan.logic.DJIConnection;
import unmannedairlines.dronepan.logic.Settings;
import unmannedairlines.dronepan.logic.SettingsManager;

public class SettingsActivity extends BaseActivity implements View.OnClickListener {

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        ViewDataBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_settings);

        Model connectedModel = DJIConnection.getInstance().getModelSafely();
        Settings settings = SettingsManager.getInstance().getSettings(connectedModel);
        binding.setVariable(BR.settings, settings);

        Button button = (Button)findViewById(R.id.goBackButton);
        button.setOnClickListener(this);

        TextView versionTextView = (TextView)findViewById(R.id.versionTextView);
        versionTextView.setText("DronePan Version: " + DronePanApplication.getBuildVersion());

        TextView sdkVersionTextView = (TextView)findViewById(R.id.sdkVersionTextView);
        sdkVersionTextView.setText("SDK Version: " + DJIConnection.getInstance().getSdkVersion());
    }

    @Override
    public void onClick(View view)
    {
        switch (view.getId())
        {
            case R.id.goBackButton:
                super.onBackPressed();
                break;
        }
    }
}
