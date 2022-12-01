package io.github.vootelerotov.util

private class Helper

fun readResourceLines(path: String): List<String> {
  val resource = Helper::class.java.classLoader.getResource(path)
    ?: throw IllegalArgumentException("No such resource $path")

  return resource.readText().lines().dropLastWhile { it.isEmpty() } // strip EOF new line
}

fun <T> split(list: List<T>, separator: T): List<List<T>> =
  if (list.isEmpty())
    emptyList()
  else
    splitInTwo(list, separator).let { (rest, last) -> split(rest, separator).plusElement(last) }

private fun <T> splitInTwo(list: List<T>, separator: T): Pair<List<T>, List<T>> =
  list.indexOfLast{ it == separator }.let { index ->
    when (index) {
      -1 -> emptyList<T>() to list
      list.lastIndex -> splitInTwo(list.subList(0, list.lastIndex), separator)
      else -> list.subList(0, index) to list.subList(index+1, list.size)
    }
  }