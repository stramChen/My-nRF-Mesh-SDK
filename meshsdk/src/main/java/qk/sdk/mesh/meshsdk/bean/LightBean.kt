package qk.sdk.mesh.meshsdk.bean

/**
 * @des:
 * @author: chensc@mxchip.com
 * @date: 2020/8/1 11:29 AM
 */
data class LightBean(var switch:String? =null,
                     var hsvColorBean:HSVColorBean? =null,
                     var lightnessLevel:Int? =null,
                     var colorTemperature:Int? =null,
                     var modeNumber:Int? =null,
                     var event:String? =null) : DeviceBaseProperty()