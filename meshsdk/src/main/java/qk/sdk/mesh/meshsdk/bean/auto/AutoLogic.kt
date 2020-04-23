package qk.sdk.mesh.meshsdk.bean.auto

data class AutoLogic(
    var triggerOpcode: ByteArray,
    var len: Byte,
    var action: ArrayList<AutoAction>
) {
    fun getBytes() {

    }
}

data class AutoAction(
    var triggerValue: Byte,
    var len: Byte,
    var operation: ArrayList<AutoOperation>
) {
    fun getBytes() {

    }
}

data class AutoOperation(
    var excutorAddr: Short,
    var excutorOpcode: ByteArray,
    var len: Byte,
    var value: ByteArray
) {
    fun getBytes(): ByteArray {
        var operation = ByteArray(2 + excutorOpcode.size + 1 + value.size)
        System.arraycopy(excutorAddr, 0, operation, 0, 2)
        System.arraycopy(excutorOpcode, 0, operation, 2, excutorOpcode.size)
        System.arraycopy(len, 0, operation, 2 + excutorOpcode.size, 1)
        System.arraycopy(value, 0, operation, 2 + excutorOpcode.size + 1, value.size)

        return operation
    }
}