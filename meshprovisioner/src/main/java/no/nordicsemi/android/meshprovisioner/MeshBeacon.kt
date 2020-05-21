package no.nordicsemi.android.meshprovisioner

import android.os.Parcelable

/**
 * Abstract class containing mesh beacon information
 */
abstract class MeshBeacon
/**
 * Constructs a [MeshBeacon] object
 *
 * @param beaconData beacon data advertised by the mesh beacon
 * @throws IllegalArgumentException if beacon data provided is empty or null
 */
constructor(data: ByteArray) : Parcelable {
     val beaconType: Int
     var beaconData: ByteArray

    init {
        beaconType = data[0].toInt()
        beaconData = data
    }

    /**
     * Returns the beacon type value
     */
    abstract fun getMeshBeaconType(): Int

//    fun getBeaconData(): ByteArray = beaconData

    companion object {
        private val TAG = "MeshBeacon"
        val MESH_BEACON = 0x2B
    }
}
