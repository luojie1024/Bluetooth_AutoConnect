package com.ypx.bluetooth_hc05.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.avos.avoscloud.AVException;
import com.avos.avoscloud.AVOSCloud;
import com.avos.avoscloud.AVObject;
import com.avos.avoscloud.SaveCallback;
import com.ypx.bluetooth_hc05.R;
import com.ypx.bluetooth_hc05.utils.ClsUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

//import android.view.Menu;            //如使用菜单加入此三包
//import android.view.MenuInflater;
//import android.view.MenuItem;

public class BTClientActivity extends Activity {
    public boolean flag=true;
    public String key="";
    private final static int REQUEST_CONNECT_DEVICE = 1;    //宏定义查询设备句柄
    private final static String MY_UUID = "00001101-0000-1000-8000-00805F9B34FB";   //SPP服务UUID号
    private InputStream is;    //输入流，用来接收蓝牙数据
    //private TextView text0;    //提示栏解句柄
    private EditText edit0;    //发送数据输入句柄
    private TextView dis;       //接收数据显示句柄
    private ScrollView sv;      //翻页句柄
    private String smsg = "";    //显示用数据缓存
    private String fmsg = "";    //保存用数据缓存
    String message ="";
    int nn=1;
    //-------
    private final String	DEBUG_TAG	= "BTClientActivity";
    private TextView	mTextView = null;
    private EditText	mEditText = null;
    private Button		mButton = null;
    //---------
    public String filename=""; //用来保存存储的文件名
    BluetoothDevice _device = null;     //蓝牙设备
    BluetoothSocket _socket = null;      //蓝牙通信socket
    boolean _discoveryFinished = false;
    boolean bRun = true;
    boolean bThread = false;
    private BluetoothAdapter _bluetooth = BluetoothAdapter.getDefaultAdapter();    //获取本地蓝牙适配器，即蓝牙设备

    //蓝牙权限
    private int MY_PERMISSION_REQUEST_CONSTANT=1;
    //广播action
    private String ACTION_UPDATEUI ="com.hnulab.sharebike.update";
    //设备mac地址key
    public static String EXTRA_DEVICE_ADDRESS = "address";
    //广播接收者
    BroadcastReceiver broadcastReceiver;


    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);   //设置画面为主画面 main.xml

        //蓝牙权限问题
//        if (Build.VERSION.SDK_INT >= 6.0) {
//            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
//                 MY_PERMISSION_REQUEST_CONSTANT);
//        }

      initView();
        AVOSCloud.initialize(this,"x8kmjWgyJYv3KafamSgebLsy-gzGzoHsz", "Ay6CdkxOdmgTu7qrClOtUtnh");
        //mEditText = (EditText)findViewById(R.id.EditTexts);
        //如果打开本地蓝牙设备不成功，提示信息，结束程序

        initbroadcast();
        if (_bluetooth == null){
            Toast.makeText(this, "无法打开手机蓝牙，请确认手机是否有蓝牙功能！", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        // 设置设备可以被搜索
        new Thread(){
            public void run(){
                if(_bluetooth.isEnabled()==false){
                    _bluetooth.enable();
                }
            }
        }.start();
    }

    //初始化视图
    private void initView() {
        sv = (ScrollView)findViewById(R.id.ScrollView01);  //得到翻页句柄
        dis = (TextView) findViewById(R.id.in);      //得到数据显示句柄
        mTextView = (TextView)findViewById(R.id.editText3);
        mButton=(Button)findViewById(R.id.btn_connect);
    }

    //注册广播
    private void initbroadcast() {
        // 动态注册广播
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_UPDATEUI);
        broadcastReceiver = new UpdateUIBroadcastReceiver();
        registerReceiver(broadcastReceiver, filter);
    }


    //发送按键响应
    public void onSendButtonClicked(View v){
        int i=0;
        int n=0;
        try{
            OutputStream os = _socket.getOutputStream();   //蓝牙连接输出流
            byte[] bos = edit0.getText().toString().getBytes();
            for(i=0;i<bos.length;i++){
                if(bos[i]==0x0a)
                    n++;
            }
            byte[] bos_new = new byte[bos.length+n];
            n=0;
            for(i=0;i<bos.length;i++){                  //手机中换行为0a,将其改为0d 0a后再发送
                if(bos[i]==0x0a){
                    bos_new[n]=0x0d;
                    n++;
                    bos_new[n]=0x0a;
                }else{
                    bos_new[n]=bos[i];
                }
                n++;
            }
            os.write(bos_new);
        }catch(IOException e){
        }
    }


    //接收活动结果，响应startActivityForResult()
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode){
            case REQUEST_CONNECT_DEVICE:     //连接结果，由DeviceListActivity设置返回
                // 响应返回结果
                if (resultCode == Activity.RESULT_OK) {   //连接成功，由DeviceListActivity设置返回
                    // MAC地址，由DeviceListActivity设置返回
                    String address = data.getExtras()
                            .getString(EXTRA_DEVICE_ADDRESS);
                    // 得到蓝牙设备句柄
                    _device = _bluetooth.getRemoteDevice(address);

                    // 用服务号得到socket
                    try{
                        _socket = _device.createRfcommSocketToServiceRecord(UUID.fromString(MY_UUID));
                    }catch(IOException e){
                        Toast.makeText(this, "连接失败！", Toast.LENGTH_SHORT).show();
                    }
                    try{
                        _socket.connect();
                        Toast.makeText(this, "连接"+_device.getName()+"成功！", Toast.LENGTH_SHORT).show();
                        mButton.setText("断开");
                    }catch(IOException e){
                        try{
                            Toast.makeText(this, "连接失败！", Toast.LENGTH_SHORT).show();
                            _socket.close();
                            _socket = null;
                        }catch(IOException ee){
                            Toast.makeText(this, "连接失败！", Toast.LENGTH_SHORT).show();
                        }
                        return;
                    }
                    //打开接收线程
                    try{
                        is = _socket.getInputStream();   //得到蓝牙数据输入流
                    }catch(IOException e){
                        Toast.makeText(this, "接收数据失败！", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if(bThread==false){
                        ReadThread.start();
                        bThread=true;
                    }else{
                        bRun = true;
                    }
                }
                break;
            default:break;
        }
    }
    //接收数据线程
    Thread ReadThread=new Thread(){

        public void run(){
            int num = 0;
            byte[] buffer = new byte[1024];
            byte[] buffer_new = new byte[1024];
            int i = 0;
            int n = 0;
            bRun = true;
            String message1="";
            //接收线程
            while(true){
                try{
                        num = is.read(buffer);         //读入数据
                        n=0;
                        String s0 = new String(buffer,0,num);
                        fmsg+=s0;    //保存收到数据
                        for(i=0;i<num;i++){
                            if((buffer[i] == 0x0d)&&(buffer[i+1]==0x0a)){
                                buffer_new[n] = 0x0a;
                                i++;
                            }else{
                                buffer_new[n] = buffer[i];
                            }
                            n++;
                        }
                        String s = new String(buffer_new,0,n);
                        smsg+=s;   //写入接收缓存

                   if(flag){
                        key=s;
                        flag=!flag;
                    }else{
                        key+=s;
                        AVObject testObject = new AVObject("TestObject");
                        testObject.put("words", key);
                        testObject.saveInBackground(new SaveCallback() {
                            @Override
                            public void done(AVException e) {
                                if(e == null){
                                    Log.d("saved","success!");
                                }
                            }
                        });
                        key="";
                       flag=!flag;
                    }
                    message=s;
                    nn=smsg.length();
                    //发送显示消息，进行显示刷新
                    handler.sendMessage(handler.obtainMessage());
                }catch(IOException e){
                }
            }
        }
    };
    //消息处理队列
    Handler handler= new Handler(){
        public void handleMessage(Message msg){
            super.handleMessage(msg);
            dis.setText(smsg);   //显示数据
          //  Log.d("000",smsg);
            sv.scrollTo(0,dis.getMeasuredHeight()); //跳至数据最后一页
        }
    };

    //关闭程序掉用处理部分
    public void onDestroy(){
        super.onDestroy();
        if(_socket!=null)  //关闭连接socket
            try{
                _socket.close();
            }catch(IOException e){}
        //	_bluetooth.disable();  //关闭蓝牙服务

        // 注销广播
        unregisterReceiver(broadcastReceiver);
    }

    //连接按键响应函数
    public void onConnectButtonClicked(View v){
        if (_bluetooth.isEnabled() == false) {  //如果蓝牙服务不可用则提示
            Toast.makeText(this, " 打开蓝牙中...", Toast.LENGTH_LONG).show();
            _bluetooth.enable();
            return;
        }
        if(_socket==null){
            _bluetooth.startDiscovery();
        }
        else{
            //关闭连接socket
            try{
                is.close();
                _socket.close();
                _socket = null;
                bRun = false;
                mButton.setText("连接");
                ClsUtils.removeBond(_device);
            }catch(IOException e){} catch (Exception e) {
                e.printStackTrace();
            }
        }
        return;
    }





    //保存按键响应函数
    public void onSaveButtonClicked(View v){
        Save();
    }
    //清除按键响应函数
    public void onClearButtonClicked(View v){
        smsg="";
        fmsg="";
        dis.setText(smsg);
        return;
    }
    //退出按键响应函数
    public void onQuitButtonClicked(View v){
        finish();
    }

    //保存功能实现
    private void Save() {
        //显示对话框输入文件名
        LayoutInflater factory = LayoutInflater.from(BTClientActivity.this);  //图层模板生成器句柄
        final View DialogView =  factory.inflate(R.layout.sname, null);  //用sname.xml模板生成视图模板
        new AlertDialog.Builder(BTClientActivity.this)
                .setTitle("文件名")
                .setView(DialogView)   //设置视图模板
                .setPositiveButton("确定",
                        new DialogInterface.OnClickListener() //确定按键响应函数
                        {
                            public void onClick(DialogInterface dialog, int whichButton){
                                EditText text1 = (EditText)DialogView.findViewById(R.id.sname);  //得到文件名输入框句柄
                                filename = text1.getText().toString();  //得到文件名
                                try{
                                    if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){  //如果SD卡已准备好

                                        filename =filename+".txt";   //在文件名末尾加上.txt
                                        File sdCardDir = Environment.getExternalStorageDirectory();  //得到SD卡根目录
                                        File BuildDir = new File(sdCardDir, "/data");   //打开data目录，如不存在则生成
                                        if(BuildDir.exists()==false)BuildDir.mkdirs();
                                        File saveFile =new File(BuildDir, filename);  //新建文件句柄，如已存在仍新建文档
                                        FileOutputStream stream = new FileOutputStream(saveFile);  //打开文件输入流
                                        stream.write(fmsg.getBytes());
                                        stream.close();
                                        Toast.makeText(BTClientActivity.this, "存储成功！", Toast.LENGTH_SHORT).show();
                                    }else{
                                        Toast.makeText(BTClientActivity.this, "没有存储卡！", Toast.LENGTH_LONG).show();
                                    }
                                }catch(IOException e){
                                    return;
                                }
                            }
                        })
                .setNegativeButton("取消",   //取消按键响应函数,直接退出对话框不做任何处理
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        }).show();  //显示对话框
    }


    /**
     * 定义广播接收器（内部类）
     */
    private class UpdateUIBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
                // 响应返回结果
                // MAC地址，由DeviceListActivity设置返回
                String address = intent.getStringExtra(EXTRA_DEVICE_ADDRESS);
                // 得到蓝牙设备句柄
                _device = _bluetooth.getRemoteDevice(address);

                // 用服务号得到socket
                try{
                    _socket = _device.createRfcommSocketToServiceRecord(UUID.fromString(MY_UUID));
                }catch(IOException e){
                    Toast.makeText(context, "连接失败！", Toast.LENGTH_SHORT).show();
                }
                try{
                    _socket.connect();
                    Toast.makeText(context, "连接"+_device.getName()+"成功！", Toast.LENGTH_SHORT).show();
                    mButton.setText("断开");
                }catch(IOException e){
                    try{
                        Toast.makeText(context, "连接失败！", Toast.LENGTH_SHORT).show();
                        _socket.close();
                        _socket = null;
                    }catch(IOException ee){
                        Toast.makeText(context, "连接失败！", Toast.LENGTH_SHORT).show();
                    }
                    return;
                }
                //打开接收线程
                try{
                    is = _socket.getInputStream();   //得到蓝牙数据输入流
                }catch(IOException e){
                    Toast.makeText(context, "接收数据失败！", Toast.LENGTH_SHORT).show();
                    return;
                }
                if(bThread==false){
                    ReadThread.start();
                    bThread=true;
                }else{
                    bRun = true;
                }
        }

    }
}
