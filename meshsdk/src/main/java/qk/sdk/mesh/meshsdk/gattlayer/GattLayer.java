package qk.sdk.mesh.meshsdk.gattlayer;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Build;

import com.realsil.sdk.core.bluetooth.GlobalGatt;
import com.realsil.sdk.core.logger.ZLogger;
import com.realsil.sdk.core.utility.DataConverter;

import java.util.HashMap;
import java.util.UUID;

import qk.sdk.mesh.meshsdk.backgroundscan.BackgroundScanAutoConnected;


public class GattLayer {
    // Log
    private final static String TAG = "GattLayer";
    private final static boolean D = true;
    private int retry = 0;

    // Gatt Layer Call
    private GattLayerCallback mCallback;
    private GattLayerSendAndControlCallback mGattLayerSendAndControlCallback;

    public void setmGattLayerSendAndControlCallback(GattLayerSendAndControlCallback mGattLayerSendAndControlCallback) {
        this.mGattLayerSendAndControlCallback = mGattLayerSendAndControlCallback;
    }

    // Bluetooth Manager
    private BluetoothGatt mBluetoothGatt;

    // MTU size
    private static int MTU_SIZE_EXPECT = 240;

    // Device info
    private String mBluetoothDeviceAddress;

    // Context
    private Context mContext;

    // Global Gatt
    private GlobalGatt mGlobalGatt;

    public void setIs_OTA_SERVICE_On(boolean is_OTA_SERVICE_On) {
        this.is_OTA_SERVICE_On = is_OTA_SERVICE_On;
    }
    //防止ota服务的回调与这边冲突
    private boolean is_OTA_SERVICE_On = false;

    // UUID
    public final static UUID SERVICE_MESH_PROV_UUID = UUID.fromString("00001827-0000-1000-8000-00805f9b34fb");
    private final static UUID CHAR_MESH_PROV_DATA_OUT_UUID = UUID.fromString("00002adb-0000-1000-8000-00805f9b34fb");
    private final static UUID CHAR_MESH_PROV_DATA_IN_UUID = UUID.fromString("00002adc-0000-1000-8000-00805f9b34fb");

    public final static UUID SERVICE_MESH_PROXY_UUID = UUID.fromString("00001828-0000-1000-8000-00805f9b34fb");
    private final static UUID CHAR_MESH_PROXY_DATA_OUT_UUID = UUID.fromString("00002add-0000-1000-8000-00805f9b34fb");
    private final static UUID CHAR_MESH_PROXY_DATA_IN_UUID = UUID.fromString("00002ade-0000-1000-8000-00805f9b34fb");

    // Characteristic
    private HashMap<String, BluetoothGattCharacteristic> mProvInCharacteristic;
    private HashMap<String, BluetoothGattCharacteristic> mProvOutCharacteristic;
    private HashMap<String, BluetoothGattCharacteristic> mProxyInCharacteristic;
    private HashMap<String, BluetoothGattCharacteristic> mProxyOutCharacteristic;

    public GattLayer(Context context, GattLayerCallback callback) {
        ZLogger.d(D, "initial.");
        mContext = context;
        // register callback
        mCallback = callback;
        // Global Gatt
        mGlobalGatt = GlobalGatt.getInstance();
        mProvInCharacteristic = new HashMap<>();
        mProvOutCharacteristic = new HashMap<>();
        mProxyInCharacteristic = new HashMap<>();
        mProxyOutCharacteristic = new HashMap<>();
    }

    public GattLayer(Context context) {
        ZLogger.d(D, "initial.");
        mContext = context;
        // Global Gatt
        mGlobalGatt = GlobalGatt.getInstance();
        mProvInCharacteristic = new HashMap<>();
        mProvOutCharacteristic = new HashMap<>();
        mProxyInCharacteristic = new HashMap<>();
        mProxyOutCharacteristic = new HashMap<>();
    }

    public boolean ProvOut(byte[] data) {
        if (mProvOutCharacteristic == null) {
            ZLogger.w(D, "CHAR_MESH_PROV_DATA_OUT_UUID not supported");
            return false;
        }
        if (!mGlobalGatt.isConnected(mBluetoothDeviceAddress)) {
            ZLogger.w(D, "disconnected, addr=" + mBluetoothDeviceAddress);
            return false;
        }
        ZLogger.d(D, "-->> " + DataConverter.bytes2HexWithSeparate(data));

        // Send the data
        mProvOutCharacteristic.get(mBluetoothDeviceAddress).setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        mProvOutCharacteristic.get(mBluetoothDeviceAddress).setValue(data);
        return mGlobalGatt.writeCharacteristic(mBluetoothDeviceAddress, mProvOutCharacteristic.get(mBluetoothDeviceAddress));
//		return true;
    }

    public boolean ProxyOut(byte[] data) {
        if (mProxyOutCharacteristic == null) {
            ZLogger.w(D, "CHAR_MESH_PROXY_DATA_OUT_UUID not supported");
            return false;
        }
        if (!mGlobalGatt.isConnected(mBluetoothDeviceAddress)) {
            ZLogger.w(D, "disconnected, addr=" + mBluetoothDeviceAddress);
            return false;
        }
        ZLogger.d(D, "-->> " + DataConverter.bytes2HexWithSeparate(data));
        mProxyOutCharacteristic.get(mBluetoothDeviceAddress).setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        mProxyOutCharacteristic.get(mBluetoothDeviceAddress).setValue(data);
        return mGlobalGatt.writeCharacteristic(mBluetoothDeviceAddress, mProxyOutCharacteristic.get(mBluetoothDeviceAddress));
    }


    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     * @return Return true if the connection is initiated successfully. The connection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public boolean connect(final String address) {
        ZLogger.d(D, "address: " + address);
        mBluetoothDeviceAddress = address;
        return mGlobalGatt.connect(address, mGattCallback);
    }

    public boolean connect(final String address, GattLayerCallback callback) {
        ZLogger.d(D, "address: " + address);
        mBluetoothDeviceAddress = address;
        mCallback = callback;
        return mGlobalGatt.connect(address, mGattCallback);
    }

    /**
     * When the le services manager close, it must disconnect and close the gatt.
     */
    public void close() {
        ZLogger.d(D, "gatt close()");
        try {
            mGlobalGatt.close(mBluetoothDeviceAddress);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public BluetoothDevice getBluetoothDevice(String addr){
        return mGlobalGatt.getBluetoothAdapter().getRemoteDevice(addr);
    }

    public int connectedDeviceSize(){
        return mGlobalGatt.getConnectDevices().size();
    }

    private void changeGattConnect(){
        if (connectedDeviceSize() == 0){
//            mGlobalGatt.closeAll();
//            SPWristbandConfigInfo.setBondedDevice(mContext,null);
            mCallback.onConnectionStateChange(false, false);
        } else {
            mBluetoothDeviceAddress = mGlobalGatt.getConnectDevices().get(0).getAddress();
        }
    }

    private void closeBlueToothCharacteristic(String addr){

    }

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            if (is_OTA_SERVICE_On){
                return;
            }
            ZLogger.d(D, "mtu=" + mtu + ", status=" + status);
            // change the mtu real payloaf size
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mGattLayerSendAndControlCallback.onDataLengthChanged(mtu);
            }

            // Attempts to discover services after successful connection.
            boolean sta = mBluetoothGatt.discoverServices();
            ZLogger.i(D, "Attempting to start service discovery: " + sta);
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (is_OTA_SERVICE_On){
                return;
            }
            if (status == BluetoothGatt.GATT_SUCCESS) {
                retry = 0;
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    mBluetoothGatt = gatt;
                    ZLogger.i(D, "Connected to GATT server.");

                    // only android 5.0 add the requestMTU feature
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {

                        // Attempts to discover services after successful connection.
                        boolean sta = mBluetoothGatt.discoverServices();
                        ZLogger.i(D, "Attempting to start service discovery: " +
                                sta);
                    } else {
                        ZLogger.i(D, "Attempting to request mtu size, expect mtu size is: " + String.valueOf(MTU_SIZE_EXPECT));
                        mBluetoothGatt.requestMtu(MTU_SIZE_EXPECT);
                    }

                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    ZLogger.i(D, "Disconnected from GATT server.");
                    // tell up stack the current connect state
                    changeGattConnect();
                }
            } else {
                ZLogger.e(D, "error: status " + status + " newState: " + newState);
                // try to close gatt
//                close();
                if (retry < 1){
                    retry++;
                    BackgroundScanAutoConnected.getInstance().connectWristbandDevice(mBluetoothDeviceAddress);
                } else {
                    retry = 0;
                    changeGattConnect();
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (is_OTA_SERVICE_On){
                return;
            }
            ZLogger.d(D, "status=" + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // set the characteristic
                // initial the service and characteristic
                BluetoothGattService service = gatt.getService(SERVICE_MESH_PROV_UUID);
                if (service == null) {
                    ZLogger.w(D, "SERVICE_MESH_PROV_UUID not supported");

                    // try to disconnect gatt
                    close();
                    return;
                }
                mProvInCharacteristic.put(mBluetoothDeviceAddress,service.getCharacteristic(CHAR_MESH_PROV_DATA_IN_UUID));
                if (mProvInCharacteristic == null) {
                    ZLogger.w(D, "CHAR_MESH_PROV_DATA_IN_UUID not supported");

                    // try to disconnect gatt
                    close();
                    return;
                }
                mGlobalGatt.setCharacteristicNotification(mBluetoothDeviceAddress, mProvInCharacteristic.get(mBluetoothDeviceAddress), true);

                mProvOutCharacteristic.put(mBluetoothDeviceAddress,service.getCharacteristic(CHAR_MESH_PROV_DATA_OUT_UUID));
                if (mProvOutCharacteristic == null) {
                    ZLogger.w(D, "CHAR_MESH_PROV_DATA_OUT_UUID not supported");

                    // try to disconnect gatt
                    close();
                    return;
                }

                BluetoothGattService service_proxy = gatt.getService(SERVICE_MESH_PROXY_UUID);
                if (service_proxy == null) {
                    ZLogger.w(D, "SERVICE_MESH_PROXY_UUID not supported");

                    // try to disconnect gatt
                    close();
                    return;
                }
                mProxyInCharacteristic.put(mBluetoothDeviceAddress,service_proxy.getCharacteristic(CHAR_MESH_PROXY_DATA_IN_UUID));
                if (mProxyInCharacteristic == null) {
                    ZLogger.w(D, "CHAR_MESH_PROXY_DATA_IN_UUID not supported");

                    // try to disconnect gatt
                    close();
                    return;
                }

                mProxyOutCharacteristic.put(mBluetoothDeviceAddress, service_proxy.getCharacteristic(CHAR_MESH_PROXY_DATA_OUT_UUID));
                if (mProxyOutCharacteristic == null) {
                    ZLogger.w(D, "CHAR_MESH_PROXY_DATA_OUT_UUID not supported");

                    // try to disconnect gatt
                    close();
                    return;
                }

                // enable notification
                // tell up stack the current connect state
//                mCallback.onConnectionStateChange(true, true);
            } else {
                // try to disconnect gatt
                close();
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            String srcAddr = gatt.getDevice().getAddress();
            ZLogger.i("<<-- get message from "+ srcAddr);
            if (!mBluetoothDeviceAddress.equals(srcAddr)){
                ZLogger.i("<<-- get message from delay, jump this message ");
                return;
            }
            byte[] data = characteristic.getValue();
            ZLogger.d(D, "<<-- olength: " + characteristic.getValue().length
                    + ", data: " + DataConverter.bytes2HexWithSeparate(data));
            if (CHAR_MESH_PROV_DATA_IN_UUID.equals(characteristic.getUuid())) {
                // tell up stack a data receive
                mGattLayerSendAndControlCallback.onProvIn(data);
            } else if (CHAR_MESH_PROXY_DATA_IN_UUID.equals(characteristic.getUuid())) {
                mGattLayerSendAndControlCallback.onProxyIn(data);
            }

        }

        @Override
        public void onCharacteristicWrite(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final int status) {
            String srcAddr = gatt.getDevice().getAddress();
            ZLogger.i("<<-- get message from "+ srcAddr);
            if (!mBluetoothDeviceAddress.equals(srcAddr)){
                ZLogger.i("<<-- get message from delay, jump this message ");
                return;
            }
            if (CHAR_MESH_PROV_DATA_OUT_UUID.equals(characteristic.getUuid())) {
                mGattLayerSendAndControlCallback.onProvOut(status == BluetoothGatt.GATT_SUCCESS);
            } else if (CHAR_MESH_PROXY_DATA_OUT_UUID.equals(characteristic.getUuid())) {
                mGattLayerSendAndControlCallback.onProxyOut(status == BluetoothGatt.GATT_SUCCESS);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            ZLogger.d(D, "<<<--- status: " + status + " value: " + DataConverter.bytes2Hex(characteristic.getValue()));
            String name = characteristic.getStringValue(0);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // tell up stack data send right
                mCallback.onNameReceive(name);
            }

        }

        @Override
        public void onDescriptorWrite(final BluetoothGatt gatt, final BluetoothGattDescriptor descriptor, final int status) {
            if (is_OTA_SERVICE_On){
                return;
            }
            if (status == BluetoothGatt.GATT_SUCCESS) {

                boolean enabled = descriptor.getValue()[0] == 1;
                if (enabled) {
                    if (descriptor.getCharacteristic().getUuid().equals(mProvInCharacteristic.get(mBluetoothDeviceAddress).getUuid())) {
                        ZLogger.d(D, "Prov Notification enabled");
                        mGlobalGatt.setCharacteristicNotification(mBluetoothDeviceAddress, mProxyInCharacteristic.get(mBluetoothDeviceAddress), true);
                    } else if (descriptor.getCharacteristic().getUuid().equals(mProxyInCharacteristic.get(mBluetoothDeviceAddress).getUuid())) {
                        ZLogger.d(D, "Proxy Notification enabled");
                        mCallback.onConnectionStateChange(true, true);
                    }

                } else {
                    ZLogger.e(D, "Notification  not enabled!!!");
                    close();
                }

            } else {
                ZLogger.e(D, "Descriptor write error: " + status);
                close();
            }
        }

    };
}
