package com.example.michaelclock;
import android.app.Activity;
import android.app.Dialog;
import android.content.ClipboardManager;
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
import android.widget.Button;
import android.widget.Toast;

import java.io.IOException;

public class AlarmRingActivity extends Activity implements OnClickListener {
	private MediaPlayer mediaPlayer;
	Uri uriSound;
	private Vibrator vibrator;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	 	//wake up the screen when alarming
		setToShowOverLockScreen(getWindow());

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_alarmring);
		//play the music
		SharedPreferences prefGet = getSharedPreferences("musicUri", MODE_PRIVATE);
		uriSound = Uri.parse(prefGet.getString("alarmingMusicUri",
                "android.resource://com.example.michaelclock/raw/music"));
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

        vibrator = (Vibrator)getSystemService(VIBRATOR_SERVICE);
        long [] pattern = {500,500,500,500};   // start stop start stop
        //vibrate repeat 2 times, if only 1 time is needed, set index -1
        vibrator.vibrate(pattern,2);
        
		findViewById(R.id.close_button).setOnClickListener(this);
		findViewById(R.id.title_button2).setOnClickListener(this);

	}
	
	@Override
	 public void onClick(View arg0) {
		switch(arg0.getId()){
			case R.id.close_button:
                Button closeBtn = (Button) findViewById(R.id.close_button);
                closeBtn.setBackgroundResource( R.drawable.empty_sand_glass_black);
				mediaPlayer.reset();
				vibrator.cancel();
				Intent stopIntent = new Intent(this, LongRunningService.class);
				stopService(stopIntent);
				Intent intentT = new Intent("com.example.michaelclock.MY_BROADCAST");
	        	intentT.putExtra("message", "Finish MainActivity");
				sendBroadcast(intentT);
				finish();
		        break;
            case R.id.title_button2:
                final Dialog dialog = new Dialog(this);
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                dialog.setContentView(R.layout.about_dialog);
                Button dialogButton = (Button) dialog.findViewById(R.id.copy_email);
                dialogButton.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                        clipboard.setText("zhdelong@foxmail.com");
                        Toast.makeText(AlarmRingActivity.this,"The email is copied to clipboard", Toast.LENGTH_SHORT).show();
                    }
                });
                dialog.show();
                break;
			default:break;
		}
	}
	
	@Override//override back button on phone
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (KeyEvent.KEYCODE_BACK == keyCode){
            Button closeBtn = (Button) findViewById(R.id.close_button);
            closeBtn.performClick();
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
