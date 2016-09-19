package com.clement.example.crashhandler;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class ShowCrashActivity extends AppCompatActivity {
    public static final String TAG = "ShowCrashActivity" ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_crash);
        Bundle bundle = getIntent().getExtras();
        String crashInfo = bundle.getString("crashInfo");
        String showInfo = getShowInfo(crashInfo) ;
        if(bundle!=null){
            TextView tv_show = (TextView)findViewById(R.id.tv_show);
            tv_show.setText(showInfo);
        }
    }

    /**构造显示报错的信息
     * @param crashInfo
     * @return
     */
    private String getShowInfo(String crashInfo){
        StringBuilder builder = new StringBuilder();
        Map<String,String> map = getDeviceInfo(this);
        builder.append("VersionName: "+map.get("versionName")+"\n");
        builder.append("DeviceSDK: "+map.get("versionSDK")+"\n");
        builder.append("Model: "+map.get("MODEL")+"\n");
        builder.append("Device: "+map.get("DEVICE")+"\n"+"\n");
        builder.append(crashInfo);
        return builder.toString();
    }

    /**获取当前设备的信息
     * @param context
     * @return
     */
    private Map<String,String> getDeviceInfo(Context context){
        Map<String,String> deviceInfo = new HashMap<>();
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(context.getPackageName(), PackageManager.GET_ACTIVITIES);
            if (pi != null) {
                String versionName = pi.versionName == null ? "null" : pi.versionName;
                String versionCode = pi.versionCode + "";
                deviceInfo.put("versionName", versionName);
                deviceInfo.put("versionCode", versionCode);
                deviceInfo.put("versionSDK", String.valueOf(Build.VERSION.SDK_INT));
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "an error occured when collect package info", e);
        }
        Field[] fields = Build.class.getDeclaredFields();
        for (Field field : fields) {
            try {
                field.setAccessible(true);
                deviceInfo.put(field.getName(), field.get(null).toString());
                Log.d(TAG, field.getName() + " : " + field.get(null));
            } catch (Exception e) {
                Log.e(TAG, "an error occured when collect crash info", e);
            }
        }
        return deviceInfo;
    }
}
