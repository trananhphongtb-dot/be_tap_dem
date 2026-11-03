package vn.danhvan.tieuhoc.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vn.danhvan.tieuhoc.R
import vn.danhvan.tieuhoc.audio.VietnameseTts
import vn.danhvan.tieuhoc.data.CountLevels
import vn.danhvan.tieuhoc.data.ProgressStore
import vn.danhvan.tieuhoc.data.StageInfo
import vn.danhvan.tieuhoc.data.levelFor
import kotlin.math.roundToInt

/**
 * Màn hình chính của Bé Tập Đếm, giữ bố cục nút của Bé Đánh Vần:
 * - Nền là ảnh Gemini anh gửi (nen_chinh.png), đã có sẵn bảng gỗ câu dẫn,
 *   thỏ lớn, đường mòn có hàng rào, cây, hoa, đá, nấm, giỏ cà rốt.
 * - Chỉ phủ nút số lên đúng vị trí các số trong ảnh để bấm được và để
 *   đổi trạng thái (qua rồi xanh lá có sao, mở xanh dương, khóa xám có
 *   ổ khóa, chặng 7 đĩa gỗ nâu). Nút phủ đúng tâm số trong ảnh nên
 *   số trong ảnh bị nút che, không bị trùng số.
 * - Giọng đọc câu dẫn vẫn đọc khi mở app, bảng luật vẫn là bảng gỗ lớn.
 */
val StageRules = mapOf(
    1 to "Bé hãy nghe số, chọn đúng số đó.",
    2 to "Bé hãy đếm số hình, chọn đúng số đó.",
    3 to "Bé hãy nhìn số, chọn hình có đúng số lượng đó.",
    4 to "Bé hãy tìm số còn trống trong dãy số liên tiếp.",
    5 to "Bé hãy chọn cặp số lớn nhất và số nhỏ nhất trong dãy.",
    6 to "Bé hãy tính phép cộng, chọn đáp án đúng.",
    7 to "Bé hãy tìm số thích hợp điền vào ô dấu hỏi.",
)

private val HomeIntro = "Bé ơi, hãy giúp bạn thỏ vượt chướng ngại vật giành được cà rốt nào!"

// Tâm các số trong ảnh Gemini, theo tỉ lệ màn hình (đo từ ảnh gốc).
private val HomeNodeFracs = listOf(
    0.157f to 0.235f,
    0.53f to 0.252f,
    0.889f to 0.31f,
    0.775f to 0.48f,
    0.48f to 0.667f,
    0.265f to 0.822f,
    0.83f to 0.885f,
)
private val StarShape = GenericShape { size, _ ->
    val cx = size.width / 2f
    val cy = size.height / 2f
    val outer = size.width / 2f
    val inner = outer * 0.65f  // Sao mập V20, cánh dày, trùm hết nút
    var angle = -90.0
    var first = true
    for (i in 0 until 10) {
        val r = if (i % 2 == 0) outer else inner
        val rad = Math.toRadians(angle)
        val x = cx + (r * kotlin.math.cos(rad)).toFloat()
        val y = cy + (r * kotlin.math.sin(rad)).toFloat()
        if (first) { moveTo(x, y); first = false } else { lineTo(x, y) }
        angle += 36.0
    }
    close()
}

@Composable
fun HomeScreen(
    stages: List<StageInfo>,
    store: ProgressStore,
    tts: VietnameseTts,
    selectedLevel: Int,
    onLevelChange: (Int) -> Unit,
    soundEnabled: Boolean,
    onSoundChange: (Boolean) -> Unit,
    onStage: (Int, Int) -> Unit,
    autoOpenStage: Int? = null,
    onAutoOpenShown: () -> Unit = {},
) {
    var selectedStage by remember { mutableStateOf<StageInfo?>(null) }
    var lockedMessage by remember { mutableStateOf<String?>(null) }
    var introSpoken by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var settingsShowDifficulty by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!introSpoken) {
            introSpoken = true
            tts.speak(HomeIntro)
        }
    }

    val displayStages = if (stages.size >= 7) stages.sortedBy { it.number }.take(7) else listOf(
        StageInfo(1, "Nghe số, chọn đúng số", ""),
        StageInfo(2, "Nhìn hình, đếm rồi chọn đúng số", ""),
        StageInfo(3, "Nhìn số, chọn đúng số lượng hình", ""),
        StageInfo(4, "Tìm số còn trống trong dãy", ""),
        StageInfo(5, "Số lớn nhất và số nhỏ nhất", ""),
        StageInfo(6, "Phép cộng", ""),
        StageInfo(7, "Điền vào ô dấu hỏi", ""),
    )

    LaunchedEffect(autoOpenStage) {
        if (autoOpenStage != null) {
            val s = displayStages.firstOrNull { it.number == autoOpenStage }
            if (s != null && store.isUnlocked(s.number, selectedLevel)) {
                selectedStage = s
            }
            onAutoOpenShown()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Nền ảnh Gemini, kéo vừa màn hình để không cắt mất số ở mép.
        Image(
            painter = painterResource(R.drawable.nen_chinh),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds,
        )
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val density = LocalDensity.current
            val wPx = with(density) { maxWidth.toPx() }
            val hPx = with(density) { maxHeight.toPx() }
            val nodeHalfPx = with(density) { 44.dp.toPx() }
            for (stage in displayStages) {
                val idx = stage.number - 1
                if (idx !in HomeNodeFracs.indices) continue
                val frac = HomeNodeFracs[idx]
                val unlocked = store.isUnlocked(stage.number, selectedLevel)
                val passed = store.isPassed(stage.number, selectedLevel)
                // V21: sao mập vàng trùm hết nút, che luôn ổ khóa in sẵn,
                // số nằm bên trong sao (sao che số in sẵn nên phải có số
                // code). Chưa mở: chỉ vùng chạm trong suốt, ổ khóa in sẵn
                // đủ biết. Thi xong (passed): thêm tích tròn béo góc trên
                // phải. Đang thi (mở nhưng chưa qua): chỉ sao, không tích.
                Box(
                    modifier = Modifier
                        .offset {
                            val x = (frac.first * wPx - nodeHalfPx).roundToInt()
                            val y = (frac.second * hPx - nodeHalfPx).roundToInt()
                            IntOffset(x, y)
                        }
                        .size(88.dp)
                        .then(
                            if (unlocked) Modifier.shadow(6.dp, StarShape).clip(StarShape)
                                .background(Color(0xFFFFC107))
                            else Modifier
                        )
                        .clickable {
                            if (unlocked) {
                                selectedStage = stage
                            } else {
                                lockedMessage = "Phần ${stage.number} đang khóa. Bé qua phần trước đã nhé!"
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    if (unlocked) {
                        Text("${stage.number}", fontSize = 32.sp, fontWeight = FontWeight.Black, color = Color(0xFF4E342E))
                    }
                }
                if (passed) {
                    Box(
                        modifier = Modifier
                            .offset {
                                val x = (frac.first * wPx + 30.dp.toPx()).roundToInt()
                                val y = (frac.second * hPx - 30.dp.toPx()).roundToInt()
                                IntOffset(x, y)
                            }
                            .size(32.dp)
                            .shadow(4.dp, CircleShape)
                            .background(Color(0xFF43A047), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("✓", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                }
            }
        }
        // Nút Cài đặt ở góc dưới bên phải màn hình chính, theo yêu
        // cầu 2026-11-03. Bấm vào mở bảng Cài đặt, trong đó có Độ khó
        // và Âm thanh; không còn hàng chọn cấp độ ở mép trên nữa.
        // Cấp độ chọn ở đây là chung cho cả 7 phần, và được lưu lại
        // nên lần sau mở app vẫn giữ nguyên.
        // Nổi bật hơn theo phản hồi 2026-11-03 (trước đây mờ quá):
        // to hơn (72dp), nền vàng cam nổi trên nền chính, viền
        // trắng dày và bóng đậm, kèm nhãn "Cài đặt" bên dưới nút.
        Column(
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .shadow(12.dp, CircleShape).clip(CircleShape)
                    .background(Color(0xFFFFB300))
                    .border(4.dp, Color.White, CircleShape)
                    .clickable { settingsShowDifficulty = false; showSettings = true },
                contentAlignment = Alignment.Center,
            ) {
                Text("⚙️", fontSize = 36.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .shadow(6.dp, RoundedCornerShape(10.dp)).clip(RoundedCornerShape(10.dp))
                    .background(Color.White).padding(horizontal = 10.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Cài đặt", fontSize = 18.sp, fontWeight = FontWeight.Black)
            }
        }
    }

    // Bảng luật LỚN bằng gỗ, không câu giải thích về giọng đọc.
    val picked = selectedStage
    if (picked != null) {
        val rule = StageRules[picked.number] ?: picked.moTa
        LaunchedEffect(picked.number) { tts.speak("Phần ${picked.number}. $rule") }
        Box(
            modifier = Modifier.fillMaxSize().background(Color(0x80000000))
                .clickable { tts.stop(); selectedStage = null },
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(0.88f)
                    .shadow(8.dp, RoundedCornerShape(24.dp)).clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFFF5E6C8)).border(3.dp, Color(0xFF8D6E63), RoundedCornerShape(24.dp))
                    .padding(20.dp),
            ) {
                Text("Phần ${picked.number}: $rule", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Cấp ${selectedLevel}: ${levelFor(selectedLevel).ten} (${levelFor(selectedLevel).min}-${levelFor(selectedLevel).max})",
                    fontSize = 22.sp, fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row {
                    BigButton(
                        text = "← Trang chủ",
                        onClick = { tts.stop(); selectedStage = null },
                        color = Color(0xFF757575), modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.size(10.dp))
                    BigButton(
                        text = "Vào thi →",
                        onClick = { tts.stop(); selectedStage = null; onStage(picked.number, selectedLevel) },
                        color = KidGreen, modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }

    // Bảng Cài đặt: nền mờ, thẻ màu kem viền gỗ, giống bảng luật
    // nhưng sáng và gọn hơn cho bố mẹ bấm. Bấm ngoài thẻ không tắt,
    // phải bấm nút X hoặc "Xong" để không vô tình mất lựa chọn.
    if (showSettings) {
        val currentLevel = levelFor(selectedLevel)
        Box(
            modifier = Modifier.fillMaxSize().background(Color(0x80000000)),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(0.88f)
                    .shadow(12.dp, RoundedCornerShape(28.dp)).clip(RoundedCornerShape(28.dp))
                    .background(Color(0xFFFFF8E1)).border(3.dp, Color(0xFF8D6E63), RoundedCornerShape(28.dp))
                    .padding(20.dp),
            ) {
                if (!settingsShowDifficulty) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(56.dp)
                                .shadow(4.dp, CircleShape).clip(CircleShape)
                                .background(KidGreen),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("⚙️", fontSize = 28.sp)
                        }
                        Spacer(modifier = Modifier.size(12.dp))
                        Text(
                            "Cài đặt", fontSize = 30.sp, fontWeight = FontWeight.Black,
                            modifier = Modifier.weight(1f),
                        )
                        Box(
                            modifier = Modifier.size(44.dp)
                                .shadow(4.dp, CircleShape).clip(CircleShape)
                                .background(Color.White)
                                .clickable { showSettings = false },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("✕", fontSize = 22.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                    // Mục 1: Độ khó. Bấm vào mở ra 3 mức.
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .shadow(4.dp, RoundedCornerShape(20.dp)).clip(RoundedCornerShape(20.dp))
                            .background(Color.White)
                            .clickable { settingsShowDifficulty = true }
                            .padding(16.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🎚️", fontSize = 28.sp)
                            Spacer(modifier = Modifier.size(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Độ khó", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    "Cấp ${currentLevel.id}: ${currentLevel.ten} (${currentLevel.min}-${currentLevel.max})",
                                    fontSize = 20.sp, color = Color(0xFF616161),
                                )
                            }
                            Text("›", fontSize = 32.sp, fontWeight = FontWeight.Black, color = KidGreen)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    // Mục 2: Âm thanh. Mặc định bật, gạt là tắt.
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .shadow(4.dp, RoundedCornerShape(20.dp)).clip(RoundedCornerShape(20.dp))
                            .background(Color.White)
                            .padding(16.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (soundEnabled) "🔊" else "🔇", fontSize = 28.sp)
                            Spacer(modifier = Modifier.size(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Âm thanh", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    if (soundEnabled) "Đang bật, bé nghe giọng đọc" else "Đã tắt, không phát tiếng",
                                    fontSize = 20.sp, color = Color(0xFF616161),
                                )
                            }
                            Switch(
                                checked = soundEnabled,
                                onCheckedChange = { onSoundChange(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = KidGreen,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = Color(0xFFBDBDBD),
                                ),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        "Độ khó đổi ở đây áp dụng cho cả 7 phần, và được lưu lại cho lần sau.",
                        fontSize = 18.sp, color = Color(0xFF616161),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    BigButton(
                        text = "Xong ✓",
                        onClick = { showSettings = false },
                        color = KidGreen, modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    // Trang con: chọn 1 trong 3 mức độ khó.
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.clickable { settingsShowDifficulty = false }
                                .padding(vertical = 6.dp),
                        ) {
                            Text("← Cài đặt", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = KidGreen)
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Text("Chọn độ khó", fontSize = 26.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    for (lv in CountLevels) {
                        val isSelected = lv.id == selectedLevel
                        val desc = when (lv.id) {
                            1 -> "Số nhỏ, cho bé mới bắt đầu"
                            2 -> "Số vừa, bé đếm tới 50"
                            else -> "Số lớn, bé đếm tới 100 bằng chấm"
                        }
                        Box(
                            modifier = Modifier.fillMaxWidth()
                                .shadow(4.dp, RoundedCornerShape(20.dp)).clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) Color(0xFFE8F5E9) else Color.White)
                                .border(
                                    if (isSelected) 3.dp else 2.dp,
                                    if (isSelected) KidGreen else Color(0xFFBDBDBD),
                                    RoundedCornerShape(20.dp),
                                )
                                .clickable {
                                    onLevelChange(lv.id)
                                    settingsShowDifficulty = false
                                }
                                .padding(16.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Cấp ${lv.id}: ${lv.ten}",
                                        fontSize = 24.sp, fontWeight = FontWeight.Bold,
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Số ${lv.min} - ${lv.max}", fontSize = 20.sp)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(desc, fontSize = 18.sp, color = Color(0xFF616161))
                                }
                                if (isSelected) {
                                    Text("✓", fontSize = 30.sp, fontWeight = FontWeight.Black, color = KidGreen)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }
    }

    if (lockedMessage != null) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color(0x80000000))
                .clickable { lockedMessage = null },
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(0.88f)
                    .shadow(8.dp, RoundedCornerShape(24.dp)).clip(RoundedCornerShape(24.dp))
                    .background(Color.White).padding(20.dp),
            ) {
                Text("Phần đang khóa 🔒", fontSize = 24.sp, fontWeight = FontWeight.Black)
                Spacer(modifier = Modifier.height(10.dp))
                Text(lockedMessage!!, fontSize = 22.sp)
                Spacer(modifier = Modifier.height(16.dp))
                BigButton(text = "Đã hiểu", onClick = { lockedMessage = null }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
