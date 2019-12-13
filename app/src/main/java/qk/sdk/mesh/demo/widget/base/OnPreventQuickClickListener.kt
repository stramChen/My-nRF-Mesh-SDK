package qk.sdk.mesh.demo.widget.base

import android.view.View
import java.util.*

open class OnPreventQuickClickListener(
        private var time: Long = 2000L, private var onViewClick: (v: View) -> Unit = {}) : View.OnClickListener {

    private var lastTime: Long = 0

    override fun onClick(v: View?) {
        val currentTime = Calendar.getInstance().timeInMillis
        if (currentTime - lastTime >= time) {
            lastTime = currentTime
            v?.let {
                onViewClick(it)
            }
        }
    }

}