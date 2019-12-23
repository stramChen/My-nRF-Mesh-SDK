package qk.sdk.mesh.meshsdk.mesh

import android.content.Context
import android.util.Log

import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.BleManagerCallbacks
//import no.nordicsemi.android.log.ILogSession
//import no.nordicsemi.android.log.LogContract
//import no.nordicsemi.android.log.Logger

/**
 * The manager that logs to nRF Logger. If nRF Logger is not installed, logs are ignored.
 *
 * @param <T> the callbacks class.
</T> */
abstract class LoggableBleManager<T : BleManagerCallbacks>
/**
 * The manager constructor.
 *
 *
 * After constructing the manager, the callbacks object must be set with
 * [.setGattCallbacks].
 *
 * @param context the context.
 */
internal constructor(context: Context) : BleManager<T>(context) {
//    private var mLogSession: ILogSession? = null

    /**
     * Sets the log session to log into.
     *
     * @param session nRF Logger log session to log inti, or null, if nRF Logger is not installed.
     */
//    fun setLogger(session: ILogSession?) {
//        mLogSession = session
//    }
//
//    override fun log(priority: Int, message: String) {
//        Logger.log(mLogSession, LogContract.Log.Level.fromPriority(priority), message)
//        Log.println(priority, "BleManager", message)
//    }
}