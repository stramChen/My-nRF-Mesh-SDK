package qk.sdk.mesh.demo.widget.base

interface OnItemLongClickListener<T> {

    fun onItemLongClick(data: T, position: Int): Boolean

}
