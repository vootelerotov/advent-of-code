package io.github.vootelerotov.aoc2022.day8

import io.github.vootelerotov.util.*

fun main() {
  val lines = readResourceLines("aoc2022/day8/input.txt")

  val forest: List<List<Tree>> = parseTreeGrid(lines)

  // 1st
  val views = listOf(
    forest,
    forest.map { it.reversed() },
    transpose(forest),
    transpose(forest).map { it.reversed() },
  )
  views.flatMap { view -> view.flatMap(::filterVisibleFromEdge) }.distinct().count().let(::println)

  // 2nd
  trees(forest)
    .map { tree -> lineOfSights(tree, forest) }
    .maxOf { (tree, lineOfSights) ->
      lineOfSights.map { lineOfSight -> countVisibleTrees(tree, lineOfSight) }.let(::calculateScenicScore)
    }
    .let(::println)
}

private fun calculateScenicScore(visibleTrees: List<Int>): Int = visibleTrees.reduce { one, other -> one * other }

private fun countVisibleTrees(tree: Tree, lineOfSight: List<Tree>) = filterVisibleFrom(tree, lineOfSight).count()

private fun trees(forest: List<List<Tree>>): List<Tree> = forest.flatten()

private fun parseTreeGrid(lines: List<String>) = lines.mapIndexed { latitude, line -> parseTreeLine(line, latitude) }
private fun parseTreeLine(line: String, latitude: Int) =
  line.toCharArray().mapIndexed { longitude, tree -> parseTree(latitude, longitude, tree) }
private fun parseTree(latitude: Int, longitude: Int, tree: Char) = Tree(latitude, longitude, tree.digitToInt())

fun lineOfSights(tree: Tree, forest: List<List<Tree>>) = tree to listOf(
  forest[tree.latitude].drop(tree.longitude + 1),
  forest[tree.latitude].take(tree.longitude).reversed(),
  transpose(forest)[tree.longitude].drop(tree.latitude + 1),
  transpose(forest)[tree.longitude].take(tree.latitude).reversed(),
)

fun filterVisibleFromEdge(trees: List<Tree>): List<Tree> = trees.first().let { tree ->
  listOf(tree) + filter(trees.drop(1), inLineOfSight(tree) )
}

private fun filterVisibleFrom(from: Tree, lineOfSight: List<Tree>): List<Tree> =
  filter(lineOfSight, visibleFrom(from))

private fun inLineOfSight(blocker: Tree): Test<Tree> = Test { tree ->
  if (tree.height > blocker.height) true to inLineOfSight(tree) else false to inLineOfSight(blocker)
}

private fun visibleFrom(start: Tree): Test<Tree> = Test {tree ->
  if (tree.height < start.height) true to visibleFrom(start) else true to never()
}

data class Tree(val latitude: Int, val longitude: Int, val height: Int)
