package qk.sdk.mesh.demo.base

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.joker.api.wrapper.ListenerWrapper
import no.nordicsemi.android.meshprovisioner.transport.MeshMessage
import qk.sdk.mesh.demo.R
import qk.sdk.mesh.meshsdk.MeshHelper
import qk.sdk.mesh.meshsdk.callback.MeshCallback
import qk.sdk.mesh.meshsdk.callback.ScanCallback
import qk.sdk.mesh.meshsdk.util.Utils
import java.util.*

abstract class BaseMeshActivity : AppCompatActivity() {
    private val TAG = "BaseMeshActivity"
    internal var BUNDLE_CONNECT_EXTRA = "isReConnect"

    internal var mPingMills = 0L

    abstract fun init()
    abstract fun setLayoutId(): Int

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(setLayoutId())
        init()
    }

    internal fun sendMessage(address: Int, message: MeshMessage, callback: MeshCallback? = null) {
        try {
            mPingMills = System.currentTimeMillis()
            MeshHelper.sendMeshPdu(address, message, callback)
        } catch (ex: IllegalArgumentException) {
            ex.printStackTrace()
            //todo 日志记录
        }
    }

    internal fun startScan(uuid: UUID, callback: ScanCallback?) {
        MeshHelper.checkPermission(this, object : ListenerWrapper.PermissionRequestListener {
            override fun permissionGranted(p0: Int) {//开启权限成功，开始扫描
                MeshHelper.startScan(uuid, callback)
            }

            override fun permissionDenied(p0: Int) {// 权限开启失败
                Utils.showToast(this@BaseMeshActivity, getString(R.string.grant_permission))
                finish()
            }

            override fun permissionRationale(p0: Int) {
            }
        })

    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

}