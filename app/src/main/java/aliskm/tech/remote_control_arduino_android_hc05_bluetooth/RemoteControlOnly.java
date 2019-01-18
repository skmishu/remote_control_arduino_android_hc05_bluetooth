package aliskm.tech.remote_control_arduino_android_hc05_bluetooth;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Objects;

public class RemoteControlOnly extends AppCompatActivity {

    private EditText etB, etM;
    private SeekBar seekBar;
    private TextView tvBLive, tvMLive, tvSpeed;
    private FirebaseDatabase database;
    private DatabaseReference myRef;
    private int deviceStatus = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remote_control_only);

        etB = findViewById(R.id.etB);
        etM = findViewById(R.id.etM);
        seekBar = findViewById(R.id.seekBar);
        tvBLive = findViewById(R.id.tvBLive);
        tvMLive = findViewById(R.id.tvMLive);
        tvSpeed = findViewById(R.id.tvSpeed);

        database = FirebaseDatabase.getInstance();
        myRef = database.getReference("sensetion-arduino");
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {

                    HashMap<String, Object> value = (HashMap<String, Object>) dataSnapshot.getValue();
                    assert value != null;
                    try {
                        if (value.get("speed") != null) {
                            deviceStatus = Integer.valueOf(Objects.requireNonNull(value.get("speed")).toString());
                            seekBar.setProgress(deviceStatus);
                            tvSpeed.setText(String.valueOf(deviceStatus));
                        }
                        if (value.get("live") != null) {
                            tvBLive.setText(value.get("live").toString());
                        }
                        if (value.get("message") != null) {
                            String message = "" + value.get("message");
                            tvMLive.setText(message);
                        }
                    } catch (Exception e) {
                        Toast.makeText(RemoteControlOnly.this, "Error", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                try {
                    deviceStatus = seekBar.getProgress();
                    myRef.child("speed").setValue(seekBar.getProgress());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void sendCommandToCloud(View view) {
        try {
            if (etB.getText().toString().length() > 0) {
                myRef.child("live").setValue(etB.getText().toString());
            } else {
                if (etB.getText().toString().length() < 1)
                    Toast.makeText(RemoteControlOnly.this, "write a command like (21)!", Toast.LENGTH_SHORT).show();

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendMessageToCloud(View view) {
        myRef.child("message").setValue("" + etM.getText().toString());
        etM.setText("");
    }
}
