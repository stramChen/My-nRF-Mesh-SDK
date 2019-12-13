package qk.sdk.mesh.meshsdk.bean

import androidx.annotation.MainThread
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer

import no.nordicsemi.android.meshprovisioner.transport.MeshMessage

class MeshMessageLiveData : SingleLiveData<MeshMessage>() {

    override fun postValue(value: MeshMessage?) {
        super.postValue(value)
    }

    @MainThread
    override fun observe(owner: LifecycleOwner, observer: Observer<in MeshMessage>) {
        super.observe(owner, observer)
    }
}
