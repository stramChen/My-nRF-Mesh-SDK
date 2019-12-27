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
internal constructor(val beaconData: ByteArray) : Parcelable {
    internal val beaconType: Int

    init {
        beaconType = beaconData[0].toInt()
    }

    /**
     * Returns the beacon type value
     */
    abstract fun getBeaconType(): Int

    companion object {
        private val TAG = "MeshBeacon"
        val MESH_BEACON = 0x2B
    }
}
