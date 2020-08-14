package qk.sdk.mesh.meshsdk.bean

/**
 * @des:设备统一接口回调类
 * @author: chensc@mxchip.com
 * @date: 2020/7/28 8:08 PM
 */
const val TYPE_DEVICE_PROPERTY = "TYPE_DEVICE_PROPERTY";
const val TYPE_DEVICE_STATUS = "TYPE_DEVICE_STATUS";
//{ "type":"TYPE_PIR_SENSOR", "message":{"bio_senser":"bio_senser_on"} }
//{ "type":"TYPE_PIR_SENSOR", "message":{"bio_senser":"bio_senser_off"} }
data class SubscribeBean(val type:String?=null, val message:Any? = null)