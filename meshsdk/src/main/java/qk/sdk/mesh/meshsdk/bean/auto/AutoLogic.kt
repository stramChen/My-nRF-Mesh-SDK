package qk.sdk.mesh.meshsdk.bean.auto

import qk.sdk.mesh.meshsdk.util.ByteUtil

const val TAG = "AutoRule"

data class Rules(var rules: ArrayList<Rule>) {
    fun getBytes(): ByteArray {
        var rulesLen = 0
        rules.forEach {
            rulesLen += it.getBytes().size
        }

        var ruleBytes = ByteArray(rulesLen)
        var index = 0
        rules.forEach {
            System.arraycopy(it.getBytes(), 0, ruleBytes, index, it.len)
            index += it.len
        }

        return ruleBytes
    }
}

data class Rule(var triggerAddress: String, var len: Int, var logic: ArrayList<AutoLogic>) {
    fun getBytes(): ByteArray {
        var logicTotalLen = 0
        logic.forEach {
            logicTotalLen += it.getBytes().size
        }

        len = logicTotalLen

        var triggerAddr = ByteUtil.hexStringToBytes(triggerAddress)
        var rule = ByteArray(triggerAddr.size + 1 + logicTotalLen)
        System.arraycopy(triggerAddr, 0, rule, 0, triggerAddr.size)
        System.arraycopy(byteArrayOf(len.toByte()), 0, rule, triggerAddr.size, 1)
        var startIndex = triggerAddr.size + 1
        for (index in 0 until logic.size) {
            var logicSize = logic[index].getBytes().size
            System.arraycopy(logic[index].getBytes(), 0, rule, startIndex, logicSize)
            startIndex += logicSize
        }

        return rule
    }
}

data class AutoLogic(
    var triggerOpcode: String,
    var len: Int,
    var action: ArrayList<AutoAction>
) {
    fun getBytes(): ByteArray {
        var actionTotalLen = 0
        action.forEach {
            actionTotalLen += it.getBytes().size
        }

        len = actionTotalLen

        var triggerOpcodeBytes = ByteUtil.hexStringToBytes(triggerOpcode)
        var logic = ByteArray(triggerOpcodeBytes.size + 1 + actionTotalLen)
        System.arraycopy(
            ByteUtil.hexStringToBytes(triggerOpcode),
            0,
            logic,
            0,
            triggerOpcodeBytes.size
        )
        System.arraycopy(byteArrayOf(len.toByte()), 0, logic, triggerOpcodeBytes.size, 1)
        var startIndex = triggerOpcodeBytes.size + 1
        for (index in 0 until action.size) {
            var actionSize = action[index].getBytes().size
            if (startIndex - (triggerOpcodeBytes.size + 1) + actionSize <= actionTotalLen) {
                System.arraycopy(action[index].getBytes(), 0, logic, startIndex, actionSize)
                startIndex += actionSize
            } else {
                break
            }
        }

        return logic
    }
}

data class AutoAction(
    var triggerValue: String,
    var len: Int,
    var operation: ArrayList<AutoOperation>
) {
    fun getBytes(): ByteArray {
        var operationTotalLen = 0
        operation.forEach {
            operationTotalLen += it.getBytes().size
        }

        len = operationTotalLen
        var action = ByteArray(1 + 1 + operationTotalLen)
        System.arraycopy(ByteUtil.hexStringToBytes(triggerValue), 0, action, 0, 1)
        System.arraycopy(byteArrayOf(len.toByte()), 0, action, 1, 1)

        var startIndex = 2
        for (index in 0 until operation.size) {
            var operationLen = operation[index].getBytes().size
            if (startIndex - 2 + operationLen <= operationTotalLen) {
                System.arraycopy(
                    operation[index].getBytes(),
                    0,
                    action,
                    startIndex,
                    operationLen
                )
                startIndex += operationLen
            } else {
                break
            }
        }

        return action
    }
}

data class AutoOperation(
    var excutorAddr: String,
    var excutorOpcode: String,
    var len: Int,
    var value: String
) {
    fun getBytes(): ByteArray {
        len = ByteUtil.hexStringToBytes(value).size
        var operation = ByteArray(2 + ByteUtil.hexStringToBytes(excutorOpcode).size + 1 + len)
        System.arraycopy(
            ByteUtil.hexStringToBytes(excutorAddr),
            0,
            operation,
            0,
            ByteUtil.hexStringToBytes(excutorAddr).size
        )
        System.arraycopy(
            ByteUtil.hexStringToBytes(excutorOpcode),
            0,
            operation,
            ByteUtil.hexStringToBytes(excutorAddr).size,
            ByteUtil.hexStringToBytes(excutorOpcode).size
        )
        System.arraycopy(
            byteArrayOf(len.toByte()),
            0,
            operation,
            ByteUtil.hexStringToBytes(excutorAddr).size + ByteUtil.hexStringToBytes(excutorOpcode).size,
            1
        )
        System.arraycopy(
            ByteUtil.hexStringToBytes(value),
            0,
            operation,
            ByteUtil.hexStringToBytes(excutorAddr).size + ByteUtil.hexStringToBytes(excutorOpcode).size + 1,
            ByteUtil.hexStringToBytes(value).size
        )

        return operation
    }
}