package vn.danhvan.tieuhoc.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vn.danhvan.tieuhoc.audio.AnswerSounds
import vn.danhvan.tieuhoc.audio.VietnameseTts
import vn.danhvan.tieuhoc.data.CountGenerator
import vn.danhvan.tieuhoc.data.CountQuestion
import vn.danhvan.tieuhoc.data.GameRules
import vn.danhvan.tieuhoc.data.ProgressStore

/**
 * Khung chung mọi phần của Bé Tập Đếm, giữ khung của Bé Đánh Vần:
 * - Nền toàn màn theo phần, điểm ở góc phải, thỏ chạy về cà rốt.
 * - Không hiện chữ "Đúng rồi!" / "Chưa đúng.", chỉ giọng nói + màu đáp án.
 * - Điểm riêng từng phần, vào phần là điểm về 0.
 * - Đúng +1, sai 2 câu trừ 1 (chỉ trừ ở câu sai thứ 2), 10 điểm thì qua.
 * - Chọn đáp án xong là DỪNG ĐỌC LUÔN.
 * - Câu trước / Câu tiếp: chỉ xem lại câu cũ, không chấm điểm lại.
 * - Không lặp câu trong 1 lần vào phần: StageScreen giữ danh sách câu đã
 *   ra và khóa key của chúng; sang lần vào phần khác thì sinh ngẫu nhiên
 *   lại, câu có thể trùng lại là chấp nhận được với app sinh câu này.
 */
@Composable
fun StageScreen(
    stage: Int,
    stageName: String,
    tts: VietnameseTts,
    sounds: AnswerSounds,
    store: ProgressStore,
    onBack: () -> Unit,
    onNextStage: () -> Unit,
) {
    var score by remember { mutableIntStateOf(0) }
    var wrongCount by remember { mutableIntStateOf(0) }
    var qIndex by remember { mutableIntStateOf(0) }
    var answeredIndices by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var completed by remember { mutableStateOf(false) }

    // Danh sách câu của lần vào phần này, sinh dần khi bé bấm "Phần tiếp".
    // Dùng danh sách thường (không phải state) để không ghi state trong lúc
    // compose: qIndex đã là state nên mỗi lần bấm Tiếp/Trước đều compose lại
    // và đọc được câu vừa thêm ngay trong lần compose đó.
    val questions = remember(stage) { mutableListOf<CountQuestion>() }
    val usedKeys = remember(stage) { mutableSetOf<String>() }
    if (qIndex >= questions.size) {
        val q = CountGenerator.generate(stage, usedKeys)
        questions += q
        usedKeys += q.key
    }
    val question = questions[qIndex]
    val isReview = qIndex in answeredIndices
    val canNext = qIndex in answeredIndices
    val canPrev = qIndex > 0

    DisposableEffect(stage) {
        onDispose { tts.stop() }
    }

    fun answer(correct: Boolean, correctText: String) {
        if (isReview || qIndex in answeredIndices) return
        if (completed) return
        tts.stop()
        // Phản hồi nói, giống Bé Đánh Vần V20; sounds giữ lại nhưng không dùng.
        if (correct) tts.speak("Đúng rồi") else tts.speak("Sai rồi")
        val (newScore, newWrong) = GameRules.applyAnswer(score, wrongCount, correct)
        score = newScore
        wrongCount = newWrong
        answeredIndices = answeredIndices + qIndex
        if (newScore >= GameRules.POINTS_TO_PASS) {
            store.markPassed(stage)
            completed = true
        }
    }

    fun next() {
        tts.stop()
        qIndex++
    }

    fun prev() {
        if (qIndex <= 0) return
        tts.stop()
        qIndex--
    }

    Box(modifier = Modifier.fillMaxSize()) {
        StageBackgroundFull(stage)
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            ScoreHeader(stageName = "Phần $stage", score = score, onBack = {
                tts.stop()
                onBack()
            })
            Spacer(modifier = Modifier.height(10.dp))
            if (isReview) {
                Text(
                    "Câu này đã trả lời rồi, chỉ xem lại thôi, không tính điểm nữa.",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = KidOrange,
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                key(question.key) {
                    when (stage) {
                        1 -> Stage1Content(question, tts, isReview, onAnswer = { c, _ -> answer(c, question.correctLabel) })
                        2 -> Stage2Content(question, tts, isReview, onAnswer = { c, _ -> answer(c, question.correctLabel) })
                        3 -> Stage3Content(question, tts, isReview, onAnswer = { c, _ -> answer(c, question.correctLabel) })
                        4 -> Stage4Content(question, tts, isReview, onAnswer = { c, _ -> answer(c, question.correctLabel) })
                        5 -> Stage5Content(question, tts, isReview, onAnswer = { c, _ -> answer(c, question.correctLabel) })
                        6 -> Stage6Content(question, tts, isReview, onAnswer = { c, _ -> answer(c, question.correctLabel) })
                        else -> Stage7Content(question, tts, isReview, onAnswer = { c, _ -> answer(c, question.correctLabel) })
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            PrevNextRow(canPrev = canPrev, canNext = canNext, onPrev = { prev() }, onNext = { next() })
        }
    }

    if (completed) {
        val allDone = store.allPassed()
        Box(
            modifier = Modifier.fillMaxSize().background(Color(0x80000000)),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(0.88f)
                    .shadow(12.dp, RoundedCornerShape(28.dp)).clip(RoundedCornerShape(28.dp))
                    .background(Color(0xFFFFF8E1)).border(4.dp, Color(0xFFFFB300), RoundedCornerShape(28.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (allDone) {
                    Text("🎆", fontSize = 64.sp)
                    Text("Chúc mừng bé!", fontSize = 32.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Bé đã hoàn thành hết 7 phần! Giờ bé quay về màn hình chính chơi lại nhé!",
                        fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    BigButton(
                        text = "🏠 Về nhà",
                        onClick = { tts.stop(); store.resetAll(); onBack() },
                        color = KidBlue, modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    Text("🏆", fontSize = 64.sp)
                    Text("Hoàn thành Phần $stage!", fontSize = 30.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Bé đã đủ 10 điểm. Phần tiếp theo đã mở!",
                        fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        BigButton(
                            text = "🏠 Về nhà",
                            onClick = { tts.stop(); onBack() },
                            color = Color(0xFF757575), modifier = Modifier.weight(1f),
                        )
                        BigButton(
                            text = if (stage < 7) "Chơi tiếp →" else "🏠 Về nhà",
                            onClick = {
                                tts.stop()
                                if (stage < 7) onNextStage() else onBack()
                            },
                            color = KidGreen, modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}
