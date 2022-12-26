package io.github.vootelerotov.aoc2022.day20

import io.github.vootelerotov.util.readResourceLines

fun main() {
  val numbers = readResourceLines("aoc2022/day20/input-small.txt").map { it.toLong() }

  // 1st
  val nodes = linkedListByIndex(numbers)
  mix(nodes)
  println(cords(nodes).sum())

  // 2nd
  val largeNodes = linkedListByIndex(numbers.map { it * 811589153L })
  mix(largeNodes, 10)
  print(cords(largeNodes).sum())
}

private fun cords(nodes: List<LinkedNode>) =
  nodes.single { it.value == 0L }.let { node ->
    generateSequence(node) { it.next!! }
      .filterIndexed { index, _ -> index % 1000 == 0 }
      .map { it.value }
      .drop(1)
      .take(3)
  }.toList()

private fun mix(nodes: List<LinkedNode>, times : Int = 1) =  repeat(times) { mix(nodes) }

private fun mix(nodes: List<LinkedNode>) {
  nodes.forEach { node ->
    if (node.value == 0L) {
      return@forEach
    }

    val oldPrev = node.prev!!
    val oldNext = node.next!!
    oldPrev.next = oldNext
    oldNext.prev = oldPrev

    if (node.value > 0) {
      generateSequence(node.next) { it.next!! }.drop((node.value % (nodes.size - 1)).toInt()).first().let { nextNode ->
        val newPrev = nextNode.prev!!
        newPrev.next = node
        nextNode.prev = node
        node.prev = newPrev
        node.next = nextNode
      }
    } else {
      generateSequence(node.prev) { it.prev!! }.drop(-(node.value % (nodes.size - 1)).toInt()).first().let { prevNode ->
        val newNext = prevNode.next!!
        newNext.prev = node
        prevNode.next = node
        node.next = newNext
        node.prev = prevNode
      }
    }
  }
}

private fun linkedListByIndex(numbers: List<Long>): List<LinkedNode> {
  val nodes = numbers.map { LinkedNode(it) }
  nodes.windowed(2).forEach { (prev, next) ->
    prev.next = next
    next.prev = prev
  }
  val first = nodes.first()
  first.prev = nodes.last()
  nodes.last().next = first
  return nodes
}

class LinkedNode(val value: Long) {
  var next: LinkedNode? = null
  var prev: LinkedNode? = null

  override fun toString() = value.toString()
}