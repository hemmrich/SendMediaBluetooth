package com.example.cbluetoothtest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.UUID;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener, OnItemClickListener {
	
	private BluetoothAdapter btAdapter;
	private BluetoothSocket bSocket;
	private Button b1;
	private TextView tv1, tv2;
	private ListView lv1;
	private Spinner spinner;
	private ArrayAdapter<BluetoothDevice> adapter;
	private ArrayAdapter<String> spinnerAdapter;
	private String[] spinnerItems;
	private UUID uuid;
	private OutputStream btOstream;
	private boolean receiverRegistered;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bSocket = null;
        receiverRegistered = false;
        
        b1 = (Button) findViewById(R.id.button1);
        tv1 = (TextView) findViewById(R.id.textView1);
        tv2 = (TextView) findViewById(R.id.textView2);
        lv1 = (ListView) findViewById(R.id.listView1);
        spinner = (Spinner) findViewById(R.id.spinner1);
        	
        Field[] drawables = com.example.cbluetoothtest.R.drawable.class.getFields();
        spinnerItems = new String[drawables.length];
        int counter = 0;
        for(Field f : drawables) {
        	spinnerItems[counter++] = f.getName();
        }
        spinnerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, spinnerItems);
        spinner.setAdapter(spinnerAdapter);
        spinner.setSelection(0);
        
        
        adapter = new ArrayAdapter<BluetoothDevice>(this, android.R.layout.simple_list_item_1);
        uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
        
        b1.setOnClickListener(this);
        lv1.setOnItemClickListener(this);
        lv1.setAdapter(adapter);
    }

	@Override
	public void onClick(View v) {
		Log.e("BT", "onClick");
		btAdapter = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
		btAdapter.enable();
		Log.e("BT", "adapter enabled");
		
		adapter.clear();
		adapter.notifyDataSetChanged();
		btAdapter.startDiscovery();
		BroadcastReceiver btReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				if(BluetoothDevice.ACTION_FOUND.equals(action)) {
					BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
					if(device.getBluetoothClass().getDeviceClass() == BluetoothClass.Device.COMPUTER_DESKTOP ||
					   device.getBluetoothClass().getDeviceClass() == BluetoothClass.Device.COMPUTER_LAPTOP) {
						Log.e("BT", "Discovered computer: " + device.getName());
						adapter.insert(device, adapter.getCount());
						adapter.notifyDataSetChanged();
					}
				}				
			}			
		};
		if(!receiverRegistered) {
			IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
			registerReceiver(btReceiver, filter);
			receiverRegistered = true;
		}
		
	}

	@SuppressLint("NewApi")
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		// TODO Auto-generated method stub
		//BluetoothDevice device = btAdapter.getRemoteDevice("58:B0:35:84:E5:68"); //work mac
		//BluetoothDevice device = btAdapter.getRemoteDevice("B8:E8:56:38:50:3F"); //my mac
		
		btAdapter.cancelDiscovery();
		BluetoothDevice device = (BluetoothDevice) parent.getItemAtPosition(position);
		ProgressDialog progDialog = new ProgressDialog(MainActivity.this);
		
		if(bSocket == null) {
			try {
				bSocket = device.createRfcommSocketToServiceRecord(uuid);
				Log.e("BT", "Created insecure rfcomm socket");
			} catch (IOException e) {
				Log.e("BT", "EXCEPTION IN CREATE SOCKET");
			}
		}
		
		try {
			if(!bSocket.isConnected()) {
				bSocket.connect();
				Log.e("BT", "bSocket connected!");
				tv1.setText("Connected to " + device.getName());
				tv1.invalidate();
			
				InputStream is = bSocket.getInputStream();
				btOstream = bSocket.getOutputStream();
				int data;
				String text = "";
				
				while((data = is.read()) != -1 || data != 126) {
					if(data == -1 || data == 126) {
						break;
					} else {
						char c = (char) data;
						//Log.e("BT", c + "");
						text += c;
					}
				}
			
				Log.e("BT", "Text: " + text);
				tv2.setText(text);
				tv2.invalidate();
			}
			
			Thread.sleep(5);			
			
			Calendar c = Calendar.getInstance();
			long time1 = c.getTimeInMillis();
			
			String resourceName = spinnerItems[(int) spinner.getSelectedItemId()];
			int resId = this.getResources().getIdentifier("com.example.cbluetoothtest:drawable/" + resourceName, null, null);
			Log.e("BT", "resName = " + resourceName + ", resId = " + resId);
			
			TypedValue val = new TypedValue();
			this.getResources().getValue(resId, val, true);
			String fullFilename = val.string.toString();
			Log.e("BT", "Full filename: " + val.string.toString());
			//inform server if media is image or video
			if(fullFilename.endsWith(".mp4")) {
				btOstream.write(255);
				Log.e("BT", "Sent 255");
			} else {
				btOstream.write(0);
				Log.e("BT", "Sent 0");
			}
			
			
			
			
			progDialog = ProgressDialog.show(MainActivity.this, "File Transfer", "Opening media file");			
			InputStream fis = getResources().openRawResource(resId);
			byte[] imgArray = new byte[fis.available()];
			progDialog.setMessage("Converting media file to byte array");
			fis.read(imgArray);			
			
			progDialog.setMessage("Sending media byte array!");
			btOstream.write(intToByteArray(imgArray.length));
			Log.e("BT", "Converted! Sending " + imgArray.length + " bytes now...");			
			btOstream.write(imgArray);
			Log.e("BT", "Sent image byte array!");
			progDialog.dismiss();
			
			c = Calendar.getInstance();
			long time2 = c.getTimeInMillis();
			float duration = (time2 - time1)/1000;
			Log.e("BT", "Duration: " + duration + " seconds");
			
			
		} catch(IOException e) {
			Log.e("BT", "IOException in OutputStream write");
			e.printStackTrace();
		} catch (Exception e) {
			Log.e("BT", "EXCEPTION IN CONNECT");
			Toast.makeText(getApplicationContext(), "socket connection failed", Toast.LENGTH_LONG).show();
			e.printStackTrace();
		} finally {
			progDialog.dismiss();
		}
	}
	
	private byte[] intToByteArray(int i) {
		byte[] bytes = new byte[4];
		bytes[3] = (byte) (i & 0xFF);
		bytes[2] = (byte) ((i >> 8) & 0xFF);
		bytes[1] = (byte) ((i >> 16) & 0xFF);
		bytes[0] = (byte) ((i >> 24) & 0xFF);
		
		//int size = (bytes[3] & 0xFF) + ((bytes[2] & 0xFF) << 8) + ((bytes[1] & 0xFF) << 16) + ((bytes[0] & 0xFF) << 24);
		
		return bytes;
	}
}
