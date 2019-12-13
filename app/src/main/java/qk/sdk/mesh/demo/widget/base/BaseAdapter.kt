package com.styd.crm.adapter.base

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import qk.sdk.mesh.demo.util.MultipleType
import qk.sdk.mesh.demo.widget.base.OnItemClickListener
import qk.sdk.mesh.demo.widget.base.OnItemLongClickListener
import qk.sdk.mesh.demo.widget.base.OnPreventQuickClickListener

abstract class BaseAdapter<T>(mContext: Context, var mData: ArrayList<T>?, private var mLayoutId: Int) : RecyclerView.Adapter<BaseViewHolder>() {

    private var mInflater: LayoutInflater? = null
    private var mTypeSupport: MultipleType<T>? = null

    private var mItemClickListener: OnItemClickListener<T>? = null
    private var mItemLongClickListener: OnItemLongClickListener<T>? = null

    init {
        mInflater = LayoutInflater.from(mContext)
    }

    open fun setData(data: ArrayList<T>?) {
        mData?.apply {
            clear()
            if (!data.isNullOrEmpty()) {
                addAll(data)
            }
        }
        notifyDataSetChanged()
    }

    open fun addData(data: ArrayList<T>?) {
        mData?.apply {
            if (!data.isNullOrEmpty()) {
                addAll(data)
            }
        }
        notifyDataSetChanged()
    }

    fun dataIsNotEmpty() = mData?.size ?: 0 > 0

    /**
     * 多类item布局
     */
    constructor(context: Context, data: ArrayList<T>, typeSupport: MultipleType<T>) : this(context, data, -1) {
        this.mTypeSupport = typeSupport
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        if (mTypeSupport != null) {
            mLayoutId = viewType
        }
        val view = mInflater?.inflate(mLayoutId, parent, false)
        return BaseViewHolder(view!!)
    }

    override fun getItemViewType(position: Int): Int {
        return if (mData == null) {
            super.getItemViewType(position)
        } else {
            mTypeSupport?.getLayoutId(mData!![position], position)
                    ?: super.getItemViewType(position)
        }
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {

        mData?.let {
            bindData(holder, it[position], position)
        }

        holder.setOnItemClickListener(OnPreventQuickClickListener {
            mData?.let {
                mItemClickListener?.onItemClick(it[position], position)
            }
        })

        holder.setOnItemLongClickListener(View.OnLongClickListener {
            mData?.let {
                mItemLongClickListener?.onItemLongClick(it[position], position)
            } ?: true
        })

    }

    override fun getItemCount(): Int {
        return mData?.size ?: 0
    }

    fun setOnItemClickListener(itemClickListener: OnItemClickListener<T>) {
        this.mItemClickListener = itemClickListener
    }

    fun setOnItemLongClickListener(itemLongClickListener: OnItemLongClickListener<T>) {
        this.mItemLongClickListener = itemLongClickListener
    }

    protected abstract fun bindData(holder: BaseViewHolder, data: T, position: Int)
}