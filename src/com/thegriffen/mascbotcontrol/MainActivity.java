package com.thegriffen.mascbotcontrol;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import com.thegriffen.widgets.JoystickMovedListener;
import com.thegriffen.widgets.JoystickView;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class MainActivity extends ActionBarActivity {

	private Menu menu;
	private boolean connected = false;
	JoystickView joystick;
	NetworkTask networkTask;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.joystick);
		joystick = (JoystickView) findViewById(R.id.joystickView);
		joystick.setOnJostickMovedListener(_listener);
	}

	private JoystickMovedListener _listener = new JoystickMovedListener() {

		@Override
		public void OnMoved(int pan, int tilt) {
			pan = pan + 1500;
			tilt = tilt * -1 + 1500;
			if (networkTask != null) {
				networkTask.sendDataToNetwork("X" + pan + "Y" + tilt + "\n");
			}
		}

		@Override
		public void OnReleased() {
			int pan = 1500;
			int tilt = 1500;
			if (networkTask != null) {
				networkTask.sendDataToNetwork("X" + pan + "Y" + tilt + "\n");
				networkTask.sendDataToNetwork("X" + pan + "Y" + tilt + "\n");
				networkTask.sendDataToNetwork("X" + pan + "Y" + tilt + "\n");
				networkTask.sendDataToNetwork("X" + pan + "Y" + tilt + "\n");
				networkTask.sendDataToNetwork("X" + pan + "Y" + tilt + "\n");
				networkTask.sendDataToNetwork("X" + pan + "Y" + tilt + "\n");
			}
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

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (networkTask != null) {
			networkTask.cancel(true);
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
		System.out.println(value);
	}

	public class NetworkTask extends AsyncTask<Void, String, Boolean> {
		Socket nsocket;
		InputStream nis;
		OutputStream nos;
		BufferedReader inFromServer;

		@Override
		protected void onPreExecute() {
			changeConnectionStatus(true);
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			boolean result = false;
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
			String ipAddr = prefs.getString("ip_address", "192.168.1.1");
			int port = Integer.parseInt(prefs.getString("port", "8888"));
			try {
				SocketAddress sockaddr = new InetSocketAddress(ipAddr, port);
				nsocket = new Socket();
				nsocket.connect(sockaddr, 5000);
				if (nsocket.isConnected()) {
					nis = nsocket.getInputStream();
					nos = nsocket.getOutputStream();
					BufferedReader dis = new BufferedReader(new InputStreamReader(nis));
					while (true) {
						String msgFromServer = dis.readLine();
						publishProgress(msgFromServer);
					}
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
			try {
				nis.close();
				nos.close();
				nsocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public void sendDataToNetwork(String cmd) {
			try {
				if (nsocket.isConnected()) {
					nos.write(cmd.getBytes());
				} else {
					Log.d("Socket",	"SendDataToNetwork: Cannot send message. Socket is closed");
				}
			} catch (Exception e) {
				Log.d("Socket",	"SendDataToNetwork: Message send failed. Caught an exception");
			}
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
