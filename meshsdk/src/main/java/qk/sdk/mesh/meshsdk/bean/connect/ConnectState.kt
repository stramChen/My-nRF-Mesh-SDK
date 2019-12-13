package qk.sdk.mesh.meshsdk.bean.connect

enum class ConnectState(var msg: String, var code: Int = 0) {
    CONNECTING("连接中"),
    DISCOVERING_SERVICE("检查服务中"),
    INITIALIZING("初始化蓝牙连接"),
    DEVICE_READY("连接初始化完成"),
    RECONNETCING("正在重连"),
    DISCONNECTING("正在断开连接"),
    DISCONNECTED("连接已断开")
}