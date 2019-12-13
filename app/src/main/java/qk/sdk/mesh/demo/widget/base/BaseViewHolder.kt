package com.styd.crm.adapter.base

import android.util.SparseArray
import android.view.View
import androidx.recyclerview.widget.RecyclerView

open class BaseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private var mView: SparseArray<View>? = null

    init {
        mView = SparseArray()
    }

    fun <T : View> getView(viewId: Int): T {
        //对已有的view做缓存
        var view: View? = mView?.get(viewId)
        if (view == null) {
            view = itemView.findViewById(viewId)
            mView?.put(viewId, view)
        }

        return view as T
    }

    /**
     * 设置条目点击事件
     */
    fun setOnItemClickListener(listener: View.OnClickListener) {
        itemView.setOnClickListener(listener)
    }

    /**
     * 设置条目长按事件
     */
    fun setOnItemLongClickListener(listener: View.OnLongClickListener) {
        itemView.setOnLongClickListener(listener)
    }

}