package io.github.vootelerotov.aoc2022.day11

import io.github.vootelerotov.util.readResourceLines
import io.github.vootelerotov.util.split

fun main() {
  val rawMonkeys = split(readResourceLines("aoc2022/day11/input-small.txt").map(String::trim), "")
  val modulo = rawMonkeys.map(::parsePrime).reduce{ one, other -> one * other }
  val itemsByMonkeys = parseMonkeys(rawMonkeys, modulo)

  // 1st
  playRounds(itemsByMonkeys, 20) { it / 3 }.let(::println)

  // 2nd
  playRounds(itemsByMonkeys, 10000).let(::println)
}

private fun playRounds(itemsByMonkeys: Map<Monkey, List<Item>>, rounds: Int, modifier: (Item) -> Item = { it } ) =
  generateSequence(playRound(itemsByMonkeys, modifier)) { prev -> playRound(prev.last(), modifier) }
    .take(rounds)
    .map { monkeyRounds -> itemsByMonkeys.keys.associateWith { monkey -> monkeyRounds[monkey.id].getValue(monkey).size } }
    .reduce { prev, next -> prev.keys.associateWith { id -> prev.getValue(id) + next.getValue(id) } }
    .values.sorted().takeLast(2).let { (one, other) -> one.toBigDecimal() * other.toBigDecimal() }

private fun parseMonkeys(rawMonkeys: List<List<String>>, modulo: Int): Map<Monkey, List<Item>>  {
  lateinit var monkeys : Map<MonkeyId, Pair<Monkey, List<Item>>>
  monkeys = rawMonkeys.map {
      rawMonkey -> parseMonkey(rawMonkey, modulo) { id -> monkeys[id]!!.let { (monkey, _) -> monkey } }
  }.associateBy { it.first.id }
  return monkeys.values.toMap()
}

fun parsePrime(rawMonkey: List<String>) = rawMonkey.let { (_, _, _, testHeader, _, _) -> parsePrime(testHeader) }
fun parsePrime(testHeader: String) = testHeader.removePrefix("Test: divisible by ").toInt()

fun playRound(monkeys: Map<Monkey, List<Item>>, modifier: (Item) -> Item): List<Map<Monkey, List<Item>>> =
  monkeys.keys.sortedBy { it.id }.runningFold(monkeys) { monkeys, monkey -> playMonkeyRound(monkey, monkeys, modifier) }

fun playMonkeyRound(monkey: Monkey, monkeysWithItems: Map<Monkey, List<Item>>, modifier: (Item) -> Item): Map<Monkey, List<Item>> =
  monkeysWithItems[monkey]!!.fold(monkeysWithItems) { monkeysWithItems, item -> playItemRound(monkey, item, monkeysWithItems, modifier) }

private fun playItemRound(monkey: Monkey, item: Item, monkeys: Map<Monkey, List<Item>>, modifier: (Item) -> Item): Map<Monkey, List<Item>> {
  val newItem = modifier(monkey.operation(item))
  val targetMonkey = monkey.test(newItem)
  return monkeys.plus(arrayOf(
    monkey to monkeys[monkey]!!.minus(item),
    targetMonkey to monkeys[targetMonkey]!!.plus(newItem)
  ))
}

fun parseMonkey(rawMonkey: List<String>, modulo: Int, monkeys: (MonkeyId) -> Monkey): Pair<Monkey, List<Item>> =
  rawMonkey.let { (header, rawItems, rawOp, testHeader, testTrue, testFalse) ->
    Monkey(
      parseId(header),
      parseOperation(rawOp, modulo),
      parseTest(testHeader, testTrue, testFalse, monkeys)
    ) to parseItems(rawItems)
  }

fun parseTest(testHeader: String, testTrue: String, testFalse: String, monkeys: (MonkeyId) -> Monkey): (Item) -> Monkey {
  val condition = parseCondition(testHeader)
  val trueMonkeyId = parseTrueTarget(testTrue)
  val falseMonkeyId = parseFalseTarget(testFalse)
  return { item -> if (condition(item)) monkeys(trueMonkeyId) else monkeys(falseMonkeyId) }
}

private fun parseTrueTarget(testTrue: String) = testTrue.removePrefix("If true: throw to monkey ").toInt()
private fun parseFalseTarget(testTrue: String) = testTrue.removePrefix("If false: throw to monkey ").toInt()


private fun parseCondition(testHeader: String) =
  testHeader.removePrefix("Test: divisible by ").toLong().let { divider -> { item: Item -> item % divider == 0L } }

fun parseId(header: String): MonkeyId = Regex("Monkey (\\d+):").find(header).let { it!!.groupValues[1].toInt() }
fun parseItems(rawItems: String): List<Item> = rawItems.removePrefix("Starting items: ").split(", ").map { it.toLong() }
fun parseOperation(rawOperation: String, modulo: Int) =
  rawOperation.removePrefix("Operation: new = ").split(" ").let { (left, op, right) ->
    val leftFactor = parseFactor(left)
    val rightFactor = parseFactor(right)
    when (op) {
      "*" -> { old: WorryLevel -> (leftFactor.invoke(old) * rightFactor.invoke(old)) % modulo }
      "+" -> { old: WorryLevel -> ( leftFactor.invoke(old) + rightFactor.invoke(old)) % modulo }
      else -> throw IllegalArgumentException("Unknown op $op")
    }
  }
fun parseFactor(rawFactor: String): (WorryLevel) -> WorryLevel = when (rawFactor) {
  "old" -> { old -> old }
  else -> { _ -> rawFactor.toLong() }
}

typealias MonkeyId = Int
typealias WorryLevel = Long
typealias Item = Long
data class Monkey(val id: MonkeyId, val operation: (WorryLevel) -> WorryLevel, val test: (Item) -> Monkey)

// Well, a man gotta do what a man gotta do
private inline operator fun <E> List<E>.component6(): E = this[5]