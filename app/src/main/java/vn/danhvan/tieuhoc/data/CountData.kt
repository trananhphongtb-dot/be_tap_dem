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
 * 3 cấp độ, chốt 2026-11-03, áp dụng cho CẢ 7 phần:
 * - Cấp 1 (Dễ): 1-10.
 * - Cấp 2 (Vừa): 11-50.
 * - Cấp 3 (Khó): 51-100.
 * Các khoảng không trùng nhau, để bé không gặp lại số 10 ở cấp 2
 * hay số 50 ở cấp 3 như cách chia 1-10 / 10-50 / 50-100 ban đầu.
 * Hình đếm: tới 20 hình vẫn dùng emoji như cũ; trên 20 hình thì
 * dùng chấm, xếp mỗi hàng 10 chấm để bé đếm theo chục.
 */
data class CountLevel(
    val id: Int,
    val ten: String,
    val min: Int,
    val max: Int,
)

val CountLevels = listOf(
    CountLevel(1, "Dễ", 1, 10),
    CountLevel(2, "Vừa", 11, 50),
    CountLevel(3, "Khó", 51, 100),
)

fun levelFor(id: Int): CountLevel =
    CountLevels.firstOrNull { it.id == id } ?: CountLevels[0]

/** Số lượng từ mức nào trở lên thì phần đếm hình dùng chấm thay emoji. */
const val DOT_COUNT_FROM = 21

/**
 * Một câu hỏi đếm, sinh ngẫu nhiên trong phạm vi của cấp độ đã chọn,
 * KHÔNG phép trừ.
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
    val level: Int = 1,
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

private val DigitWords = mapOf(
    1 to "một", 2 to "hai", 3 to "ba", 4 to "bốn", 5 to "năm",
    6 to "sáu", 7 to "bảy", 8 to "tám", 9 to "chín",
)

private val NumberWords = mapOf(
    1 to "một", 2 to "hai", 3 to "ba", 4 to "bốn", 5 to "năm",
    6 to "sáu", 7 to "bảy", 8 to "tám", 9 to "chín", 10 to "mười",
    11 to "mười một", 12 to "mười hai", 13 to "mười ba", 14 to "mười bốn",
    15 to "mười lăm", 16 to "mười sáu", 17 to "mười bảy", 18 to "mười tám",
    19 to "mười chín", 20 to "hai mươi",
)

/**
 * Đọc số 1-100 bằng tiếng Việt, dùng cho giọng đọc vi-VN.
 * Từ 21 trở lên: "hai mươi mốt", "hai mươi lăm", "ba mươi",
 * "năm mươi", "một trăm". Số 1 sau chục đọc "mốt", số 5
 * sau chục đọc "lăm", đúng cách đọc tiếng Việt.
 */
fun vietnameseNumber(n: Int): String {
    NumberWords[n]?.let { return it }
    if (n == 100) return "một trăm"
    if (n < 1 || n > 100) return "$n"
    val tens = n / 10
    val ones = n % 10
    val tensWord = DigitWords[tens] ?: return "$n"
    if (ones == 0) return "$tensWord mươi"
    val onesWord = when (ones) {
        1 -> "mốt"
        5 -> "lăm"
        else -> DigitWords[ones] ?: return "$n"
    }
    return "$tensWord mươi $onesWord"
}

private val CountEmojis = listOf("🍎", "🐱", "⭐", "🐶", "🍓", "🚗", "🐟", "🍊", "🦋", "⚽")

/**
 * 3 số gây nhiễu khác số đúng, trong [min]..[max], ưu tiên số gần
 * đáp án. Ở cấp khó, đáp án gần nhau vẫn có ±1, ±2, và thêm ±5, ±10
 * để đủ 3 số gây nhiễu mà không trùng.
 */
private fun numberDistractors(correct: Int, min: Int, max: Int, random: Random): List<Int> {
    val near = listOf(
        correct - 10, correct - 5, correct - 2, correct - 1,
        correct + 1, correct + 2, correct + 5, correct + 10,
    ).filter { it in min..max && it != correct }.shuffled(random)
    val out = mutableListOf<Int>()
    for (n in near) {
        if (out.size >= 3) break
        if (n !in out) out += n
    }
    var tries = 0
    while (out.size < 3 && tries < 200) {
        tries++
        val n = random.nextInt(min, max + 1)
        if (n != correct && n !in out) out += n
    }
    return out
}

private fun numberOptions(correct: Int, min: Int, max: Int, random: Random): List<String> =
    (listOf(correct) + numberDistractors(correct, min, max, random))
        .map { "$it" }.shuffled(random)

/** Sinh 1 câu hỏi cho phần [stage], cấp độ [level], khóa key không trùng [usedKeys]. */
object CountGenerator {
    fun generate(
        stage: Int,
        level: Int,
        usedKeys: Set<String>,
        random: Random = Random.Default,
    ): CountQuestion {
        repeat(300) {
            val q = generateOnce(stage, level, random)
            if (q.key !in usedKeys) return q
        }
        // Hết vòng thử mà vẫn trùng (hầu như không xảy ra vì không gian
        // câu hỏi rất lớn): trả về câu cuối, StageScreen vẫn chấp nhận.
        return generateOnce(stage, level, random)
    }

    // Giữ lại cho tương thích: không truyền cấp độ thì mặc định cấp 1.
    fun generate(stage: Int, usedKeys: Set<String>, random: Random = Random.Default): CountQuestion =
        generate(stage, 1, usedKeys, random)

    private fun generateOnce(stage: Int, level: Int, random: Random): CountQuestion {
        val lv = levelFor(level)
        val min = lv.min
        val max = lv.max
        fun inLevel(): Int = random.nextInt(min, max + 1)
        return when (stage) {
            1 -> {
                val n = inLevel()
                CountQuestion(
                    key = "s1-L${lv.id}-$n",
                    stage = 1,
                    level = lv.id,
                    options = numberOptions(n, min, max, random),
                    correctLabel = "$n",
                    targetNumber = n,
                    speakText = vietnameseNumber(n),
                )
            }
            2 -> {
                val n = inLevel()
                val emoji = CountEmojis.random(random)
                CountQuestion(
                    key = "s2-L${lv.id}-$emoji-$n",
                    stage = 2,
                    level = lv.id,
                    options = numberOptions(n, min, max, random),
                    correctLabel = "$n",
                    targetNumber = n,
                    emoji = emoji,
                    speakText = if (n >= DOT_COUNT_FROM) {
                        "Bé hãy đếm chấm theo hàng, mỗi hàng 10 chấm."
                    } else {
                        "Bé hãy đếm xem có bao nhiêu hình."
                    },
                )
            }
            3 -> {
                val target = inLevel()
                val emoji = CountEmojis.random(random)
                // 3 ô còn lại khác số đúng, ưu tiên số gần số đúng.
                val others = numberDistractors(target, min, max, random)
                val quadrants = (listOf(target) + others).shuffled(random)
                val correctPos = quadrants.indexOf(target) // 0..3 -> Hình 1..4
                CountQuestion(
                    key = "s3-L${lv.id}-$emoji-$target-${quadrants.joinToString("-")}",
                    stage = 3,
                    level = lv.id,
                    options = listOf("Hình 1", "Hình 2", "Hình 3", "Hình 4").shuffled(random),
                    correctLabel = "Hình ${correctPos + 1}",
                    targetNumber = target,
                    emoji = emoji,
                    quadrants = quadrants,
                    speakText = "Số ${vietnameseNumber(target)}. Bé hãy chọn hình có đúng số lượng đó.",
                )
            }
            4 -> {
                // Dãy liên tiếp dài 5, bắt đầu trong cấp độ sao cho
                // dãy không vượt quá max của cấp độ.
                // Ô trống ở giữa (vị trí 1, 2 hoặc 3), không ở đầu/cuối.
                val start = random.nextInt(min, max - 4 + 1)
                val seq = (start..start + 4).toList()
                val blank = random.nextInt(1, 4)
                val answer = seq[blank]
                val speakSeq = seq.mapIndexed { i, v ->
                    if (i == blank) "trống" else vietnameseNumber(v)
                }.joinToString(", ")
                CountQuestion(
                    key = "s4-L${lv.id}-${seq.joinToString("-")}-$blank",
                    stage = 4,
                    level = lv.id,
                    options = numberOptions(answer, min, max, random),
                    correctLabel = "$answer",
                    targetNumber = answer,
                    sequence = seq,
                    blankIndex = blank,
                    speakText = "Dãy số: $speakSeq. Bé hãy tìm số còn trống.",
                )
            }
            5 -> {
                val len = if (random.nextBoolean()) 4 else 5
                val seq = (min..max).shuffled(random).take(len)
                val maxV = seq.max()
                val minV = seq.min()
                val correct = "$maxV và $minV"
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
                var tries = 0
                while (distractors.size < 3 && tries < 200) {
                    tries++
                    val a = random.nextInt(min + 1, max + 1)
                    val b = random.nextInt(min, a)
                    val label = "$a và $b"
                    if (label != correct && label !in distractors) distractors += label
                }
                CountQuestion(
                    key = "s5-L${lv.id}-${seq.joinToString("-")}",
                    stage = 5,
                    level = lv.id,
                    options = (listOf(correct) + distractors.take(3)).shuffled(random),
                    correctLabel = correct,
                    sequence = seq,
                    speakText = "Dãy số: ${seq.joinToString(", ") { vietnameseNumber(it) }}. " +
                        "Bé hãy chọn cặp số lớn nhất và số nhỏ nhất.",
                )
            }
            6 -> {
                // Phép cộng, kết quả LUÔN trong khoảng của cấp độ.
                // Chọn tổng trước rồi tách tổng thành các số hạng,
                // để tổng không bao giờ vượt khoảng, kể cả cấp khó.
                val sum = inLevel().coerceAtLeast(2)
                val count = when (lv.id) {
                    1 -> 2
                    else -> if (random.nextBoolean()) 2 else 3
                }
                val addends = when (count) {
                    2 -> {
                        val first = random.nextInt(1, sum)
                        listOf(first, sum - first)
                    }
                    else -> {
                        val first = random.nextInt(1, sum - 1)
                        val second = random.nextInt(1, sum - first)
                        listOf(first, second, sum - first - second)
                    }
                }
                val equation = addends.joinToString(" + ") + " = ?"
                val speakEq = addends.joinToString(" cộng ") { vietnameseNumber(it) } + " bằng mấy"
                CountQuestion(
                    key = "s6-L${lv.id}-${addends.joinToString("-")}",
                    stage = 6,
                    level = lv.id,
                    options = numberOptions(sum, min, max, random),
                    correctLabel = "$sum",
                    targetNumber = sum,
                    equation = equation,
                    speakText = speakEq,
                )
            }
            else -> {
                // Phần 7: tìm x, CHỈ phép cộng, không phép trừ.
                // Dạng "? + a = b" hoặc "a + ? = b", với b trong khoảng
                // của cấp độ (cấp 1: tổng từ 3 trở lên để có số cần tìm).
                // Đáp án gây nhiễu lấy quanh số cần tìm, từ 1 tới
                // max(tổng, số cần tìm + 10), vì số cần tìm có thể nhỏ
                // hơn min của cấp độ (ví dụ 55 = 50 + 5 thì đáp án là 5),
                // và vì tổng nhỏ (ví dụ 3) thì 1..tổng không đủ 3 số
                // gây nhiễu để có đủ 4 nút đáp án.
                val total = random.nextInt(maxOf(min, 3), max + 1)
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
                    key = "s7-L${lv.id}-$equation",
                    stage = 7,
                    level = lv.id,
                    options = numberOptions(unknown, 1, maxOf(total, unknown + 10), random),
                    correctLabel = "$unknown",
                    targetNumber = unknown,
                    equation = equation,
                    speakText = speakEq,
                )
            }
        }
    }
}
