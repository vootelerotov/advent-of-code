package io.github.vootelerotov.aoc2022.day9

import io.github.vootelerotov.aoc2022.day9.Direction.*
import io.github.vootelerotov.util.readResourceLines

fun main() {
  val lines = readResourceLines("aoc2022/day9/input.txt")
  val moves = lines.map(::parseMove)
  val directions = moves.flatMap(::flattenMoves)

  // 1st
  val initialState = State(0 to 0, listOf(0 to 0))
  directions.runningFold(initialState) { state, direction -> move(state, direction) }
    .let(::countTailPositions)
    .let(::println)

  // 2st
  val initialStateForLongerRope = State(0 to 0, List(9) { 0 to 0 })
  directions.runningFold(initialStateForLongerRope) { state, direction -> move(state, direction) }
    .let(::countTailPositions)
    .let(::println)
}

private fun countTailPositions(states: List<State>) = states.map { (_, tail) -> tail.last() }.distinct().count()

fun move(state: State, direction: Direction): State = state.let { (head, tail) ->
  moveInDirection(head, direction).let { newHead -> State(newHead, moveTail(tail, newHead)) }
}

fun moveTail(tail: List<Position>, newHead: Pair<Int, Int>): List<Position> =
  tail.fold(newHead to emptyList<Position>()) { context, tail -> context.let { (head, acc) ->
    val newTail = moveTail(tail, head)
    newTail to (acc + newTail)
  } }.let { (_, acc) -> acc }

fun moveTail(tail: Position, head: Position): Position = relativePosition(tail, head).let {
  when (it) {
    2 to 0 -> moveInDirection(tail, RIGHT)
    -2 to 0  -> moveInDirection(tail, LEFT)
    0 to 2 -> moveInDirection(tail, UP)
    0 to -2 -> moveInDirection(tail, DOWN)
    2 to 1, 1 to 2, 2 to 2 -> moveInDirection(moveInDirection(tail, RIGHT), UP)
    2 to -1, 1 to -2, 2 to -2  -> moveInDirection(moveInDirection(tail, RIGHT), DOWN)
    -2 to 1, -1 to 2, -2 to 2 -> moveInDirection(moveInDirection(tail, LEFT), UP)
    -2 to -1, -1 to -2, -2 to -2  -> moveInDirection(moveInDirection(tail, LEFT), DOWN)
    else -> tail
  }
}

fun relativePosition(tail: Position, newHead: Position): Position = tail.let { (xTail, yTail) ->
  newHead.let { (xHead, yHead) -> (xHead - xTail) to (yHead - yTail) }
}

fun moveInDirection(head: Position, direction: Direction): Position = when(direction) {
  RIGHT -> head.first + 1 to head.second
  LEFT -> head.first - 1 to head.second
  UP -> head.first to head.second + 1
  DOWN -> head.first to head.second - 1
}

fun flattenMoves(move: Move): List<Direction> = List(move.distance) { move.direction }

private fun parseMove(line: String): Move =
  line.split(" ").let { (rawDirection, rawDistance) -> Move(parseDirection(rawDirection), rawDistance.toInt()) }

fun parseDirection(rawDirection: String): Direction = when(rawDirection) {
  "U" -> UP
  "D" -> DOWN
  "L" -> LEFT
  "R" -> RIGHT
  else -> throw IllegalArgumentException("Unknown direction $rawDirection")
}

typealias Position = Pair<Int, Int>
data class State(val head: Position, val tail: List<Position>)
data class Move(val direction: Direction, val distance: Int)
enum class Direction { UP, DOWN, LEFT, RIGHT }