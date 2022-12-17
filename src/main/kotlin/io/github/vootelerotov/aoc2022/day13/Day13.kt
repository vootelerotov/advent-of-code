package io.github.vootelerotov.aoc2022.day13

import io.github.vootelerotov.aoc2022.day13.Order.*
import io.github.vootelerotov.util.head
import io.github.vootelerotov.util.readResourceLines
import io.github.vootelerotov.util.split

fun main() {
  val lines = readResourceLines("aoc2022/day13/input-small.txt")

  val rawPackagePairs = split(lines, "")
  val distressSignal = rawPackagePairs.map { (one, other) -> parsePackage(one) to parsePackage(other) }

  // 1st
  distressSignal.withIndex()
    .filter { (_, pair) -> pair.let { (onePackage, otherPackage) -> isInOrder(onePackage, otherPackage) == IN_ORDER } }
    .sumOf { (index, _) -> index + 1 }.let(::println)

  // 2nd
  val firstDivider = parsePackage("[[2]]")
  val secondDivider = parsePackage("[[6]]")
  val dividedDistressSignal =
    lines.filter { it != "" }.map { parsePackage(it) }.plus(listOf(firstDivider, secondDivider))
  val sortedPackages = dividedDistressSignal.sortedWith { one, other ->
    isInOrder(one, other).let { order -> if (order == IN_ORDER) -1 else if (order == OUT_OF_ORDER) 1 else 0 }
  }

  ((sortedPackages.indexOf(firstDivider) + 1) * (sortedPackages.indexOf(secondDivider) + 1)).let(::println)
}

fun isInOrder(onePackage: Package, otherPackage: Package): Order =
  when (onePackage) {
    is Simple -> when (otherPackage) {
      is Simple -> areSimplePackagesInOrder(onePackage, otherPackage)
      is Combined -> isInOrder(Combined(onePackage), otherPackage)
    }
    is Combined -> when (otherPackage) {
      is Simple -> isInOrder(onePackage, Combined(otherPackage))
      is Combined -> areCombinedInOrder(onePackage, otherPackage)
    }
  }

fun areSimplePackagesInOrder(one: Simple, other: Simple): Order {
  return when {
    one.value < other.value -> IN_ORDER
    one.value > other.value -> OUT_OF_ORDER
    else -> UNDETERMINED
  }
}

fun areCombinedInOrder(onePackage: Combined, otherPackage: Combined): Order = areCombinedInOrder(onePackage.packages, otherPackage.packages)

fun areCombinedInOrder(one: List<Package>, other: List<Package>): Order = when {
  one.isEmpty() && other.isEmpty() -> UNDETERMINED
  one.isEmpty() -> IN_ORDER
  other.isEmpty() -> OUT_OF_ORDER
  else -> isInOrder(one.first(), other.first()).let { order ->
   if (order == UNDETERMINED) areCombinedInOrder(one.drop(1), other.drop(1)) else order
  }
}

private fun parsePackage(one: String) = parsePackages(one).single()

fun parsePackages(string: String): List<Package>  =
  head(string)?.let { (head, tail) ->
    when (head) {
      '[' -> takeUntilClosingBracket(tail).let { (packageString, rest) ->
        listOf(Combined(parsePackages(packageString))) + parsePackages(rest)
      }
      ']' -> emptyList()
      ',' -> parsePackages(tail)
      else -> takeDigits(string).let{ (digits, rest) -> listOf(Simple(digits.toInt())) + parsePackages(rest)}
    }
  } ?: emptyList()

fun takeDigits(rawPackage: String): Pair<String, String> = head(rawPackage)?.let { (head, tail) ->
  when (head) {
    ',', '[', ']' -> "" to tail
    else -> takeDigits(tail).let { (digits, rest) -> (head + digits) to rest }
  }
} ?: ("" to "")

fun takeUntilClosingBracket(rawPackage: String, count: Int = 1, packageChars: String = ""): Pair<String, String> =
  head(rawPackage)?.let { (head, tail) -> when(head) {
    '[' -> takeUntilClosingBracket(tail, count + 1, packageChars + head)
    ']' -> if (count == 1) packageChars to tail else takeUntilClosingBracket(tail, count - 1, packageChars + head)
    else -> takeUntilClosingBracket(tail, count, packageChars + head)
  }} ?: ("" to rawPackage)

fun toString(pair: Pair<Package, Package>) = "${pair.first}\n${pair.second}"

sealed interface Package
data class Simple(val value: Int) : Package {
  override fun toString(): String {
    return value.toString()
  }
}

data class Combined(val packages: List<Package>) : Package {
  constructor(vararg packages: Package) : this(packages.toList())

  override fun toString(): String {
    return "[${packages.joinToString(",")}]"
  }

}

enum class Order { IN_ORDER, OUT_OF_ORDER, UNDETERMINED }
