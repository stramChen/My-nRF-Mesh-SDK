package no.nordicsemi.android.meshprovisioner

import android.os.Parcel
import android.os.Parcelable

import java.nio.ByteBuffer
import java.util.UUID

/**
 * Contains the information related to a secure network beacon.
 */
class UnprovisionedBeacon
/**
 * Constructs a [UnprovisionedBeacon] object
 *
 * @param beaconData beacon data advertised by the mesh beacon
 * @throws IllegalArgumentException if advertisement data provide is empty or null
 */
    (beaconData: ByteArray) : MeshBeacon(beaconData) {
    /**
     * Returns the Device UUID advertised by an unprovisioned beacon
     */
    val uuid: UUID
    /**
     * Returns the oob information advertised by an unprovisioned beacon
     */
    val oobInformation = ByteArray(2)
    /**
     * Returns the uri hash advertised by an unprovisioned beacon
     */
    val uriHash = ByteArray(4)

    init {
        require(beaconData.size >= BEACON_DATA_LENGTH) { "Invalid unprovisioned beacon data" }

        val buffer = ByteBuffer.wrap(beaconData)
        buffer.position(1)
        val msb = buffer.long
        val lsb = buffer.long
        uuid = UUID(msb, lsb)
        buffer.get(oobInformation, 0, 2)
        if (buffer.remaining() == 4) {
            buffer.get(uriHash, 0, 4)
        }

    }

    override fun getMeshBeaconType(): Int {
        return getMeshBeaconType()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeByteArray(beaconData)
    }

    companion object {
        private val BEACON_DATA_LENGTH = 19
        private val OOB_INDEX = 17
        private val URI_HASH_INDEX = 19

        @JvmField
        val CREATOR: Parcelable.Creator<UnprovisionedBeacon> =
            object : Parcelable.Creator<UnprovisionedBeacon> {
                override fun createFromParcel(source: Parcel): UnprovisionedBeacon {
                    return UnprovisionedBeacon(source.createByteArray()!!)
                }

                override fun newArray(size: Int): Array<UnprovisionedBeacon?> {
                    return arrayOfNulls(size)
                }
            }
    }
}
