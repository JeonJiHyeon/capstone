package com.capstone.riders;

import android.content.DialogInterface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Map;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.ViewHolder>{
    private ArrayList<Map<String,String>> localDataSet;
    private static OnItemClickInterface evtListener;

    //----- 생성자 --------------------------------------
    // 생성자를 통해서 데이터를 전달받도록 함
    public DeviceAdapter (ArrayList<Map<String,String>> dataSet, OnItemClickInterface listener) {
        localDataSet = dataSet;
        evtListener = listener;
    }
    //--------------------------------------------------
    //===== 뷰홀더 클래스 =====================================================
    public static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView device_address, device_name;
        private LinearLayout obj_device_elem;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            device_address = itemView.findViewById(R.id.device_address);
            device_name = itemView.findViewById(R.id.device_name);

            // 아이템 클릭 이벤트 처리.
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // TODO : process click event.
                    Log.d("items", "저 클릭했어용~!");
                    AlertDialog.Builder alert = new AlertDialog.Builder(ViewHolder.super.itemView.getContext());
                    alert.setTitle("연결");
                    alert.setMessage(device_name.getText()+" 에 연결할까요? ");

                    alert.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Log.d("user_dialog", "확인을 눌렀습니다!!");
                            String address = (String) device_address.getText();
                            String name = (String) device_name.getText();
                            evtListener.OnItemSelected(v, getAdapterPosition(), address, name);
                        }
                    });

                    alert.setNegativeButton("취소", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            Log.d("user_dialog", "취소를 눌렀습니다!!");
                        }
                    });

                    alert.show();
                }
            });

        }
        public TextView get_device_name_txtView() {
            return device_name;
        }
        public TextView get_device_address_txtView() {
            return device_address;
        }
    }
    //========================================================================


    @NonNull
    @Override   // ViewHolder 객체를 생성하여 리턴한다.
    public DeviceAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recyclerview_item, parent, false);
        DeviceAdapter.ViewHolder viewHolder = new DeviceAdapter.ViewHolder(view);

        return viewHolder;
    }

    @Override   // ViewHolder안의 내용을 position에 해당되는 데이터로 교체한다.
    public void onBindViewHolder(@NonNull DeviceAdapter.ViewHolder holder, int position) {
        Map<String, String> map = localDataSet.get(position);
        String name = map.get("device_name");
        String add = map.get("device_address");
        holder.device_name.setText(name);
        holder.device_address.setText(add);
    }

    @Override   // 전체 데이터의 갯수를 리턴한다.
    public int getItemCount() {
        return localDataSet.size();
    }

}
