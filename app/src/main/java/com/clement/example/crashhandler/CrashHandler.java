package com.clement.example.crashhandler;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**接收系统没有处理的Exception,并进行处理
 * Created by clement on 16/7/29.
 */

public class CrashHandler implements Thread.UncaughtExceptionHandler {
    public static final String TAG = "CrashHandler" ;
    public  final String BROADCAST_ACTION = "CrashAction";
    private static CrashHandler instance ;
    private static Context mContext;
    //用来保存设备信息
    private Map<String,String> deviceInfo = new HashMap<>();
    private String crashInfo = "";
    //获取系统默认的UncaughtException处理器
    private static Thread.UncaughtExceptionHandler mDefaultHandler;
    /**
     * 判断是否进行处理,如果不需要处理,则交回给系统处理
     */
    private static boolean isHandler = false ;
    //日期格式化
    private DateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.CHINA) ;
    private String path = Environment.getDataDirectory()+"/" ;    //默认crash信息保存路径
    //crash回调
    private CrashCallback crashCallback;
    //单例模式
    private CrashHandler(){}
    public static CrashHandler getInstance(){
        if(instance==null){
            synchronized(CrashHandler.class){
                if(instance==null){
                    instance = new CrashHandler();
                }
            }
        }
        return instance;
    }

    /**初始化
     * @param context
     */
    public static void init(Context context,boolean mHandler){
        mContext = context;
        isHandler = mHandler ;
        //获取系统默认的UncaughtException处理器
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
    }

    /**当异常发生时,回调
     * @param thread
     * @param ex
     */
    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        if(isHandler()){
            //设置该CrashHandler作为程序的默认异常处理器
            Thread.setDefaultUncaughtExceptionHandler(this);
            //执行处理
            handleException(ex) ;
            //处理完成后,退出
            exit();
        }
        else{
            //如果用户没有处理则让系统默认的异常处理器来处理
            mDefaultHandler.uncaughtException(thread, ex);
        }
    }
    /**
     * 自定义错误处理,收集错误信息 发送错误报告等操作均在此完成.
     *
     * @param ex
     * @return true:如果处理了该异常信息;否则返回false.
     */
    private void handleException(Throwable ex) {
        if(ex==null){
            return ;
        }
        //收集设备信息
        deviceInfo = getDeviceInfo(mContext);
        //获取crash信息
        crashInfo = getCrashInfo(ex);
        //保存日志(android6.0以上要申请权限)
//        saveCrashInfo(crashInfo,getPath());
        //跳转到crash信息的页面
        intent2show();
        //发送广播
        sentBroadcast();
        //响应回调
        if(crashCallback!=null){
            crashCallback.onCrashCallback(crashInfo,deviceInfo);
        }
    }

    /**设置回调的接口
     * @param crashCallback
     */
    public void setOnCrashCallback(CrashCallback crashCallback){
        this.crashCallback = crashCallback ;
    }
    /**
     * 退出
     */
    private void exit(){
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(1);
    }
    /**
     * 发送广播
     */
    private void sentBroadcast(){
        Intent intent = new Intent();
        intent.setAction(BROADCAST_ACTION);
        mContext.getApplicationContext().sendBroadcast(intent);
    }
    /**
     * 跳转到展示crash信息的页面
     */
    private void intent2show(){
        Intent intent = new Intent(mContext,ShowCrashActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("crashInfo",crashInfo);
        intent.putExtras(bundle);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);

    }

    /**
     * 保存错误信息到文件中
     *
     * @param ex
     * @return  返回文件名称,便于将文件传送到服务器
     */
    private String saveCrashInfo2File(Throwable ex) {

        StringBuffer sb = new StringBuffer();
        for (Map.Entry<String, String> entry : deviceInfo.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            sb.append(key + "=" + value + "\n");
        }

        Writer writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        ex.printStackTrace(printWriter);
        Throwable cause = ex.getCause();
        while (cause != null) {
            cause.printStackTrace(printWriter);
            cause = cause.getCause();
        }
        printWriter.close();
        String result = writer.toString();
        sb.append(result);
        try {
            long timestamp = System.currentTimeMillis();
            String time = format.format(new Date());
            String fileName = "crash-" + time + "-" + timestamp + ".log";
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                String path = "/sdcard/crash/";
                File dir = new File(path);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                FileOutputStream fos = new FileOutputStream(path + fileName);
                fos.write(sb.toString().getBytes());
                fos.close();
            }
            return fileName;
        } catch (Exception e) {
            Log.e(TAG, "an error occured while writing file...", e);
        }
        return null;
    }


    /**获取crash的详细信息
     * @param ex
     * @return
     */
    private String getCrashInfo(Throwable ex){
        Writer writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        ex.printStackTrace(printWriter);
        Throwable cause = ex.getCause();
        while (cause != null) {
            cause.printStackTrace(printWriter);
            cause = cause.getCause();
        }
        printWriter.close();
        String crashInfo = writer.toString();
        return crashInfo;
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

    /**保存Crash到特定路径下
     * @param crashInfo
     * @param path
     */
    private void saveCrashInfo(String crashInfo,String path){
        try {
            long timestamp = System.currentTimeMillis();
            String time = format.format(new Date());
            String fileName = "crash-" + time + "-" + timestamp + ".log";
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                File dir = new File(path);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                FileOutputStream fos = new FileOutputStream(path + fileName);
                fos.write(crashInfo.getBytes());
                fos.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "an error occured while writing file...", e);
        }
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean isHandler() {
        return isHandler;
    }

    public void setHandler(boolean handler) {
        isHandler = handler;
    }
}
