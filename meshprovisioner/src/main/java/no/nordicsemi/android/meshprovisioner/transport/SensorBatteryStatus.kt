package no.nordicsemi.android.meshprovisioner.transport

class SensorBatteryStatus : MeshMessage {

    val SENSOR_STATUS = 0x8224

    var battery = 0

    constructor (accessMsg: AccessMessage) {
        mMessage = accessMsg
        parseSensor(accessMsg.parameters)
    }

    fun parseSensor(params: ByteArray) {
        if (params.isNotEmpty())
            battery = params[0].toInt()
    }

    override fun getAid(): Int {
        return mMessage.aid
    }

    override fun getAkf(): Int {
        return mMessage.akf
    }

    override fun getOpCode(): Int {
        return SENSOR_STATUS
    }

    override fun getParameters(): ByteArray {
        return mMessage.parameters
    }

}