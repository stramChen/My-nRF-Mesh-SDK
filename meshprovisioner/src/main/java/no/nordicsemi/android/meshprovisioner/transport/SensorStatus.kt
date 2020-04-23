package no.nordicsemi.android.meshprovisioner.transport

import no.nordicsemi.android.meshprovisioner.utils.ByteUtil

class SensorStatus : MeshMessage {

    val SENSOR_STATUS = 0x52

    var msensorData = ArrayList<SensorData>()

    constructor (accessMsg: AccessMessage) {
        mMessage = accessMsg
        parseSensor(accessMsg.parameters)
    }

    fun parseSensor(params: ByteArray) {
        var curPos = 0
        while (curPos < params.size) {
            var formatParams = ByteUtil.byteTobitArray(params[curPos])
            if (formatParams[7].toInt() == 1) {//目前只解析format B
                formatParams[7] = 0
                var valueLen =
                    ByteUtil.bitToByte(ByteUtil.bitArrayToString(formatParams)).toInt() shr 1
                curPos += 2
                if (curPos + valueLen <= params.size) {
                    var propertyId = byteArrayOf(params[curPos], params[curPos - 1])
                    var valueArr = ByteArray(valueLen)
                    curPos++
                    System.arraycopy(params, curPos, valueArr, 0, valueLen)
                    msensorData.add(SensorData(propertyId, valueArr))
                    curPos += valueLen
                    continue
                }
            } else {
                //todo mxchip
                break
            }
        }
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

    data class SensorData(var propertyId: ByteArray, var value: ByteArray)
}