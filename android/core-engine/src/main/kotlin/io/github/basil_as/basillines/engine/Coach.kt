package io.github.basil_as.basillines.engine

import kotlin.random.Random

data class CoachText(val title: String, val explanation: String, val hint: String, val success: String, val wrong: String)
data class StrategyLesson(val id: String, val setup: StepSetup, val from: Point, val to: Point,
    val expectedCleared: Int, val text: Map<String, CoachText>,
    val acceptedTargets: List<Point> = listOf(to))

/** Hand-authored pattern practice, not an optimal-move solver. */
private val authoredLessons: List<StrategyLesson> = listOf(
    StrategyLesson("open-ends", StepSetup(
        listOf(Triple(2, 4, BallColor.RED), Triple(3, 4, BallColor.RED), Triple(7, 2, BallColor.RED)),
        listOf(BallColor.BLUE, BallColor.GREEN, BallColor.YELLOW),
        listOf(Point(8, 8), Point(0, 8), Point(8, 0))),
        Point(7, 2), Point(4, 4), 0, mapOf(
            "en" to CoachText("Keep both ends open", "Build a three-ball row with empty cells at both ends. It can grow in either direction; this is a useful pattern, not a guarantee of a win.", "Move the red ball at column 8, row 3 to column 5, row 5.", "The three red balls now have space at both ends. Plan a fourth while keeping access open.", "That move misses the open-ended three. Try extending the pair without closing an end."),
            "ru" to CoachText("Оставляйте оба конца открытыми", "Соберите тройку с пустыми клетками с обоих концов. Её можно продолжить в любую сторону; это полезный приём, а не гарантия победы.", "Переместите красный шар из столбца 8, строки 3 в столбец 5, строку 5.", "У красной тройки открыты оба конца. Планируйте четвёртый шар, сохраняя доступ.", "Этот ход не создаёт открытую тройку. Продолжите пару, не закрывая её конец.")), acceptedTargets = listOf(Point(1, 4), Point(4, 4))),
    StrategyLesson("gap", StepSetup(
        listOf(Triple(1, 4, BallColor.RED), Triple(2, 4, BallColor.RED), Triple(4, 4, BallColor.RED), Triple(5, 4, BallColor.RED), Triple(7, 2, BallColor.RED)),
        listOf(BallColor.BLUE, BallColor.GREEN, BallColor.YELLOW),
        listOf(Point(8, 8), Point(0, 8), Point(8, 0))),
        Point(7, 2), Point(3, 4), 5, mapOf(
            "en" to CoachText("Fill the gap first", "Four matching balls are split by one gap. Filling it clears five immediately and avoids a new spawn on this turn.", "Put the distant red ball in the gap at column 4, row 5.", "The gap became a five-ball line. An immediate clear also gives you a turn without new balls.", "Look inside the broken row: its single gap matters more than extending an end."),
            "ru" to CoachText("Сначала заполните разрыв", "Четыре одинаковых шара разделены одной клеткой. Заполните её: пятёрка исчезнет, и в этот ход новые шары не появятся.", "Поставьте дальний красный шар в разрыв: столбец 4, строка 5.", "Разрыв превратился в пятёрку. Немедленная линия также даёт ход без новых шаров.", "Ищите пустую клетку внутри ряда: сейчас она важнее продолжения с края."))),
    StrategyLesson("diagonal", StepSetup(
        listOf(Triple(1, 1, BallColor.RED), Triple(2, 2, BallColor.RED), Triple(3, 3, BallColor.RED), Triple(4, 4, BallColor.RED), Triple(7, 2, BallColor.RED)),
        listOf(BallColor.BLUE, BallColor.GREEN, BallColor.YELLOW),
        listOf(Point(8, 8), Point(0, 8), Point(8, 0))),
        Point(7, 2), Point(5, 5), 5, mapOf(
            "en" to CoachText("Scan the diagonals", "A diagonal counts just like a row or column. Look along both sloping directions before starting a new pattern.", "Extend the diagonal to column 6, row 6 with the distant red ball.", "Five diagonal balls cleared. Include diagonals in your scan for immediate opportunities.", "The opportunity slopes across the board. Extend that diagonal to five."),
            "ru" to CoachText("Проверяйте диагонали", "Диагональ считается так же, как строка или столбец. Проверяйте оба наклонных направления, прежде чем начинать новый рисунок.", "Продолжите диагональ дальним красным шаром: столбец 6, строка 6.", "Диагональная пятёрка исчезла. Проверяйте диагонали в поиске готовых возможностей.", "Нужная цепочка идёт наискосок. Продолжите эту диагональ до пяти.")), acceptedTargets = listOf(Point(5, 5), Point(0, 0))),
    StrategyLesson("cross", StepSetup(
        listOf(Triple(2, 2, BallColor.RED), Triple(3, 3, BallColor.RED), Triple(5, 5, BallColor.RED), Triple(6, 6, BallColor.RED), Triple(2, 6, BallColor.RED), Triple(3, 5, BallColor.RED), Triple(5, 3, BallColor.RED), Triple(6, 2, BallColor.RED), Triple(0, 0, BallColor.RED)),
        listOf(BallColor.BLUE, BallColor.GREEN, BallColor.YELLOW),
        listOf(Point(8, 8), Point(0, 8), Point(8, 0))),
        Point(0, 0), Point(4, 4), 9, mapOf(
            "en" to CoachText("Use a shared intersection", "One empty cell belongs to two almost-complete lines. Filling the intersection clears both; the shared ball is counted once.", "Move the red corner ball to the central intersection, column 5, row 5.", "Both lines cleared: nine distinct balls. Look for cells shared by more than one pattern.", "The central gap completes two lines together. Aim for their intersection."),
            "ru" to CoachText("Используйте пересечение", "Одна пустая клетка принадлежит двум почти готовым линиям. Заполнение пересечения уберёт обе; общий шар считается один раз.", "Переместите красный шар из угла в пересечение: столбец 5, строка 5.", "Обе линии исчезли: девять разных шаров. Ищите общие клетки нескольких рисунков.", "Центральный разрыв завершает сразу две линии. Используйте их пересечение."))),
    StrategyLesson("corridor", StepSetup(
        listOf(Triple(4, 0, BallColor.RED), Triple(4, 1, BallColor.GREEN), Triple(4, 2, BallColor.RED), Triple(4, 3, BallColor.GREEN), Triple(4, 5, BallColor.GREEN), Triple(4, 6, BallColor.RED), Triple(4, 7, BallColor.GREEN), Triple(4, 8, BallColor.RED), Triple(4, 4, BallColor.BLUE), Triple(1, 4, BallColor.RED)),
        listOf(BallColor.BLUE, BallColor.GREEN, BallColor.YELLOW),
        listOf(Point(8, 8), Point(0, 8), Point(8, 0))),
        Point(4, 4), Point(6, 6), 0, mapOf(
            "en" to CoachText("Open a passage", "The occupied central cell seals a wall from top to bottom. Move its blue blocker aside so later balls can cross through the opening.", "Move the blue ball from column 5, row 5 to column 7, row 7.", "The passage is open. Keeping routes available can matter even when a move scores nothing.", "This move leaves the wall sealed. Move the blue blocker away from the central passage."),
            "ru" to CoachText("Освободите проход", "Занятая центральная клетка замыкает стену сверху донизу. Уберите синий шар, чтобы следующие шары могли пройти через отверстие.", "Уберите синий шар из столбца 5, строки 5 в столбец 7, строку 7.", "Проход открыт. Сохранение маршрутов бывает важным даже без очков за ход.", "Стена осталась закрытой. Уберите синий шар из центрального прохода."))),
    StrategyLesson("preview", StepSetup(
        listOf(Triple(1, 4, BallColor.RED), Triple(2, 4, BallColor.RED), Triple(3, 4, BallColor.RED), Triple(7, 2, BallColor.RED)),
        listOf(BallColor.RED, BallColor.BLUE, BallColor.GREEN),
        listOf(Point(5, 4), Point(8, 8), Point(0, 8))),
        Point(7, 2), Point(4, 4), 5, mapOf(
            "en" to CoachText("Plan with the preview", "The marked next red ball will land at column 6, row 5 if that cell stays free. Build a four-ball row beside it so the announced spawn completes five.", "Move the distant red ball to column 5, row 5; leave the next red ball’s cell empty.", "The announced red spawn completed your five. Preview planning works when you preserve the marked landing cell.", "Build four beside the marked red spawn. Do not occupy its landing cell."),
            "ru" to CoachText("Планируйте по прогнозу", "Следующий красный шар отмечен в столбце 6, строке 5 и появится там, если клетка свободна. Соберите рядом четвёрку, чтобы появление завершило пятёрку.", "Переместите дальний красный шар в столбец 5, строку 5; клетку следующего красного оставьте пустой.", "Предсказанный красный шар завершил пятёрку. Такой план работает, пока отмеченная клетка свободна.", "Соберите четвёрку рядом с будущим красным шаром. Не занимайте его клетку."))),
 )

/** Do not replace the wall blocker with a blocker at either mouth of its passage. */
val STRATEGY_LESSONS: List<StrategyLesson> = authoredLessons.map { lesson ->
    if (lesson.id != "corridor") lesson else lesson.copy(acceptedTargets = (0..80)
        .map { Point(it % 9, it / 9) }
        .filter { p -> p.x != 4 && !(p.y == 4 && (p.x == 3 || p.x == 5)) &&
            lesson.setup.balls.none { it.first == p.x && it.second == p.y } })
}

fun strategyEngine(lesson: StrategyLesson): GameEngine =
    stepEngine(TutorialStep(StepId.MOVE, setup = lesson.setup), Random(0))!!

fun judgeStrategy(lesson: StrategyLesson, from: Point, to: Point, success: Boolean, cleared: Int): Verdict =
    if (success && from == lesson.from && to in lesson.acceptedTargets && cleared == lesson.expectedCleared) Verdict.DONE else Verdict.RETRY
