package qk.sdk.mesh.demo.ui

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.os.Bundle
//import com.realsil.sdk.core.bluetooth.scanner.ExtendedBluetoothDevice
//import com.realsil.sdk.core.bluetooth.scanner.ScannerCallback
//import com.realsil.sdk.core.bluetooth.scanner.ScannerParams
//import com.realsil.sdk.core.bluetooth.scanner.ScannerPresenter
//import com.realsil.sdk.core.logger.ZLogger
//import kotlinx.android.synthetic.main.activity_gatt_dfu.*
//import mxchip.sdk.rtkota.RTKOTAHelper
//import mxchip.sdk.rtkota.util.SettingsHelper
//import qk.sdk.mesh.demo.R
import qk.sdk.mesh.demo.base.BaseMeshActivity

class GattDfuActivity : BaseMeshActivity() {
//    private var mDfuHelper = RTKOTAHelper.getDfuHelper()
//    private var mDfuConfig = RTKOTAHelper.getDfuConfig()
    private var mFilePath = "storage/emulated/0/0605_light_fsl_ota-4becd3b720ca819c5dbc6fd8b646117c.bin"

    protected var mBtAdapter: BluetoothAdapter? = null
    protected var mSelectedDevice: BluetoothDevice? = null
//    protected var mScannerPresenter: ScannerPresenter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

//    override fun setLayoutId(): Int = R.layout.activity_gatt_dfu
    override fun setLayoutId(): Int = 0

    override fun init() {
//        tv_file_path.text = "File Path: $mFilePath"


    }

    private fun initialize(){
        mBtAdapter = BluetoothAdapter.getDefaultAdapter()
        if (mBtAdapter == null) {
            finish()
        }


//        val scannerParams = ScannerParams(ScannerParams.SCAN_MODE_GATT)
//        scannerParams.setScanPeriod(60 * 1000.toLong())
//        mScannerPresenter = ScannerPresenter(this, scannerParams, mScannerCallback)
//        mScannerPresenter?.init()
    }

//    private var mScannerCallback = object : ScannerCallback() {
//        override fun onNewDevice(device: ExtendedBluetoothDevice) {
//            super.onNewDevice(device)
//            if (device != null) {
//                val btDevice = device.getDevice()
//                //                ZLogger.d(">> " + device.toString());
//                if (btDevice != null && mSelectedDevice != null && btDevice.address == mSelectedDevice!!.address) {
////                    if (SettingsHelper.getInstance()?.isDfuActiveAndResetAckEnabled?:false && !isOtaProcessing()) {
//////                        banklink
////                        ZLogger.v("bankLink: $btDevice")
//////                        connectRemoteDevice(btDevice, false)
////                    }
//////                    notifyScanLock()
//                }
//            }
//        }
//    }

//    fun isOtaProcessing(): Boolean {
//        return mState and com.realsil.ota.function.BaseDfuActivity.STATE_OTA_PROCESSING == com.realsil.ota.function.BaseDfuActivity.STATE_OTA_PROCESSING
//    }
}
