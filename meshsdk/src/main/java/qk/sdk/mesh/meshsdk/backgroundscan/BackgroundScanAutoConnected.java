package qk.sdk.mesh.meshsdk.backgroundscan;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import qk.sdk.mesh.meshsdk.gattlayer.GattLayer;
import qk.sdk.mesh.meshsdk.gattlayer.GattLayerCallback;

import static qk.sdk.mesh.meshsdk.gattlayer.GattLayer.SERVICE_MESH_PROV_UUID;
import static qk.sdk.mesh.meshsdk.gattlayer.GattLayer.SERVICE_MESH_PROXY_UUID;


/**
 * Created by Administrator on 2016/5/23.
 * <p>
 * java.lang.NoClassDefFoundError: com.realsil.android.wristbanddemo.backgroundscan.BackgroundScanAutoConnected$4
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class BackgroundScanAutoConnected {
    // Log
    private final static String TAG = "BackgroundScanAutoConnected";
    private final static boolean D = true;

    // State
    public static final int STATE_IDLE = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_DISCONNECTED = 2;
    public static final int STATE_ERROR = 3;
    public static final int STATE_MESH_PROVISION = 4;
    public static final int STATE_MESH_ADD_APPKEY = 5;
    public static final int STATE_MESH_ADD_BIND_LIGHT = 6;
    public static final int STATE_MESH_ADD_BIND_COLOR = 7;
    public static final int STATE_MESH_OK = 8;
    private int mState = STATE_IDLE;

    // Message
    public static final int MSG_STATE_CONNECTED = 0;
    public static final int MSG_STATE_DISCONNECTED = 1;
    public static final int MSG_FIND_BONDED_DEVICE = 2;
    public static final int MSG_MESH_PROVISION = 3;
    public static final int MSG_MESH_ADD_APPKEY = 4;
    public static final int MSG_ERROR = 5;
    public static final int MSG_MESH_ADD_BIND_LIGHT = 6;
    public static final int MSG_MESH_ADD_BIND_COLOR = 7;
    public static final int MSG_MESH_OK = 8;

    private static Context mContext;

    ArrayList<BackgroundScanCallback> mCallbacks;

    private boolean isInLogin = false;

    // instance
    private static BackgroundScanAutoConnected mInstance;

    private BluetoothOnOffStateReceiver mBluetoothOnOffStateReceiver;

    private boolean isConnected = false;

    private String mDeviceAddress;

    // Gatt Layer
    private GattLayer mGattLayer;

    public GattLayer getGattLayerInstance() {
        return mGattLayer;
    }

    public boolean isConnect() {
        ZLogger.d(D, "isConnected: " + isConnected);
        return isConnected;
    }

    private BackgroundScanAutoConnected(Context context) {
        mContext = context;
        mCallbacks = new ArrayList<>();
    }

    private ScannerParams scannerParams;
    private ScannerPresenter mScannerPresenter;
    private ScannerCallback mScannerCallback;
    // Stops scanning after 120 seconds. This is too long
    // Stops scanning after 30 seconds. 30 seconds is enough
    private static final long SCAN_PERIOD = 45000;
    private static final long RE_SCAN_PERIOD = 10000;
    public static final int SCAN_PROXY = 0;
    public static final int SCAN_PROV = 1;
    public static final int SCAN_STOP = 2;
    public static final int SCAN_BROADCAST_FIRST = 3;

    private static final UUID[] serviceProxyUUIDs = {SERVICE_MESH_PROXY_UUID};
    private static final UUID[] serviceProvUUIDs = {SERVICE_MESH_PROV_UUID};

    private Handler mScanHandler = new Handler();

    private int scanType = 0;
    private Map<String, byte[]> meshprovservicedata = new HashMap<>();

//    public static void initial(Context context) {
//        if (mInstance == null) {
//            synchronized (BackgroundScanAutoConnected.class) {
//                if (mInstance == null) {
//                    mInstance = new BackgroundScanAutoConnected(context.getApplicationContext());
//                }
//            }
//        }
//
//        if (mInstance.mGattLayer == null) {
//            mInstance.mGattLayer = new GattLayer(mContext);
//        }
//
//        if (mInstance.scannerParams == null) {
//            mInstance.scannerParams = new ScannerParams(ScannerParams.SCAN_MODE_GATT);
//            mInstance.scannerParams.setScanPeriod(SCAN_PERIOD);
//        }
//
//        if (mInstance.mScannerCallback == null) {
//            mInstance.mScannerCallback = new ScannerCallback() {
//                @Override
//                public void onAutoScanTrigger() {
//                    super.onAutoScanTrigger();
//                }
//
//                @Override
//                public void onScanStateChanged(int i) {
//                    super.onScanStateChanged(i);
//                    boolean scanState = false;
//                    if (i == STATE_DISCOVERY_STARTED) {
//                        scanState = true;
//                    }
//                    for (BackgroundScanCallback callback : mInstance.mCallbacks) {
//                        callback.onLeScanEnable(scanState);
//                    }
//                }
//
//                @Override
//                public void onNewDevice(ExtendedBluetoothDevice extendedBluetoothDevice) {
//                    super.onNewDevice(extendedBluetoothDevice);
//
//                    SpecScanRecord record = extendedBluetoothDevice.specScanRecord;
//                    ZLogger.d(D, record.toString());
//
//                    if (record.getServiceUuids() == null) {
//                        return;
//                    }
//                    if (mInstance.scanType == SCAN_BROADCAST_FIRST) {
//                        final String bondedDeviceAddress = SPWristbandConfigInfo.getBondedDevice(mContext);
//                        if (bondedDeviceAddress != null) {
//                            if (bondedDeviceAddress.equals(extendedBluetoothDevice.device.getAddress())) {
//                                ParcelUuid prov_uuid = new ParcelUuid(SERVICE_MESH_PROXY_UUID);
//                                byte[] netIDFromBroadcast = record.getServiceData(prov_uuid);
//                                if (netIDFromBroadcast == null) {
//                                    MeshDevice device = GlobalGreenDAO.getInstance().getMeshDevice(bondedDeviceAddress);
//                                    device.setIsProvision(false);
//                                    device.setIsLightModelBind(false);
//                                    GlobalGreenDAO.getInstance().updateMeshDevice(device);
//                                    SPWristbandConfigInfo.setBondedDevice(mContext,null);
//                                    return;
//                                }
//                                if (SPWristbandConfigInfo.getNetId(mContext).equals(new String(netIDFromBroadcast, 1, 8))) {
//                                    mInstance.scanType = SCAN_PROXY;
//                                    mInstance.connectWristbandDevice(bondedDeviceAddress);
//                                } else {
//                                    MeshDevice device = GlobalGreenDAO.getInstance().getMeshDevice(bondedDeviceAddress);
//                                    device.setIsProvision(false);
//                                    device.setIsLightModelBind(false);
//                                    GlobalGreenDAO.getInstance().updateMeshDevice(device);
//                                    SPWristbandConfigInfo.setBondedDevice(mContext,null);
//                                }
//                            }
//                            return;
//                        } else {
//                            mInstance.scanType = SCAN_PROXY;
//                        }
//                    }
//
//                    if (mInstance.scanType == SCAN_PROXY) {
//                        ParcelUuid prov_uuid = new ParcelUuid(SERVICE_MESH_PROXY_UUID);
//                        byte[] netIDFromBroadcast = record.getServiceData(prov_uuid);
//                        if (netIDFromBroadcast == null) {
//                            return;
//                        }
//                        if (SPWristbandConfigInfo.getNetId(mContext).equals(new String(netIDFromBroadcast, 1, 8))) {
//                            if (SPWristbandConfigInfo.getScanProvisionedDevice(mContext)) {
//                                //在切换代理界面展示可选设备
//                                for (BackgroundScanCallback callback : mInstance.mCallbacks) {
//                                    callback.onProxyDeviceFind(extendedBluetoothDevice);
//                                }
//                            } else if (GlobalGreenDAO.getInstance().getHomeMeshDevice(SPWristbandConfigInfo.getHomeId(mContext)).size() != 0){
//                                //直接连GATT
//                                mInstance.connectWristbandDevice(extendedBluetoothDevice.device.getAddress());
//                            }
//                        }
//                        return;
//                    }
//
//                    ZLogger.d(D, "UnProvision Device is searched");
//                    ParcelUuid prov_uuid = new ParcelUuid(SERVICE_MESH_PROV_UUID);
//                    byte[] mesh_prov = record.getServiceData(prov_uuid);
//                    if (mesh_prov == null) {
//                        return;
//                    }
//                    String searchedAddress = extendedBluetoothDevice.device.getAddress();
//                    mInstance.meshprovservicedata.put(searchedAddress, mesh_prov);
//
//                    final String addr = SPWristbandConfigInfo.getBondedDevice(mContext);
//
//                    for (BackgroundScanCallback callback : mInstance.mCallbacks) {
//                        callback.onWristbandDeviceFind(extendedBluetoothDevice.device, extendedBluetoothDevice.rssi, extendedBluetoothDevice.getScanRecord());
//                    }
////
////                    if (addr == null || !addr.equals(searchedAddress)) {
////                        return;
////                    }
////
////                    scanLeDevice(false);
////
////                    ZLogger.d(D, "reconnect to disconnected device:"+ searchedAddress);
////                    //通过数据状态判断一下下一步状态
////                    SendMessage(MSG_FIND_BONDED_DEVICE, device, -1, -1);
//                }
//            };
//        }
//
//        if (mInstance.mScannerPresenter == null) {
//            mInstance.mScannerPresenter = new ScannerPresenter(mContext, mInstance.scannerParams, mInstance.mScannerCallback);
//            mInstance.mScannerPresenter.init();
//        }
//
//    }

    public void resetScannerPresenter() {
        mScannerPresenter = new ScannerPresenter(mContext, scannerParams, mScannerCallback);
        mScannerPresenter.init();
    }

//    public void setCoreMeshCallBack() {
//        CoreMesh.getInstance().setCoreMeshCallback(meshCallback);
//    }

    public static BackgroundScanAutoConnected getInstance() {
        return mInstance;
    }


    public void registerCallback(BackgroundScanCallback callback) {
        if (!mCallbacks.contains(callback)) {
            mCallbacks.add(callback);
        }
    }

    public void unregisterCallback(BackgroundScanCallback callback) {
        if (mCallbacks.contains(callback)) {
            mCallbacks.remove(callback);
        }
    }

    public void closeConnect() {
        isInLogin = false;
        mGattLayer.close();
    }

    public void startAutoConnect() {
        String bondedDeviceAddr = mDeviceAddress;
        if (!isConnected) {
            ZLogger.d(D, "startAutoConnect()");

            if (!TextUtils.isEmpty(bondedDeviceAddr)) {
                scanType = SCAN_BROADCAST_FIRST;
                connectWristbandDevice(bondedDeviceAddr);
            } else {
                scanLeDevice(true, scanType);
            }
        } else {
            stopAutoConnect();
        }
    }

    public void connectWristbandDevice(String addr) {
        stopAutoConnect();
        ZLogger.d(D, "connect to " + addr);
        if (scanType == SCAN_BROADCAST_FIRST) {
            scanLeDevice(true, SCAN_BROADCAST_FIRST);
        } else {
            SendMessage(MSG_FIND_BONDED_DEVICE, addr, -1, -1);
        }
    }

    public void stopAutoConnect() {
        ZLogger.d(D, "stopAutoConnect()");
        scanLeDevice(false, scanType);
    }

    public void registerBluetoothOnOffAutoStartBroadcast() {
        ZLogger.d(D, "registerBluetoothOnOffAutoStartBroadcast");
        mBluetoothOnOffStateReceiver = new BluetoothOnOffStateReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        mContext.registerReceiver(mBluetoothOnOffStateReceiver, filter);
    }

    public void unregisterBluetoothOnOffAutoStartBroadcast() {
        ZLogger.d(D, "unregisterBluetoothOnOffAutoStartBroadcast");
        if (mBluetoothOnOffStateReceiver != null) {
            mContext.unregisterReceiver(mBluetoothOnOffStateReceiver);
        }
        mBluetoothOnOffStateReceiver = null;
    }

    // Control le scan

    /**
     * java.lang.SecurityException: Need ACCESS_COARSE_LOCATION or ACCESS_FINE_LOCATION permission to get scan results
     */
    public void scanLeDevice(boolean enable, int scanType) {
        this.scanType = scanType;
        if (scanType == SCAN_PROV) {
            ZLogger.i("ENTER TO SCAN_PROV");
            scannerParams.setServiceUuids(serviceProvUUIDs);
        } else if (scanType == SCAN_PROXY) {
            ZLogger.i("ENTER TO SCAN_PROXY");
            scannerParams.setServiceUuids(serviceProxyUUIDs);
        } else if (scanType == SCAN_BROADCAST_FIRST) {
            ZLogger.i("ENTER TO SCAN_BROADCAST_FIRST");
            scannerParams.setServiceUuids(null);
        }

        mScannerPresenter.scanDevice(enable);

        for (BackgroundScanCallback callback : mCallbacks) {
            callback.onLeScanEnable(enable);
        }
    }

    // Application Layer callback
    private GattLayerCallback mWristbandManagerCallback = new GattLayerCallback() {
        @Override
        public void onConnectionStateChange(final boolean status, final boolean newState) {
            ZLogger.d(D, "status: " + status + ", newState: " + newState);
            // if already connect to the remote device, we can do more things here.
            if (status && newState) {
                SendMessage(MSG_STATE_CONNECTED, null, -1, -1);
            } else {
                SendMessage(MSG_STATE_DISCONNECTED, null, -1, -1);
            }
        }

    };

    /**
     * send message
     *
     * @param msgType Type message type
     * @param obj     object sent with the message set to null if not used
     * @param arg1    parameter sent with the message, set to -1 if not used
     * @param arg2    parameter sent with the message, set to -1 if not used
     **/
    private void SendMessage(int msgType, Object obj, int arg1, int arg2) {
        if (mHandler != null) {
            //	Message msg = new Message();
            Message msg = Message.obtain();
            msg.what = msgType;
            if (arg1 != -1) {
                msg.arg1 = arg1;
            }
            if (arg2 != -1) {
                msg.arg2 = arg2;
            }
            if (null != obj) {
                msg.obj = obj;
            }
            mHandler.sendMessage(msg);
        } else {
            ZLogger.w(D, "handler is null, can't send message");
        }
    }

    // Broadcast to receive BT on/off broadcast
    public class BluetoothOnOffStateReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
                ZLogger.d(D, "ACTION_STATE_CHANGED: state: " + state);
                if (state == BluetoothAdapter.STATE_ON) {
                    // Need wait a while
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    isInLogin = false;
                    startAutoConnect();
                } else if (state == BluetoothAdapter.STATE_TURNING_OFF) {
                    scanLeDevice(false, scanType);
                    for (BackgroundScanCallback callback : mCallbacks) {
                        callback.onLeScanEnable(false);
                    }
                } else if (state == BluetoothAdapter.STATE_OFF) {
                    if (isConnected) {
                        ZLogger.w(D, "May be close bluetooth, but not disconnect, something may be error!");
                        for (BackgroundScanCallback callback : mCallbacks) {
                            callback.onWristbandLoginStateChange(false);
                            callback.onWristbandState(false, STATE_DISCONNECTED);
                        }
                    }
                    isInLogin = false;
                    mGattLayer.close();
                }
            }
        }
    }

//    private void chooseNextHandelMessage() {
//        MeshDevice meshDevice = GlobalGreenDAO.getInstance().getMeshDevice(mDeviceAddress);
//        if (meshDevice.getIsProvision()) {
//            if (meshDevice.getIsAppKeyAdd()) {
//                if (meshDevice.getIsLightModelBind()) {
//                    if (meshDevice.getIsColorModelBind()) {
//                        SendMessage(MSG_MESH_OK, null, -1, -1);
//                    } else {
//                        SendMessage(MSG_MESH_ADD_BIND_COLOR, null, -1, -1);
//                    }
//                } else {
//                    SendMessage(MSG_MESH_ADD_BIND_LIGHT, null, -1, -1);
//                }
//            } else {
//                SendMessage(MSG_MESH_ADD_APPKEY, null, -1, -1);
//            }
//        } else {
//            SendMessage(MSG_MESH_PROVISION, null, -1, -1);
//        }
//    }

    // The Handler that gets information back from test thread
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_STATE_CONNECTED:
                    isConnected = true;
                    isInLogin = false;
//                    SPWristbandConfigInfo.setBondedDevice(mContext, mDeviceAddress);
//                    ZLogger.d(D, "MSG_STATE_CONNECTED, connect");
//                    if (scanType == SCAN_PROV) {
//                        if (!GlobalGreenDAO.getInstance().isMeshDeviceExist(mDeviceAddress)) {
//                            MeshDevice meshDevice = new MeshDevice();
//                            meshDevice.setDeviceAddress(mDeviceAddress);
//                            String name = SPWristbandConfigInfo.getInfoKeyValue(mContext, mDeviceAddress);
//                            if (name == null || name.isEmpty()) {
//                                name = "no name";
//                            }
//                            long homeID = SPWristbandConfigInfo.getHomeId(mContext);
//                            meshDevice.setHomeId(homeID);
//                            meshDevice.setName(name);
//                            meshDevice.setUnicast(GlobalGreenDAO.getInstance().getMaxAddr());
//                            meshDevice.setIsProvision(false);
//                            meshDevice.setIsAppKeyAdd(false);
//                            meshDevice.setIsLightModelBind(false);
//                            meshDevice.setIsColorModelBind(false);
//                            GlobalGreenDAO.getInstance().insertMeshDevice(meshDevice);
//                            SendMessage(MSG_MESH_PROVISION, null, -1, -1);
//                        } else {
//                            chooseNextHandelMessage();
//                        }
//                    } else if (scanType == SCAN_PROXY) {
//                        if (!GlobalGreenDAO.getInstance().isMeshDeviceExist(mDeviceAddress)) {
////                            SPWristbandConfigInfo.setBondedDevice(mContext,null);//防止分享过后的设备如果存储地址下次再重新打开app时就会走到下面重新创建一个meshDeivce了
//                            SendMessage(MSG_MESH_OK, null, -1, -1);
//                        } else {
//                            chooseNextHandelMessage();
//                        }
//                    }
                    break;
//                case MSG_MESH_PROVISION:
//                    mState = STATE_MESH_PROVISION;
//                    ZLogger.d(D, "MSG_MESH_PROVISION");
//                    for (BackgroundScanCallback callback : mCallbacks) {
//                        callback.onWristbandState(true, mState);
//                    }
//                    byte[] data = meshprovservicedata.get(mDeviceAddress);
//                    if (data == null || data.length == 0) {
//                        return;
//                    }
//                    ZLogger.d("MSG_MESH_PROVISION DATA:" + DataConverter.bytes2HexWithSeparate(data));
//                    final byte[] devUUid = new byte[16];
//                    byte[] oob = new byte[2];
//                    System.arraycopy(data, 0, devUUid, 0, 16);
//                    System.arraycopy(data, 16, oob, 0, 2);
//                    final short temp_oob = (short) ((oob[0] & 0xff) << 8 | (oob[1] & 0xff));
//                    new Thread(new Runnable() {
//                        @Override
//                        public void run() {
//                            CoreMesh.getInstance().meshInviteDevice(devUUid, temp_oob);
//                        }
//                    }).start();
//                    break;
//                case MSG_MESH_ADD_APPKEY:
//                    mState = STATE_MESH_ADD_APPKEY;
//                    ZLogger.d(D, "STATE_MESH_ADD_APPKEY");
//                    for (BackgroundScanCallback callback : mCallbacks) {
//                        callback.onWristbandState(true, mState);
//                    }
//
//                    CoreMesh.getInstance().meshBindAppKey(SPWristbandConfigInfo.getProv_AppKey_Index(mContext));
//
//                    break;
//                case MSG_MESH_ADD_BIND_LIGHT:
//                    mState = STATE_MESH_ADD_BIND_LIGHT;
//                    ZLogger.d(D, "STATE_MESH_ADD_BIND_LIGHT");
//                    for (BackgroundScanCallback callback : mCallbacks) {
//                        callback.onWristbandState(true, mState);
//                    }
//                    final int light_model = LIGHT_MODEL;
//
//                    CoreMesh.getInstance().meshBindModelToApp(light_model, SPWristbandConfigInfo.getProv_AppKey_Index(mContext), 0);
//
//                    break;
//                case MSG_MESH_ADD_BIND_COLOR:
//                    mState = STATE_MESH_ADD_BIND_COLOR;
//                    ZLogger.d(D, "STATE_MESH_ADD_BIND_COLOR");
//                    for (BackgroundScanCallback callback : mCallbacks) {
//                        callback.onWristbandState(true, mState);
//                    }
//                    final int color_model = COLOR_MODEL;
//
//                    CoreMesh.getInstance().meshBindModelToApp(color_model, SPWristbandConfigInfo.getProv_AppKey_Index(mContext), 0);
//
//                    break;
//                case MSG_MESH_OK:
//                    mState = STATE_MESH_OK;
//                    ZLogger.d(D, "STATE_MESH_OK");
//                    for (BackgroundScanCallback callback : mCallbacks) {
//                        callback.onWristbandState(true, mState);
//                    }
//                    if (mGattLayer.connectedDeviceSize() > 1) {
//                        closeConnect();
//                    }
//
//                    break;
//                case MSG_STATE_DISCONNECTED:
//                    // do something
//                    isInLogin = false;
//                    mState = STATE_DISCONNECTED;
//                    if (isConnected) {
//                        closeConnect();
//                    } else {
//                        startAutoConnect();
//                    }
//                    isConnected = false;
//                    for (BackgroundScanCallback callback : mCallbacks) {
//                        callback.onWristbandState(false, mState);
//                    }
//                    //mWristbandManager.close();----for test
//                    break;
//                case MSG_ERROR:
//                    isConnected = false;
//                    isInLogin = false;
//                    closeConnect();
//                    ToastUtils.getInstance().showToast(R.string.something_error);
//                    // Need start scan
//                    mState = STATE_ERROR;
//                    for (BackgroundScanCallback callback : mCallbacks) {
//                        callback.onWristbandState(false, mState);
//                    }
//                    break;
//                case MSG_FIND_BONDED_DEVICE:
//                    isInLogin = true;
//                    mState = STATE_CONNECTING;
//                    mDeviceAddress = (String) msg.obj;
//                    ZLogger.d(D, "MSG_FIND_BONDED_DEVICE addr=" + mDeviceAddress);
//                    // update state info
//
//                    new Thread(new Runnable() {
//                        @Override
//                        public void run() {
//                            mGattLayer.connect(mDeviceAddress, mWristbandManagerCallback);
//                        }
//                    }).start();
//                    break;
                default:
                    break;
            }
        }
    };

//    private CoreMeshCallback meshCallback = new CoreMeshCallback() {
//        @Override
//        public void meshChoosePath() {
//            super.meshChoosePath();
//        }
//
//        @Override
//        public void meshProvSuccess(boolean bSucess) {
//            super.meshProvSuccess(bSucess);
//            if (bSucess) {
//                MeshDevice meshDevice = GlobalGreenDAO.getInstance().getMeshDevice(mDeviceAddress);
//                meshDevice.setIsProvision(true);
//                GlobalGreenDAO.getInstance().updateMeshDevice(meshDevice);
//                SendMessage(MSG_MESH_ADD_APPKEY, null, -1, -1);
//            }
//        }
//
//        @Override
//        public void meshResetNodeResult(boolean bSucess) {
//            super.meshResetNodeResult(bSucess);
//            for (BackgroundScanCallback callback : mCallbacks) {
//                callback.onNodeReset(bSucess);
//            }
//        }
//
//        @Override
//        public void meshReportCompoData(List<List<MeshModel>> data) {
//            super.meshReportCompoData(data);
//        }
//
//        @Override
//        public void meshAddApplicationSuccess(boolean bSucess) {
//            super.meshAddApplicationSuccess(bSucess);
//            if (bSucess) {
//                if (mState == STATE_MESH_ADD_BIND_LIGHT) {
//                    return;
//                }
//                MeshDevice meshDevice = GlobalGreenDAO.getInstance().getMeshDevice(mDeviceAddress);
//                meshDevice.setIsAppKeyAdd(true);
//                GlobalGreenDAO.getInstance().updateMeshDevice(meshDevice);
//                SendMessage(MSG_MESH_ADD_BIND_LIGHT, null, -1, -1);
//            }
//        }
//
//        @Override
//        public void meshBindModelToAppSuccess(boolean bSucess) {
//            super.meshBindModelToAppSuccess(bSucess);
//            if (bSucess) {
//                if (mState == STATE_MESH_ADD_BIND_LIGHT) {
//                    MeshDevice meshDevice = GlobalGreenDAO.getInstance().getMeshDevice(mDeviceAddress);
//                    meshDevice.setIsLightModelBind(true);
//                    GlobalGreenDAO.getInstance().updateMeshDevice(meshDevice);
//                    SendMessage(MSG_MESH_ADD_BIND_COLOR, null, -1, -1);
//                } else if (mState == STATE_MESH_ADD_BIND_COLOR) {
//                    MeshDevice meshDevice = GlobalGreenDAO.getInstance().getMeshDevice(mDeviceAddress);
//                    meshDevice.setIsColorModelBind(true);
//                    GlobalGreenDAO.getInstance().updateMeshDevice(meshDevice);
//                    SendMessage(MSG_MESH_OK, null, -1, -1);
//                }
//            }
//        }
//
//        @Override
//        public void meshBindModelToGroupSuccess(boolean bSucess) {
//            super.meshBindModelToGroupSuccess(bSucess);
//            if (bSucess) {
//
//            }
//        }
//    };

    public static class BackgroundScanCallback {
        public void onWristbandDeviceFind(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {

        }

        public void onLeScanEnable(boolean enable) {

        }

        public void onWristbandLoginStateChange(boolean connected) {

        }

        public void onWristbandState(boolean connected, final int state) {

        }

        public void onProxyDeviceFind(final ExtendedBluetoothDevice device) {

        }

        public void onNodeReset(final boolean status) {

        }
    }
}
