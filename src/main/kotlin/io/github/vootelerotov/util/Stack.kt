package io.github.vootelerotov.util

class Stack<T>(private val list: List<T> = emptyList()) {

  fun push(element: T): Stack<T> = Stack(list.plusElement(element))

  fun pop(): Pair<T, Stack<T>> = list.last() to Stack(list.dropLast(1))

  fun peek(): Pair<T, Stack<T>> = list.last() to this

  fun isEmpty(): Boolean = list.isEmpty()

  fun size(): Int = list.size

  override fun toString(): String {
    return list.toString()
  }


}