package com.jiayuan.xuhuawei.keepappalive.mutiservice;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;

import com.jiayuan.xuhuawei.keepappalive.MyApp;
import com.jiayuan.xuhuawei.keepappalive.R;
import com.jiayuan.xuhuawei.keepappalive.constant.ActionConst;
import com.jiayuan.xuhuawei.keepappalive.notification.CancelNoticeService;
import com.jiayuan.xuhuawei.keepappalive.services.MainService;

/**
 * 闹钟service
 */
public class AlarmService extends Service {
    public static final int NOTICE_ID = 100;

    public static void startAlarmService() {
        Context context= MyApp.getAppContext();
        Intent intent=new Intent(context,AlarmService.class);
        context.startService(intent);
    }

    private static final int INTERVAL_TIME=10 * 1000;

    @Override
    public IBinder onBind(Intent intent) {
       return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        stopAlarm();
        startAlarm();

        //如果API大于18，需要弹出一个可见通知
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2){
            Notification.Builder builder = new Notification.Builder(this);
            builder.setSmallIcon(R.mipmap.ic_launcher);
            builder.setContentTitle("KeepAppAlive");
            builder.setContentText("DaemonService is runing...");
            startForeground(NOTICE_ID,builder.build());
            // 如果觉得常驻通知栏体验不好
            // 可以通过启动CancelNoticeService，将通知移除，oom_adj值不变
            Intent intent = new Intent(this, CancelNoticeService.class);
            startService(intent);
        }else{
            startForeground(NOTICE_ID,new Notification());
        }

    }

    @Override
    public int onStartCommand(Intent intent,  int flags, int startId) {
         super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // 如果Service被杀死，干掉通知
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2){
            NotificationManager mManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
            mManager.cancel(NOTICE_ID);
        }
        // 重启自己
        Intent intent = new Intent(getApplicationContext(), AlarmService.class);
        startService(intent);

    }

    protected void startAlarm(){
        // 立刻执行，此后5分钟走一次//SystemClock.elapsedRealtime()
        AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(getApplicationContext(), MainService.class);
        intent.setAction(ActionConst.ALARM_ACTION);
        PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        //触发服务的起始时间
        long triggerAtTime = SystemClock.elapsedRealtime();
        alarm.setRepeating(AlarmManager.RTC_WAKEUP,System.currentTimeMillis(), INTERVAL_TIME, pendingIntent);
    }

    private void stopAlarm() {
        AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(getApplicationContext(), MainService.class);
        PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarm.cancel(pendingIntent);
    }

}
