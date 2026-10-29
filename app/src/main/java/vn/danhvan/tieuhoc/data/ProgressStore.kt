package vn.danhvan.tieuhoc.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Hồ sơ trên máy, vì app KHÔNG đăng nhập, KHÔNG tài khoản, KHÔNG online.
 * Lưu: chặng nào đã qua, và câu nào đã dùng ở từng chặng (luật không lặp).
 */
class ProgressStore(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("betapdem_offline", Context.MODE_PRIVATE)

    fun isPassed(stage: Int): Boolean = prefs.getBoolean("passed_$stage", false)

    fun markPassed(stage: Int) {
        prefs.edit().putBoolean("passed_$stage", true).apply()
    }

    fun allPassed(): Boolean = (1..7).all { isPassed(it) }

    /** Reset về như ban đầu khi đã qua hết 7 phần: xóa dấu qua và
     *  xóa lịch sử câu đã dùng của mọi phần, để chơi lại từ đầu. */
    fun resetAll() {
        val editor = prefs.edit()
        for (i in 1..7) {
            editor.remove("passed_$i")
            editor.remove(usedKey(i))
        }
        editor.apply()
    }

    fun isUnlocked(stage: Int): Boolean {
        if (UnlockRules.UNLOCK_ALL) return true
        if (stage <= 1) return true
        return isPassed(stage - 1)
    }

    private fun usedKey(stage: Int) = "used_stage_$stage"

    fun usedIds(stage: Int): Set<String> =
        prefs.getStringSet(usedKey(stage), emptySet())?.toSet() ?: emptySet()

    fun markUsed(stage: Int, id: String) {
        val set = usedIds(stage).toMutableSet()
        set += id
        prefs.edit().putStringSet(usedKey(stage), set).apply()
    }

    fun clearUsed(stage: Int) {
        prefs.edit().remove(usedKey(stage)).apply()
    }
}
