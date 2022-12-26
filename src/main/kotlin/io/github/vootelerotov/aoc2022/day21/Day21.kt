package io.github.vootelerotov.aoc2022.day21

import io.github.vootelerotov.aoc2022.day21.Operation.*
import io.github.vootelerotov.util.readResourceLines
import java.lang.IllegalStateException

fun main() {
  val lines = readResourceLines("aoc2022/day21/input-small.txt")
  val root = parseMonkeys(lines)

  // 1st
  println(yell(root))

  // 2nd
  val pathToHuman = pathToHuman(root)!!
  val (withHuman, withoutHuman) = if (root.left in pathToHuman) root.left to root.right else root.right to root.left
  val expectedValue = yell(withoutHuman)
  toYell(withHuman, expectedValue, pathToHuman).let(::println)
}

fun toYell(monkey: Monkey, expectedValue: Long, pathToHuman: Set<Monkey>): Long =
  when (monkey) {
    is SimpleMonkey -> if (monkey.name == "humn") expectedValue else throw IllegalStateException("Expected human")
    is BinaryMonkey ->
      if (monkey.left in pathToHuman)
        toYellLeft(monkey.operation, monkey.left, yell(monkey.right), expectedValue, pathToHuman)
      else
        toYellRight(monkey.operation, monkey.right, yell(monkey.left), expectedValue, pathToHuman)
  }

fun toYellLeft(operation: Operation, left: Monkey, rightValue: Long, expectedValue: Long, pathToHuman: Set<Monkey>): Long =
  when (operation) {
    PLUS -> toYell(left, expectedValue - rightValue, pathToHuman)
    MINUS -> toYell(left, expectedValue + rightValue, pathToHuman)
    TIMES -> toYell(left, expectedValue / rightValue, pathToHuman)
    DIV -> toYell(left, expectedValue * rightValue, pathToHuman)
  }

fun toYellRight(operation: Operation, right: Monkey, leftValue: Long, expectedValue: Long, pathToHuman: Set<Monkey>): Long =
  when (operation) {
    PLUS -> toYell(right, expectedValue - leftValue, pathToHuman)
    MINUS -> toYell(right, leftValue - expectedValue, pathToHuman)
    TIMES -> toYell(right, expectedValue / leftValue, pathToHuman)
    DIV -> toYell(right, leftValue / expectedValue, pathToHuman)
  }

fun pathToHuman(monkey: Monkey): Set<Monkey>? =
  when (monkey) {
    is SimpleMonkey -> monkey.takeIf { monkey.name == "humn" }?.let { setOf(it) }
    is BinaryMonkey -> pathToHuman(monkey.left)?.let { it.plus(monkey) } ?: pathToHuman(monkey.right)?.let { it.plus(monkey) }
  }

fun yell(monkey : Monkey): Long = when (monkey) {
  is SimpleMonkey -> monkey.number
  is BinaryMonkey -> toFunction(monkey.operation)(yell(monkey.left), yell(monkey.right))
}

fun toFunction(operation: Operation): (Long, Long) -> Long = when (operation) {
  PLUS -> Long::plus
  MINUS -> Long::minus
  TIMES -> Long::times
  DIV -> Long::div
}

fun parseMonkeys(lines: List<String>): BinaryMonkey {
  lateinit var monkeysByName: Map<String, Monkey>
  monkeysByName = lines.map { line ->
    line.split(":").let { (name, yells) ->
      val yellsList = yells.trim().split(" ")
      when (yellsList.size) {
        1 -> SimpleMonkey(name, yellsList.single().toLong())
        3 -> yellsList.let{ (a, op, b) -> parseComplexMonkey(name, { monkeysByName[a]!! }, op, { monkeysByName[b]!!}) }
        else -> throw IllegalArgumentException("Invalid yells: $yells")
      }
    }
  }.associateBy { it.name }
  return monkeysByName["root"] as BinaryMonkey
}

fun parseComplexMonkey(name : String, oneMonkey: () -> Monkey, rawOperation: String, otherMonkey: () -> Monkey) =
  BinaryMonkey(name,  parseOperation(rawOperation), lazy(oneMonkey), lazy(otherMonkey))

fun parseOperation(rawOperation: String): Operation = when(rawOperation) {
  "+" -> PLUS
  "-" -> MINUS
  "*" -> TIMES
  "/" -> DIV
  else -> throw IllegalArgumentException("Invalid operation: $rawOperation")
}

enum class Operation { PLUS, MINUS, TIMES, DIV }

sealed interface Monkey { val name: String }
data class SimpleMonkey(override val name: String, val number: Long) : Monkey
class BinaryMonkey(
  override val name: String,
  val operation : Operation,
  left: Lazy<Monkey>,
  right: Lazy<Monkey>
) : Monkey {
  val left by left
  val right by right

  override fun toString() =  "BinaryMonkey(name='$name')"
}