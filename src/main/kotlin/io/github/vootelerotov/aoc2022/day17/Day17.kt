package io.github.vootelerotov.aoc2022.day17

import io.github.vootelerotov.aoc2022.day17.JetPattern.LEFT
import io.github.vootelerotov.aoc2022.day17.JetPattern.RIGHT
import io.github.vootelerotov.aoc2022.day17.RoomState.*
import io.github.vootelerotov.aoc2022.day17.Shape.*
import io.github.vootelerotov.util.readResourceLines

fun main() {
  val (line) = readResourceLines("aoc2022/day17/input-small.txt")

  val jetStream = parseJetStream(line)

  val width = 7
  val initialChamber = Chamber(width)

  val shapes = listOf(HORIZONTAL_LINE, CROSS, MIRRORED_L, VERTICAL_LINE, SQUARE)
  val fallingShapes = { generateSequence { shapes }.flatten() }

  // 1st
  tetrisSequence(fallingShapes, initialChamber, jetStream).drop(2022).first().let(::countRows).let(::println)

  // 2nd
  val (firstIndex, repeatIndex) = fallingShapes()
    .runningFold(Triple<Chamber, Int, Shape?>(initialChamber, 0, null)) { (chamber, jetStreamIndex, _), shape ->
      fall(chamber, jetStream, shape, jetStreamIndex).let { (chamber, jetStreamIndex) ->
        Triple(chamber, jetStreamIndex, shape)
      }
    }
    .map { (chamber, index, shape) -> Triple(chamber.rows, index, shape) }
    .runningFold(emptyList<Triple<List<Row>, Int, Shape?>>()) { acc, elem -> acc + elem }
    .first { it.distinct() != it }
    .let { it.indexOf(it.last()) to it.lastIndex }
  val repetitionLength = repeatIndex - firstIndex

  val rowsBeforeRepetition = tetrisSequence(fallingShapes, initialChamber, jetStream).drop(firstIndex).first()
    .let { chamber -> countRows(chamber) }.toLong()
  val rowsInRepetition = tetrisSequence(fallingShapes, initialChamber, jetStream).drop(repeatIndex).first()
    .let { chamber -> countRows(chamber) } - rowsBeforeRepetition

  val withoutBeginning1: Long = 1_000_000_000_000L - firstIndex
  val rowsWhileRepeating = (withoutBeginning1 / repetitionLength) * rowsInRepetition
  val rowsAfterRepeating =
    tetrisSequence(fallingShapes, initialChamber, jetStream).drop(firstIndex + (withoutBeginning1 % repetitionLength).toInt())
      .first().let { chamber ->
      countRows(
        chamber
      ) - rowsBeforeRepetition
    }

  println(rowsBeforeRepetition + rowsWhileRepeating + rowsAfterRepeating)
}

private fun countRows(chamber: Chamber) = chamber.rows.size + chamber.cutRows

private fun tetrisSequence(
  fallingShapes: () -> Sequence<Shape>,
  initialChamber: Chamber,
  jetStream: List<JetPattern>
) = fallingShapes().runningFold(initialChamber to 0) { (chamber, jetStreamIndex), shape ->
  fall(chamber, jetStream, shape, jetStreamIndex)
}.map { (chamber, _) -> chamber }

fun fall(chamber: Chamber, jetStream: JetStream, fallingShape: Shape, jetStreamIndex: Int): Pair<Chamber, Int> {
  val fallingRows = pad(generateFallingShape(fallingShape), chamber.width)
  val padding = List(3) { List(chamber.width) { EMPTY } }
  val falls =
    generateSequence(chamber.copy(rows = chamber.rows + padding + fallingRows) to jetStreamIndex % jetStream.size) { (chamber, index) ->
      fallCycle(chamber, jetStream[index]) to (index + 1) % jetStream.size
    }
  return falls.first { (chamber, _) -> !chamber.rows.flatten().contains(FALLING_ROCK) }
    .let { (chamber, index) -> compact(chamber) to index % jetStream.size }
}

fun compact(chamber: Chamber): Chamber =
  neededRows(chamber.rows.asReversed()).let { (neededRows, dropped) ->
    chamber.copy(rows = neededRows.asReversed(), cutRows = chamber.cutRows + dropped)
  }

fun neededRows(
  rows: List<Row>,
  index: Int = 0,
  notCoveredIndices: List<Int> = listOf(0, 1, 2, 3, 4, 5, 6)
): Pair<List<Row>, Int> {
  if (notCoveredIndices.isEmpty()) {
    return rows.take(index + 1).let { it to rows.size - it.size }
  }

  if (index < rows.size) {
    return neededRows(rows, index + 1, notCoveredIndices.filterNot { rows[index][it] == STABLE_ROCK })
  }
  return rows to 0
}

fun fallCycle(chamber: Chamber, jetPattern: JetPattern) = pushByJet(chamber, jetPattern).let(::fallDown)
fun fallDown(chamber: Chamber): Chamber = if (canFall(chamber)) shiftDown(chamber) else ossify(chamber)

private fun ossify(chamber: Chamber) =
  chamber.copy(rows = chamber.rows.map { it.map { state -> if (state == FALLING_ROCK) STABLE_ROCK else state } })

fun canFall(chamber: Chamber): Boolean = !chamber.rows.first().contains(FALLING_ROCK) && chamber.rows.windowed(2)
  .filter { (_, above) -> above.contains(FALLING_ROCK) }
  .all { (below, above) ->
    below.zip(above).none { (elemBelow, elemAbove) -> elemBelow == STABLE_ROCK && elemAbove == FALLING_ROCK }
  }

fun shiftDown(chamber: Chamber): Chamber =
  chamber.copy(rows = chamber.rows.windowed(2, partialWindows = true).mapNotNull {
    when (it.size) {
      1 -> it.let { (row) ->
        if (row.contains(STABLE_ROCK)) row.map { state -> if (FALLING_ROCK == state) EMPTY else state } else null
      }

      2 -> it.let { (below, above) ->
        below.zip(above).map { (elemBelow, elemAbove) ->
          if (elemAbove == FALLING_ROCK) elemAbove else if (elemBelow == FALLING_ROCK) EMPTY else elemBelow
        }
      }

      else -> throw IllegalStateException("Unexpected size of windowed list: ${it.size}")
    }
  })


fun pushByJet(chamber: Chamber, jetPattern: JetPattern) =
  when (jetPattern) {
    LEFT -> pushLeft(chamber)
    RIGHT -> pushRight(chamber)
  }

fun pushRight(chamber: Chamber): Chamber {
  val reversedRows = chamber.rows.map(Row::reversed)
  return if (canMoveLeft(reversedRows)) {
    chamber.copy(rows = reversedRows.map { shiftLeft(it) }.map(Row::reversed))
  } else {
    chamber
  }

}

fun pushLeft(chamber: Chamber): Chamber = chamber.rows.let { rows ->
  if (canMoveLeft(rows)) chamber.copy(rows = rows.map { shiftLeft(it) }) else chamber
}

fun shiftLeft(row: Row): Row = row.windowed(2, partialWindows = true).map {
  when (it.size) {
    1 -> it.let { (elem) -> if (elem == FALLING_ROCK) EMPTY else elem }
    2 -> it.let { (left, right) -> if (right == FALLING_ROCK) FALLING_ROCK else if (left == FALLING_ROCK) EMPTY else left }
    else -> throw IllegalStateException("Unexpected row size: ${it.size}")
  }
}

fun canMoveLeft(rows: List<Row>): Boolean = rows.filter { it.contains(FALLING_ROCK) }.all { canMoveRowLeft(it) }
fun canMoveRowLeft(row: Row): Boolean =
  row.windowed(2).firstOrNull { (_, right) -> right == FALLING_ROCK }?.let { (left, _) -> left == EMPTY } ?: false

fun generateFallingShape(shape: Shape): List<Row> = when (shape) {
  HORIZONTAL_LINE -> generateHorizontalLine()
  CROSS -> generateCross()
  MIRRORED_L -> generateMirroredL()
  VERTICAL_LINE -> generateVerticalLine()
  SQUARE -> generateSquare()
}

fun generateHorizontalLine(): List<Row> = listOf(List(4) { FALLING_ROCK })

fun generateCross(): List<Row> = listOf(
  listOf(EMPTY, FALLING_ROCK, EMPTY),
  listOf(FALLING_ROCK, FALLING_ROCK, FALLING_ROCK),
  listOf(EMPTY, FALLING_ROCK, EMPTY)
)

fun generateMirroredL(): List<Row> = listOf(
  listOf(EMPTY, EMPTY, FALLING_ROCK),
  listOf(EMPTY, EMPTY, FALLING_ROCK),
  listOf(FALLING_ROCK, FALLING_ROCK, FALLING_ROCK)
).reversed()

fun generateVerticalLine(): List<Row> = List(4) { listOf(FALLING_ROCK) }

fun generateSquare(): List<Row> = List(2) { List(2) { FALLING_ROCK } }

fun toString(chamber: Chamber): String = chamber.rows.reversed().joinToString(separator = "\n") { row -> toString(row) }
fun toString(row: Row) = row.joinToString(" ") { roomState -> toString(roomState) }
fun toString(roomState: RoomState) = when (roomState) {
  EMPTY -> "."
  STABLE_ROCK -> "#"
  FALLING_ROCK -> "@"
}


fun pad(rows: List<Row>, width: Int): List<Row> = rows.map { row ->
  (sequenceOf(EMPTY, EMPTY) + row.asSequence() + generateSequence { EMPTY }).take(width).toList()
}

fun parseJetStream(line: String): List<JetPattern> = line.map {
  when (it) {
    '<' -> LEFT
    '>' -> RIGHT
    else -> throw IllegalArgumentException("Invalid jet pattern $it")
  }
}

enum class Shape { HORIZONTAL_LINE, CROSS, MIRRORED_L, VERTICAL_LINE, SQUARE }
enum class RoomState { FALLING_ROCK, STABLE_ROCK, EMPTY }
enum class JetPattern { LEFT, RIGHT }
typealias JetStream = List<JetPattern>
typealias Row = List<RoomState>

data class Chamber(val width: Int, val rows: List<Row> = emptyList(), val cutRows: Int = 0) {
  override fun toString(): String = toString(this)
}
