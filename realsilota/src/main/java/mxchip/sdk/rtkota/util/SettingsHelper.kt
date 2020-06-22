package mxchip.sdk.rtkota.util

import android.content.Context
import android.text.TextUtils
import com.realsil.sdk.core.base.BaseSharedPrefes
import com.realsil.sdk.core.logger.ZLogger
import com.realsil.sdk.core.utility.DataConverter
import com.realsil.sdk.dfu.DfuConstants
import com.realsil.sdk.dfu.model.DfuConfig
import java.util.*
import java.util.regex.Pattern


/**
 * @author bingshanguxue
 * @date 13/08/2017
 */

class SettingsHelper private constructor(context: Context) : BaseSharedPrefes(context) {

    val otaServiceUUID: String
        get() {
            var value = getString(KEY_DFU_OTA_SERVICE_UUID, null)

            if (TextUtils.isEmpty(value)) {
                value = OTA_SERVICE.toString()
                set(KEY_DFU_OTA_SERVICE_UUID, value)
            }

            return if (checkUuid(value)) {
                value
            } else {
                ""
            }
        }

    val dfuUsbEpInAddr: Int
        get() {
            val value = getString(KEY_DFU_USB_EP_IN_ADDR, null)
            if (TextUtils.isEmpty(value)) {
                set(KEY_DFU_USB_EP_IN_ADDR, "0")
                return 0
            } else {
                return Integer.parseInt(value)
            }
        }

    val dfuUsbEpOutAddr: Int
        get() {
            val value = getString(KEY_DFU_USB_EP_OUT_ADDR, null)
            if (TextUtils.isEmpty(value)) {
                set(KEY_DFU_USB_EP_OUT_ADDR, "0")
                return 0
            } else {
                return Integer.parseInt(value)
            }
        }
    private val SECRET_KEY = byteArrayOf(0x4E.toByte(), 0x46.toByte(), 0xF8.toByte(), 0xC5.toByte(), 0x09.toByte(), 0x2B.toByte(), 0x29.toByte(), 0xE2.toByte(), 0x9A.toByte(), 0x97.toByte(), 0x1A.toByte(), 0x0C.toByte(), 0xD1.toByte(), 0xF6.toByte(), 0x10.toByte(), 0xFB.toByte(), 0x1F.toByte(), 0x67.toByte(), 0x63.toByte(), 0xDF.toByte(), 0x80.toByte(), 0x7A.toByte(), 0x7E.toByte(), 0x70.toByte(), 0x96.toByte(), 0x0D.toByte(), 0x4C.toByte(), 0xD3.toByte(), 0x11.toByte(), 0x8E.toByte(), 0x60.toByte(), 0x1A.toByte())

    val dfuAesKey: String
        get() = getString(KEY_DFU_AES_KEY, DataConverter.bytes2Hex(SECRET_KEY))

    val isDfuDebugEnabled: Boolean
        get() {
            if (!contains(KEY_DFU_DEBUG)) {
                set(KEY_DFU_DEBUG, true)
                return true
            }

            return getBoolean(KEY_DFU_DEBUG, true)
        }

    val dfuDebugLevel: Int
        get() {
            val value = getString(KEY_DFU_DEBUG_LEVEL, null)
            if (TextUtils.isEmpty(value)) {
                set(KEY_DFU_DEBUG_LEVEL, ZLogger.VERBOSE.toString())
                return ZLogger.VERBOSE
            } else {
                return Integer.parseInt(value)
            }
        }

    val isDfuVersionCheckEnabled: Boolean
        get() {
            if (!contains(KEY_DFU_VERSION_CHECK)) {
                set(KEY_DFU_VERSION_CHECK, false)
                return false
            }

            return getBoolean(KEY_DFU_VERSION_CHECK, false)
        }

    val isDfuChipTypeCheckEnabled: Boolean
        get() {
            if (!contains(KEY_DFU_CONFIG_CHIP_TYPE_CHECK)) {
                set(KEY_DFU_CONFIG_CHIP_TYPE_CHECK, true)
                return true
            }

            return getBoolean(KEY_DFU_CONFIG_CHIP_TYPE_CHECK, true)
        }

    val isDfuImageSectionSizeCheckEnabled: Boolean
        get() {
            if (!contains(KEY_DFU_IMAGE_SECTION_SIZE_CHECK)) {
                set(KEY_DFU_IMAGE_SECTION_SIZE_CHECK, false)
                return false
            }

            return getBoolean(KEY_DFU_IMAGE_SECTION_SIZE_CHECK, false)
        }

    val isDfuBatteryCheckEnabled: Boolean
        get() {
            if (!contains(KEY_DFU_BATTERY_CHECK)) {
                set(KEY_DFU_BATTERY_CHECK, true)
                return true
            }

            return getBoolean(KEY_DFU_BATTERY_CHECK, true)
        }

    val dfuBatteryLevelFormat: Int
        get() {
            val value = getString(KEY_DFU_BATTERY_LEVEL_FORMAT, null)
            if (TextUtils.isEmpty(value)) {
                set(KEY_DFU_BATTERY_LEVEL_FORMAT, DfuConfig.BATTERY_LEVEL_FORMAT_PERCENTAGE.toString())
                return DfuConfig.BATTERY_LEVEL_FORMAT_PERCENTAGE
            } else {
                return Integer.parseInt(value)
            }
        }

    val dfuLowBatteryThreshold: Int
        get() {
            val value = getString(KEY_DFU_BATTERY_LOW_THRESHOLD, null)
            if (TextUtils.isEmpty(value)) {
                set(KEY_DFU_BATTERY_LOW_THRESHOLD, DfuConfig.MIN_POWER_LEVER.toString())
                return DfuConfig.MIN_POWER_LEVER
            } else {
                return Integer.parseInt(value)
            }
        }

    val isDfuAutomaticActiveEnabled: Boolean
        get() {
            if (!contains(KEY_DFU_AUTOMATIC_ACTIVE)) {
                set(KEY_DFU_AUTOMATIC_ACTIVE, true)
                return true
            }

            return getBoolean(KEY_DFU_AUTOMATIC_ACTIVE, true)
        }

    val isDfuBreakpointResumeEnabled: Boolean
        get() {
            if (!contains(KEY_DFU_BREAKPOINT_RESUME)) {
                set(KEY_DFU_BREAKPOINT_RESUME, true)
                return true
            }

            return getBoolean(KEY_DFU_BREAKPOINT_RESUME, true)
        }

    val isDfuActiveAndResetAckEnabled: Boolean
        get() {
            if (!contains(KEY_DFU_ACTIVE_AND_RESET_ACK)) {
                set(KEY_DFU_ACTIVE_AND_RESET_ACK, false)
                return false
            }

            return getBoolean(KEY_DFU_ACTIVE_AND_RESET_ACK, false)
        }


    val dfuSpeedControlLevel: Int
        get() {
            val value = getString(KEY_DFU_SPEED_CONTROL_LEVEL, null)
            if (TextUtils.isEmpty(value)) {
                set(KEY_DFU_SPEED_CONTROL_LEVEL, DfuConstants.SPEED_LEVEL_AUTOMATIC.toString())
                return DfuConstants.SPEED_LEVEL_AUTOMATIC
            } else {
                return Integer.parseInt(value)
            }
        }

    /**
     * @return
     */
    val dfuBufferCheckLevel: Int
        get() {
            val value = getString(KEY_DFU_BUFFER_CHECK_LEVEL, null)
            if (TextUtils.isEmpty(value)) {
                set(KEY_DFU_BUFFER_CHECK_LEVEL, DfuConfig.BUFFER_CHECK_ORIGINAL.toString())
                return DfuConfig.BUFFER_CHECK_ORIGINAL
            } else {
                return Integer.parseInt(value)
            }
        }

    val dfuMaxReconnectTimes: Int
        get() {
            val value = getString(KEY_DFU_MAX_RECONNECT_TIMES, null)

            if (TextUtils.isEmpty(value)) {
                set(KEY_DFU_MAX_RECONNECT_TIMES, "3")
                return 3
            }

            try {
                return Integer.parseInt(value)
            } catch (e: Exception) {
                set(KEY_DFU_MAX_RECONNECT_TIMES, "3")
                return 3
            }

        }

    val isDfuHidDeviceEnabled: Boolean
        get() {
            if (!contains(KEY_DFU_HID_AUTO_PAIR)) {
                set(KEY_DFU_HID_AUTO_PAIR, false)
                return false
            }

            return getBoolean(KEY_DFU_HID_AUTO_PAIR, false)
        }

    val isDfuThroughputEnabled: Boolean
        get() {
            if (!contains(KEY_DFU_THROUGHPUT)) {
                set(KEY_DFU_THROUGHPUT, false)
                return false
            }

            return getBoolean(KEY_DFU_THROUGHPUT, false)
        }

    val isDfuMtuUpdateEnabled: Boolean
        get() {
            if (!contains(KEY_DFU_MTU_UPDATE)) {
                set(KEY_DFU_MTU_UPDATE, false)
                return false
            }

            return getBoolean(KEY_DFU_MTU_UPDATE, false)
        }

    val isDfuConnectionParameterLatencyEnabled: Boolean
        get() {
            if (!contains(KEY_DFU_CONNECTION_PARAMETER_LATENCY)) {
                set(KEY_DFU_CONNECTION_PARAMETER_LATENCY, false)
                return false
            }

            return getBoolean(KEY_DFU_CONNECTION_PARAMETER_LATENCY, false)
        }

    val isWorkModePromptEnabled: Boolean
        get() {
            if (!contains(KEY_WORK_MODE_PROMPT)) {
                set(KEY_WORK_MODE_PROMPT, false)
                return false
            }

            return getBoolean(KEY_WORK_MODE_PROMPT, false)
        }

    val isUploadFilePromptEnabled: Boolean
        get() {
            if (!contains(KEY_UPLOAD_FILE_PROMPT)) {
                set(KEY_UPLOAD_FILE_PROMPT, false)
                return false
            }

            return getBoolean(KEY_UPLOAD_FILE_PROMPT, false)
        }

    val isDfuBankLinkEnabled: Boolean
        get() {
            if (!contains(KEY_BANK_LINK)) {
                set(KEY_BANK_LINK, false)
                return false
            }

            return getBoolean(KEY_BANK_LINK, false)
        }

    /**
     * it's recommended to turn on for bbpro ic.
     */
    val isDfuSuccessHintEnabled: Boolean
        get() {
            if (!contains(KEY_DFU_SUCCESS_HINT)) {
                set(KEY_DFU_SUCCESS_HINT, false)
                return false
            }

            return getBoolean(KEY_DFU_SUCCESS_HINT, false)
        }

    val isFixedImageFileEnabled: Boolean
        get() {
            if (!contains(KEY_DFU_FIXED_IMAGE_FILE)) {
                set(KEY_DFU_FIXED_IMAGE_FILE, false)
                return false
            }

            return getBoolean(KEY_DFU_FIXED_IMAGE_FILE, false)
        }

    val isDfuProductionPhoneBanklinkEnabled: Boolean
        get() {
            if (!contains(KEY_DFU_PRODUCTION_PHONE_BANKLINK)) {
                set(KEY_DFU_PRODUCTION_PHONE_BANKLINK, false)
                return false
            }

            return getBoolean(KEY_DFU_PRODUCTION_PHONE_BANKLINK, false)
        }

    val dfuProductionPriorityWorkMode: Int
        get() {
            val value = getString(KEY_DFU_PRODUCTION_PRIORITY_WORK_MODE, null)
            if (TextUtils.isEmpty(value)) {
                set(KEY_DFU_PRODUCTION_PRIORITY_WORK_MODE, DfuConstants.OTA_MODE_SILENT_FUNCTION.toString())
                return DfuConstants.OTA_MODE_SILENT_FUNCTION
            } else {
                return Integer.parseInt(value)
            }
        }

    val isDfuProductionSuccessInspectionEnabled: Boolean
        get() {
            if (!contains(KEY_DFU_PRODUCTION_SUCCESS_INSPECTION)) {
                set(KEY_DFU_PRODUCTION_SUCCESS_INSPECTION, false)
                return false
            }

            return getBoolean(KEY_DFU_PRODUCTION_SUCCESS_INSPECTION, false)
        }

    val isDfuProductionSuccessAutoScanEnabled: Boolean
        get() {
            if (!contains(KEY_DFU_PRODUCTION_SUCCESS_AUTO_SCAN)) {
                set(KEY_DFU_PRODUCTION_SUCCESS_AUTO_SCAN, false)
                return false
            }

            return getBoolean(KEY_DFU_PRODUCTION_SUCCESS_AUTO_SCAN, false)
        }

    val selectFileType: String
        get() {
            val value = getString(KEY_RTK_SELECT_FILE_TYPE, null)
            if (TextUtils.isEmpty(value)) {
                set(KEY_RTK_SELECT_FILE_TYPE, "*/*")
                return "*/*"
            } else {
                return value
            }
        }

    val fileLocation: Int
        get() {
            val value = getString(KEY_RTK_FILE_LOCATION, null)
            if (TextUtils.isEmpty(value)) {
                set(KEY_RTK_FILE_LOCATION, DfuConfig.FILE_LOCATION_SDCARD.toString())
                return DfuConfig.FILE_LOCATION_SDCARD
            } else {
                return Integer.parseInt(value)
            }
        }

    init {
        ZLogger.v("isDfuDebugEnabled:$isDfuDebugEnabled")
        ZLogger.v("getDfuDebugLevel:$dfuDebugLevel")

        ZLogger.v("getOtaServiceUUID:$otaServiceUUID")

        ZLogger.v("getDfuUsbEpInAddr:$dfuUsbEpInAddr")
        ZLogger.v("getDfuUsbEpOutAddr:$dfuUsbEpOutAddr")

        ZLogger.v("getDfuAesKey:$dfuAesKey")
        ZLogger.v("isDfuVersionCheckEnabled:$isDfuVersionCheckEnabled")
        ZLogger.v("isDfuChipTypeCheckEnabled:$isDfuChipTypeCheckEnabled")
        ZLogger.v("isDfuImageSectionSizeCheckEnabled:$isDfuImageSectionSizeCheckEnabled")
        ZLogger.v("isDfuBatteryCheckEnabled:$isDfuBatteryCheckEnabled")
        ZLogger.v("getDfuLowBatteryThreshold:$dfuLowBatteryThreshold")
        ZLogger.v("isDfuAutomaticActiveEnabled:$isDfuAutomaticActiveEnabled")
        ZLogger.v("isDfuBreakpointResumeEnabled:$isDfuBreakpointResumeEnabled")
        ZLogger.v("isDfuActiveAndResetAckEnabled:$isDfuActiveAndResetAckEnabled")
        ZLogger.v("getDfuSpeedControlLevel:$dfuSpeedControlLevel")
        ZLogger.v("getDfuBufferCheckLevel:$dfuBufferCheckLevel")
        ZLogger.v("getDfuMaxReconnectTimes:$dfuMaxReconnectTimes")
        ZLogger.v("isDfuHidDeviceEnabled:$isDfuHidDeviceEnabled")
        ZLogger.v("isDfuThroughputEnabled:$isDfuThroughputEnabled")
        ZLogger.v("isDfuMtuUpdateEnabled:$isDfuMtuUpdateEnabled")

        ZLogger.v("isWorkModePromptEnabled:$isWorkModePromptEnabled")
        ZLogger.v("isUploadFilePromptEnabled:$isUploadFilePromptEnabled")
        ZLogger.v("isDfuBankLinkEnabled:$isDfuBankLinkEnabled")
        ZLogger.v("isDfuSuccessHintEnabled:$isDfuSuccessHintEnabled")

        ZLogger.v("isDfuP roductionPhoneBanklinkEnabled:$isDfuProductionPhoneBanklinkEnabled")
        ZLogger.v("getDfuProductionPriorityWorkMode:$dfuProductionPriorityWorkMode")
        ZLogger.v("isDfuProductionSuccessAutoScanEnabled:$isDfuProductionSuccessAutoScanEnabled")
        ZLogger.v("isDfuProductionSuccessInspectionEnabled:$isDfuProductionSuccessInspectionEnabled")

        ZLogger.v("getSelectFileType:$selectFileType")
        ZLogger.v("getFileLocation:$fileLocation")

    }

    companion object {
        @JvmField
        val PREF_MASK = 0x0000
        @JvmField
        val PREF_DFU = 0x0001
        @JvmField
        val PREF_DFU_DEV = 0x0002
        @JvmField
        val PREF_FUNCTION_TEST = 0x0004
        @JvmField
        val PREF_PRESSURE_TEST = 0x0008
        @JvmField
        val PREF_QUALITY_CASE = 0x0010
        @JvmField
        val PREF_QUALITY_HRP = 0x0020
        @JvmField
        val PREF_PRODUCTION_TEST = 0x0040
        @JvmField
        val PREF_KEY_ASSISTANT = 0x0080
        @JvmField
        val PREF_KEY_GENERAL = 0x0100

        @JvmField
        val KEY_DFU_DEBUG = "switch_dfu_debug"
        @JvmField
        val KEY_DFU_DEBUG_LEVEL = "rtk_dfu_debug_level"
        private val KEY_DFU_VERSION_CHECK = "switch_dfu_version_check"
        private val KEY_DFU_CONFIG_CHIP_TYPE_CHECK = "switch_dfu_config_chip_check"
        private val KEY_DFU_IMAGE_SECTION_SIZE_CHECK = "switch_dfu_image_section_size_check"
        @JvmField
        val KEY_DFU_BATTERY_CHECK = "switch_dfu_battery_check"
        private val KEY_DFU_BATTERY_LEVEL_FORMAT = "dfu_battery_check_format"
        @JvmField
        val KEY_DFU_BATTERY_LOW_THRESHOLD = "dfu_battery_low_threshold"
        private val KEY_DFU_AUTOMATIC_ACTIVE = "switch_dfu_automatic_active"
        private val KEY_DFU_BREAKPOINT_RESUME = "switch_dfu_breakpoint_resume"
        private val KEY_DFU_ACTIVE_AND_RESET_ACK = "switch_dfu_active_and_reset_ack"
        private val KEY_DFU_SPEED_CONTROL_LEVEL = "dfu_speed_control_level_v2"
        private val KEY_DFU_BUFFER_CHECK_LEVEL = "dfu_buffer_check_level_v3"
        @JvmField
        val KEY_DFU_MAX_RECONNECT_TIMES = "edittext_max_reconnect_times"
        private val KEY_DFU_THROUGHPUT = "switch_dfu_throughput"
        private val KEY_DFU_MTU_UPDATE = "switch_dfu_mtu_update"
        private val KEY_DFU_CONNECTION_PARAMETER_LATENCY = "switch_dfu_connection_params_latency"
        private val KEY_DFU_HID_AUTO_PAIR = "switch_hid_auto_pair"
        private val KEY_WORK_MODE_PROMPT = "switch_dfu_work_mode_prompt"
        private val KEY_UPLOAD_FILE_PROMPT = "switch_dfu_upload_file_prompt"
        private val KEY_BANK_LINK = "switch_dfu_backlink"
        private val KEY_DFU_SUCCESS_HINT = "switch_dfu_success_hint"
        private val KEY_DFU_FIXED_IMAGE_FILE = "switch_dfu_fixed_image_file"

        private val KEY_DFU_PRODUCTION_PHONE_BANKLINK = "switch_dfu_production_phone_banklink"
        private val KEY_DFU_PRODUCTION_PRIORITY_WORK_MODE = "dfu_production_priotiry_work_mode"
        private val KEY_DFU_PRODUCTION_SUCCESS_INSPECTION = "switch_dfu_production_success_inspection"
        private val KEY_DFU_PRODUCTION_SUCCESS_AUTO_SCAN = "switch_dfu_production_success_auto_scan"

        private val KEY_RTK_SELECT_FILE_TYPE = "rtk_select_file_type"
        private val KEY_RTK_FILE_LOCATION = "rtk_file_location"

        //GATT
        @JvmField
        val KEY_DFU_OTA_SERVICE_UUID = "rtk_dfu_ota_service_uuid"
        //USB
        @JvmField
        val KEY_DFU_USB_EP_IN_ADDR = "rtk_dfu_usb_ep_in_addr_1"
        @JvmField
        val KEY_DFU_USB_EP_OUT_ADDR = "rtk_dfu_usb_ep_out_addr_1"

        @JvmField
        val KEY_DFU_AES_KEY = "rtk_dfu_aes_key"

        @Volatile
        private var instance: SettingsHelper? = null

        fun initialize(context: Context) {
            if (instance == null) {
                synchronized(SettingsHelper::class.java) {
                    if (instance == null) {
                        instance = SettingsHelper(context.applicationContext)
                    }
                }
            }
        }

        fun getInstance(): SettingsHelper? {
            if (instance == null) {
                ZLogger.w("not initialized, please call initialize(Context context) first")
            }
            return instance
        }

        fun checkUuid(uuid: String): Boolean {
            if (!TextUtils.isEmpty(uuid) && uuid.matches("(\\w{8}(-\\w{4}){3}-\\w{12}?)".toRegex())) {
                try {
                    UUID.fromString(uuid)
                    return true
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }

            return false
        }

        private val OTA_SERVICE = UUID.fromString("0000d0ff-3c17-d293-8e48-14fe2e4da212")
        //    private static final UUID OTA_SERVICE = UUID.fromString("0000d0ff-3c17-d293-8e48-14fe2e4da222");
        //    private byte[] SECRET_KEY = new byte[]{
        //            (byte) 0x1A, (byte) 0x60, (byte) 0x8E, (byte) 0x11, (byte) 0xD3, (byte) 0x4C, (byte) 0x0D, (byte) 0x96,
        //            (byte) 0x70, (byte) 0x7E, (byte) 0x7A, (byte) 0x80, (byte) 0xDF, (byte) 0x63, (byte) 0x67,(byte) 0x1F,
        //            (byte) 0xFB, (byte) 0x10, (byte) 0xF6, (byte) 0xD1, (byte) 0x0C, (byte) 0x1A, (byte) 0x97,(byte) 0x9A,
        //            (byte) 0xE2, (byte) 0x29, (byte) 0x2B, (byte) 0x09, (byte) 0xC5, (byte) 0xF8, (byte) 0x46,(byte) 0x4E
        //
        //    };
        private val AES_KEY_PATTERN = Pattern.compile("([a-zA-Z0-9]+)")

        fun checkAesKey(key: String): Boolean {
            if (!TextUtils.isEmpty(key)) {
                if (key.length == 64) {
                    return AES_KEY_PATTERN.matcher(key).matches()
                } else {
                    ZLogger.w("aes key length is invalid")
                    return false
                }
            }

            return false
        }
    }

}
