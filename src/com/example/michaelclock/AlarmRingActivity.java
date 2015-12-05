package com.example.michaelclock;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;

import java.io.IOException;

public class AlarmRingActivity extends Activity implements OnClickListener {

	private MediaPlayer mediaPlayer;
	Uri uriSound;
	private Vibrator vibrator;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
	 	//锁屏状态下显示出来
		setToShowOverLockScreen(getWindow());
		setContentView(R.layout.activity_alarmring);
		//放闹钟音乐
		SharedPreferences prefGet = getSharedPreferences("musicUri", MODE_PRIVATE);
		uriSound = Uri.parse(prefGet.getString("alarmingMusicUri", "android.resource://com.example.michaelclock/raw/music"));

		try {
			mediaPlayer = new MediaPlayer();
			mediaPlayer.setDataSource(this, uriSound);
			mediaPlayer.prepare();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (!mediaPlayer.isPlaying()) {
			mediaPlayer.start();
		}
		 /* 
         * 想设置震动大小可以通过改变pattern来设定，如果开启时间太短，震动效果可能感觉不到 
         * */  
        vibrator = (Vibrator)getSystemService(VIBRATOR_SERVICE);  
        long [] pattern = {500,500,500,500};   // 停止 开启 停止 开启   
        vibrator.vibrate(pattern,2);           //重复两次上面的pattern 如果只想震动一次，index设为-1   
        
		findViewById(R.id.close_button).setOnClickListener(this);
	}
	
	//监听按键
	@Override
	 public void onClick(View arg0) {
		switch(arg0.getId()){
			case R.id.close_button:
				mediaPlayer.reset();
				vibrator.cancel();
				Intent stopIntent = new Intent(this, LongRunningService.class);
				stopService(stopIntent); // 停止服务
				Intent intentT = new Intent("com.example.michaelclock.MY_BROADCAST");
	        	intentT.putExtra("message", "Finish MainActivity");
				sendBroadcast(intentT);
				finish();
		        break;
			default:break;
		}
	}
	
	@Override//重写返回键
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (KeyEvent.KEYCODE_BACK == keyCode){	 
			mediaPlayer.reset();
			vibrator.cancel();
			Intent stopIntent = new Intent(this, LongRunningService.class);
			stopService(stopIntent); // 停止服务
			Intent intentT = new Intent("com.example.michaelclock.MY_BROADCAST");
        	intentT.putExtra("message", "Finish MainActivity");
			sendBroadcast(intentT);
			finish();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
	
	private void setToShowOverLockScreen(Window win) {
        win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
    }
}
