package vn.danhvan.tieuhoc.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vn.danhvan.tieuhoc.audio.VietnameseTts
import vn.danhvan.tieuhoc.data.CountQuestion
import vn.danhvan.tieuhoc.data.DOT_COUNT_FROM

/**
 * Nội dung 7 phần của Bé Tập Đếm. Mỗi phần nhận 1 CountQuestion đã sinh
 * sẵn (không lặp trong 1 lần vào phần, do StageScreen giữ danh sách câu).
 * - reviewOnly: chỉ xem lại câu đã trả lời, nút tắt, đáp án đúng xanh lá.
 * - Chọn đáp án xong là dừng đọc luôn, giống khung của Bé Đánh Vần.
 * - Phần 1, 4, 5, 6, 7 tự đọc câu hỏi bằng giọng vi-VN khi câu mới hiện;
 *   phần 2 đọc hướng dẫn chung; phần 3 chỉ nhìn số, không tự đọc số
 *   (bé bấm "Nghe lại" nếu muốn nghe số).
 */

@Composable
private fun AnswerState(
    question: CountQuestion,
    reviewOnly: Boolean,
    onAnswer: (Boolean, String) -> Unit,
    content: @Composable (answered: Boolean, picked: String?, pick: (String) -> Unit) -> Unit,
) {
    var answered by remember(question.key) { mutableStateOf(false) }
    var pickedOpt by remember(question.key) { mutableStateOf<String?>(null) }
    content(answered || reviewOnly, pickedOpt) { opt ->
        if (!answered && !reviewOnly) {
            answered = true
            pickedOpt = opt
            onAnswer(opt == question.correctLabel, question.correctLabel)
        }
    }
}

@Composable
private fun OptionGrid(
    question: CountQuestion,
    answered: Boolean,
    picked: String?,
    onPick: (String) -> Unit,
) {
    AnswerGrid2x2(
        options = question.options,
        enabled = !answered,
        selected = picked,
        correctOption = question.correctLabel,
        answered = answered,
        onPick = onPick,
    )
}

@Composable
private fun PromptCard(text: String, fontSize: Int = 44) {
    Box(
        modifier = Modifier.fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(20.dp)).clip(RoundedCornerShape(20.dp))
            .background(Color.White).padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, fontSize = fontSize.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
    }
}

/**
 * Co đều hình theo số lượng để lưới không cao quá, không đẩy đáp án
 * xuống khỏi màn hình. Mọi hình trong 1 lần hiển thị luôn cùng cỡ.
 * 1-10 hình: giữ cỡ gốc. 11-15 hình: 75%. 16-20 hình: 60%.
 * Ví dụ phần 2: 36sp -> 27sp -> 21sp. Phần 3: 26sp -> 19sp -> 15sp.
 * Ở phần 3, caller phải tính cỡ 1 lần theo ô nhiều hình nhất rồi truyền
 * chung cho cả 4 ô, để 4 ô đều nhau, không ô to ô nhỏ.
 */
private fun scaledEmojiSize(count: Int, baseSize: Int): Int = when {
    count <= 10 -> baseSize
    count <= 15 -> (baseSize * 0.75f).toInt()
    else -> (baseSize * 0.6f).toInt()
}

/** Lưới hình đếm: [count] biểu tượng, mỗi hàng 5 hình, cùng 1 cỡ. */
@Composable
private fun EmojiGrid(emoji: String, count: Int, emojiSize: Int = 36) {
    val spacing = if (count > 15) 2.dp else 4.dp
    Column(verticalArrangement = Arrangement.spacedBy(spacing), horizontalAlignment = Alignment.CenterHorizontally) {
        for (row in (1..count).chunked(5)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (i in row) {
                    Text(emoji, fontSize = emojiSize.sp)
                }
            }
        }
    }
}

/**
 * Lưới chấm đếm cho số lượng lớn (từ 21 trở lên): mỗi hàng ĐÚNG 10
 * chấm, hàng cách hàng rõ hơn, để bé đếm theo chục (4 hàng đủ là 40,
 * cộng hàng lẻ) thay vì đếm từng chấm tới 50 hay 100.
 *
 * Tối ưu 2026-11-03 theo phản hồi "chấm bé quá": không dùng ký tự ●
 * nữa, vì ở cùng cỡ chữ thì đường kính thật của ký tự nhỏ hơn cỡ
 * chữ nhiều nên nhìn lọt thỏm trong ô. Chấm giờ là hình tròn đặc
 * vẽ bằng Box, cỡ lấy theo chiều rộng của ô: đúng 10 chấm lấp đầy
 * chiều rộng ô (trừ khoảng cách giữa các chấm), nhưng chặn trên ở
 * [maxDotSizeDp] để không to quá. Ở phần 2 (ô rộng) chấm tới 26dp,
 * ở phần 3 (ô chỉ bằng nửa màn hình) chấm tới khoảng 13dp là đã
 * lấp đầy ô, không thể to hơn nếu vẫn giữ 10 chấm/hàng.
 */
@Composable
private fun DotGrid(count: Int, maxDotSizeDp: Int, spacingDp: Int = 4) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val spacing = spacingDp.dp
        val rowSpacing = spacing + 2.dp
        val rows = (count + 9) / 10
        val widthBased = ((maxWidth.value - spacing.value * 9) / 10).dp
        // Cỡ theo chiều cao còn lại: lưới không được cao quá phần
        // còn lại của màn hình, để đáp án luôn hiện cùng hình chứ
        // không bị hình đẩy xuống / che đi. Nếu chiều cao không
        // giới hạn thì chỉ dùng cỡ theo chiều rộng và cỡ tối đa.
        val heightBased = if (maxHeight.value < Float.POSITIVE_INFINITY && rows > 0) {
            ((maxHeight.value - rowSpacing.value * (rows - 1)) / rows).dp
        } else {
            maxDotSizeDp.dp
        }
        val dotSize = minOf(widthBased, maxDotSizeDp.dp, heightBased).coerceAtLeast(6.dp)
        Column(
            verticalArrangement = Arrangement.spacedBy(rowSpacing),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Mỗi hàng là 1 chục, tô màu xen kẽ theo hàng (đen /
            // xanh dương) để bé đếm theo chục dễ hơn: nhìn màu là
            // biết đã đếm hết 1 hàng 10 chấm hay chưa, thay vì
            // đếm từng chấm từ đầu tới 50 hay 100.
            for ((rowIdx, row) in (1..count).chunked(10).withIndex()) {
                val rowColor = if (rowIdx % 2 == 0) Color(0xFF212121) else Color(0xFF1565C0)
                Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                    for (i in row) {
                        Box(
                            modifier = Modifier.size(dotSize)
                                .clip(CircleShape)
                                .background(rowColor),
                        )
                    }
                }
            }
        }
    }
}

/** Phần 1: nghe số, chọn đúng số. */
@Composable
fun Stage1Content(
    question: CountQuestion,
    tts: VietnameseTts,
    reviewOnly: Boolean,
    onAnswer: (Boolean, String) -> Unit,
) {
    if (!reviewOnly) {
        LaunchedEffect(question.key) { tts.speak(question.speakText) }
    }
    AnswerState(question, reviewOnly, onAnswer) { answered, picked, pick ->
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.weight(1f))
            SpeakButton(textToSpeak = question.speakText, tts = tts)
            Spacer(modifier = Modifier.height(12.dp))
            OptionGrid(question, answered, picked, pick)
        }
    }
}

/** Phần 2: nhìn hình, đếm rồi chọn đúng số. */
@Composable
fun Stage2Content(
    question: CountQuestion,
    tts: VietnameseTts,
    reviewOnly: Boolean,
    onAnswer: (Boolean, String) -> Unit,
) {
    if (!reviewOnly) {
        LaunchedEffect(question.key) { tts.speak(question.speakText) }
    }
    AnswerState(question, reviewOnly, onAnswer) { answered, picked, pick ->
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
            // Khung hình chỉ chiếm tối đa phần còn lại, không được đẩy
            // đáp án xuống khỏi màn hình. Hình đã co theo số lượng ở trên.
            Box(
                modifier = Modifier.weight(1f, fill = false).fillMaxWidth()
                    .shadow(6.dp, RoundedCornerShape(20.dp)).clip(RoundedCornerShape(20.dp))
                    .background(Color.White).padding(12.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (question.targetNumber >= DOT_COUNT_FROM) {
                    DotGrid(
                        count = question.targetNumber,
                        // Phần 2 cần hiển thị tốt nhất cho dễ đếm:
                        // chấm to nhất có thể, chiều cao còn lại
                        // trong DotGrid sẽ tự co lại nếu màn hình
                        // thấp, nên đáp án vẫn hiện cùng hình.
                        maxDotSizeDp = if (question.targetNumber <= 50) 26 else 22,
                        spacingDp = 6,
                    )
                } else {
                    EmojiGrid(
                        emoji = question.emoji,
                        count = question.targetNumber,
                        emojiSize = scaledEmojiSize(question.targetNumber, 36),
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            OptionGrid(question, answered, picked, pick)
        }
    }
}

/**
 * Phần 3, DẠNG GHÉP: 4 ô hình 2x2, mỗi ô chứa 1 số lượng đồ vật
 * CÙNG loại, số lượng mỗi ô khác nhau, 1 ô đúng bằng số cần tìm.
 *
 * Đổi theo yêu cầu 2026-11-03, để tiết kiệm hiển thị tối đa:
 * - Không còn ô báo số ở trên cùng, không còn hàng đáp án riêng
 *   "Hình 1".."Hình 4" ở dưới cùng.
 * - 4 ô hình tự lấp đầy màn hình (mỗi ô chiếm 1/4 màn hình),
 *   căn đều nhau, bấm trực tiếp vào ô hình là trả lời luôn.
 * - Số cần tìm chỉ còn qua giọng đọc: câu mới hiện là tự đọc
 *   số cần tìm, bé bấm "Nghe số" để nghe lại. Không hiển thị
 *   số cần tìm bằng chữ nữa, đúng yêu cầu bỏ ô báo số.
 * - Sau khi trả lời: ô đúng viền xanh lá, ô bé bấm sai viền đỏ.
 */
@Composable
fun Stage3Content(
    question: CountQuestion,
    tts: VietnameseTts,
    reviewOnly: Boolean,
    onAnswer: (Boolean, String) -> Unit,
) {
    if (!reviewOnly) {
        LaunchedEffect(question.key) { tts.speak(question.speakText) }
    }
    AnswerState(question, reviewOnly, onAnswer) { answered, picked, pick ->
        val correctIdx = question.correctLabel.removePrefix("Hình ").toIntOrNull()?.minus(1) ?: -1
        val maxQuadrant = question.quadrants.maxOrNull() ?: 0
        // Ô giờ lớn gấp đôi trước (lấp màn hình), nên emoji cũng
        // to hơn trước (cỡ gốc 36 thay vì 26) cho bé đếm dễ.
        val quadrantEmojiSize = scaledEmojiSize(maxQuadrant, 36)
        val quadrantUseDots = maxQuadrant >= DOT_COUNT_FROM
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) {
                for (row in 0 until 2) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                    ) {
                        for (col in 0 until 2) {
                            val idx = row * 2 + col
                            val isCorrect = answered && idx == correctIdx
                            val isWrongPick = answered && picked == "Hình ${idx + 1}" && idx != correctIdx
                            val bg = when {
                                isCorrect -> Color(0xFFE8F5E9)
                                isWrongPick -> Color(0xFFFFEBEE)
                                else -> Color.White
                            }
                            Box(
                                modifier = Modifier.weight(1f).fillMaxHeight()
                                    .shadow(4.dp, RoundedCornerShape(16.dp))
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(bg)
                                    .border(
                                        width = if (isCorrect || isWrongPick) 5.dp else 0.dp,
                                        color = if (isCorrect) KidGreen else if (isWrongPick) Color.Red else Color.Transparent,
                                        shape = RoundedCornerShape(16.dp),
                                    )
                                    .clickable(enabled = !answered) { pick("Hình ${idx + 1}") }
                                    .padding(10.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (quadrantUseDots) {
                                    DotGrid(
                                        count = question.quadrants.getOrNull(idx) ?: 0,
                                        maxDotSizeDp = if (maxQuadrant > 50) 14 else 16,
                                        spacingDp = 3,
                                    )
                                } else {
                                    EmojiGrid(
                                        emoji = question.emoji,
                                        count = question.quadrants.getOrNull(idx) ?: 0,
                                        emojiSize = quadrantEmojiSize,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            SpeakButton(textToSpeak = question.speakText, tts = tts, label = "🔊 Nghe số")
        }
    }
}

/** Phần 4: dãy liên tiếp, trống 1 ô ở giữa, tìm số còn trống. */
@Composable
fun Stage4Content(
    question: CountQuestion,
    tts: VietnameseTts,
    reviewOnly: Boolean,
    onAnswer: (Boolean, String) -> Unit,
) {
    if (!reviewOnly) {
        LaunchedEffect(question.key) { tts.speak(question.speakText) }
    }
    AnswerState(question, reviewOnly, onAnswer) { answered, picked, pick ->
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                for ((i, n) in question.sequence.withIndex()) {
                    val isBlank = i == question.blankIndex
                    Box(
                        modifier = Modifier.weight(1f).size(64.dp)
                            .shadow(4.dp, RoundedCornerShape(14.dp))
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isBlank) Color(0xFFFFF8E1) else Color.White)
                            .border(3.dp, if (isBlank) KidOrange else Color.Black, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(if (isBlank) "?" else "$n", fontSize = 36.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            SpeakButton(textToSpeak = question.speakText, tts = tts)
            Spacer(modifier = Modifier.height(12.dp))
            OptionGrid(question, answered, picked, pick)
        }
    }
}

/** Phần 5: dãy bất kỳ, đáp án là cặp "lớn nhất và nhỏ nhất". */
@Composable
fun Stage5Content(
    question: CountQuestion,
    tts: VietnameseTts,
    reviewOnly: Boolean,
    onAnswer: (Boolean, String) -> Unit,
) {
    if (!reviewOnly) {
        LaunchedEffect(question.key) { tts.speak(question.speakText) }
    }
    AnswerState(question, reviewOnly, onAnswer) { answered, picked, pick ->
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                for (n in question.sequence) {
                    Box(
                        modifier = Modifier.weight(1f).size(64.dp)
                            .shadow(4.dp, RoundedCornerShape(14.dp))
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White)
                            .border(3.dp, Color.Black, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("$n", fontSize = 34.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            SpeakButton(textToSpeak = question.speakText, tts = tts)
            Spacer(modifier = Modifier.height(12.dp))
            OptionGrid(question, answered, picked, pick)
        }
    }
}

/** Phần 6: phép cộng, kết quả luôn trong khoảng của cấp độ. */
@Composable
fun Stage6Content(
    question: CountQuestion,
    tts: VietnameseTts,
    reviewOnly: Boolean,
    onAnswer: (Boolean, String) -> Unit,
) {
    if (!reviewOnly) {
        LaunchedEffect(question.key) { tts.speak(question.speakText) }
    }
    AnswerState(question, reviewOnly, onAnswer) { answered, picked, pick ->
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.weight(1f))
            PromptCard(question.equation, fontSize = 52)
            Spacer(modifier = Modifier.height(16.dp))
            SpeakButton(textToSpeak = question.speakText, tts = tts)
            Spacer(modifier = Modifier.height(12.dp))
            OptionGrid(question, answered, picked, pick)
        }
    }
}

/** Phần 7: điền vào ô ?, kiểu tìm x, CHỈ phép cộng, không phép trừ. */
@Composable
fun Stage7Content(
    question: CountQuestion,
    tts: VietnameseTts,
    reviewOnly: Boolean,
    onAnswer: (Boolean, String) -> Unit,
) {
    if (!reviewOnly) {
        LaunchedEffect(question.key) { tts.speak(question.speakText) }
    }
    AnswerState(question, reviewOnly, onAnswer) { answered, picked, pick ->
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.weight(1f))
            PromptCard(question.equation, fontSize = 52)
            Spacer(modifier = Modifier.height(16.dp))
            SpeakButton(textToSpeak = question.speakText, tts = tts)
            Spacer(modifier = Modifier.height(12.dp))
            OptionGrid(question, answered, picked, pick)
        }
    }
}
