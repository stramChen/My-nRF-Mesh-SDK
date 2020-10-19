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
const val VENDOR_MSG_OPCODE_HEART_BEAT = "14"
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
const val SUBSCRIBE_ALL_DEVICE: String = "D000";
const val SUBSCRIBE_ALL_DEVICE_ADDR: Int = 0xD000;

//设备同步组播地址
const val ALL_DEVICE_SYNC: String = "D002";
const val ALL_DEVICE_SYNC_ADDR: Int = 0xD002;
//本地联动组播地址
const val LOCAL_LINKAGE: String = "D003";
const val LOCAL_LINKAGE_ADDR: Int = 0xD003;

/**
 * 指定的CallBack key
 */
//向设备添加 app key
const val ADD_APPKEYS = "addAppkeys"

//获取设备的model
const val GET_COMPOSITION_DATA = "getCompositionData"

//attr type占两个字节
const val ATTR_LEN = 2

/**
 * 通用attr type
 */
const val ATTR_TYPE_COMMON_GET_STATUS = "0100" //设备上报属性
const val ATTR_TYPE_COMMON_SET_PROTOCAL_VERSION = "0200" //设置协议版本
const val ATTR_TYPE_COMMON_GET_QUADRUPLES = "0300" // 获取设备五元组信息
const val ATTR_TYPE_GET_VERSION = "0500" // 获取设备固件版本号
const val ATTR_TYPE_REBOOT_GATEWAY = "0600" // 重启设备
const val ATTR_TYPE_VIRTUAL_BUTTON = "0700" // 虚拟按钮

//佛照产品
//产品PID
//五路灯
@JvmField
val PRODUCT_ID_LIGHT_5 = MeshProduct(10004, arrayListOf("4752526"))

//var PRODUCT_TYPE_LIGHT_2 = arrayListOf<String>("4752526")
//var PRODUCT_TYPE_LIGHT_5 = arrayListOf<String>("3808464")

//二路灯
@JvmField
val PRODUCT_ID_LIGHT_2 = MeshProduct(10003, arrayListOf("3808464","5494080"))

//单火开关
@JvmField
val PRODUCT_ID_SOCKKET_SINGLE = MeshProduct(10006, arrayListOf("4284236","5504728"))

//var PRODUCT_TYPE_SOCKET = arrayListOf<String>("4284236","4791464","4139203")
//var PRODUCT_TYPE_SOCKET_DOBULE = arrayListOf<String>("4284236","4791464","4139203")
//var PRODUCT_TYPE_SOCKET_TRIPLE = arrayListOf<String>("4284236","4791464","4139203")

//双键单火开关
@JvmField
val PRODUCT_ID_SOCKKET_DOBULE = MeshProduct(10007, arrayListOf("4791464"))
//三键单火开关
@JvmField
val PRODUCT_ID_SOCKKET_TRIPLE = MeshProduct(10008, arrayListOf("4139203"))

//Pir传感器
@JvmField
val PRODUCT_ID_PIR_SENSOR = MeshProduct(10016, arrayListOf("3987829","5504974"))

//var PRODUCT_TYPE_PIR_SENSOR = arrayListOf<String>("3987829")

//网关
@JvmField
val PRODUCT_ID_GATEWAY = MeshProduct(10060, arrayListOf("3808465"))

//var PRODUCT_TYPE_GATEWAY = arrayListOf<String>("3808465")

//智家精灵产品
////五路灯
//const val PRODUCT_ID_LIGHT_5 = "5049550"
//
////二路灯
//const val PRODUCT_ID_LIGHT_2 = "5005926"
////单火开关
//const val PRODUCT_ID_SOCKKET_SINGLE = "5005932"
//
////双键单火开关
//const val PRODUCT_ID_SOCKKET_DOBULE = "5005934"
//
////三键单火开关
//const val PRODUCT_ID_SOCKKET_TRIPLE = "5005929"
//
////Pir传感器
//const val PRODUCT_ID_PIR_SENSOR = "5005928"
//
////网关
//const val PRODUCT_ID_GATEWAY = "5005925"


object DeviceConstantsCode {


    /*************************************灯****************************************/

    //内部封装相关操作码

    //开
    const val CODE_SWITCH_ON = "01";

    //关
    const val CODE_SWITCH_OFF = "00";

    val lightCons: HashMap<String, String> = hashMapOf(
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
        SWITCH to "0001",
        SWITCH_SECOND to "2401",
        SWITCH_THIRD to "2501",
        EVENT to "09F0"
    )

    /*************************************插座/单火开关***************************************/

    /*************************************PIR传感器****************************************/


    //内部封装相关操作码
    val pirSensorCons: HashMap<String, String> = hashMapOf(
        BIO_SENSER to "0104",
        REMAINING_ELECTRICITY to "0401",
        EVENT to "09F0"
    )

    /*************************************PIR传感器***************************************/

    /*************************************Mesh 网关****************************************/

    //内部封装相关操作码
    val MeshGateWayCons: HashMap<String, String> = hashMapOf(
    )
    /*************************************PIR传感器***************************************/


}
