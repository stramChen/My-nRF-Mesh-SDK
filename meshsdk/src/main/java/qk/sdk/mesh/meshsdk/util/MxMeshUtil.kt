package qk.sdk.mesh.meshsdk.util

/**
 * @des:
 * @author: chensc@mxchip.com
 * @date: 2020/7/30 4:18 PM
 */
object MxMeshUtil {
    fun getProductIdByUUID(uuid: String): Int {
        var uuidHex = uuid.replace("-", "")
        var uuidBytes = ByteUtil.hexStringToBytes(uuidHex)
        if (uuidBytes.size >= 11) {
            val cid = ByteUtil.getDataFromUUID(uuidBytes, ByteArray(2), 0)
            if (cid == 0x0922) {//公司ID，VBS9010是0x0922，天猫精灵是0x01A8，APP 以此作为过滤条件
                val bytes = ByteUtil.byteTobitArray(uuidBytes[2])
                val pid = ByteUtil.getPId(bytes, ByteArray(4), 4)
                if (pid == 0x01)//Bit0-3 蓝牙广播包版本号，目前是0x01
                    return ByteUtil.getDataFromUUID(uuidBytes, ByteArray(4), 3)
            }
        }
        return -1
    }
}
