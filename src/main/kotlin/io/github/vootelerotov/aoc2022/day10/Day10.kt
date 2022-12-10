package io.github.vootelerotov.aoc2022.day10

import io.github.vootelerotov.util.readResourceLines

fun main() {
  val instructions = readResourceLines("aoc2022/day10/input.txt").map(::parseInstruction)

  // 1st
  val cycles = instructions.runningFold(1 to listOf(1)) { context, instruction ->
    val (state, _) = context
    execute(instruction, state).let { insCycles -> insCycles.last() to insCycles }
  }.map { (_, cycles) -> cycles }.flatten()

  val strengths = generateSequence(20) { it + 40 }.takeWhile { it <= cycles.size }.map { it * cycles[it-1] }.toList()
  strengths.sum().let(::println)

  // 2nd
  cycles
    .mapIndexed { index, cycle -> if (index % 40 in sprite(cycle)) "#" else "." }
    .chunked(40)
    .map { it.joinToString(" ") }
    .forEach(::println)
}

fun sprite(value: Int): Sprite = value-1..value+1

fun execute(instruction: Instruction, state: Int): List<State> = when (instruction) {
  is Noop -> listOf(state)
  is Add -> listOf(state, state + instruction.value)
}

fun parseInstruction(line: String): Instruction = when {
  line.startsWith("noop") -> Noop
  line.startsWith("addx") -> line.split(" ").let { (_, arg) -> Add(arg.toInt()) }
  else -> throw IllegalArgumentException("Unknown instruction: $line")
}

typealias State = Int
typealias Sprite = IntRange

sealed interface Instruction
object Noop: Instruction
data class Add(val value: Int): Instruction