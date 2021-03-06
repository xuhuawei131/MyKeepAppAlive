package com.jiayuan.xuhuawei.keepappalive;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.jiayuan.xuhuawei.keepappalive.entity.AppEntity;
import com.jiayuan.xuhuawei.keepappalive.utils.Utils;
import com.lingdian.keepservicealive.KSABaseService;
import com.lingdian.push.PushConfigHelper;
import com.lingdian.push.comm.GetMsgHelper;
import com.lingdian.push.comm.OnMsgReceiverEventListener;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 运行的主项目 可以在这里吗 做逻辑业务
 * https://blog.csdn.net/l2show/article/details/47421961
 */
public class MainService extends KSABaseService implements Runnable{
    private static final int TYPE_JUMP_WIFI=1;
    private static final int TYPE_JUMP_TARGET=2;
    private static final int TYPE_JUMP_MAIN_COVER =3;
    private static final int TYPE_JUMP_ALI=4;

    private static final String PACKAGE_NAME = "com.sand.airdroid";
    private static final String PACKAGE_COVER_NAME = "com.alibaba.android.rimet";


    private static final long INTERVAL_TIME = 60*10 ;
    private static final long INTERVAL_MAX_COMM_TIME =INTERVAL_TIME*2 ;//每隔一段时间 启动app 重置状态
    private static AtomicInteger saleInteger=new AtomicInteger(0);
    private Thread thread;
    private boolean isRunning=false;
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        onKSACreate();
        GetMsgHelper.getInstance().addOnMsgReceiverEventListener(msgReceiverEventListener);
        PushConfigHelper.getInstance().init(this, PushConfigHelper.PUSH_TYPE.TYPE_GETUI);

        isRunning=true;
        thread=new Thread(this);
        thread.start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        onKSAStartCommand();
        Log.v("xhw", "MainService onStartCommand");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        GetMsgHelper.getInstance().removeOnMsgReceiverEventListener(msgReceiverEventListener);
        handler.removeMessages(1);
    }

    @Override
    public void run() {
        {
            while (isRunning) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                int week = AppUtils.getDayofweek();
                boolean isWeekend = week == 1 || week == 7;


                int index = saleInteger.getAndIncrement();
                long comValue = index % INTERVAL_TIME;
                long maxValue = index % INTERVAL_MAX_COMM_TIME;

                // 如果是周末，那么执行周末时段
                if (isWeekend) {
                    if (comValue == 0 && maxValue != 0) {
                        Log.v("xhw","小检查");
                        if (isAtEndImportTime()){
                            excuteSetup(false);
                            Log.e("xhw","周末 小检查启动！ ");
                        }else{
                            doNetCheck(false);
                            Log.v("xhw","小检查 不再敏感期");
                        }
                    }else if (maxValue==0){
                        Log.v("xhw","reset timer!");
                        if (isAtEndImportTime()){
                            saleInteger.getAndSet(1);
                            excuteSetup(true);
                            Log.e("xhw","周末 大检查启动！ ");
                        }else{
                            doNetCheck(true);
                            Log.v("xhw","大检查 不再敏感期");
                        }
                    }
                } else {
                    //如果是平时 执行平时时段
                    if (comValue == 0 && maxValue != 0) {
                        Log.v("xhw","小检查");
                        if (isAtCommImportTime()){
                            Log.e("xhw","平时 小检查启动！ ");
                            excuteSetup(false);
                        }else{
                            doNetCheck(false);
                            Log.v("xhw","小检查 不再敏感期");
                        }
                    } else if (maxValue == 0) {
                        Log.v("xhw","大检查");
                        if (isAtCommImportTime()){
                            saleInteger.getAndSet(1);
                            excuteSetup(true);
                            Log.e("xhw","平时 大检查启动！ ");
                        }else{
                            doNetCheck(true);
                            Log.v("xhw","大检查 不再敏感期");
                        }
                    }
                }
            }
        }
    }

    private void doNetCheck(boolean isBigCheck){
        if (!AppUtils.isNetworkConnected(this)) {
            Utils.wakeUpAndUnlock(getApplication());
            handler.sendEmptyMessage(TYPE_JUMP_WIFI);
        }
    }

    private void excuteSetup(boolean isForce){
        if (!isForce){
            List<AppEntity> list = AppUtils.getAndroidProcess(this);
            boolean isRunning = false;
            if (list!=null){
                for (AppEntity bean : list) {
                    if (bean.getPackageName().equals(PACKAGE_NAME)) {
                        isRunning = true;
                        break;
                    }
                }
            }

            if (!isRunning) {
                if (AppUtils.checkPackInfo(this, PACKAGE_NAME)) {
                    executeProxyJump();
                } else {
                    Log.v("xhw", "target app is not install");
                }
                Log.v("xhw", "target app is not running");
            } else {
                Log.v("xhw", "target app is  running");
            }
        }else{
            Log.v("xhw", "force setup！");
            if (AppUtils.checkPackInfo(this, PACKAGE_NAME)) {
                executeProxyJump();
            }else{
                Log.v("xhw", "target app is not install");
            }
        }
    }


    /**
     * 执行跳转
     */
    private void executeProxyJump(){
        if (AppUtils.isNetworkConnected(this)) {
            handler.sendEmptyMessage(TYPE_JUMP_TARGET);
            handler.sendEmptyMessageDelayed(TYPE_JUMP_MAIN_COVER,5000);
        } else {
            handler.sendEmptyMessage(TYPE_JUMP_WIFI);
            handler.sendEmptyMessageDelayed(TYPE_JUMP_TARGET, 10000);
            handler.sendEmptyMessageDelayed(TYPE_JUMP_MAIN_COVER,15000);
        }
    }

    /**
     * 跳转到ali
     */
    private void executeALiJump(){
            Utils.wakeUpAndUnlock(getApplication());
            handler.sendEmptyMessage(TYPE_JUMP_ALI);
            handler.sendEmptyMessageDelayed(TYPE_JUMP_MAIN_COVER,5000);
    }
    /**
     * 是否在敏感时间
     * @return  8、9点或者在16、17、或者在21
     */
    private boolean isAtEndImportTime() {
        Calendar now = Calendar.getInstance();
        int hour = now.get(Calendar.HOUR_OF_DAY);
        if ((hour > 7 && hour < 11) || (hour > 15 && hour < 18)||(hour > 19 && hour < 22)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 是否在敏感时间
     * @return  8、9点或者在16、17、或者在21
     */
    private boolean isAtCommImportTime() {
        Calendar now = Calendar.getInstance();
        int hour = now.get(Calendar.HOUR_OF_DAY);
        if ((hour > 7 && hour < 10) ||(hour > 19 && hour < 22)) {
            return true;
        } else {
            return false;
        }

    }


    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int what=msg.what;
            if(what==TYPE_JUMP_WIFI){
                AppUtils.jump2WifiSetting(MainService.this);
            }else if (what==TYPE_JUMP_TARGET){
                PackageManager packageManager = getPackageManager();
                Intent jumpIntent = packageManager.getLaunchIntentForPackage(PACKAGE_NAME);
                startActivity(jumpIntent);
            } else if (what== TYPE_JUMP_MAIN_COVER){
                Intent i= new Intent(Intent.ACTION_MAIN);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                i.addCategory(Intent.CATEGORY_HOME);
                startActivity(i);

//                PackageManager packageManager = getPackageManager();
//                Intent jumpIntent = packageManager.getLaunchIntentForPackage(PACKAGE_COVER_NAME);
//                startActivity(jumpIntent);
            }else if (what== TYPE_JUMP_ALI){
                PackageManager packageManager = getPackageManager();
                Intent jumpIntent = packageManager.getLaunchIntentForPackage(PACKAGE_COVER_NAME);
                startActivity(jumpIntent);
            }
        }
    };

    private OnMsgReceiverEventListener msgReceiverEventListener=new OnMsgReceiverEventListener(){

        @Override
        public void onReceiveMessageData(Context context, String msg, String msgId) {
            if (!TextUtils.isEmpty(msg)){
                Log.v("xhw","msg coming "+msg);
                if (msg.contains("1")){
                    Log.e("xhw","推送启动！ ");
                    executeProxyJump();
                }else if (msg.contains("2")){
                    AppUtils.clearAllNotification(context);
                }else if (msg.contains("3")){//直接跳转
                    executeALiJump();
                }
            }
        }
        @Override
        public void onReceiveClientId(Context context, String clientid) {

        }

        @Override
        public void onBindClientId(Context context, String msg, boolean isSuccess) {

        }

        @Override
        public void onUnBindClientId(Context context, String msg, boolean isSuccess) {

        }
    };
}
