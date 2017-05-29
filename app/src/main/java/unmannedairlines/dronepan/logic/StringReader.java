package unmannedairlines.dronepan.logic;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class StringReader {
    private static final String TAG = StringReader.class.getName();

    public static String Read(InputStream inputStream) {
        try {
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            StringBuilder stringBuilder = new StringBuilder();

            String receiveString = "";
            while ((receiveString = bufferedReader.readLine()) != null) {
                stringBuilder.append(receiveString);
            }

            inputStream.close();

            return stringBuilder.toString();
        }
        catch (IOException e) {
            Log.e(TAG, "Read() - Unexpected IO error.", e);
        }

        return "";
    }

    public static JSONObject ReadJson(InputStream inputStream) throws JSONException
    {
        String result = Read(inputStream);
        if (result.length() == 0)
        {
            result = "{}";
        }

        return new JSONObject(result);
    }
}
