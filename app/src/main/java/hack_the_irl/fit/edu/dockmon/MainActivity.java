package hack_the_irl.fit.edu.dockmon;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;



import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private TextView mTextMessage;

    /*BLUETOOTH VARS*/
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
    /*BLUETOOTH VARS*/


    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    mTextMessage.setText(R.string.title_home);

                    InputStream is = getResources().openRawResource(R.raw.data);
                    Scanner reader = new Scanner(is);
                    String data = "";
                    while (reader.hasNextLine()) {
                        data += reader.nextLine() + "\r\n";
                    }
                    mTextMessage.setText(data);

                    return true;
                case R.id.bluetooth_manager:
                    launchBluetoothManager();
                    return true;
                /*
                case R.id.navigation_dashboard:
                    mTextMessage.setText(R.string.title_dashboard);
                    return true;
                    */
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);




        mTextMessage = (TextView) findViewById(R.id.message);
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        try {
            findBT();
            openBT();

        } catch (IOException ioe) {
            Log.d("DocMan","Error opening bluetooth");
        }
    }
    protected void launchBluetoothManager() {
        Intent intent = new Intent(this, BluetoothManager.class);
        startActivity(intent);
    }
    void findBT()
    {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null)
        {
            Log.d("DocMan","No bluetooth adapter available");
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
        Log.d("DocMan","Bluetooth Device Found");
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

        Log.d("DocMan","Bluetooth Opened");
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
                            Log.d("DocMan",stringData);
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
                            //THIS IS WHERE WE GET THE DATA
                            //Object[] data = returnLastDataPoint(msg);
                            //ArrayList<String> sdbijufbisjkub = parseData(msg);
                            //Toast.makeText(getApplicationContext(), "Retreived", Toast.LENGTH_SHORT).show();



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


    void closeBT() throws IOException
    {
        stopWorker = true;
        mmOutputStream.close();
        mmInputStream.close();
        mmSocket.close();
        Log.d("DocMan","Bluetooth Closed");
    }
    ArrayList<String> parseData(String in){
        ArrayList<String> arr = new ArrayList();

        String tempString = "";
        for(int i = 0; i < in.length(); i++){
            char c = in.charAt(i);
            if(!Character.isWhitespace(c)){
                if(in.charAt(i) == ',') {
                    arr.add(tempString);
                    tempString = "";
                } else {
                    tempString += c;
                }
            }
        }
        return arr;
    }
    Object[] returnLastDataPoint(String msg) {
        Object[] objs = new Object[3];

        String[] c = msg.split("\n");
        String f = c[c.length - 2];
        String[] fs = f.split(",");
        boolean err = false;
        // Get Date Marker
        String dateTime = fs[0];//f.substring(0, f.indexOf(','));
        Log.d("BTMan",f);
        objs[0] = dateTime;

        // Get humidity
        String humStr = fs[1];//(f.substring(f.indexOf(',')+1, f.lastIndexOf(',')));
        try {
            float humFlt = Float.parseFloat(humStr);
            objs[1] = humFlt;
        } catch (Exception e) {
            err = true;
        }

        // Get temp
        String tempStr = fs[2];//f.substring(f.lastIndexOf(','));
        try {
            float tempFlt = Float.parseFloat(tempStr);
            objs[2] = tempFlt;
        } catch (Exception e) {
            err = true;
        }
        if (err) {
            //throw new Exception("Data parsing failed.");
        }
        return objs;
    }
}
