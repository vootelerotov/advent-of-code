package io.github.vootelerotov.aoc2022.day12

import io.github.vootelerotov.util.readResourceLines

fun main() {
  val lines = readResourceLines("aoc2022/day12/input-small.txt")

  val locs = lines.map {
    it.toCharArray().map { char ->
      when (char) {
        'S' -> Start
        'E' -> End
        else -> Loc(char - 'a')
      }
    }
  }

  val (startVertex, endVertex) = graph(locs)

  // 1st
  bfs(startVertex, { current, neighbor -> neighbor.height - current.height <= 1 }) { it == endVertex }?.let { (_, distance) -> println(distance) }

  // 2nd
  bfs(endVertex, { current, neighbor -> neighbor.height - current.height >= -1 }) { it.height == 0 }?.let { (_, distance) -> println(distance) }

}

private fun graph(locs: List<List<Location>>): Pair<StartVertex, EndVertex> {
  lateinit var graph: Map<Pos, Vertex>
  val neighbor = neighbor { graph }
  graph = locs.mapIndexed { y, row ->
    row.mapIndexed { x, loc ->
      val position = x to y
      when (loc) {
        is Start -> position to StartVertex(
          neighbor(up(position)),
          neighbor(down(position)),
          neighbor(left(position)),
          neighbor(right(position)),
          position
        )

        is End -> position to EndVertex(
          neighbor(up(position)),
          neighbor(down(position)),
          neighbor(left(position)),
          neighbor(right(position)),
          position
        )

        is Loc -> position to LocVertex(
          neighbor(up(position)),
          neighbor(down(position)),
          neighbor(left(position)),
          neighbor(right(position)),
          loc.height,
          position
        )
      }
    }
  }.flatten().toMap()
  return graph.values.find { it is StartVertex } as StartVertex to graph.values.find { it is EndVertex } as EndVertex
}

private fun neighbor(graph: () -> Map<Pos, Vertex>): (Pos) -> Lazy<Vertex?> =  { pos -> lazy { graph()[pos] } }

fun up(position: Pos) = position.let { (x, y) -> x to y - 1 }
fun down(position: Pos) = position.let { (x, y) -> x to y + 1 }
fun left(position: Pos) = position.let { (x, y) -> x - 1 to y }
fun right(position: Pos) = position.let { (x, y) -> x + 1 to y }

fun bfs(start: Vertex, isLegal: (Vertex, Vertex) -> Boolean, end: (Vertex) -> Boolean) =
  bfsHelper(listOf(start to 0), isLegal, end)

tailrec fun bfsHelper(
  layer: List<Pair<Vertex, Int>>,
  legal: (Vertex, Vertex) -> Boolean,
  end: (Vertex) -> Boolean,
  visited: Set<Vertex> = emptySet()
): Pair<Vertex, Int>? {
  if (layer.isEmpty()) return null

  layer.firstOrNull { (vertex, _) -> end(vertex) }?.let { return it }
  val visitedIncludingCurrentLayer = visited + layer.map { (vertex, _) -> vertex }

  val nextLayer = layer.flatMap { (vertex, distance) ->
    vertex.neighbors()
      .filterNot { visitedIncludingCurrentLayer.contains(it)}
      .filter { legal(vertex, it)}
      .map { it to distance + 1 }
  }.distinct()

  return bfsHelper(nextLayer, legal, end, visitedIncludingCurrentLayer)
}

sealed interface Location
data class Loc(val height: Int) : Location
object Start : Location
object End : Location

typealias LazyNode = Lazy<Vertex?>
typealias Pos = Pair<Int, Int>

sealed class Vertex(up: LazyNode, down: LazyNode, left: LazyNode, right: LazyNode, val height: Int, val pos: Pos) {
  private val up: Vertex? by up
  private val down: Vertex? by down
  private val left: Vertex? by left
  private val right: Vertex? by right

  fun neighbors() = listOfNotNull(up, down, left, right)

  override fun toString() = pos.let { (x, y) -> "($x, $y) -- $height" }

}

class StartVertex(up: LazyNode, down: LazyNode, left: LazyNode, right: LazyNode, pos: Pos) :
  Vertex(up, down, left, right, 0, pos)

class EndVertex(up: LazyNode, down: LazyNode, left: LazyNode, right: LazyNode, pos: Pos) :
  Vertex(up, down, left, right, 'z' - 'a', pos)

class LocVertex(up: LazyNode, down: LazyNode, left: LazyNode, right: LazyNode, height: Int, pos: Pos) :
  Vertex(up, down, left, right, height, pos)