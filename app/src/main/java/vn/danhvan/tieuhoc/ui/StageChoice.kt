package vn.danhvan.tieuhoc.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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

/** Lưới hình đếm: [count] biểu tượng, mỗi hàng 5 hình. */
@Composable
private fun EmojiGrid(emoji: String, count: Int, emojiSize: Int = 36) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        for (row in (1..count).chunked(5)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (i in row) {
                    Text(emoji, fontSize = emojiSize.sp)
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
            Box(
                modifier = Modifier.fillMaxWidth()
                    .shadow(6.dp, RoundedCornerShape(20.dp)).clip(RoundedCornerShape(20.dp))
                    .background(Color.White).padding(12.dp),
                contentAlignment = Alignment.Center,
            ) {
                EmojiGrid(emoji = question.emoji, count = question.targetNumber)
            }
            Spacer(modifier = Modifier.weight(1f))
            OptionGrid(question, answered, picked, pick)
        }
    }
}

/**
 * Phần 3, DẠNG GHÉP (chốt 2026-10-29): 1 hình lớn gồm 4 hình nhỏ 2x2,
 * mỗi ô ghi "Hình 1".."Hình 4" và chứa 1 số lượng đồ vật CÙNG loại,
 * số lượng mỗi ô khác nhau, 1 ô đúng bằng số đã cho. Đáp án là 4 nút
 * chữ "Hình 1".."Hình 4", không phải nút hình riêng lẻ.
 */
@Composable
fun Stage3Content(
    question: CountQuestion,
    tts: VietnameseTts,
    reviewOnly: Boolean,
    onAnswer: (Boolean, String) -> Unit,
) {
    AnswerState(question, reviewOnly, onAnswer) { answered, picked, pick ->
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
            PromptCard("Số: ${question.targetNumber}", fontSize = 56)
            Spacer(modifier = Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                for (row in 0 until 2) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        for (col in 0 until 2) {
                            val idx = row * 2 + col
                            Box(
                                modifier = Modifier.weight(1f)
                                    .shadow(4.dp, RoundedCornerShape(16.dp))
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White).padding(10.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Hình ${idx + 1}", fontSize = 22.sp, fontWeight = FontWeight.Black)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    EmojiGrid(
                                        emoji = question.emoji,
                                        count = question.quadrants.getOrNull(idx) ?: 0,
                                        emojiSize = 26,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            SpeakButton(textToSpeak = question.speakText, tts = tts, label = "🔊 Nghe số")
            Spacer(modifier = Modifier.height(10.dp))
            OptionGrid(question, answered, picked, pick)
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

/** Phần 6: phép cộng, kết quả luôn dưới 20. */
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
