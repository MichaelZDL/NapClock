package com.example.michaelclock;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.TextView;
import android.view.View.OnClickListener;

public class MainActivity extends Activity implements OnClickListener {

	//�豸������
	DevicePolicyManager mPolicyManager;
	//ComponentName��������ƣ�������������Ӧ�ó����е�Activity������
	ComponentName componentName;
	static Activity activity;
	//requestCode
	static TextView timeView,changeCountTextMin,changeCountTextSec;
	public static final int MY_REQUEST_CODE = 10086;
	public static final int OPEN_FILE_REQUEST_CODE = 10087;
	public static int SERVICE_OPENED = 0;
	public static int CLOSEME = 0;
	private static int count;
	private static LongRunningService.ChangeCountBinder changeCountBinder;

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
		changeCountTextSec = (EditText)findViewById(R.id.changeCountSec);
		changeCountTextMin = (EditText)findViewById(R.id.changeCountMin);
		activity = this;
		findViewById(R.id.cancel_button).setOnClickListener(this);
		findViewById(R.id.changeTime_button).setOnClickListener(this);
		findViewById(R.id.openFile).setOnClickListener(this);
		//��SharedPreference�ж�ȡ��һ������
		SharedPreferences prefGet = getSharedPreferences("countNum",MODE_PRIVATE);
		int c = prefGet.getInt("lastCountNum",30*60);
		changeCountTextSec.setText(String.valueOf(c%60));
		changeCountTextMin.setText(String.valueOf(c/60));
		//��һ������ʱ  ִ����������
		if(SERVICE_OPENED == 0){
			SERVICE_OPENED = 1;
			
			//����豸����������
			mPolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
			
			//�����MyReceiver��DeviceAdminReceiver������
			componentName = new ComponentName(this, MyReceiver.class);
			
			//���ж��Ƿ�߱�����Ȩ�ޣ�������У���ִ��lockNow()����������finish()��ǰActivity
			//��װ���һ�����ж�Ҫȥ���Ȩ��
			if (mPolicyManager.isAdminActive(componentName)){
				Intent intent = new Intent(this, LongRunningService.class);
				startService(intent);
				// �󶨷���
				Intent bindIntent = new Intent(this, LongRunningService.class);
				bindService(bindIntent, connection, BIND_AUTO_CREATE);
				//����
				mPolicyManager.lockNow();
			} else {
				getAdminActive();//��ȡȨ��
			}
		}
	}
	
	//��������
	@Override
	public void onClick(View arg0) {
		switch(arg0.getId()){
			case R.id.cancel_button:
				Intent stopIntent = new Intent(this, LongRunningService.class);
				unbindService(connection); // ������
				stopService(stopIntent); // ֹͣ����
				finish();
				android.os.Process.killProcess(android.os.Process.myPid());
		        break;
			case R.id.openFile:
		        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
				startActivityForResult(intent, OPEN_FILE_REQUEST_CODE);
				break;
			case R.id.changeTime_button:
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
				//����SharedPreference��
				SharedPreferences.Editor editor = getSharedPreferences("countNum",MODE_PRIVATE).edit();
				editor.putInt("lastCountNum", count);
				editor.commit();
				changeCountBinder.changeCount(count);
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
	
	/**
	 * ��ȡ����Ȩ��
	 */
	private void getAdminActive() {
		
		// �����豸����(��ʽIntent) - ��AndroidManifest.xml���趨��Ӧ������
		Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
		//Ȩ���б�
		intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName);
		//����������ڵ�һ��������ҪȨ�޼���ʱ�����Կ����Զ��������
		intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "�������������ʹ���������� =]");
		startActivityForResult(intent, MY_REQUEST_CODE);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		
		 //��ȡȨ�޳ɹ���������finish()�����������ȡȨ��
		if (requestCode == MY_REQUEST_CODE && resultCode == RESULT_OK){
			Intent intent = new Intent(this, LongRunningService.class);
			startService(intent);
			// �󶨷���
			Intent bindIntent = new Intent(this, LongRunningService.class);
			bindService(bindIntent, connection, BIND_AUTO_CREATE);
			//����
			mPolicyManager.lockNow();
		} else if (requestCode == MY_REQUEST_CODE){
			getAdminActive();//������ȡȨ��
		}

		if (requestCode == OPEN_FILE_REQUEST_CODE && resultCode == RESULT_OK){
			Uri uriSound = data.getData();
			//����SharedPreference��
			SharedPreferences.Editor editor = getSharedPreferences("musicUri",MODE_PRIVATE).edit();
			editor.putString("alarmingMusicUri", uriSound.toString());
			editor.commit();
		}
			super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	protected void onStart() {
		super.onStart();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
	}
	
	@Override
	protected void onStop() {
		super.onStop();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if(CLOSEME == 1){
			unbindService(connection);
			android.os.Process.killProcess(android.os.Process.myPid());
		}
	}

	@Override
	protected void onRestart() {
		super.onRestart();
	}
}
