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

fun <T> head(list: List<T>): Pair<T, List<T>>? = list.firstOrNull()?.let { it to list.drop(1) }

fun head(str: String): Pair<Char, String>? = str.firstOrNull()?.let { it to str.substring(1) }

fun <T> transpose(matrix: List<List<T>>) : List<List<T>> =
  if (matrix.all { it.isEmpty() })
    listOf()
  else
    listOf(matrix.map { it.first() }) + transpose(matrix.map { it.subList(1, it.size) })

fun <T> filter(list: List<T>, test: Test<T>): List<T> =
  list.fold(test to emptyList<T>()) { context, elem ->
    val (currentTest, elems) = context
    val (result, nextTest) = currentTest.test(elem)
    nextTest to if (result) elems + elem else elems
  }.let { (_, elems) -> elems }

fun interface Test<T> {
  fun test(elem: T): Pair<Boolean, Test<T>>
}
fun <T> never(): Test<T> = Test { false to never() }