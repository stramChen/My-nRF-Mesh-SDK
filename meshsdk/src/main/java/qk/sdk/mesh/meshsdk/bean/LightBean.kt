package qk.sdk.mesh.meshsdk.bean

/**
 * @des:
 * @author: chensc@mxchip.com
 * @date: 2020/8/1 11:29 AM
 */
data class LightBean(var switch:String? =null,
                     var color:String? =null,
                     var lightnessLevel:String? =null,
                     var colorTemperature:String? =null,
                     var modeNumber:String? =null,
                     var event:String? =null) :
    DeviceBaseProperty()