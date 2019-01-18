package aliskm.tech.remote_control_arduino_android_hc05_bluetooth;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;


public class ledControl extends AppCompatActivity {

    //SPP UUID. Look for it
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    Button btnOn, btnOff, btnDis, btnSendMessage;
    SeekBar speedBar;
    TextView lumn, tvMessage;
    EditText etMessage;
    String address = null;
    BluetoothAdapter myBluetooth = null;
    BluetoothSocket btSocket = null;
    private ProgressDialog progress;
    private boolean isBtConnected = false;
    private int deviceStatus = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent newint = getIntent();
        address = newint.getStringExtra(DeviceList.EXTRA_ADDRESS); //receive the address of the bluetooth device

        //view of the ledControl
        setContentView(R.layout.activity_led_control);

        //call the widgtes
        btnOn = (Button) findViewById(R.id.button2);
        btnSendMessage = (Button) findViewById(R.id.sendBtn);
        etMessage = (EditText) findViewById(R.id.etMessage);
        btnOff = (Button) findViewById(R.id.button3);
        btnDis = (Button) findViewById(R.id.button4);
        speedBar = (SeekBar) findViewById(R.id.seekBar);
        lumn = (TextView) findViewById(R.id.lumn);
        tvMessage = (TextView) findViewById(R.id.messageTv);

        new ConnectBT().execute(); //Call the class to connect


        // firebase database
        // Write a message to the database
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        final DatabaseReference myRef = database.getReference("sensetion-arduino");


        // read value
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {

                    HashMap<String, Object> value = (HashMap<String, Object>) dataSnapshot.getValue();
                    assert value != null;

                    String message = "" + value.get("message");
                    tvMessage.setText(message);



                    if (btSocket != null) {
                        try {
                            deviceStatus = Integer.valueOf( Objects.requireNonNull(value.get("speed")).toString());
                            speedBar.setProgress(deviceStatus);
                            lumn.setText(String.valueOf(deviceStatus));
                        } catch (Exception e) {
                            msg("Error");
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        btnSendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myRef.child("message").setValue("" + etMessage.getText().toString());
                etMessage.setText("");
            }
        });

        //commands to be sent to bluetooth
        btnOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myRef.child("speed").setValue(50);
            }
        });

        btnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myRef.child("speed").setValue(0);
            }
        });

        btnDis.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Disconnect(); //close connection
            }
        });

        speedBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                lumn.setText(String.valueOf(seekBar.getProgress()));
                try {
                    deviceStatus = seekBar.getProgress();
                    btSocket.getOutputStream().write(String.valueOf(seekBar.getProgress()).getBytes());
                    myRef.child("speed").setValue(seekBar.getProgress());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }


    private void Disconnect() {
        if (btSocket != null) //If the btSocket is busy
        {
            try {
                btSocket.close(); //close connection
            } catch (IOException e) {
                msg("Error");
            }
        }
        finish(); //return to the first layout
    }

    // fast way to call Toast
    private void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_led_control, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private class ConnectBT extends AsyncTask<Void, Void, Void> { // UI thread
        private boolean ConnectSuccess = true; //if it's here, it's almost connected

        @Override
        protected void onPreExecute() {
            progress = ProgressDialog.show(ledControl.this, "Connecting...", "Please wait!!!");  //show a progress dialog
        }

        @Override
        protected Void doInBackground(Void... devices) //while the progress dialog is shown, the connection is done in background
        {
            try {
                if (btSocket == null || !isBtConnected) {
                    myBluetooth = BluetoothAdapter.getDefaultAdapter();//get the mobile bluetooth device
                    BluetoothDevice dispositivo = myBluetooth.getRemoteDevice(address);//connects to the device's address and checks if it's available
                    btSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);//create a RFCOMM (SPP) connection
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    btSocket.connect();//start connection
                }
            } catch (IOException e) {
                ConnectSuccess = false;//if the try failed, you can check the exception here
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
        {
            super.onPostExecute(result);

            if (!ConnectSuccess) {
                msg("Connection Failed. Is it a SPP Bluetooth? Try again.");
                finish();
            } else {
                msg("Connected.");
                isBtConnected = true;
            }
            progress.dismiss();
        }
    }
}
