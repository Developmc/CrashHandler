package com.clement.example.crashhandler;

import java.util.Map;

/**Crash时回调
 * Created by clement on 16/8/1.
 */

public interface CrashCallback {
    void onCrashCallback(String crashInfo, Map<String,String> deviceInfo);
}
