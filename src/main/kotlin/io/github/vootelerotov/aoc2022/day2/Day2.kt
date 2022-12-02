package io.github.vootelerotov.aoc2022.day2

import io.github.vootelerotov.aoc2022.day2.Result.*
import io.github.vootelerotov.aoc2022.day2.Shape.*
import io.github.vootelerotov.util.readResourceLines

fun main() {
  val lines = readResourceLines("aoc2022/day2/input.txt")

  // 1st
  lines.map(::parseRoundAsUnderstood).map(::playRound).map(::scoreRound).sum().let(::println)

  // 2nd
  lines.map(::parseRoundAsDesigned).map(::deduceRound).map(::scoreRound).sum().let(::println)

}

fun deduceRound(roundDescription: Pair<Shape, Result>) = roundDescription.let {
    (other, result) -> Shape.values().filter { result(it, other) == result }.let { (mine) -> mine to result }
}

fun parseRoundAsDesigned(line: String): Pair<Shape, Result> =
  line.split(" ").let { (otherShape, result) -> parseShape(otherShape) to parseResult(result) }

fun parseResult(result: String) = when (result) {
  "X" -> LOSS
  "Y" -> DRAW
  "Z" -> WIN
  else -> throw IllegalArgumentException("Unknown result: $result")
}

fun scoreRound(roundResult: Pair<Shape, Result>) =
  roundResult.let { (shape, result) -> scoreShape(shape) + scoreResult(result) }

fun scoreResult(result: Result) = when (result) {
  WIN -> 6
  DRAW -> 3
  LOSS -> 0
}

fun scoreShape(shape: Shape) = when (shape) {
  ROCK -> 1
  PAPER -> 2
  SCISSORS -> 3
}

fun playRound(round: Pair<Shape, Shape>): Pair<Shape, Result> = round.let { (other, mine) -> mine to result(mine, other)}

fun parseRoundAsUnderstood(line: String): Pair<Shape, Shape> =
  line.split(" ").let { (one, other) ->  parseShape(one) to parseShape(other) }

fun parseShape(shape: String): Shape =
  when (shape) {
    "A", "X" -> ROCK
    "B", "Y" -> PAPER
    "C", "Z" -> SCISSORS
    else -> throw IllegalArgumentException("Unknown shape $shape")
  }

fun result(mine: Shape, other: Shape) : Result = when {
  mine == other -> DRAW
  mine == ROCK && other == SCISSORS -> WIN
  mine == SCISSORS && other == PAPER -> WIN
  mine == PAPER && other == ROCK -> WIN
  else -> LOSS
}

enum class Result { WIN, LOSS, DRAW }

enum class Shape { ROCK, PAPER, SCISSORS }