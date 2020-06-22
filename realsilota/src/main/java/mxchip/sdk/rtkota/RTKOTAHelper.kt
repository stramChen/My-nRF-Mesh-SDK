package mxchip.sdk.rtkota

import android.app.Application
import android.content.Context
import com.realsil.sdk.core.BuildConfig
import com.realsil.sdk.core.RtkConfigure
import com.realsil.sdk.core.RtkCore
import com.realsil.sdk.dfu.RtkDfu
import com.realsil.sdk.dfu.image.BaseBinInputStream
import com.realsil.sdk.dfu.model.DfuConfig
import com.realsil.sdk.dfu.quality.DfuQualitySDK
import com.realsil.sdk.dfu.utils.DfuHelper
import com.realsil.sdk.support.RtkSupport
import mxchip.sdk.rtkota.util.SettingsHelper

object RTKOTAHelper {
    var mContext: Context? = null
    var mIsDebug = BuildConfig.DEBUG

    private var mDfuConfig: DfuConfig? = null

    fun init(context: Application) {
        mContext = context
        SettingsHelper.initialize(context)

        SettingsHelper.getInstance()?.dfuDebugLevel?.apply {
            val configure = RtkConfigure.Builder()
                .debugEnabled(mIsDebug)
                .printLog(true)
                .logTag("OTA")
                .globalLogLevel(this)
                .build()
            RtkCore.initialize(context, configure)
        }

        // Optional
        RtkSupport.initialize(context, mIsDebug)

        RtkDfu.initialize(context, mIsDebug)

        // Optional
        BaseBinInputStream.MPHEADER_PARSE_FORMAT = BaseBinInputStream.MPHEADER_PARSE_HEADER

        // Optional for quality test
        DfuQualitySDK.initialize(context)
        DfuQualitySDK.DBG = mIsDebug
    }

    fun getDfuHelper(): DfuHelper = DfuHelper.getInstance(mContext)

    fun getDfuConfig(): DfuConfig? {
        if (mDfuConfig == null)
            mDfuConfig = DfuConfig()

        return mDfuConfig
    }

}