package com.capstone.riders;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter bluetoothAdapter;
    private Handler bluetoothHandler;
    private Button stop_btn,hot_btn,ice_btn;
    private TextView degree,blestate;
    private BluetoothSocket bluetoothSocket = null; // 블루투스 소켓
    private OutputStream outputStream = null; // 블루투스에 데이터를 출력하기 위한 출력 스트림
    private InputStream inputStream = null; // 블루투스에 데이터를 입력하기 위한 입력 스트림
    private Thread workerThread = null; // 문자열 수신에 사용되는 쓰레드
    private byte[] readBuffer; // 수신 된 문자열을 저장하기 위한 버퍼
    private byte[] writeBuffer; // 송신할 문자열 저장 버퍼
    private int readBufferPosition, inputAvailable; // 버퍼 내 문자 저장 위치\
    private String is_connected;
    private String[] permissions = new String[]{
            android.Manifest.permission.BLUETOOTH,
            android.Manifest.permission.BLUETOOTH_SCAN,
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_ADMIN,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.ACCESS_FINE_LOCATION

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        stop_btn = findViewById(R.id.stop_btn);
        hot_btn = findViewById(R.id.hot_btn);
        ice_btn = findViewById(R.id.ice_btn);
        degree = findViewById(R.id.currentdgree);
        blestate = findViewById(R.id.blestate);
        stop_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendOrder("stop");
            }

        });
        hot_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendOrder("hot");
            }

        });
        ice_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendOrder("ice");
            }

        });

        Intent Intent = getIntent();
        is_connected = Intent.getStringExtra("is_bt_selected");
        if(Objects.equals(is_connected, "true")){
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            String name = Intent.getStringExtra("bt_name");
            String address = Intent.getStringExtra("bt_address");

            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

            bluetoothHandler = new Handler(Looper.getMainLooper()) {
                public void handleMessage(@NonNull Message msg) {
                    Log.i("this", "Main Handler - Handle Message");
                    super.handleMessage(msg);

                    Bundle bundle = msg.getData();
                    String str = bundle.getString("degree");


                    UpdateTextView(str);
                }


            };
            stop_btn.setEnabled(false);
            hot_btn.setEnabled(false);
            ice_btn.setEnabled(false);
            deviceConnecting(name, address);
        }
        else{
            stop_btn.setEnabled(false);
            hot_btn.setEnabled(false);
            ice_btn.setEnabled(false);
        }

    }

    public void start_BLE_setting(View v){

        if(Objects.equals(is_connected, "true")){
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setTitle("확인해주세요!");
            alert.setMessage("블루투스 세팅을 변경할까요?\n현재 연결은 종료됩니다.");

            alert.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Log.d("user_dialog", "확인을 눌렀습니다!!");
                    Intent intent = new Intent(getApplicationContext(), BluetoothActivity.class);
                    startActivity(intent);
                    finish();
                }
            });

            alert.setNegativeButton("취소", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    Log.d("user_dialog", "취소를 눌렀습니다!!");
                }
            });

            alert.show();
        }else{
            Intent intent = new Intent(getApplicationContext(), BluetoothActivity.class);
            startActivity(intent);
            finish();
        }

    }

    public void deviceConnecting(String name, String address){
        //아두이노로 디버깅해도 오류나면 아래 변수를 수정해야함
        BluetoothDevice remotedevice = bluetoothAdapter.getRemoteDevice(address);

        //Progress Dialog
        Log.d("process_show!", "블루투스 연결중. 다른 버튼이나 행동 하지 않는걸 권장");
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    permissions,
                    1
            );
            return;
        }

        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); //HC-06 UUID
        //UUID uuid = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66"); //안드 채팅 예제
        // 소켓 생성, 아래코드에서 오류날 수 있음. 이상하게 오류가나는데 이유를 모르겠다 싶으면 빨간글씨들 천천히 읽어보고 구글링해야함..,

        try {
            bluetoothSocket = remotedevice.createRfcommSocketToServiceRecord(uuid);
            //bluetoothSocket = createBluetoothSocket(remotedevice, uuid);


            // RFCOMM 채널을 통한 연결
            bluetoothSocket.connect();

            // 데이터 송수신을 위한 스트림 열기
            outputStream = bluetoothSocket.getOutputStream();
            inputStream = bluetoothSocket.getInputStream();
            Toast.makeText(getApplicationContext(), name + " 연결 완료", Toast.LENGTH_LONG).show();
            blestate.setTextColor(Color.WHITE);
            blestate.setText(name + " Connected");

            Log.d("process_show!", "블루투스 연결완료");

            stop_btn.setEnabled(true);
            hot_btn.setEnabled(true);
            ice_btn.setEnabled(true);
            receive_Data();
        } catch (IOException E) {
            E.printStackTrace();

            Toast.makeText(getApplicationContext(), "블루투스 연결 오류", Toast.LENGTH_SHORT).show();
            Log.d("process_show!", "블루투스 연결 오류. 로그확인");
        }
    }

    public void receive_Data() {
        // 아래 스레드부터가 연결 과정임. 소켓으로 통신해야하고 try-catch 구문을 통해 에러 없으면 try, 있으면 catch로 빠져나가게 됨.
        // 이 이후 코드는 짜지 않았지만, 아두이노와 안드간의 데이터 통신은 발열on / 냉방on 요 버튼 두개에 함수 걸어서 사용하면 될듯함?
        readBuffer = new byte[1024];
        readBufferPosition = 0;
        workerThread = new Thread(new Runnable() {
            public void run() {
                while (!Thread.currentThread().isInterrupted()) {
                    // 아마도 스레드가 끝나기 전까지 무한루프를 돌려야 하고,
                    // 보통 멀티스레드는 권장하지 않는 방식이라니까 아마도 이 스레스 에서 하면 될거야
                    // runOnUiThread - 그냥 메인 스레드? 그런 의미로 생각하믄대 겉에있는건 신경 x 요기가 메인

                    try {
                        //데이터 수신 여부 확인.
                        inputAvailable = inputStream.available();
                        //수신되면 되면 0보다 커짐.
                        if (inputAvailable != 0) {
                            Log.d("process_show!", "일단메시지 받음 : " + inputAvailable);
                            byte[] temp_bytes = new byte[inputAvailable];
                            inputStream.read(temp_bytes);
                            for (int i = 0; i < inputAvailable; i++) {
                                byte tmpbyte = temp_bytes[i];
                                if (tmpbyte == '\n') {
                                    // readBuffer 배열을 encodedBytes로 복사
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    // 인코딩 된 바이트 배열을 문자열로 변환
                                    final String text = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;
                                    Log.d("process_show!", "그건 이거얌 : " + text);
                                    Message msg = bluetoothHandler.obtainMessage();
                                    Bundle bundle = new Bundle();
                                    bundle.putString("degree",text);
                                    msg.setData(bundle);

                                    bluetoothHandler.sendMessage(msg);
                                } // 개행 문자가 아닐 경우
                                else {
                                    readBuffer[readBufferPosition++] = tmpbyte;
                                }
                            }

                        }
                    } catch (IOException e) {

                        e.printStackTrace();
                        Log.d("process_show!_onError", "데이터 수신 중 오류가 발생하였습니다. readBufferPosition : " + readBufferPosition);

                    }


                }
            }
        });

        workerThread.start();

    }
    public void UpdateTextView(String value) {
        value = value + " ºC";
        degree.setText(value);

    }
    public void sendOrder(String value) //OnClick에 지정된 함수를 정의
    {
        writeBuffer = value.getBytes();
        try {
            outputStream.write(writeBuffer);
            Log.d("apple", "지횬바보");
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), "데이터 전송 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show();
            Log.d("grape", "지횬완전바보");
        }
        Log.d("grape", "일단되긴됨");
    }


}