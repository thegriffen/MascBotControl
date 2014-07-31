package com.thegriffen.mascbotcontrol;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.thegriffen.mascbotcontrol.UdpSendService.LocalBinder;
import com.thegriffen.widgets.JoystickMovedListener;
import com.thegriffen.widgets.JoystickView;
import com.thegriffen.widgets.VerticleSwitchListener;
import com.thegriffen.widgets.VerticleSwitchView;

public class MainActivity extends ActionBarActivity {

	private Menu menu;
	private boolean connected = false;
	JoystickView joystick;
	VerticleSwitchView mascotHead;
	TcpNetworkTask tcpNetworkTask;
	UdpSendService mService;
	boolean mBound = false;

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
				sendTcpData("h" + String.valueOf(down));
			}
		});
		Button leftForward = (Button) findViewById(R.id.leftForward);
		leftForward.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				sendTcpData("lf");
			}
		});
		Button leftBackward = (Button) findViewById(R.id.leftBackward);
		leftBackward.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				sendTcpData("lb");
			}
		});
		Button leftStop = (Button) findViewById(R.id.leftStop);
		leftStop.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				sendTcpData("ls");
			}
		});
		
		Button rightForward = (Button) findViewById(R.id.rightForward);
		rightForward.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				sendTcpData("rf");
			}
		});
		Button rightBackward = (Button) findViewById(R.id.rightBackward);
		rightBackward.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				sendTcpData("rb");
			}
		});
		Button rightStop = (Button) findViewById(R.id.rightStop);
		rightStop.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				sendTcpData("rs");
			}
		});
	}

	private JoystickMovedListener _listener = new JoystickMovedListener() {

		@Override
		public void OnMoved(int pan, int tilt) {
			pan = pan + 1500;
			tilt = tilt * -1 + 1500;
			sendUdpData("x" + pan + "y" + tilt);
		}

		@Override
		public void OnReleased() {
			int pan = 1500;
			int tilt = 1500;
			sendUdpData("x" + pan + "y" + tilt);
			sendUdpData("x" + pan + "y" + tilt);
			sendUdpData("x" + pan + "y" + tilt);
			sendUdpData("x" + pan + "y" + tilt);
			sendUdpData("x" + pan + "y" + tilt);
			sendUdpData("x" + pan + "y" + tilt);
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
		switch (item.getItemId()) {
			case R.id.action_connect:
				if (!connected) {
					connect();
				} else {
					disconnect();
				}
				break;
			case R.id.action_settings:
				Intent intent = new Intent(this, SettingsActivity.class);
				startActivity(intent);
				break;
		}
		return true;
	}
	
	private void connect() {
		tcpNetworkTask = new TcpNetworkTask();
		tcpNetworkTask.execute();
		Intent intent = new Intent(this, UdpSendService.class);
		bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
	}
	
	private void disconnect() {
		if (tcpNetworkTask != null) {
			tcpNetworkTask.closeSocket();
			tcpNetworkTask.cancel(false);
			tcpNetworkTask = null;
		}
		if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
	}
	
	private void sendTcpData(String data) {
		if(tcpNetworkTask != null) {
			tcpNetworkTask.sendDataToNetwork(data + "\n");
		}
	}
	
	private void sendUdpData(String data) {
		if(mBound) {
			mService.send(data);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		disconnect();
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
	
	private ServiceConnection mConnection = new ServiceConnection() {
		@Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            LocalBinder binder = (LocalBinder) service;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
	};
	
	public class TcpNetworkTask extends AsyncTask<Void, String, Boolean> {
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
					sendTcpData("Hello Arduino");
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
				if (!nsocket.isClosed()) {
					nos.write(cmd.getBytes());
				} else {
					Log.d("Socket",	"SendDataToNetwork: Cannot send message. Socket is closed");
				}
			} catch (Exception e) {
				Log.e("Socket",	"SendDataToNetwork: Message send failed. Caught an exception");
				e.printStackTrace();
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
			closeSocket();
			changeConnectionStatus(false);
		}

		@Override
		protected void onPostExecute(Boolean result) {
			if (result) {
				Log.d("Socket", "onPostExecute: Completed with an Error.");
			} else {
				Log.d("Socket", "onPostExecute: Completed.");
			}
			closeSocket();
			changeConnectionStatus(false);
		}
	}

}
