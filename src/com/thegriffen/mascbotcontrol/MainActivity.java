package com.thegriffen.mascbotcontrol;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.thegriffen.widgets.JoystickMovedListener;
import com.thegriffen.widgets.JoystickView;
import com.thegriffen.widgets.VerticleSwitchListener;
import com.thegriffen.widgets.VerticleSwitchView;

public class MainActivity extends ActionBarActivity {

	private Menu menu;
	private boolean connected = false;
	JoystickView joystick;
	VerticleSwitchView mascotHead;
	NetworkTask networkTask;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		joystick = (JoystickView) findViewById(R.id.joystickView);
		joystick.setOnJostickMovedListener(_listener);
		mascotHead = (VerticleSwitchView) findViewById(R.id.mascotHeadSwitch);
		mascotHead.setOnSwitchedListener(new VerticleSwitchListener() {			
			@Override
			public void OnSwitched(boolean down) {
				sendData("h" + String.valueOf(down));
			}
		});
		Button leftForward = (Button) findViewById(R.id.leftForward);
		leftForward.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				sendData("lf");
			}
		});
		Button leftBackward = (Button) findViewById(R.id.leftBackward);
		leftBackward.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				sendData("lb");
			}
		});
		Button leftStop = (Button) findViewById(R.id.leftStop);
		leftStop.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				sendData("ls");
			}
		});
		
		Button rightForward = (Button) findViewById(R.id.rightForward);
		rightForward.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				sendData("rf");
			}
		});
		Button rightBackward = (Button) findViewById(R.id.rightBackward);
		rightBackward.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				sendData("rb");
			}
		});
		Button rightStop = (Button) findViewById(R.id.rightStop);
		rightStop.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				sendData("rs");
			}
		});
	}

	private JoystickMovedListener _listener = new JoystickMovedListener() {

		@Override
		public void OnMoved(int pan, int tilt) {
			pan = pan + 1500;
			tilt = tilt * -1 + 1500;
			sendData("x" + pan + "y" + tilt);
		}

		@Override
		public void OnReleased() {
			int pan = 1500;
			int tilt = 1500;
			sendData("x" + pan + "y" + tilt);
			sendData("x" + pan + "y" + tilt);
			sendData("x" + pan + "y" + tilt);
			sendData("x" + pan + "y" + tilt);
			sendData("x" + pan + "y" + tilt);
			sendData("x" + pan + "y" + tilt);
		}
	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		this.menu = menu;
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.action_connect) {
			if (!connected) {
				networkTask = new NetworkTask();
				networkTask.execute();
			} else {
				if (networkTask != null) {
					networkTask.closeSocket();
					networkTask.cancel(true);
				}
			}
		} else if (item.getItemId() == R.id.action_settings) {
			Intent intent = new Intent(this, SettingsActivity.class);
			startActivity(intent);
		}
		return true;
	}
	
	private void sendData(String data) {
		if(networkTask != null) {
			networkTask.sendDataToNetwork(data);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (networkTask != null) {
			networkTask.closeSocket();
			networkTask.cancel(true);
			networkTask = null;
		}
	}

	public void changeConnectionStatus(boolean connected) {
		if (connected) {
			menu.findItem(R.id.action_connect).setTitle(R.string.action_disconnect);
			this.connected = true;
		} else {
			menu.findItem(R.id.action_connect).setTitle(R.string.action_connect);
			this.connected = false;
		}
	}
	
	private void updateBattery(String value) {
		TextView battery = (TextView) findViewById(R.id.batteryVoltage);
		battery.setText(value);
	}

	public class NetworkTask extends AsyncTask<Void, String, Boolean> {
		DatagramSocket clientSocket;
		int port;
		InetAddress IPAddress;
		String dataToSend;

		@Override
		protected void onPreExecute() {
			changeConnectionStatus(true);
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			boolean result = false;
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
			String ipAddr = prefs.getString("ip_address", "192.168.1.1");
			port = Integer.parseInt(prefs.getString("port", "8888"));
			try {
				IPAddress = InetAddress.getByName(ipAddr);
				clientSocket = new DatagramSocket();
				byte[] receiveData = new byte[1024];
				byte[] sendData = new byte[1024];
				sendData = "Hello Arduino".getBytes();
				DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
				clientSocket.send(sendPacket);
				while(true) {
					if(dataToSend != null) {
						try {
							sendData = dataToSend.getBytes();
							sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
							clientSocket.send(sendPacket);
							dataToSend = null;
						} catch (IOException e) {
							Log.e("Socket",	"SendDataToNetwork: Message send failed. Caught an exception");
							e.printStackTrace();
						}
					}
					
//					DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
//					clientSocket.receive(receivePacket);
//					String input = new String(receivePacket.getData());
//					System.out.println(input);
//					publishProgress(input);
				}
				
			} catch (IOException e) {
				e.printStackTrace();
				result = true;
			} catch (Exception e) {
				e.printStackTrace();
				result = true;
			} finally {
				closeSocket();
			}
			return result;
		}

		public void closeSocket() {
			clientSocket.close();
		}

		public void sendDataToNetwork(String cmd) {
			dataToSend = cmd;
		}
		
		@Override
		protected void onProgressUpdate(String... values) {
			if (values.length > 0) {
				updateBattery(values[0]);
			}
		}

		@Override
		protected void onCancelled() {
			changeConnectionStatus(false);
		}

		@Override
		protected void onPostExecute(Boolean result) {
			if (result) {
				Log.d("Socket", "onPostExecute: Completed with an Error.");
			} else {
				Log.d("Socket", "onPostExecute: Completed.");
			}
			changeConnectionStatus(false);
		}
	}

}
