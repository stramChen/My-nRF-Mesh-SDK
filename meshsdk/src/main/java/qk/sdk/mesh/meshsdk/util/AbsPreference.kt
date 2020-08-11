package qk.sdk.mesh.meshsdk.util

import android.content.Context
import android.content.SharedPreferences

abstract class AbsPreference {
    private var mContext: Context? = null
    private val TAG = "AbsPreference"
    private var mAppFlag: String = "";

    private lateinit var sPreferences: SharedPreferences

    fun init(appFlag: String, context: Context) {
        mContext = context
        mAppFlag = appFlag;
        sPreferences = context.getSharedPreferences(appFlag, Context.MODE_PRIVATE)
    }

    fun getInstance(): SharedPreferences {
        checkInit()
        return sPreferences
    }

    fun checkInit() {
        if (::sPreferences.isInitialized) {
            sPreferences = mContext?.getSharedPreferences(mAppFlag, Context.MODE_PRIVATE)!!
        }
    }

    /**
     * 清空SharedPreferences
     */
    fun clear() {
        getInstance().edit().clear().apply()
    }

    /**
     * 根据Key移除SharedPreferences
     * @param key key
     */
    fun remove(key: String) {
        getInstance().edit().remove(key).apply()
    }

    /**
     * 保存数据的方法，我们需要拿到保存数据的具体类型，然后根据类型调用不同的保存方法
     *
     * @param key    Key
     * @param object 需要存的引用对象
     * @return 数据是否成功存储
     */
    fun setObject(key: String, obj: Any): Boolean {
        val type = obj.javaClass.simpleName
        val editor = getInstance().edit()
        when (type) {
            "String" -> editor.putString(key, obj as String)
            "Integer" -> editor.putInt(key, obj as Int)
            "Boolean" -> editor.putBoolean(key, obj as Boolean)
            "Float" -> editor.putFloat(key, obj as Float)
            "Long" -> editor.putLong(key, obj as Long)
        }
        return editor.commit() //为保证后面取值的正确性,这里使用同步存储(线程阻塞)commit方法
    }

    /**
     * 得到保存数据的方法，我们根据默认值得到保存的数据的具体类型，然后调用相对于的方法获取值
     *
     * @param key           Key
     * @param defaultObject 默认值
     * @return 取值
     */
    fun getObject(key: String, obj: Any): Any? {
        val type = obj.javaClass.simpleName
        return when (type) {
            "String" -> getInstance().getString(key, obj as String)
            "Integer" -> getInstance().getInt(key, obj as Int)
            "Boolean" -> getInstance().getBoolean(key, obj as Boolean)
            "Float" -> getInstance().getFloat(key, obj as Float)
            "Long" -> getInstance().getLong(key, obj as Long)
            else -> null
        }

    }

}