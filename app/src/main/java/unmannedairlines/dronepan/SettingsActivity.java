package unmannedairlines.dronepan;

import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import dji.common.product.Model;
import dji.sdk.base.DJIBaseProduct;

public class SettingsActivity extends BaseActivity implements View.OnClickListener {

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        ViewDataBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_settings);

        Model connectedModel = DJIConnection.getModelSafely();
        Settings settings = SettingsManager.getInstance().getSettings(connectedModel);
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
