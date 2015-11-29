package com.example.michaelclock;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;

public class AlarmRingActivity extends Activity implements OnClickListener {

	private MediaPlayer mediaPlayer;
	private Vibrator vibrator;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		//����״̬����ʾ����
		setToShowOverLockScreen(getWindow());
		
		setContentView(R.layout.activity_alarmring);

		//����������
		try {
			mediaPlayer = MediaPlayer.create(AlarmRingActivity.this, R.raw.music);
			mediaPlayer.prepare();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if (!mediaPlayer.isPlaying()) {
			mediaPlayer.start();
		}
		 /* 
         * �������𶯴�С����ͨ���ı�pattern���趨���������ʱ��̫�̣���Ч�����ܸо����� 
         * */  
        vibrator = (Vibrator)getSystemService(VIBRATOR_SERVICE);  
        long [] pattern = {500,500,500,500};   // ֹͣ ���� ֹͣ ����   
        vibrator.vibrate(pattern,2);           //�ظ����������pattern ���ֻ����һ�Σ�index��Ϊ-1   
        
		findViewById(R.id.close_button).setOnClickListener(this);
	}
	
	//��������
	@Override
	 public void onClick(View arg0) {
		switch(arg0.getId()){
			case R.id.close_button:
				mediaPlayer.reset();
				vibrator.cancel();
				Intent stopIntent = new Intent(this, LongRunningService.class);
				stopService(stopIntent); // ֹͣ����
				Intent intentT = new Intent("com.example.michaelclock.MY_BROADCAST");
	        	intentT.putExtra("message", "Finish MainActivity");
				sendBroadcast(intentT);
				finish();
		        break;
			default:break;
		}
	}
	
	@Override//��д���ؼ�
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (KeyEvent.KEYCODE_BACK == keyCode){	 
			mediaPlayer.reset();
			vibrator.cancel();
			Intent stopIntent = new Intent(this, LongRunningService.class);
			stopService(stopIntent); // ֹͣ����
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