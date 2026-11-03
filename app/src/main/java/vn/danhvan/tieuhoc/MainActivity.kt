package vn.danhvan.tieuhoc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import vn.danhvan.tieuhoc.audio.AnswerSounds
import vn.danhvan.tieuhoc.audio.VietnameseTts
import vn.danhvan.tieuhoc.data.ProgressStore
import vn.danhvan.tieuhoc.data.CountStages
import vn.danhvan.tieuhoc.ui.DanhVanTheme
import vn.danhvan.tieuhoc.ui.HomeScreen
import vn.danhvan.tieuhoc.ui.StageScreen

/**
 * Mở app là vào thi luôn: KHÔNG màn hình đăng nhập, KHÔNG tài khoản,
 * KHÔNG Firebase, KHÔNG online, KHÔNG xếp hạng, KHÔNG quảng cáo.
 * Giọng đọc tiếng Việt (vi-VN) dùng chung cho mọi phần. App tập đếm:
 * 3 cấp độ 1-10 / 11-50 / 51-100, không phép trừ.
 */
class MainActivity : ComponentActivity() {
    private lateinit var tts: VietnameseTts
    private lateinit var sounds: AnswerSounds
    private lateinit var store: ProgressStore

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        store = ProgressStore(this)
        tts = VietnameseTts(this)
        sounds = AnswerSounds(this)
        // Cài đặt đã lưu: âm thanh mặc định bật, cấp độ lần trước
        // chọn ở màn hình chính được giữ nguyên cho cả 7 phần.
        tts.enabled = store.isSoundEnabled()
        sounds.enabled = store.isSoundEnabled()

        setContent {
            DanhVanTheme {
                var currentStage by remember { mutableStateOf<Int?>(null) }
                var currentLevel by remember { mutableStateOf(store.selectedLevel()) }
                var soundEnabled by remember { mutableStateOf(store.isSoundEnabled()) }
                // Chặng tiếp theo sau khi thi xong: về màn chính và tự mở
                // bảng luật của chặng đó, không vào thẳng chặng.
                // Cấp độ giữ nguyên của lần thi vừa xong.
                var autoOpenStage by remember { mutableStateOf<Int?>(null) }
                val stage = currentStage
                if (stage == null) {
                    HomeScreen(
                        stages = CountStages,
                        store = store,
                        tts = tts,
                        selectedLevel = currentLevel,
                        onLevelChange = {
                            currentLevel = it
                            store.setSelectedLevel(it)
                        },
                        soundEnabled = soundEnabled,
                        onSoundChange = {
                            soundEnabled = it
                            store.setSoundEnabled(it)
                            tts.enabled = it
                            sounds.enabled = it
                            if (!it) tts.stop()
                        },
                        onStage = { pickedStage, pickedLevel ->
                            currentStage = pickedStage
                            currentLevel = pickedLevel
                            store.setSelectedLevel(pickedLevel)
                        },
                        autoOpenStage = autoOpenStage,
                        onAutoOpenShown = { autoOpenStage = null },
                    )
                } else {
                    BackHandler { currentStage = null }
                    val stageName = CountStages
                        .firstOrNull { it.number == stage }?.ten
                        ?: "Phần $stage"
                    StageScreen(
                        stage = stage,
                        level = currentLevel,
                        stageName = "Phần $stage: $stageName",
                        tts = tts,
                        sounds = sounds,
                        store = store,
                        onBack = { currentStage = null },
                        onNextStage = {
                            if (stage < 7) {
                                currentStage = null
                                autoOpenStage = stage + 1
                            } else {
                                currentStage = null
                            }
                        },
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!isChangingConfigurations && ::tts.isInitialized) tts.shutdown()
        if (!isChangingConfigurations && ::sounds.isInitialized) sounds.release()
    }
}
