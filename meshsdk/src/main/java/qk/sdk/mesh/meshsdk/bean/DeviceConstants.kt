package qk.sdk.mesh.meshsdk.bean

/**
 * @des:所有设备的指令以及信息
 * @author: chensc@mxchip.com
 * @date: 2020/7/30 2:43 PM
 */
object DeviceConstants {
    //给调用着使用

    /*************************************灯****************************************/
    //产品ID
    const val PRODUCT_ID = "product_key";

    //开关
    const val SWITCH = "switch";

    //亮度
    const val COLOR = "Color";

    //颜色
    const val LIGHTNESS_LEVEL = "lightness_level";

    //色温
    const val COLOR_TEMPERATURE = "color_temperature";

    //模式
    const val MODE_NUMBER = "mode_number";

    //故障
    const val EVENT = "Event"

    const val ATTR_TYPE_LIGHT_ON_OFF = "0001" //开关
    const val ATTR_TYPE_LIGHT_BRIGHTNESS = "2101" //亮度
    const val ATTR_TYPE_LIGHT_TEMPRETURE = "2201" //色温
    const val ATTR_TYPE_LIGHT_HSV = "2301" //颜色hsv
    //内部封装相关操作码
    val lightCons: HashMap<String, Any> = hashMapOf(
        PRODUCT_ID to 5494080,
        SWITCH to "0001",
        COLOR to "2301",
        LIGHTNESS_LEVEL to "2101",
        COLOR_TEMPERATURE to "2201",
        MODE_NUMBER to "04F0",
        EVENT to "09F0"
    )
    /*************************************灯***************************************/

    /*************************************插座/单火开关****************************************/

    //开关
    const val SWITCH_SECOND = "switch_second"

    //开关
    const val SWITCH_THIRD = "switch_third"

    //内部封装相关操作码
    val socketCons: HashMap<String, Any> = hashMapOf(
        PRODUCT_ID to 5504728,
        SWITCH to "01",
        SWITCH_SECOND to "2401",
        SWITCH_THIRD to "2501",
        EVENT to "09F0"
    )
    /*************************************插座/单火开关***************************************/

    /*************************************PIR传感器****************************************/

    //生物感应
    const val BIO_SENSER = "bio_senser"

    //电量百分比
    const val REMAINING_ELECTRICITY = "remaining_electricity"

    //内部封装相关操作码
    val PIRSensorCons: HashMap<String, Any> = hashMapOf(
        PRODUCT_ID to 5504974,
        BIO_SENSER to "01",
        REMAINING_ELECTRICITY to "2401",
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
