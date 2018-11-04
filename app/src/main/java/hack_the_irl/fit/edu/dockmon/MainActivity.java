package hack_the_irl.fit.edu.dockmon;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.TextView;


import java.io.InputStream;
import java.util.Scanner;

public class MainActivity extends AppCompatActivity {

    private TextView mTextMessage;

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
    }
    protected void launchBluetoothManager() {
        Intent intent = new Intent(this, BluetoothManager.class);
        startActivity(intent);
    }
}
