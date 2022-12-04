package io.github.vootelerotov.aoc2022.day4

import io.github.vootelerotov.util.readResourceLines

fun main() {
  val assignmentPairs = readResourceLines("aoc2022/day4/input.txt").map(::parsePair)

  // 1st
  assignmentPairs.count(::isOneSubsetOfOther).let(::println)

  // 2nd
  assignmentPairs.count(::overlaps).let(::println)
}

fun overlaps(pair: Pair<Assignment, Assignment>): Boolean = pair.let { (one, two) -> !doesNotOverlap(one, two)}

fun isOneSubsetOfOther(pair: Pair<Assignment, Assignment>): Boolean =
  pair.let { (one, other) -> includes(one, other) || includes(other, one) }

private fun includes(one: Assignment, other: Assignment) = one.from <= other.from && one.to >= other.to

private fun doesNotOverlap(one: Assignment, other: Assignment) = one.from > other.to || one.to < other.from

fun parsePair(line: String): Pair<Assignment, Assignment> =
  line.split(",").let { (one, other) -> parseAssignment(one) to parseAssignment(other) }

fun parseAssignment(rawAssignment: String): Assignment =
  rawAssignment.split("-").let { (from, to) -> Assignment(from.toInt(), to.toInt()) }

data class Assignment(val from: SectionId, val to: SectionId)
typealias SectionId = Int
