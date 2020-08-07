package qk.sdk.mesh.meshsdk

import org.junit.Test

import org.junit.Assert.*
import qk.sdk.mesh.meshsdk.util.LongLog

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
//        assertEquals(4, 2 + 2)
        var s:String = "";
        for (i in 1..4000){
            s+="我"
        }
        LongLog.d("Test",s);
    }
}
