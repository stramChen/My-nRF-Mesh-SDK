/*
 * Copyright (c) 2018, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package qk.sdk.mesh.meshsdk.util

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import android.preference.PreferenceManager
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import no.nordicsemi.android.meshprovisioner.NetworkKey

import java.util.Comparator
import java.util.UUID

import no.nordicsemi.android.support.v18.scanner.ScanResult
import qk.sdk.mesh.meshsdk.BuildConfig

object Utils {
    private val TAG = "Utils"
    val EXTRA_DATA_PROVISIONING_SERVICE = "EXTRA_DATA_PROVISIONING_SERVICE"
    val HEX_PATTERN = "^[0-9a-fA-F]+$"
    val EXTRA_MODEL_ID = "EXTRA_MODEL_ID"
    val EXTRA_ELEMENT_ADDRESS = "EXTRA_ELEMENT_ADDRESS"
    val EXTRA_DATA_MODEL_NAME = "EXTRA_DATA_MODEL_NAME"

    val EXTRA_DEVICE = "EXTRA_DEVICE"
    val ACTIVITY_RESULT = "RESULT_KEY"
    val PROVISIONING_COMPLETED = "PROVISIONING_COMPLETED"
    val PROVISIONER_UNASSIGNED = "PROVISIONER_UNASSIGNED"
    val COMPOSITION_DATA_COMPLETED = "COMPOSITION_DATA_COMPLETED"
    val DEFAULT_GET_COMPLETED = "DEFAULT_GET_COMPLETED"
    val APP_KEY_ADD_COMPLETED = "APP_KEY_ADD_COMPLETED"
    val NETWORK_TRANSMIT_SET_COMPLETED = "NETWORK_TRANSMIT_SET_COMPLETED"
    val EXTRA_DATA = "EXTRA_DATA"
    private val PREFS_LOCATION_NOT_REQUIRED = "location_not_required"
    private val PREFS_PERMISSION_REQUESTED = "permission_requested"
    private val PREFS_READ_STORAGE_PERMISSION_REQUESTED = "read_storage_permission_requested"
    private val PREFS_WRITE_STORAGE_PERMISSION_REQUESTED = "write_storage_permission_requested"
    val PROVISIONING_SUCCESS = 2112
    val CONNECT_TO_NETWORK = 2113
    val RESULT_KEY = "RESULT_KEY"
    private val APPLICATION_KEYS = "APPLICATION_KEYS"
    val RANGE_TYPE = "RANGE_TYPE"
    val DIALOG_FRAGMENT_KEY_STATUS = "DIALOG_FRAGMENT_KEY_STATUS"

    //Message timeout in case the message fails to lost/received
    val MESSAGE_TIME_OUT = 10000
    //Manage ranges
    val UNICAST_RANGE = 0
    val GROUP_RANGE = 1
    val SCENE_RANGE = 2

    //Manage app keys
    val MANAGE_NET_KEY = 0
    val ADD_NET_KEY = 1

    //Manage app keys
    val MANAGE_APP_KEY = 2
    val ADD_APP_KEY = 3
    val BIND_APP_KEY = 4
    val PUBLICATION_APP_KEY = 5
    val SELECT_KEY = 2011 //Random number

    val netKeyComparator = Comparator<NetworkKey> { key1, key2 ->
        Integer.compare(
            key1.getKeyIndex(),
            key2.getKeyIndex()
        )
    }

    val appKeyComparator = Comparator<NetworkKey> { key1, key2 ->
        Integer.compare(
            key1.getKeyIndex(),
            key2.getKeyIndex()
        )
    }

    /**
     * Checks whether Bluetooth is enabled.
     *
     * @return true if Bluetooth is enabled, false otherwise.
     */
    val isBleEnabled: Boolean
        get() {
            val adapter = BluetoothAdapter.getDefaultAdapter()
            return adapter != null && adapter.isEnabled
        }

    val isMarshmallowOrAbove: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M

    val isLollipopOrAbove: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP

    val isKitkatOrAbove: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT

    /**
     * Checks for required permissions.
     *
     * @return true if permissions are already granted, false otherwise.
     */
    fun isLocationPermissionsGranted(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Returns true if location permission has been requested at least twice and
     * user denied it, and checked 'Don't ask again'.
     *
     * @param activity the activity
     * @return true if permission has been denied and the popup will not come up any more, false otherwise
     */
    fun isLocationPermissionDeniedForever(activity: Activity): Boolean {
        val preferences = PreferenceManager.getDefaultSharedPreferences(activity)

        return (!isLocationPermissionsGranted(activity) // Location permission must be denied

                && preferences.getBoolean(
            PREFS_PERMISSION_REQUESTED,
            false
        ) // Permission must have been requested before

                && !ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            Manifest.permission.ACCESS_FINE_LOCATION
        )) // This method should return false
    }

    /**
     * On some devices running Android Marshmallow or newer location services must be enabled in order to scan for Bluetooth LE devices.
     * This method returns whether the Location has been enabled or not.
     *
     * @return true on Android 6.0+ if location mode is different than LOCATION_MODE_OFF. It always returns true on Android versions prior to Marshmallow.
     */
    fun isLocationEnabled(context: Context): Boolean {
        if (isMarshmallowOrAbove) {
            var locationMode = Settings.Secure.LOCATION_MODE_OFF
            try {
                locationMode =
                    Settings.Secure.getInt(context.contentResolver, Settings.Secure.LOCATION_MODE)
            } catch (e: Settings.SettingNotFoundException) {
                // do nothing
            }

            return locationMode != Settings.Secure.LOCATION_MODE_OFF
        }
        return true
    }

    /**
     * Location enabled is required on some phones running Android Marshmallow or newer (for example on Nexus and Pixel devices).
     *
     * @param context the context
     * @return false if it is known that location is not required, true otherwise
     */
    fun isLocationRequired(context: Context): Boolean {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        return preferences.getBoolean(PREFS_LOCATION_NOT_REQUIRED, isMarshmallowOrAbove)
    }

    /**
     * When a Bluetooth LE packet is received while Location is disabled it means that Location
     * is not required on this device in order to scan for LE devices. This is a case of Samsung phones, for example.
     * Save this information for the future to keep the Location info hidden.
     *
     * @param context the context
     */
    fun markLocationNotRequired(context: Context) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        preferences.edit().putBoolean(PREFS_LOCATION_NOT_REQUIRED, false).apply()
    }

    /**
     * The first time an app requests a permission there is no 'Don't ask again' checkbox and
     * [ActivityCompat.shouldShowRequestPermissionRationale] returns false.
     * This situation is similar to a permission being denied forever, so to distinguish both cases
     * a flag needs to be saved.
     *
     * @param context the context
     */
    fun markLocationPermissionRequested(context: Context) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        preferences.edit().putBoolean(PREFS_PERMISSION_REQUESTED, true).apply()
    }

    /**
     * Checks for required permissions.
     *
     * @return true if permissions are already granted, false otherwise.
     */
    fun isWriteExternalStoragePermissionsGranted(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * The first time an app requests a permission there is no 'Don't ask again' checkbox and
     * [ActivityCompat.shouldShowRequestPermissionRationale] returns false.
     * This situation is similar to a permission being denied forever, so to distinguish both cases
     * a flag needs to be saved.
     *
     * @param context the context
     */
    fun markWriteStoragePermissionRequested(context: Context) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        preferences.edit().putBoolean(PREFS_WRITE_STORAGE_PERMISSION_REQUESTED, true).apply()
    }

    /**
     * Returns true if write external permission has been requested at least twice and
     * user denied it, and checked 'Don't ask again'.
     *
     * @param activity the activity
     * @return true if permission has been denied and the popup will not come up any more, false otherwise
     */
    fun isWriteExternalStoragePermissionDeniedForever(activity: Activity): Boolean {
        val preferences = PreferenceManager.getDefaultSharedPreferences(activity)

        return (!isWriteExternalStoragePermissionsGranted(activity) // Location permission must be denied

                && preferences.getBoolean(
            PREFS_WRITE_STORAGE_PERMISSION_REQUESTED,
            false
        ) // Permission must have been requested before

                && !ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )) // This method should return false
    }

    fun showToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    fun isValidUint8(i: Int): Boolean {
        return i and -0x100 == 0 || i and -0x100 == -0x100
    }

    fun checkIfVersionIsOreoOrAbove(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    }

    fun getServiceData(result: ScanResult, serviceUuid: UUID): ByteArray? {
        val scanRecord = result.scanRecord
        return scanRecord?.getServiceData(ParcelUuid(serviceUuid))
    }

    fun printLog(tag: String, msg: String) {
//        if (BuildConfig.DEBUG) {
            Log.e(tag, msg)
//        }
    }

    fun getMacFromUUID(uuid: String): String {
        var newUUID = uuid.replace("-", "")
        if (newUUID.length >= 32) {
            var macSB = StringBuffer(newUUID.substring(10, 22))
            var macStr = macSB.toString()
            for (i in 0 until (macStr.length / 2 - 1)) {
                macSB.insert(2 + i * 3, ":")
            }
            return macSB.toString()
        }
        return ""
    }

    fun isUUIDEqualsMac(uuid: String, mac: String): Boolean {
        var macSB = StringBuffer()
        mac.split(":").forEach {
            macSB.append(it)
        }
        return uuid == macSB.toString()
    }

}
