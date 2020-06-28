package qk.sdk.mesh.demo.base

//import com.joker.api.wrapper.ListenerWrapper
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import no.nordicsemi.android.meshprovisioner.transport.MeshMessage
import qk.sdk.mesh.demo.util.StatusBarUtil
import qk.sdk.mesh.meshsdk.MeshHelper
import qk.sdk.mesh.meshsdk.callback.MeshCallback
import qk.sdk.mesh.meshsdk.callback.ScanCallback
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

    protected open fun initAppBar(isDrak: Boolean) {
        StatusBarUtil.setRootViewFitsSystemWindows(this, true)
        //设置状态栏透明
        StatusBarUtil.setTranslucentStatus(this)
        if (isDrak) {
            if (!StatusBarUtil.setStatusBarDarkTheme(this, true)) {
                //如果不支持设置深色风格 为了兼容总不能让状态栏白白的看不清, 于是设置一个状态栏颜色为半透明,
                //这样半透明+白=灰, 状态栏的文字能看得清
                StatusBarUtil.setStatusBarColor(this, 0x55000000)
            }
        }
    }

    internal fun sendMessage(method:String,address: Int, message: MeshMessage, callback: MeshCallback? = null) {
        try {
            mPingMills = System.currentTimeMillis()
            MeshHelper.sendMeshPdu(method,address, message, callback)
        } catch (ex: IllegalArgumentException) {
            ex.printStackTrace()
            //todo 日志记录
        }
    }

    internal fun startScan(uuid: UUID, callback: ScanCallback?) {
//        MeshHelper.checkPermission(this, object : ListenerWrapper.PermissionRequestListener {
//            override fun permissionGranted(p0: Int) {//开启权限成功，开始扫描
                MeshHelper.startScan(uuid, callback)
//            }
//
//            override fun permissionDenied(p0: Int) {// 权限开启失败
//                Utils.showToast(this@BaseMeshActivity, getString(R.string.grant_permission))
//                finish()
//            }
//
//            override fun permissionRationale(p0: Int) {
//            }
//        })

    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

}