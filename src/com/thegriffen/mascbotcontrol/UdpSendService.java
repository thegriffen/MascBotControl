package com.thegriffen.mascbotcontrol;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

public class UdpSendService extends Service {
	
	private final IBinder mBinder = new LocalBinder();
	private String dataToSend;
	
	public class LocalBinder extends Binder {
		UdpSendService getService() {
			return UdpSendService.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		Thread t = new Thread() {
			@Override
			public void run() {
				while(true) {
					
				}
			}
		};
		return mBinder;
	}
	
	public void send(String data) {
		dataToSend = data;
	}

}
