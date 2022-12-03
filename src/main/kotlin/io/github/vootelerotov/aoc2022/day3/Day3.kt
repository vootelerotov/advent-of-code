package io.github.vootelerotov.aoc2022.day3

import io.github.vootelerotov.util.readResourceLines

fun main() {
  val lines = readResourceLines("aoc2022/day3/input.txt")

  val rucksacks = lines.map(::parseLine)

  // 1st
  rucksacks
    .map { (oneCompartment, otherCompartment) -> oneCompartment.items.intersect(otherCompartment.items.toSet()) }
    .flatten()
    .sumOf(Item::priority)
    .let(::println)

  // 2nd
  rucksacks
    .map(::allItemsInRucksack)
    .chunked(3)
    .map { (one, two, three) -> one.intersect(two.toSet()).intersect(three.toSet()).toList() }
    .flatten()
    .sumOf(Item::priority)
    .let(::println)
}

fun allItemsInRucksack(rucksack: Rucksack): List<Item> =
  rucksack.let { (oneCompartment, otherCompartment) -> oneCompartment.items + otherCompartment.items }

fun parseLine(line: String): Rucksack =
  parseRucksack(line.subSequence(0, line.length/2)) to parseRucksack(line.subSequence(line.length/2, line.length))

fun parseRucksack(rawRucksack: CharSequence): Compartment = Compartment(rawRucksack.map(::parseItem))

fun parseItem(item: Char): Item = Item(item, if (item.isUpperCase())  item - 'A' + 27 else item - 'a' + 1)

typealias Rucksack = Pair<Compartment, Compartment>
data class Compartment(val items: List<Item>)
data class Item(val charCode: Char, val priority: Int)