package vn.danhvan.tieuhoc.data

import kotlin.random.Random

/** Danh sách 7 phần, theo đúng chốt của người dùng 2026-10-29. */
val CountStages = listOf(
    StageInfo(1, "Nghe số, chọn đúng số", "Bé hãy nghe số, chọn đúng số đó."),
    StageInfo(2, "Nhìn hình, đếm rồi chọn đúng số", "Bé hãy đếm số hình, chọn đúng số đó."),
    StageInfo(3, "Nhìn số, chọn đúng số lượng hình", "Bé hãy nhìn số, chọn hình có đúng số lượng đó."),
    StageInfo(4, "Tìm số còn trống trong dãy", "Bé hãy tìm số còn trống trong dãy số liên tiếp."),
    StageInfo(5, "Số lớn nhất và số nhỏ nhất", "Bé hãy chọn cặp số lớn nhất và số nhỏ nhất trong dãy."),
    StageInfo(6, "Phép cộng", "Bé hãy tính phép cộng, chọn đáp án đúng."),
    StageInfo(7, "Điền vào ô dấu hỏi", "Bé hãy tìm số thích hợp điền vào ô dấu hỏi."),
)

/**
 * Một câu hỏi đếm, sinh ngẫu nhiên trong phạm vi CHỈ 1-20, KHÔNG phép trừ.
 * - key: khóa không lặp trong 1 lần vào phần (StageScreen giữ danh sách
 *   câu đã ra và không sinh lại key đã có trong lần vào đó).
 * - options: nhãn 4 nút đáp án, đã trộn; correctLabel: nhãn nút đúng.
 * - targetNumber: số đúng (phần 1, 2, 4, 6, 7) hoặc số cần tìm ở phần 3.
 * - sequence: dãy số (phần 4: dãy liên tiếp, phần 5: dãy bất kỳ).
 * - blankIndex: vị trí ô trống ở phần 4, luôn ở giữa, không ở đầu/cuối.
 * - quadrants: số lượng ở 4 ô Hình 1..4 của phần 3, theo đúng thứ tự ô.
 * - equation: phép tính hiển thị (phần 6: "4 + 5 = ?", phần 7: "? + 4 = 9").
 * - speakText: câu giọng đọc vi-VN đọc khi câu hỏi hiện ra.
 */
data class CountQuestion(
    val key: String,
    val stage: Int,
    val options: List<String>,
    val correctLabel: String,
    val targetNumber: Int = 0,
    val emoji: String = "🍎",
    val sequence: List<Int> = emptyList(),
    val blankIndex: Int = -1,
    val quadrants: List<Int> = emptyList(),
    val equation: String = "",
    val speakText: String = "",
)

private val NumberWords = mapOf(
    1 to "một", 2 to "hai", 3 to "ba", 4 to "bốn", 5 to "năm",
    6 to "sáu", 7 to "bảy", 8 to "tám", 9 to "chín", 10 to "mười",
    11 to "mười một", 12 to "mười hai", 13 to "mười ba", 14 to "mười bốn",
    15 to "mười lăm", 16 to "mười sáu", 17 to "mười bảy", 18 to "mười tám",
    19 to "mười chín", 20 to "hai mươi",
)

/** Đọc số 1-20 bằng tiếng Việt, dùng cho giọng đọc vi-VN. */
fun vietnameseNumber(n: Int): String = NumberWords[n] ?: "$n"

private val CountEmojis = listOf("🍎", "🐱", "⭐", "🐶", "🍓", "🚗", "🐟", "🍊", "🦋", "⚽")

/** 3 số gây nhiễu khác số đúng, trong 1-20, ưu tiên số gần đáp án. */
private fun numberDistractors(correct: Int, random: Random): List<Int> {
    val near = listOf(correct - 2, correct - 1, correct + 1, correct + 2, correct - 3, correct + 3)
        .filter { it in 1..20 && it != correct }.shuffled(random)
    val out = mutableListOf<Int>()
    for (n in near) {
        if (out.size >= 3) break
        if (n !in out) out += n
    }
    var candidate = 1
    while (out.size < 3) {
        val n = random.nextInt(1, 21)
        if (n != correct && n !in out) out += n
        candidate++
        if (candidate > 100) break
    }
    return out
}

private fun numberOptions(correct: Int, random: Random): List<String> =
    (listOf(correct) + numberDistractors(correct, random)).map { "$it" }.shuffled(random)

/** Sinh 1 câu hỏi cho phần [stage], khóa key không trùng [usedKeys]. */
object CountGenerator {
    fun generate(stage: Int, usedKeys: Set<String>, random: Random = Random.Default): CountQuestion {
        repeat(300) {
            val q = generateOnce(stage, random)
            if (q.key !in usedKeys) return q
        }
        // Hết vòng thử mà vẫn trùng (hầu như không xảy ra vì không gian
        // câu hỏi rất lớn): trả về câu cuối, StageScreen vẫn chấp nhận.
        return generateOnce(stage, random)
    }

    private fun generateOnce(stage: Int, random: Random): CountQuestion {
        return when (stage) {
            1 -> {
                val n = random.nextInt(1, 21)
                CountQuestion(
                    key = "s1-$n",
                    stage = 1,
                    options = numberOptions(n, random),
                    correctLabel = "$n",
                    targetNumber = n,
                    speakText = vietnameseNumber(n),
                )
            }
            2 -> {
                val n = random.nextInt(1, 21)
                val emoji = CountEmojis.random(random)
                CountQuestion(
                    key = "s2-$emoji-$n",
                    stage = 2,
                    options = numberOptions(n, random),
                    correctLabel = "$n",
                    targetNumber = n,
                    emoji = emoji,
                    speakText = "Bé hãy đếm xem có bao nhiêu hình.",
                )
            }
            3 -> {
                val target = random.nextInt(1, 21)
                val emoji = CountEmojis.random(random)
                val others = (1..20).filter { it != target }.shuffled(random).take(3)
                val quadrants = (listOf(target) + others).shuffled(random)
                val correctPos = quadrants.indexOf(target) // 0..3 -> Hình 1..4
                CountQuestion(
                    key = "s3-$emoji-$target-${quadrants.joinToString("-")}",
                    stage = 3,
                    options = listOf("Hình 1", "Hình 2", "Hình 3", "Hình 4").shuffled(random),
                    correctLabel = "Hình ${correctPos + 1}",
                    targetNumber = target,
                    emoji = emoji,
                    quadrants = quadrants,
                    speakText = "Số ${vietnameseNumber(target)}. Bé hãy chọn hình có đúng số lượng đó.",
                )
            }
            4 -> {
                // Dãy liên tiếp dài 5, bắt đầu 1..16 để dãy không vượt quá 20.
                // Ô trống ở giữa (vị trí 1, 2 hoặc 3), không ở đầu/cuối.
                val start = random.nextInt(1, 17)
                val seq = (start..start + 4).toList()
                val blank = random.nextInt(1, 4)
                val answer = seq[blank]
                val speakSeq = seq.mapIndexed { i, v ->
                    if (i == blank) "trống" else vietnameseNumber(v)
                }.joinToString(", ")
                CountQuestion(
                    key = "s4-${seq.joinToString("-")}-$blank",
                    stage = 4,
                    options = numberOptions(answer, random),
                    correctLabel = "$answer",
                    targetNumber = answer,
                    sequence = seq,
                    blankIndex = blank,
                    speakText = "Dãy số: $speakSeq. Bé hãy tìm số còn trống.",
                )
            }
            5 -> {
                val len = if (random.nextBoolean()) 4 else 5
                val seq = (1..20).shuffled(random).take(len)
                val max = seq.max()
                val min = seq.min()
                val correct = "$max và $min"
                val distractors = mutableListOf<String>()
                val pool = seq.toList()
                for (a in pool) {
                    for (b in pool) {
                        if (a > b && "$a và $b" != correct && "$a và $b" !in distractors) {
                            distractors += "$a và $b"
                        }
                    }
                }
                distractors.shuffle(random)
                while (distractors.size < 3) {
                    val a = random.nextInt(2, 21)
                    val b = random.nextInt(1, a)
                    val label = "$a và $b"
                    if (label != correct && label !in distractors) distractors += label
                }
                CountQuestion(
                    key = "s5-${seq.joinToString("-")}",
                    stage = 5,
                    options = (listOf(correct) + distractors.take(3)).shuffled(random),
                    correctLabel = correct,
                    sequence = seq,
                    speakText = "Dãy số: ${seq.joinToString(", ") { vietnameseNumber(it) }}. " +
                        "Bé hãy chọn cặp số lớn nhất và số nhỏ nhất.",
                )
            }
            6 -> {
                // Cộng 2 số dưới 10, hoặc cộng 3-4 số, kết quả LUÔN dưới 20.
                val count = when (random.nextInt(0, 3)) {
                    0 -> 2
                    1 -> 3
                    else -> 4
                }
                var addends: List<Int>
                var sum: Int
                do {
                    addends = List(count) { random.nextInt(1, 10) }
                    sum = addends.sum()
                } while (sum >= 20 || sum < 2)
                val equation = addends.joinToString(" + ") + " = ?"
                val speakEq = addends.joinToString(" cộng ") { vietnameseNumber(it) } + " bằng mấy"
                CountQuestion(
                    key = "s6-${addends.joinToString("-")}",
                    stage = 6,
                    options = numberOptions(sum, random),
                    correctLabel = "$sum",
                    targetNumber = sum,
                    equation = equation,
                    speakText = speakEq,
                )
            }
            else -> {
                // Phần 7: tìm x, CHỈ phép cộng, không phép trừ.
                // Dạng "? + a = b" hoặc "a + ? = b", với b trong 3..20.
                val total = random.nextInt(3, 21)
                val known = random.nextInt(1, total)
                val unknown = total - known
                val firstUnknown = random.nextBoolean()
                val equation = if (firstUnknown) "? + $known = $total" else "$known + ? = $total"
                val speakEq = if (firstUnknown) {
                    "mấy cộng ${vietnameseNumber(known)} bằng ${vietnameseNumber(total)}"
                } else {
                    "${vietnameseNumber(known)} cộng mấy bằng ${vietnameseNumber(total)}"
                }
                CountQuestion(
                    key = "s7-$equation",
                    stage = 7,
                    options = numberOptions(unknown, random),
                    correctLabel = "$unknown",
                    targetNumber = unknown,
                    equation = equation,
                    speakText = speakEq,
                )
            }
        }
    }
}
