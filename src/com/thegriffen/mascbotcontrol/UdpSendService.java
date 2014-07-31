package com.thegriffen.mascbotcontrol;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;

public class UdpSendService extends Service {
	
	private final IBinder mBinder = new LocalBinder();
	String dataToSend = "";
	String oldData = "";
	DatagramSocket clientSocket = null;
	
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
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(UdpSendService.this);
				String ipAddr = prefs.getString("ip_address", "192.168.1.1");
				int port = Integer.parseInt(prefs.getString("port", "8888"));
				InetAddress IPAddress = null;
				try {
					IPAddress = InetAddress.getByName(ipAddr);
					clientSocket = new DatagramSocket();
				} catch (Exception e) {
					e.printStackTrace();
				}
				System.out.println("Socket Created");
				while(true) {
					if(!oldData.equals(dataToSend)) {
						try {
							byte[] sendData = new byte[1024];
							DatagramPacket sendPacket;
							sendData = dataToSend.getBytes();
							sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
							clientSocket.send(sendPacket);				
						} catch (Exception e) {
							e.printStackTrace();
						}
						oldData = dataToSend;
					}
					try {
						Thread.sleep(25);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		};
		t.start();
		return mBinder;
	}
	
	public void send(String data) {
		dataToSend = data;
	}
	
	@Override
	public boolean onUnbind(Intent intent) {
		clientSocket.close();
		System.out.println("Service Stopped");
		return false;
	}

}
