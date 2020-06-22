package mxchip.sdk.baseutil.bean

class LogMsg(var timeStamp: String, var data: String) {

    override fun toString(): String {
        return "$timeStamp  $data"
    }

}