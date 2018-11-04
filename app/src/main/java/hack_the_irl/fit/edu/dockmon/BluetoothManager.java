package hack_the_irl.fit.edu.dockmon;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Button;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;

public class BluetoothManager extends Activity
{
    TextView myLabel;
    TextView scrollView;
    EditText myTextbox;
    BluetoothAdapter mBluetoothAdapter;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    OutputStream mmOutputStream;
    InputStream mmInputStream;
    Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition;
    volatile boolean stopWorker;
    StringBuilder stringBuilder = new StringBuilder();

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_manager);
        Toast.makeText(getApplicationContext(),"Got to onCreate", Toast.LENGTH_SHORT).show();
        Button openButton = (Button)findViewById(R.id.open);
        Button sendButton = (Button)findViewById(R.id.send);
        Button closeButton = (Button)findViewById(R.id.close);
        myLabel = (TextView)findViewById(R.id.label);
        scrollView = (TextView) findViewById(R.id.data);
        myTextbox = (EditText)findViewById(R.id.entry);

        //Open Button
        openButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                try
                {
                    findBT();
                    openBT();
                }
                catch (IOException ex) { }
            }
        });

        //Send Button
        sendButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                try
                {
                    sendData();
                }
                catch (IOException ex) { Log.wtf("BTMan79", ex.getMessage());}
            }
        });

        //Close button
        closeButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                try
                {
                    closeBT();
                }
                catch (IOException ex) { }
            }
        });
    }

    void findBT()
    {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null)
        {
            myLabel.setText("No bluetooth adapter available");
        }

        if(!mBluetoothAdapter.isEnabled())
        {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if(pairedDevices.size() > 0)
        {
            for(BluetoothDevice device : pairedDevices)
            {
                if(device.getName().equals("raspberrypi"))
                {
                    mmDevice = device;
                    break;
                }
            }
        }
        myLabel.setText("Bluetooth Device Found");
    }

    void openBT() throws IOException
    {
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //Standard SerialPortService ID
        mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
        Toast.makeText(getApplicationContext(),"Got here", Toast.LENGTH_SHORT).show();
        mmSocket.connect();
        if (mmSocket.isConnected()) {
            Toast.makeText(getApplicationContext(),"Socket Connected", Toast.LENGTH_SHORT).show();
            Log.d("BTMan","Sock connected to " + mmDevice.getAddress());
        } else {
            Toast.makeText(getApplicationContext(),"Socket did not Connect", Toast.LENGTH_SHORT).show();
        }
        mmOutputStream = mmSocket.getOutputStream();

        mmInputStream = mmSocket.getInputStream();

        beginListenForData();

        myLabel.setText("Bluetooth Opened");
    }

    public String convert(byte[] inputStream, int avail) throws IOException {


        //String line = null;
        for (int i = 0; i < avail;i++) {
            char c = (char) inputStream[i];
            if ((int)c == 0x04) {
                Log.d("BTMan","End of transmission");
                final String stringData = stringBuilder.toString();
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        if (!stringData.isEmpty()) {
                            scrollView.setText("Time                   Hum     Temp\r\n" + stringData.replaceAll(",",",   "));
                        }
                    }
                });
                stringBuilder = new StringBuilder();
            } else {
                stringBuilder.append(c);
            }
        }

        return stringBuilder.toString();
    }
    void beginListenForData()
    {

        Log.d("BTMan190", "Listening");
        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        workerThread = new Thread(new Runnable()
        {
            public void run()
            {
                while(!Thread.currentThread().isInterrupted() && !stopWorker)
                {
                    try
                    {
                        int bytesAvailable = mmInputStream.available();

                        if(bytesAvailable > 0)
                        {
                            Log.d("BTMan206", bytesAvailable + " available");
                            byte[] packetBytes = new byte[bytesAvailable];

                            mmInputStream.read(packetBytes);
                            String msg = convert(packetBytes, bytesAvailable);
                            Log.d("BTMan210", msg);

                        }
                    }
                    catch (IOException ex)
                    {
                        Log.wtf("BTMan216",ex.getMessage());
                        stopWorker = true;
                    }
                }
            }
        });

        workerThread.start();
    }

    void sendData() throws IOException
    {
        String msg = myTextbox.getText().toString();
        msg += "\n";
        byte[] sending = new byte[1024];

        System.arraycopy(msg.getBytes(),0,sending,0, msg.getBytes().length);
        mmOutputStream.write(msg.getBytes());
        myLabel.setText("Data Sent");
        Toast.makeText(getApplicationContext(),"Sent :" + msg, Toast.LENGTH_SHORT).show();
        Log.d("BTMan","Sent: " + Arrays.toString(sending));
    }

    void closeBT() throws IOException
    {
        stopWorker = true;
        mmOutputStream.close();
        mmInputStream.close();
        mmSocket.close();
        myLabel.setText("Bluetooth Closed");
    }
}
