package com.capstone.riders;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BluetoothActivity extends AppCompatActivity implements OnItemClickInterface{
    private BluetoothAdapter bluetoothAdapter;
    private Set<BluetoothDevice> paired_devices;
    private RecyclerView recyclerView;
    private TextView info_textView;
    private Button search_btn,pair_list_btn,close_bt_btn;

    private List<Map<String, String>> r_Device = new ArrayList<Map<String, String>>(); // 검색으로 받은 디바이스 기기 목록

    private String[] permissions = new String[]{
            android.Manifest.permission.BLUETOOTH,
            android.Manifest.permission.BLUETOOTH_SCAN,
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_ADMIN,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.ACCESS_FINE_LOCATION

    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);

        recyclerView = findViewById(R.id.device_list);
        info_textView = findViewById(R.id.hintView);
        search_btn = findViewById(R.id.search_bt);
        pair_list_btn = findViewById(R.id.paired_list_btn);
        close_bt_btn = findViewById(R.id.close_btconnect_btn);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);  // LayoutManager 설정


    }

    public void getPairedList(View v){
        //블루투스 어댑터를 디폴트 어댑터로 설정
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            //기기가 블루투스를 지원하지 않을때
            Log.d("bluetoothAdapter_NULL", "Bluetooth 미지원 기기");
            //처리코드 작성

        } else {
            // 기기가 블루투스를 지원할 때
            Log.d("bluetoothAdapter_can", "Bluetooth 지원 기기");

            if (bluetoothAdapter.isEnabled()) { // 기기의 블루투스 기능이 켜져있을 경우
                Log.d("bluetoothAdapter_ON", "Bluetooth 기능 켜진 상태");
                Call_List(); // 페어링된 기기 목록 가져오기
            } else { // 기기의 블루투스 기능이 꺼져있을 경우 활성화 시키도록 해야한다.
                Log.d("bluetoothAdapter_ON", "Bluetooth 기능 꺼진 상태 - 이동여부 확인 창 노출중");
                AlertDialog.Builder alert = new AlertDialog.Builder(this);
                alert.setTitle("블루투스 설정 경고");
                alert.setMessage("아두이노에 연결하려면\n블루투스를 활성화 시켜야 합니다.\n설정창으로 이동할까요?");

                alert.setPositiveButton("네", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d("user_dialog", "블투 키러 이동");
                        Intent intent = new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
                        startActivity(intent);
                    }
                });

                alert.setNegativeButton("아니요", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        Log.d("user_dialog", "이동 안한답니다.");
                    }
                });

                alert.show();
            }

        }

    }

    public void Call_List() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    permissions,
                    1
            );
            return;
        }
        //페어링 되어있던 기기들 목록 가져옴 - 원하는 기기 이미 있는지 확인할라고
        paired_devices = bluetoothAdapter.getBondedDevices();
        Log.d("Paring_returns", "페어링된 기기 수 : " + paired_devices.size());

        if (paired_devices.size() > 0) {
            info_textView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            ArrayList<Map<String, String>> DataSet = new ArrayList<Map<String, String>>();
            for (BluetoothDevice device : paired_devices) {
                String devide_name = device.getName();
                String devide_HW_Address = device.getAddress();

                Map<String, String> maps = new HashMap<String, String>();
                maps.put("device_name", devide_name);
                maps.put("device_address", devide_HW_Address);
                DataSet.add(maps);

            }

            DeviceAdapter customAdapter = new DeviceAdapter(DataSet, this);
            recyclerView.setAdapter(customAdapter); // 어댑터 설정

            search_btn.setEnabled(true);

        }

    }

    public void startDeviceSearch(View v){
        info_textView.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        Toast.makeText(this, "기기 검색을 시작합니다.", Toast.LENGTH_SHORT).show();
        close_bt_btn.setEnabled(false);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    permissions,
                    1
            );
            return;
        }
        search_btn.setEnabled(false);
        pair_list_btn.setEnabled(false);
        // Register for broadcasts when a device is discovered.

        //브로드캐스터를 위한 리시버 설정

        IntentFilter searchFilter = new IntentFilter();
        searchFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        searchFilter.addAction(BluetoothDevice.ACTION_FOUND);
        searchFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(receiver, searchFilter);

        bluetoothAdapter.startDiscovery();
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case BluetoothAdapter.ACTION_DISCOVERY_STARTED:

                    r_Device.clear();
                    Toast.makeText(getApplicationContext(), "기기 검색을 시작합니다.", Toast.LENGTH_LONG).show();

                    Log.d("search_s", "기기 검색 시작");

                    break;
                case BluetoothDevice.ACTION_FOUND:
                    //검색한 디바이스 객체 가져옴
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    //검색한거 리스트에 넣어줌. 이따가 선택하면 리스트에서 뽑아서 연결해야하니까 미리 넣어두는겨 근데안댐. 오류남 ㅋㅋ
                    //searched_devices.add(intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE));
                    //list map에 넣는다.
                    Map map = new HashMap();
                    map.put("name", device.getName());
                    map.put("address", device.getAddress());
                    r_Device.add(map);

                    Log.d("search_s", "기기 검색 찾아서 데이터 넣음");
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    Toast.makeText(getApplicationContext(), "검색을 종료하였습니다.", Toast.LENGTH_LONG).show();
                    info_textView.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                    pair_list_btn.setEnabled(true);
                    close_bt_btn.setEnabled(true);

                    ArrayList<Map<String, String>> DataSet = new ArrayList<Map<String, String>>();
                    for (Map<String, String> data : r_Device) {
                        String name = data.get("name");
                        if (name == null) {
                            name = "알 수 없는 기기";
                        }
                        //[{address=6C:AC:C2:48:8C:E4, name=현우의 Z Flip5}, {address=72:8A:C3:13:9A:91, name=null}, {address=3C:A3:08:A0:9F:B4, name=YUNMAI-ISM2-KR}, {address=77:CA:4F:9C:69:F8, name=null}]
                        //arraylist 형태의 배열리스트에, map형태로 데이터를 집어넣은것. map은 배열을 key / value 형태로 저장할 수 있는 것을 이야기함.

                        Map<String, String> maps = new HashMap<String, String>();
                        maps.put("device_name", name);
                        maps.put("device_address", data.get("address"));
                        DataSet.add(maps);


                    }
                    Log.d("search_s", "검색 끝");
                    DeviceAdapter customAdapter = new DeviceAdapter(DataSet, BluetoothActivity.this);
                    recyclerView.setAdapter(customAdapter); // 어댑터 설정

                    break;

            }
        }
    };

    @Override
    public void OnItemSelected(View v, int position, String Address, String name) {
        // 원래는 DevicedAdapter 클래스에서 정의해야하는데, 인터페이스를 사용해서 메인액티비티로 끌어왔음.
        // 블루투스는 여기서 이루어져야 하기 때문에...
        Log.d("inMain", position + " 번째 리스트 클릭, 이벤트 실행");
        Log.d("inMain", "address : " + Address); // 기기 하드웨어 주소
        Log.d("inMain", "name : " + name); // 기기 명

        //UUID 아두이노로 되어있으니 아래 함수 사용 !!
        SendMain(name, Address);
        //bluetoothDevice = bluetoothAdapter.getRemoteDevice(address); 이렇게 연결하는게 아닌가?
    }

    public void SendMain(String name, String address){
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);

        intent.putExtra("is_bt_selected","true");
        intent.putExtra("bt_name",name);
        intent.putExtra("bt_address",address);

        startActivity(intent);
        finish();

    }
    public void cancel_connect(View v){
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);

        intent.putExtra("is_bt_selected","false");

        startActivity(intent);
        finish();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Don't forget to unregister the ACTION_FOUND receiver.
        //unregisterReceiver(receiver);
    }



}
