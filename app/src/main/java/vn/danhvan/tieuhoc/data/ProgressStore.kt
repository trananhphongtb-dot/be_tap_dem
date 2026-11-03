package vn.danhvan.tieuhoc.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Hồ sơ trên máy, vì app KHÔNG đăng nhập, KHÔNG tài khoản, KHÔNG online.
 * Lưu theo từng cấp độ: chặng nào đã qua ở cấp nào, và câu nào đã
 * dùng ở từng chặng của cấp đó (luật không lặp).
 * Tiến độ cũ (chưa chia cấp độ, khóa "passed_1"...) không dùng nữa,
 * vì phạm vi số của cấp độ mới khác hẳn phạm vi 1-20 cũ.
 */
class ProgressStore(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("betapdem_offline", Context.MODE_PRIVATE)

    /** Âm thanh: mặc định BẬT, gạt tắt trong Cài đặt thì lưu lại. */
    fun isSoundEnabled(): Boolean = prefs.getBoolean("sound_enabled", true)

    fun setSoundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("sound_enabled", enabled).apply()
    }

    /** Cấp độ đang chọn ở màn hình chính, áp dụng cho cả 7 phần. */
    fun selectedLevel(): Int = prefs.getInt("selected_level", 1).coerceIn(1, 3)

    fun setSelectedLevel(level: Int) {
        prefs.edit().putInt("selected_level", level.coerceIn(1, 3)).apply()
    }

    fun isPassed(stage: Int, level: Int): Boolean =
        prefs.getBoolean("passed_L${level}_$stage", false)

    fun markPassed(stage: Int, level: Int) {
        prefs.edit().putBoolean("passed_L${level}_$stage", true).apply()
    }

    fun allPassed(level: Int): Boolean = (1..7).all { isPassed(it, level) }

    /** Reset riêng cấp độ đã qua hết 7 phần, các cấp độ khác giữ nguyên. */
    fun resetLevel(level: Int) {
        val editor = prefs.edit()
        for (i in 1..7) {
            editor.remove("passed_L${level}_$i")
            editor.remove(usedKey(i, level))
        }
        editor.apply()
    }

    /** Reset hết mọi cấp độ, dùng khi cần chơi lại từ đầu. */
    fun resetAll() {
        val editor = prefs.edit()
        for (level in 1..3) {
            for (i in 1..7) {
                editor.remove("passed_L${level}_$i")
                editor.remove(usedKey(i, level))
            }
        }
        editor.apply()
    }

    fun isUnlocked(stage: Int, level: Int): Boolean {
        if (UnlockRules.UNLOCK_ALL) return true
        if (stage <= 1) return true
        return isPassed(stage - 1, level)
    }

    private fun usedKey(stage: Int, level: Int) = "used_L${level}_stage_$stage"

    fun usedIds(stage: Int, level: Int): Set<String> =
        prefs.getStringSet(usedKey(stage, level), emptySet())?.toSet() ?: emptySet()

    fun markUsed(stage: Int, level: Int, id: String) {
        val set = usedIds(stage, level).toMutableSet()
        set += id
        prefs.edit().putStringSet(usedKey(stage, level), set).apply()
    }

    fun clearUsed(stage: Int, level: Int) {
        prefs.edit().remove(usedKey(stage, level)).apply()
    }
}
