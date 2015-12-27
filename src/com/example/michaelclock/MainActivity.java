package com.example.michaelclock;
import android.app.Activity;
import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.view.View.OnClickListener;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener {
    //preparation of locking screen
    static DevicePolicyManager mPolicyManager;
    static ComponentName componentName;
    //for static MyBroadcastReceiver
	static Activity activity;

	static TextView timeView, songInfo,changeCountTextMin,changeCountTextSec;
	public static final int MY_REQUEST_CODE = 10086;
	public static final int OPEN_FILE_REQUEST_CODE = 10087;
	public static int SERVICE_OPENED = 0;
	public static int CLOSE_ME = 0;
	public static int lockScreenIn = 2;
    boolean mBound = false;
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
		findViewById(R.id.title_button).setOnClickListener(this);
        //get last CountNum from SharedPreference and show in textView
		SharedPreferences prefGet = getSharedPreferences("countNum",MODE_PRIVATE);
		int c = prefGet.getInt("lastCountNum", 30 * 60);
        String tView = "00:00";
        if(c < 60039){
            if((c/60)<10){
                changeCountTextMin.setText("0"+String.valueOf(c/60));
                tView = "0"+String.valueOf(c/60);
            }else{
                changeCountTextMin.setText(String.valueOf(c/60));
                tView = String.valueOf(c/60);
            }
            if((c%60)<10){
                changeCountTextSec.setText("0"+String.valueOf(c%60));
                tView = tView + ":" + "0"+String.valueOf(c%60);
            }else {
                changeCountTextSec.setText(String.valueOf(c%60));
                tView = tView + ":" + String.valueOf(c%60);
            }
        }
        timeView.setText(tView);
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
            //check lock screen admission
            mPolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
            componentName = new ComponentName(this, MyReceiver.class);
            if (!(mPolicyManager.isAdminActive(componentName))){
                getAdminActive();//if no admission, get it
            }else{
                SERVICE_OPENED = 1;
//                final Toast toast = Toast.makeText(activity,
//                        "Screen will be locked in 1s", Toast.LENGTH_LONG);
//                toast.show();
                Intent intent = new Intent(this, LongRunningService.class);
                startService(intent);
                mPolicyManager.lockNow();
            }
		}
        EditText editText = (EditText) findViewById(R.id.changeCountSec);
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    Button ChangeTimeButton = (Button) findViewById(R.id.changeTime_button);
                    ChangeTimeButton.performClick();
                }
                return handled;
            }
        });

	}
	
	//onClick all buttons
	@Override
	public void onClick(View arg0) {
		switch(arg0.getId()){
            case R.id.title_button:
                final Dialog dialog = new Dialog(this);
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                dialog.setContentView(R.layout.about_dialog);
                Button dialogButton = (Button) dialog.findViewById(R.id.copy_email);
                dialogButton.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                        clipboard.setText("zhdelong@foxmail.com");
                        Toast.makeText(MainActivity.this, "The email is copied to clipboard", Toast.LENGTH_SHORT).show();
                    }
                });
                dialog.show();
                break;
            case R.id.cancel_button:
                Button closeBtn = (Button) findViewById(R.id.cancel_button);
                closeBtn.setBackgroundResource( R.drawable.cancel_opp);
                Intent stopIntent = new Intent(this, LongRunningService.class);
				stopService(stopIntent);
                if (mBound) {
                    unbindService(connection);
                    mBound = false;
                }
                CLOSE_ME = 1;
                finish();
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
                    Toast.makeText(MainActivity.this, "New Time has been Saved", Toast.LENGTH_SHORT).show();

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
//        public static boolean closeToast = false;
        public static boolean lockRunning = false;
        @Override
		public void onReceive(Context context, Intent intent) {
			if(!(intent.getStringExtra("message").equals("Finish MainActivity"))){
                if(intent.getStringExtra("message").equals("00:02")){
                    //set timeView 00:01 in 1s
                        Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            timeView.setText("00:01");
                        }
                    }, 1000);
                }

                if(lockRunning == false){
                    timeView.setText(intent.getStringExtra("message"));
                }
			}else{
//                if(closeToast == false){
//                    lockScreenIn--;
//                    //hold on textView and delay 2s, against from shaking in textView
//                    if(lockScreenIn == 1){
//                        lockRunning = true;
//                        Handler handler = new Handler();
//                        handler.postDelayed(new Runnable() {
//                            @Override
//                            public void run() {
//                                lockRunning = false;
//                            }
//                        }, 2000);
//                    }
//                    if(lockScreenIn == 1){
//                        closeToast = true;
//                        //lock screen
//                        mPolicyManager.lockNow();
//                    }
//                }
                timeView.setText("00:00");
                CLOSE_ME = 1;
                activity.finish();
            }
		}
	}

	private void getAdminActive() {
		Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
		intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName);
		startActivityForResult(intent, MY_REQUEST_CODE);
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Your admission is required to lock screen for more convenient =]");
    }

    @Override
    protected void  onActivityResult(int requestCode, int resultCode, Intent data) {
        SERVICE_OPENED = 1;
        if (requestCode == MY_REQUEST_CODE && resultCode == RESULT_OK){
            final Toast toast = Toast.makeText(activity,
                    "Screen will be locked in 1s", Toast.LENGTH_LONG);
            toast.show();
            Intent intent = new Intent(this, LongRunningService.class);
            startService(intent);
        } else if (requestCode == MY_REQUEST_CODE){
            CLOSE_ME = 1;
            activity.finish();
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
            Toast.makeText(MainActivity.this, "New song has been Saved", Toast.LENGTH_SHORT).show();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(SERVICE_OPENED == 1){
            // Bind to LocalService
            Intent bindIntent = new Intent(this, LongRunningService.class);
            bindService(bindIntent, connection, BIND_AUTO_CREATE);
            mBound = true;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mBound) {
            unbindService(connection);
            mBound=false;
        }
    }

    @Override
	protected void onDestroy() {
		super.onDestroy();
		if(CLOSE_ME == 1){
			android.os.Process.killProcess(android.os.Process.myPid());
		}
	}
}
