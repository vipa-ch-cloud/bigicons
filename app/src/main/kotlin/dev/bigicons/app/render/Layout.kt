package dev.bigicons.app.render

import kotlin.math.max
import kotlin.math.roundToInt

/** Чистая логика раскладки (проверяется юнит-тестами). */
object Layout {
    /** Ячейка сетки лаунчера: size(dp) = 70 * n - 30. Обратное преобразование. */
    fun cells(sizeDp: Float): Int = max(1, ((sizeDp + 30f) / 70f).roundToInt())

    fun sizeDp(cells: Int): Int = 70 * cells - 30

    private val re = Regex("""Widget(\d)x(\d)$""")

    /** Имя класса провайдера -> (колонки, строки). */
    fun parseProvider(className: String?): Pair<Int, Int>? {
        val m = re.find(className ?: return null) ?: return null
        return m.groupValues[1].toInt() to m.groupValues[2].toInt()
    }
}
