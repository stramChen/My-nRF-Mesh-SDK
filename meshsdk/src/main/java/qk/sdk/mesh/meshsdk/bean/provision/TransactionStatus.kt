package qk.sdk.mesh.meshsdk.bean.provision

class TransactionStatus internal constructor(
    /**
     * Returns the element address of the failed transaction
     */
    var elementAddress: Int,
    /**
     * Returns if incomplete timer expired of the failed transaction
     */
    var isIncompleteTimerExpired: Boolean
)
