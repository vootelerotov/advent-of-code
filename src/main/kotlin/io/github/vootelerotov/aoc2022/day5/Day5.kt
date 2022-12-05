package io.github.vootelerotov.aoc2022.day5

import io.github.vootelerotov.util.*

fun main() {
  val lines = readResourceLines("aoc2022/day5/input.txt")

  val (rawState, rawMoves) = split(lines, "")

  val initialState = parseState(rawState)
  val moves = parseMoves(rawMoves)

  //1st
  moves.fold(initialState) { state, move -> applyMove(state, move, ::crateMover9000) }.let(::printTopCrates)

  //2nd
  moves.fold(initialState) { state, move -> applyMove(state, move, ::crateMover9001) }.let(::printTopCrates)
}

private fun printTopCrates(finalState: Map<Position, Stack<Crate>>) =
  finalState.values.map { it.peek().let { (element, _) -> element } }.joinToString(separator = "").let(::println)

fun applyMove(state: Map<Position, Stack<Crate>>, move: Move, crane: Crane): Map<Position, Stack<Crate>> {
  val (number, from, toPos) = move
  val fromCrates = state.getValue(from)
  val toCrates = state.getValue(toPos)

  val (fromAfter, toAfter) = crane(fromCrates, toCrates, number)

  return state
    .plus(from to fromAfter)
    .plus(toPos to toAfter)
}

fun crateMover9000(from: Stack<Crate>, to: Stack<Crate>, quantity: Int): Pair<Stack<Crate>, Stack<Crate>> =
  generateSequence(from to to) { (from, to) -> move(from, to) }.drop(quantity).first()

fun move(from: Stack<Crate>, to: Stack<Crate>): Pair<Stack<Crate>, Stack<Crate>> =
  from.pop().let { (crate, fromStack) -> fromStack to to.push(crate) }

fun crateMover9001(from: Stack<Crate>, to: Stack<Crate>, quantity: Int): Pair<Stack<Crate>, Stack<Crate>> {
  val (fromAfter, poppedCrates) = pops(from).drop(quantity).first()
  return fromAfter to poppedCrates.reversed().fold(to) { stack, crate -> stack.push(crate) }
}

private fun pops(from: Stack<Crate>): Sequence<Pair<Stack<Crate>, List<Crate>>> =
  generateSequence(from to emptyList()) { (crateStack, poppedElements) ->
    crateStack.pop().let { (element, newStack) -> newStack to poppedElements.plus(element) }
  }

fun parseMoves(rawMoves: List<String>): List<Move> = rawMoves.map(::parseMove)

fun parseMove(rawMove: String):Move =
  rawMove.split(" ").let { (_, number, _, from, _, to) ->  Move(number.toInt(), from.toInt(), to.toInt()) }

fun parseState(rawState: List<String>): Map<Position, Stack<Crate>> =
  parseCrateIndexes(rawState)
    .map { index -> extractColumn(rawState, index) }
    .map(::parseCrateStack)
    .associateBy( { (position, _) -> position }, { (_, stack) -> stack })

private fun parseCrateIndexes(rawState: List<String>) =
  rawState.last().withIndex().filterNot { (_, char) -> char == ' ' }.map { (index, _) -> index }

private fun extractColumn(rawState: List<String>, index: Int) =
  rawState.map { it.getOrElse(index) { ' ' } }.filterNot { char -> char == ' ' }.reversed()

fun parseCrateStack(rawCrateStack: List<Char>): Pair<Int, Stack<Crate>> =
  head(rawCrateStack)?.let { (position, rawCrates) -> position.digitToInt() to Stack(rawCrates) } ?: throw IllegalArgumentException("No crates found")

typealias Position = Int
typealias Crate = Char

fun interface Crane : (Stack<Crate>, Stack<Crate>, Int) -> Pair<Stack<Crate>, Stack<Crate>>
data class Move(val quantity: Int, val from: Position, val to: Position)

// Well, a man gotta do what a man gotta do
private operator fun <E> List<E>.component6(): E = this[5]
