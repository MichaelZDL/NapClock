package com.example.michaelclock;

import java.util.Date;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.util.Log;

public class LongRunningService extends Service {
	public static int count;
	public String f,m,muf;
	private MyCountDownTimer mc;
	private ChangeCountBinder mBinder = new ChangeCountBinder();
	class ChangeCountBinder extends Binder {
		public void changeCount(int changedCount) {
			mc.cancel();
			mc = new MyCountDownTimer(changedCount*1000,1000);
			mc.start();
		}
	}
	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
	class MyCountDownTimer extends CountDownTimer {
		public MyCountDownTimer(long millisInFuture, long countDownInterval) {
			super(millisInFuture, countDownInterval);
		}
    public void onTick(long millisUntilFinished) {
        //"count=()+1" for get rid of starting at the Num which has 1 short
        //eg. we set 1000, it may start at 999. No, We want it stat at 1000.

        //"millisUntilFinished-1" for sometimes millisUntilFinished will be 18000000,
        //but mostly be 1799****, it will cause count not stable.
        //So "millisUntilFinished-1", making millisUntilFinished equals 1799**** every time
        count=(int) ((millisUntilFinished-1)/1000)+1;

        muf=Long.toString(millisUntilFinished);
        if((count / 60)>9){
            f = Integer.toString(count / 60);
        }else{
            f = "0"+Integer.toString(count / 60);
        }

        if((count % 60)>9){
            m = Integer.toString(count % 60);
        }else{
            m = "0"+Integer.toString(count % 60);
        }

        Log.d("millisUntilFinished",muf);
        Log.d("countdowntimer", "executed at " + new Date().toString());
        Log.d("countdowntimer", "s=" + f+":"+m);
        Intent intentT = new Intent("com.example.michaelclock.MY_BROADCAST");

        intentT.putExtra("message", f+":"+m);
        sendBroadcast(intentT);
    }
  
        @Override  
        public void onFinish() {  
        	Intent intentT = new Intent("com.example.michaelclock.MY_BROADCAST");
        	intentT.putExtra("message", "Alarm");
			sendBroadcast(intentT);
            //against from killed by Android System, but it seems no use
        	Intent intentNew = new Intent(LongRunningService.this,AlarmRingActivity.class);
        	intentNew.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        	startActivity(intentNew);
        }  
    }

    @Override
    public void onCreate() {
	    super.onCreate();
	    Notification notification = new Notification(R.drawable.ic_launcher,
                "NapClock is running...", System. currentTimeMillis());
	    Intent notificationIntent = new Intent(this, MainActivity.class);
	    PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,notificationIntent, 0);
	    notification.setLatestEventInfo(this, "NapClock is running...", "Touch to see more", pendingIntent);
	    startForeground(1, notification);
	    Log.d("MyService", "onCreate executed");	
    }

    @Override
    public void onDestroy() {
    	mc.cancel();
	    super.onDestroy();
    }

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		//when service first start, we get countNum from SharedPreference we set,
        //if nothing there,we set 30 minutes
		SharedPreferences prefGet = getSharedPreferences("countNum",MODE_PRIVATE);
		int c = prefGet.getInt("lastCountNum",30*60);
		//start countDownTimer
		mc = new MyCountDownTimer(c*1000,1000);
		mc.start();
		return super.onStartCommand(intent, flags, startId);
	}

}
