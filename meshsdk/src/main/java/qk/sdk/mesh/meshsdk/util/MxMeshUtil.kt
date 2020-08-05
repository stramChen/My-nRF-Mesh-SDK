package qk.sdk.mesh.meshsdk.util

import java.util.concurrent.atomic.AtomicInteger

/**
 * @des:
 * @author: chensc@mxchip.com
 * @date: 2020/7/30 4:18 PM
 */
object MxMeshUtil {
    var timeout:AtomicInteger = AtomicInteger(0);
    var interval :Long= 10*1000;
    var lastTime:Long = 0;
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

    /**
     * 生成Tid，后面会作为sequence来标示回唯一的回调
     * 0做保留sequence，不作为唯一的回调标示
     * sequence逻辑为1-253作为10s内的唯一标示符，超过10s则从1开始重新计数
     */
    fun generateTid(): Int {
        var res :Int;
        var currentTime:Long = System.currentTimeMillis();
        if(currentTime - lastTime< interval){
            res =  1+timeout.incrementAndGet()%253
        }else{
            timeout.set(1)
            res =  timeout.get()
        }
        lastTime = currentTime;
        return res;
    }
}
