package qk.sdk.mesh.meshsdk;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.Context;

import com.realsil.sdk.core.bluetooth.profile.BluetoothInputDeviceManager;
import com.realsil.sdk.core.bluetooth.scanner.ExtendedBluetoothDevice;
import com.realsil.sdk.core.bluetooth.scanner.ScannerCallback;
import com.realsil.sdk.core.bluetooth.scanner.ScannerParams;
import com.realsil.sdk.core.bluetooth.scanner.ScannerPresenter;
import com.realsil.sdk.core.logger.ZLogger;
import com.realsil.sdk.dfu.image.SubFileIndicator;
import com.realsil.sdk.dfu.model.BinInfo;
import com.realsil.sdk.dfu.model.DfuConfig;
import com.realsil.sdk.dfu.model.FileTypeInfo;
import com.realsil.sdk.dfu.utils.DfuHelper;

import java.util.List;

public class MXDfuUtil {
    private static BluetoothAdapter mBtAdapter;
    private static ScannerPresenter mScannerPresenter;
    private static String mAddress;
    private static boolean isOtaRunning = false;
    protected static DfuHelper mDfuHelper = null;
    protected static DfuConfig mDfuConfig = null;
    protected static BluetoothDevice mSelectedDevice;
    private static ScannerCallback mScannerCallback = new ScannerCallback() {
        @Override
        public void onNewDevice(final ExtendedBluetoothDevice device) {
            super.onNewDevice(device);

            if (device != null) {
                BluetoothDevice btDevice = device.getDevice();
                if (btDevice != null && btDevice.getAddress().equals(mAddress)) {
                    if (!isOtaRunning) {
                        connectRemoteDevice(btDevice, false);
                    }
                    notifyScanLock();
                }
            }
        }

        @Override
        public void onScanStateChanged(int state) {
            super.onScanStateChanged(state);
            if (!mScannerPresenter.isScanning()) {
                notifyScanLock();
            }
        }
    };

    private static Object mScanLock = new Object();

    private static void notifyScanLock() {
        synchronized (mScanLock) {
            try {
                mScanLock.notifyAll();
            } catch (Exception e) {
                ZLogger.e(e.toString());
            }
        }
    }

    public static Boolean initialize(Context context, String address) {
        mAddress = address;
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBtAdapter == null) {
            return false;
        }

        ScannerParams scannerParams = new ScannerParams(ScannerParams.SCAN_MODE_GATT);
        mScannerPresenter = new ScannerPresenter(context, scannerParams, mScannerCallback);
        mScannerPresenter.init();
        return true;
    }

    public static boolean checkFileContent(DfuHelper dfuHelper, DfuConfig dfuConfig, BinInfo binInfo) {
        final List<FileTypeInfo> fileContentTypeInfos = dfuHelper.getSupportedFileContents(binInfo);
        if (fileContentTypeInfos == null || fileContentTypeInfos.size() <= 0) {
            dfuConfig.setFileIndicator(SubFileIndicator.INDICATOR_FULL);
            return true;
        }

        if (fileContentTypeInfos.size() == 1) {
            FileTypeInfo fileTypeInfo = fileContentTypeInfos.get(0);
            dfuConfig.setFileIndicator((1 << fileTypeInfo.getBitNumber()));
            return true;
        }

        return false;
    }

    protected static void connectRemoteDevice(BluetoothDevice bluetoothDevice, boolean isHid) {
        mSelectedDevice = bluetoothDevice;
//        if (SettingsHelper.getInstance().isHidAutoPairEnabled()) {//todo
        BluetoothClass bluetoothClass = bluetoothDevice.getBluetoothClass();
        isHid = BluetoothInputDeviceManager.getInstance().isHidDevice(mSelectedDevice.getAddress());
//        } else {
//            isHid = false;
//        }

        getDfuConfig().setAddress(mAddress);

        if (mDfuHelper != null) {
            mDfuHelper.connectDevice(mSelectedDevice, isHid);
        }
    }

    protected static DfuConfig getDfuConfig() {
        if (mDfuConfig == null) {
            mDfuConfig = new DfuConfig();
        }
        return mDfuConfig;
    }
}
