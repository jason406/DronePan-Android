package unmannedairlines.dronepan;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.common.product.Model;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.products.Aircraft;
import dji.sdk.products.HandHeld;
import dji.sdk.sdkmanager.DJISDKManager;

public class DJIConnection {
    private static final String TAG = DJIConnection.class.getName();
    public static final String FLAG_CONNECTION_CHANGE = "dji_sdk_connection_change";

    private static DJIConnection instance;
    public static DJIConnection getInstance() {
        if (instance == null) {
            instance = new DJIConnection();
        }

        return instance;
    }

    private BaseProduct connectedProduct;
    private Context context;
    private Handler handler;

    public DJIConnection() {

    }

    public void initialize(Context context) {
        this.context = context;

        // Initialize DJI SDK Manager
        this.handler = new Handler(Looper.getMainLooper());
        DJISDKManager.getInstance().registerApp(this.context, mDJISDKManagerCallback);
    }

    private DJISDKManager.SDKManagerCallback mDJISDKManagerCallback = new DJISDKManager.SDKManagerCallback() {
        @Override
        public void onRegister(DJIError error) {
            Log.d(TAG, error == null ? "success" : error.getDescription());
            if(error == DJISDKError.REGISTRATION_SUCCESS) {
                DJISDKManager.getInstance().startConnectionToProduct();
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, "SDK registered successfully.", Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, "SDK registration failed. Please check your network connection.", Toast.LENGTH_LONG).show();
                    }
                });
            }

            Log.e("TAG", error.toString());
        }
        @Override
        public void onProductChange(BaseProduct oldProduct, BaseProduct newProduct) {
            connectedProduct = newProduct;
            if(connectedProduct != null) {
                connectedProduct.setBaseProductListener(mDJIBaseProductListener);
            }

            notifyStatusChange();
        }
    };

    private BaseProduct.BaseProductListener mDJIBaseProductListener = new BaseProduct.BaseProductListener() {
        @Override
        public void onComponentChange(BaseProduct.ComponentKey key, BaseComponent oldComponent, BaseComponent newComponent) {
            if(newComponent != null) {
                newComponent.setComponentListener(mDJIComponentListener);
            }
            notifyStatusChange();
        }

        @Override
        public void onConnectivityChange(boolean isConnected) {
            notifyStatusChange();
        }
    };

    private BaseComponent.ComponentListener mDJIComponentListener = new BaseComponent.ComponentListener() {
        @Override
        public void onConnectivityChange(boolean isConnected) {
            notifyStatusChange();
        }
    };

    private void notifyStatusChange() {
        handler.removeCallbacks(updateRunnable);
        handler.postDelayed(updateRunnable, 500);
    }

    private Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            Intent intent = new Intent(FLAG_CONNECTION_CHANGE);
            context.sendBroadcast(intent);
        }
    };

    public BaseProduct getProduct() {
        return this.connectedProduct;
    }

    public Model getModelSafely()
    {
        Model model = null;

        BaseProduct product = getProduct();
        if (product != null)
        {
            model = product.getModel();
        }

        if (model == null)
        {
            model = Model.UNKNOWN_AIRCRAFT;
        }

        return model;
    }

    public boolean isAircraftConnected() {
        return connectedProduct != null && connectedProduct instanceof Aircraft;
    }

    public boolean isHandHeldConnected() {
        return connectedProduct != null && connectedProduct instanceof HandHeld;
    }

    public synchronized Camera getCamera() {

        if (connectedProduct == null) return null;

        Camera camera = null;

        if (isAircraftConnected()){
            camera = ((Aircraft)connectedProduct).getCamera();

        } else if (isHandHeldConnected()) {
            camera = ((HandHeld)connectedProduct).getCamera();
        }

        return camera;
    }

    public String getSdkVersion()
    {
        return DJISDKManager.getInstance().getSDKVersion();
    }
}
