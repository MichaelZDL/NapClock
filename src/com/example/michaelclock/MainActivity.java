package com.example.michaelclock;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.view.View.OnClickListener;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener {
    //preparation of locking screen
	DevicePolicyManager mPolicyManager;
	ComponentName componentName;
    //for static MyBroadcastReceiver
	static Activity activity;

	static TextView timeView, songInfo,changeCountTextMin,changeCountTextSec;
	public static final int MY_REQUEST_CODE = 10086;
	public static final int OPEN_FILE_REQUEST_CODE = 10087;
	public static int SERVICE_OPENED = 0;
	public static int CLOSEME = 0;
	private static int count;
	private static LongRunningService.ChangeCountBinder changeCountBinder;
    String  song_title, song_artist;

	protected ServiceConnection connection = new ServiceConnection() {
		@Override
		public void onServiceDisconnected(ComponentName name) {
		}
        @Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			changeCountBinder = (LongRunningService.ChangeCountBinder) service;
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_main);
		timeView = (TextView)findViewById(R.id.myTime);
		songInfo = (TextView)findViewById(R.id.songInfo);
		changeCountTextSec = (EditText)findViewById(R.id.changeCountSec);
		changeCountTextMin = (EditText)findViewById(R.id.changeCountMin);
		activity = this;
		findViewById(R.id.cancel_button).setOnClickListener(this);
		findViewById(R.id.changeTime_button).setOnClickListener(this);
		findViewById(R.id.openFile).setOnClickListener(this);
        //SharedPreference
		SharedPreferences prefGet = getSharedPreferences("countNum",MODE_PRIVATE);
		int c = prefGet.getInt("lastCountNum",30*60);
        if(c < 60039){
            if((c/60)<10){
                changeCountTextMin.setText("0"+String.valueOf(c/60));
            }else{
                changeCountTextMin.setText(String.valueOf(c/60));
            }
            if((c%60)<10){
                changeCountTextSec.setText("0"+String.valueOf(c%60));
            }else {
                changeCountTextSec.setText(String.valueOf(c%60));
            }
        }
        //read last songInfo in SharedPreference
        SharedPreferences prefGetMusic = getSharedPreferences("musicUri", MODE_PRIVATE);
        String uriSoundString = prefGetMusic.getString("alarmingMusicUri", null);

        if(uriSoundString != null){
            Uri uriSound = Uri.parse(uriSoundString);
            String[] proJ = {MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.DATA,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.ARTIST,};
            Cursor tempCursor = managedQuery(uriSound,proJ,null,null,null);
            tempCursor.moveToFirst(); //reset the cursor
            int col_index;
            do{
                col_index = tempCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
                song_title = tempCursor.getString(col_index);
                col_index = tempCursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.ARTIST);
                song_artist = tempCursor.getString(col_index);
            }while(tempCursor.moveToNext());
            songInfo.setText(song_title + "\n" + song_artist);
        }else {
            songInfo.setText("斑马,斑马 (Live)\n张婧");
        }
        //start countDownTimer and lock screen only in first start this app
		if(SERVICE_OPENED == 0){
			SERVICE_OPENED = 1;
			//for lock screen
			mPolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
			componentName = new ComponentName(this, MyReceiver.class);
            //check lock screen admission
			if (mPolicyManager.isAdminActive(componentName)){

                Intent intent = new Intent(this, LongRunningService.class);
				startService(intent);
                Intent bindIntent = new Intent(this, LongRunningService.class);
				bindService(bindIntent, connection, BIND_AUTO_CREATE);
				//lock screen
				mPolicyManager.lockNow();

			} else {
				getAdminActive();//if no admission, get it
			}
		}
	}
	
	//onClick all buttons
	@Override
	public void onClick(View arg0) {
		switch(arg0.getId()){
			case R.id.cancel_button:
				Intent stopIntent = new Intent(this, LongRunningService.class);
				unbindService(connection); // unbindService if bound
				stopService(stopIntent);
				finish();
				android.os.Process.killProcess(android.os.Process.myPid());
		        break;
			case R.id.openFile:
		        Intent intent = new Intent(Intent.ACTION_PICK,
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
				startActivityForResult(intent, OPEN_FILE_REQUEST_CODE);
				break;
			case R.id.changeTime_button:
                //close IME when button done
                InputMethodManager inputManager = (InputMethodManager)
                        getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);

				int m,s;
				if(changeCountTextMin.getText().toString().equals("")){
					m = 0;
				}
				else{
					m = Integer.parseInt(changeCountTextMin.getText().toString());
				}
				if(changeCountTextSec.getText().toString().equals("")){
					s = 0;
				}else{
					s = Integer.parseInt(changeCountTextSec.getText().toString());
				}
				count = s + m * 60;
                if((count >10 )&&(count <60000)){
                    //save new setting of countNum in SharedPreference
                    SharedPreferences.Editor editor =
                            getSharedPreferences("countNum",MODE_PRIVATE).edit();
                    editor.putInt("lastCountNum", count);
                    editor.commit();

                    changeCountBinder.changeCount(count);
                    Log.d("changedCountIs", Integer.toString(count));
                }else{
                    Toast.makeText(getApplicationContext(),
                            "Over 10s and under 60000s is required", Toast.LENGTH_LONG).show();
                }
				break;
			default:break;
		}
	}

	public static class MyBroadcastReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if(!(intent.getStringExtra("message").equals("Finish MainActivity"))){
				timeView.setText(intent.getStringExtra("message"));
			}else{
				timeView.setText("Hello");
				CLOSEME = 1;
				activity.finish();
			}
		}
	}

	private void getAdminActive() {
		Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
		intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName);
		intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Your admission is required to lock screen for more convenient =]");
		startActivityForResult(intent, MY_REQUEST_CODE);
	}

    @Override
    protected void  onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == MY_REQUEST_CODE && resultCode == RESULT_OK){
            Intent intent = new Intent(this, LongRunningService.class);
            startService(intent);
            Intent bindIntent = new Intent(this, LongRunningService.class);
            bindService(bindIntent, connection, BIND_AUTO_CREATE);
            mPolicyManager.lockNow();
        } else if (requestCode == MY_REQUEST_CODE){
            getAdminActive();
        }
        if (requestCode == OPEN_FILE_REQUEST_CODE && resultCode == RESULT_OK){
            Uri uriSound = data.getData();
            String[] proJ = {MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.DATA,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.ARTIST,
            };
            Cursor tempCursor = managedQuery(uriSound,proJ,null,null,null);
            tempCursor.moveToFirst(); //reset the cursor
            int col_index;
            do{
                col_index = tempCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
                song_title = tempCursor.getString(col_index);
                col_index = tempCursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.ARTIST);
                song_artist = tempCursor.getString(col_index);
            }while(tempCursor.moveToNext());
            songInfo.setText(song_title + "\n" + song_artist);
            //save the new choice of alarming song in SharedPreference
            SharedPreferences.Editor editor = getSharedPreferences("musicUri",MODE_PRIVATE).edit();
            editor.putString("alarmingMusicUri", uriSound.toString());
            editor.commit();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if(CLOSEME == 1){
			unbindService(connection);
			android.os.Process.killProcess(android.os.Process.myPid());
		}
	}
}
