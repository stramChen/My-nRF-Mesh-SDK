package qk.sdk.mesh.meshsdk.bean

/**
 * @des:所有设备的指令以及信息
 * @author: chensc@mxchip.com
 * @date: 2020/7/30 2:43 PM
 */

/**
 * opcode
 */
const val VENDOR_MSG_OPCODE_ATTR_GET = "10"
const val VENDOR_MSG_OPCODE_ATTR_SET = "11"
const val VENDOR_MSG_OPCODE_ATTR_RECEIVE = "13"
const val VENDOR_MSG_ATTR_SET_UNACKED = "12"

//同步设备消息
const val VENDOR_MSG_OPCODE_SYNC = 0x15

//虚拟按钮
const val VENDOR_MSG_OPCODE_STATUS = 0x13

//向设备添加APP_KEY
const val VENDOR_MSG_ADD_APP_KEY = "8003"

//获取设备的配置信息，如model之类的
const val VENDOR_MSG_GET_COMPOSITION_DATA = "02"


//设备订阅组播地址
const val SUBSCRIBE_ALL_DEVICE: String = "0xD000";
const val SUBSCRIBE_ALL_DEVICE_ADDR: Int = 0xD000;

//设备同步组播地址
const val ALL_DEVICE_SYNC: String = "0xD002";
const val ALL_DEVICE_SYNC_ADDR: Int = 0xD002;

/**
 * 指定的CallBack key
 */
//向设备添加 app key
const val ADD_APPKEYS = "addAppkeys"

//获取设备的model
const val GET_COMPOSITION_DATA = "getCompositionData"

/**
 * 通用attr type
 */
const val ATTR_TYPE_COMMON_GET_STATUS = "0100" //设备上报属性
const val ATTR_TYPE_COMMON_SET_PROTOCAL_VERSION = "0200" //设置协议版本
const val ATTR_TYPE_COMMON_GET_QUADRUPLES = "0300" // 获取设备五元组信息
const val ATTR_TYPE_GET_VERSION = "0500" // 获取设备固件版本号
const val ATTR_TYPE_REBOOT_GATEWAY = "0600" // 重启设备
const val ATTR_TYPE_VIRTUAL_BUTTON = "0700" // 虚拟按钮

object DeviceConstantsCode {


    /*************************************灯****************************************/

    //内部封装相关操作码

    //开
    const val CODE_SWITCH_ON = "01";

    //关
    const val CODE_SWITCH_OFF = "00";

    val lightCons: HashMap<String, String> = hashMapOf(
//            PRODUCT_ID to "5494080",
            PRODUCT_ID to "3808464",
            SWITCH to "0001",//开关
            COLOR to "2301",//颜色hsv
            LIGHTNESS_LEVEL to "2101",//亮度
            COLOR_TEMPERATURE to "2201",//色温
            MODE_NUMBER to "04F0",
            EVENT to "09F0"
    )

    /*************************************灯***************************************/

    /*************************************插座/单火开关****************************************/

    //内部封装相关操作码
    val socketCons: HashMap<String, String> = hashMapOf(
//            PRODUCT_ID to "5504728",
            PRODUCT_ID to "4284236",
            SWITCH to "0001",
            SWITCH_SECOND to "2401",
            SWITCH_THIRD to "2501",
            EVENT to "09F0"
    )

    /*************************************插座/单火开关***************************************/

    /*************************************PIR传感器****************************************/


    //内部封装相关操作码
    val pirSensorCons: HashMap<String, String> = hashMapOf(
//            PRODUCT_ID to "5504974",
//            PRODUCT_KEY to "a1cbgysD8Q6",
            PRODUCT_ID to "3987829",
            PRODUCT_KEY to "a13Bv5xagdy",
            BIO_SENSER to "0104",
            REMAINING_ELECTRICITY to "0401",
            EVENT to "09F0"
    )

    /*************************************PIR传感器***************************************/

    /*************************************Mesh 网关****************************************/

    //内部封装相关操作码
    val MeshGateWayCons: HashMap<String, String> = hashMapOf(
        PRODUCT_ID to "3808465"
    )
    /*************************************PIR传感器***************************************/


}
