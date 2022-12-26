package io.github.vootelerotov.aoc2022.day22

import io.github.vootelerotov.aoc2022.day22.Direction.*
import io.github.vootelerotov.aoc2022.day22.Type.*
import io.github.vootelerotov.util.head
import io.github.vootelerotov.util.readResourceLines
import io.github.vootelerotov.util.split
import kotlin.math.sqrt

fun main() {
  solve(Task("aoc2022/day22/input-small.txt", moveAcrossFaceSample(::faceSample), ::faceSample))
  //solve(Task("aoc2022/day22/input.txt", moveAcrossFaceInput(::faceInput), ::faceInput))
}

private fun solve(task: Task) {
  val (rawMap, rawInstructions) = split(readResourceLines(task.pathToInput), "")
    .let { (map, instructions) -> map to instructions.single() }

  val lineLength = rawMap.maxOf { it.length }
  val paddedRawMap = rawMap.map { it.padEnd(lineLength, ' ') }

  val instructions = parseInstructions(rawInstructions)
  val rawTiles: List<List<RawTile>> = parseMap(paddedRawMap)
  val (_, startingTile: Space<Tile>) = condense(rawTiles)

  // 1st
  val stoppingPosition = followInstructions(instructions, startingTile)
  stoppingPosition.let { (tile, direction) -> password(tile, direction) }.let(::println)

  // 2nd
  val cubeSize = sqrt(rawTiles.flatten().filterNot { it.type == EMPTY }.size / 6.0).toInt()
  val (cubeTiles, cubeStartingTile) = condenseCube(rawTiles, cubeSize, task.faceFinder)

  val cubeMover = cubeMover(cubeTiles, cubeSize, task.faceCrosser)
  val stoppingCubePosition = followInstructions(instructions, cubeStartingTile, cubeMover)
  stoppingCubePosition.let { (tile, direction) -> password(tile, direction) }.let(::println)
}

private fun password(tile: Space<*>, direction: Direction) =
  (tile.y + 1) * 1000 + (tile.x + 1) * 4 + facingNumber(direction)

private fun followInstructions(instructions: List<Instruction>, startingTile: Space<Tile>) =
  instructions.fold(startingTile to RIGHT) { (tile, direction), instruction ->
    when (instruction) {
      is TurnLeft -> tile to turnLeft(direction)
      is TurnRight -> tile to turnRight(direction)
      is Move -> move(tile, direction, instruction.steps)
    }
  }

private fun followInstructions(
  instructions: List<Instruction>,
  startingTile: Space<Tile?>,
  cubeMover: (Space<Tile?>, Direction, Int) -> Pair<Space<Tile?>, Direction>
): Pair<Space<Tile?>, Direction> {
  return instructions.fold(startingTile to RIGHT) { (tile, direction), instruction ->
    when (instruction) {
      is TurnLeft -> tile to turnLeft(direction)
      is TurnRight -> tile to turnRight(direction)
      is Move -> cubeMover(tile, direction, instruction.steps)
    }
  }
}
fun cubeMover(
  tiles: Map<Pos3D, Tile>,
  cubeSize: Int,
  faceCrosser: FaceCrosser
): (Space<Tile?>, Direction, Int) -> Pair<Space<Tile?>, Direction> {
  lateinit var mover : (Space<Tile?>, Direction, Int) -> Pair<Space<Tile?>, Direction>
  mover = mover@{ tile, dir, steps ->
    if (steps == 0) {
      return@mover tile to dir
    }

    neighborInDirection(tile, dir)?.let {
      when(it) {
        is Space<*> -> return@mover mover(it as Space<Tile?> ,dir, steps - 1 )
        is Wall -> return@mover tile to dir
      }
    }

    val wallCheck = wallCheck(tile, dir)

    return@mover faceCrosser(tile, dir, cubeSize)
      .let { (pos, newDir) -> wallCheck(tiles[pos]!!, newDir) }
      .let { (newTile, newDir) -> mover(newTile, newDir, steps - 1) }
  }
  return mover
}

fun moveAcrossFaceInput(faceFinder: FaceFinder): FaceCrosser = { tile, dir, cubeSize ->
  val inverter = inverter(cubeSize)
  val (x, y) = tile.let { it.x % cubeSize to it.y % cubeSize }
  val max = cubeSize - 1
  when (faceFinder(tile.x to tile.y, cubeSize)!! to dir) {

    (Face.TOP to TOP) -> Triple(Face.BACK, 0, x) to RIGHT
    (Face.TOP to BOTTOM) -> Triple(Face.FRONT, x, 0) to BOTTOM
    (Face.TOP to LEFT) -> Triple(Face.LEFT, 0, inverter(y)) to RIGHT
    (Face.TOP to RIGHT) -> Triple(Face.RIGHT, 0, y) to RIGHT

    (Face.FRONT to TOP) -> Triple(Face.TOP, x, max) to TOP
    (Face.FRONT to BOTTOM) -> Triple(Face.BOTTOM, x, 0) to BOTTOM
    (Face.FRONT to LEFT) -> Triple(Face.LEFT, y, 0) to BOTTOM
    (Face.FRONT to RIGHT) -> Triple(Face.RIGHT, y, max) to TOP

    (Face.RIGHT to TOP) -> Triple(Face.BACK, x, max) to TOP
    (Face.RIGHT to BOTTOM) -> Triple(Face.FRONT, max, x) to LEFT
    (Face.RIGHT to LEFT) -> Triple(Face.TOP, max, y) to LEFT
    (Face.RIGHT to RIGHT) -> Triple(Face.BOTTOM, max, inverter(y)) to LEFT

   (Face.BOTTOM to TOP) -> Triple(Face.FRONT, x, max) to TOP
   (Face.BOTTOM to BOTTOM) -> Triple(Face.BACK, max, x) to LEFT
   (Face.BOTTOM to LEFT) -> Triple(Face.LEFT, max, y) to LEFT
   (Face.BOTTOM to RIGHT) -> Triple(Face.RIGHT, max, inverter(y)) to LEFT

    (Face.LEFT to TOP) -> Triple(Face.FRONT, 0, x) to RIGHT
    (Face.LEFT to BOTTOM) -> Triple(Face.BACK, x, 0) to BOTTOM
    (Face.LEFT to LEFT) -> Triple(Face.TOP, 0, inverter(y)) to RIGHT
    (Face.LEFT to RIGHT) -> Triple(Face.BOTTOM, 0, y) to RIGHT

    (Face.BACK to TOP) -> Triple(Face.LEFT, x, max) to TOP
    (Face.BACK to BOTTOM) -> Triple(Face.RIGHT, x, 0) to BOTTOM
    (Face.BACK to LEFT) -> Triple(Face.TOP, y, 0) to BOTTOM
    (Face.BACK to RIGHT) -> Triple(Face.BOTTOM, y, max) to TOP

    else -> throw IllegalStateException("Should not happen")
  }
}

fun moveAcrossFaceSample(faceFinder: FaceFinder): FaceCrosser = { tile, dir, cubeSize ->
  val inverter = inverter(cubeSize)
  val (x, y) = tile.let { it.x % cubeSize to it.y % cubeSize }
  val max = cubeSize - 1
  when (faceFinder(tile.x to tile.y, cubeSize)!! to dir) {
    (Face.TOP to BOTTOM) -> Triple(Face.FRONT, x, 0) to BOTTOM
    (Face.TOP to TOP) -> Triple(Face.BACK, inverter(x), 0) to BOTTOM
    (Face.TOP to LEFT) -> Triple(Face.LEFT, inverter(y), 0) to BOTTOM
    (Face.TOP to RIGHT) -> Triple(Face.RIGHT, inverter(y), max) to LEFT

    (Face.FRONT to BOTTOM) -> Triple(Face.BOTTOM, x, 0) to BOTTOM
    (Face.FRONT to TOP) -> Triple(Face.TOP, x, max) to TOP
    (Face.FRONT to LEFT) -> Triple(Face.LEFT, y, max) to LEFT
    (Face.FRONT to RIGHT) -> Triple(Face.RIGHT, inverter(y), 0) to BOTTOM

    (Face.BACK to BOTTOM) -> Triple(Face.BOTTOM, inverter(x), max) to TOP
    (Face.BACK to TOP) -> Triple(Face.TOP, inverter(x), 0) to BOTTOM
    (Face.BACK to LEFT) -> Triple(Face.LEFT, y, max) to BOTTOM
    (Face.BACK to RIGHT) -> Triple(Face.LEFT, 0, y) to RIGHT

    (Face.LEFT to BOTTOM) -> Triple(Face.BOTTOM, 0, inverter(x)) to RIGHT
    (Face.LEFT to TOP) -> Triple(Face.TOP, 0, x) to RIGHT
    (Face.LEFT to LEFT) -> Triple(Face.BACK, max, y) to LEFT
    (Face.LEFT to RIGHT) -> Triple(Face.FRONT, 0, y) to RIGHT

    (Face.RIGHT to BOTTOM) -> Triple(Face.BACK, 0, inverter(x)) to LEFT
    (Face.RIGHT to TOP) -> Triple(Face.FRONT, max, inverter(x)) to RIGHT
    (Face.RIGHT to LEFT) -> Triple(Face.BOTTOM, max, y) to LEFT
    (Face.RIGHT to RIGHT) -> Triple(Face.TOP, max, inverter(y)) to LEFT

    (Face.BOTTOM to TOP) -> Triple(Face.FRONT, x, max) to TOP
    (Face.BOTTOM to BOTTOM) -> Triple(Face.BACK, inverter(x), max) to TOP
    (Face.BOTTOM to LEFT) -> Triple(Face.LEFT, inverter(y), max) to TOP
    (Face.BOTTOM to RIGHT) -> Triple(Face.RIGHT, 0, y) to RIGHT

    else -> throw IllegalStateException("Should not happen")
  }
}

private fun inverter(cubeSize: Int): (Int) -> (Int) = { cord  -> cubeSize - 1 - cord }

fun <T> wallCheck(tile : Space<T>, dir: Direction): (Tile, Direction) -> Pair<Space<T>, Direction> =
  { tileArg, dirArg -> when(tileArg) {
    is Space<*> -> tileArg as Space<T> to dirArg
    is Wall   -> tile to dir }
  }

fun facingNumber(direction: Direction): Int = when(direction) {
  TOP -> 3
  LEFT -> 2
  BOTTOM -> 1
  RIGHT -> 0
}

fun move(tile: Space<Tile>, direction: Direction, steps: Int): Pair<Space<Tile>, Direction> =
 generateSequence(tile) { t ->
   when (val neighborInDirection = neighborInDirection(t, direction)) {
      is Space<*> -> neighborInDirection as Space<Tile>
      is Wall -> t
   }
 }.drop(steps).first() to direction

 fun <T> neighborInDirection(tile: Space<T>, direction: Direction): T = when (direction) {
   TOP -> tile.top
   LEFT -> tile.left
   BOTTOM -> tile.bottom
   RIGHT -> tile.right
 }

fun turnRight(direction: Direction): Direction = when (direction) {
  TOP -> RIGHT
  RIGHT -> BOTTOM
  BOTTOM -> LEFT
  LEFT -> TOP
}

fun turnLeft(direction: Direction): Direction = when (direction) {
  RIGHT -> TOP
  TOP -> LEFT
  LEFT -> BOTTOM
  BOTTOM -> RIGHT
}

tailrec fun parseInstructions(rawInstructions: String, acc: List<Instruction> = emptyList()): List<Instruction> = head(rawInstructions)?.let { (head, tail) ->
  when (head) {
    'L' -> parseInstructions(tail, acc + TurnLeft)
    'R' -> parseInstructions(tail, acc + TurnRight)
    else -> takeDigits(rawInstructions).let { (digits, rest) -> parseInstructions(rest, acc + Move(digits.toInt())) }
  }
} ?: acc

fun takeDigits(rawInstructions: String): Pair<String, String> = head(rawInstructions)?.let { (head, tail) ->
  head.takeIf(Char::isDigit)?.let {
    val (digits, rest) = takeDigits(tail)
    (head + digits) to rest
  }
} ?: ("" to rawInstructions)

fun condense(rawTiles: List<List<RawTile>>): Pair<Map<Pos,Tile>,Space<Tile>>{
  lateinit var tiles: Map<Pos, Tile>
  tiles = rawTiles.flatten().mapNotNull {
    when(it.type) {
      EMPTY -> null
      SPACE -> Space(
        it.x,
        it.y,
        { tiles[firstNotEmpty(it, RawTile::top)]!! },
        { tiles[firstNotEmpty(it, RawTile::left)]!! },
        { tiles[firstNotEmpty(it, RawTile::bottom)]!! },
        { tiles[firstNotEmpty(it, RawTile::right)]!! })
      WALL -> Wall
    }?.let { tile -> (it.x to it.y) to tile }
  }.toMap()
  return tiles to  rawTiles.first().first { it.type == SPACE }.let { tiles[it.x to it.y]!! as Space<Tile>}
}

fun condenseCube(rawTiles: List<List<RawTile>>, cubeSize: Int, faceFinder: FaceFinder): Pair<Map<Triple<Face, Int, Int>, Tile>,Space<Tile?>>{
  lateinit var tiles: Map<Pos, Tile>
  tiles = rawTiles.flatten().mapNotNull {
    when(it.type) {
      EMPTY -> null
      SPACE -> Space(
        it.x,
        it.y,
        { if (it.y % cubeSize > 0) tiles[it.top.x to it.top.y] else null},
        { if (it.x % cubeSize > 0) tiles[it.left.x to it.left.y] else null },
        { if (it.y % cubeSize < cubeSize - 1) tiles[it.bottom.x to it.bottom.y] else null },
        { if (it.x % cubeSize < cubeSize - 1) tiles[it.right.x to it.right.y] else null })
      WALL -> Wall
    }?.let { tile -> (it.x to it.y) to tile }
  }.toMap()
  return Pair(
    tiles.mapKeys { (key, _) -> Triple(faceFinder(key, cubeSize)!!, key.first % cubeSize, key.second % cubeSize) },
    rawTiles.first().first { it.type == SPACE }.let { tiles[it.x to it.y]!! as Space<Tile?>}
  )
}

fun firstNotEmpty(rawTile: RawTile, next: (RawTile) -> RawTile) =
  generateSequence(next(rawTile), next).first { it.type != EMPTY }.let { it.x to it.y }


fun parseMap(rawMap: List<String>): List<List<RawTile>> {
  lateinit var map: List<List<RawTile>>
  map = rawMap.mapIndexed { y, line ->
    line.mapIndexed { x, c ->
      val type = when (c) {
        '#' -> WALL
        '.' -> SPACE
        ' ' -> EMPTY
        else -> throw IllegalArgumentException("Unknown tile type: $c")
      }
       RawTile(x, y, type,
         { map[(y - 1 + map.size) % map.size][x] },
         { map[y][(x - 1 + line.length) % line.length] },
         { map[(y + 1 + map.size) % map.size][x] },
         { map[y][(x + 1 + line.length) % line.length] }
       )
      }
    }
  return map
}

fun faceInput(pos: Pos, cubeSize: Int): Face? = when (pos.let { (x, y) -> x / cubeSize to y / cubeSize }) {
  1 to 0 -> Face.TOP
  2 to 0 -> Face.RIGHT
  1 to 1 -> Face.FRONT
  0 to 2 -> Face.LEFT
  1 to 2 -> Face.BOTTOM
  0 to 3 -> Face.BACK
  else -> null
}

fun faceSample(pos: Pos, cubeSize: Int): Face? = when(pos.let { (x, y) -> x / cubeSize to y / cubeSize }) {
  2 to 0 -> Face.TOP
  0 to 1 -> Face.BACK
  1 to 1 -> Face.LEFT
  2 to 1 -> Face.FRONT
  2 to 2 -> Face.BOTTOM
  3 to 2 -> Face.RIGHT
  else -> null
}

typealias Pos = Pair<Int, Int>
typealias Pos3D = Triple<Face, Int, Int>
typealias FaceCrosser = (Space<Tile?>, Direction, Int) -> Pair<Pos3D, Direction>
typealias FaceFinder = (Pos, Int) -> Face?

enum class Type { EMPTY, SPACE, WALL }
class RawTile(val x: Int, val y: Int, val type: Type, top: () -> RawTile, left: () -> RawTile, bottom: () -> RawTile, right: () -> RawTile) {
  val top by lazy(top)
  val left by lazy(left)
  val bottom by lazy(bottom)
  val right by lazy(right)
}

enum class Direction { TOP, LEFT, BOTTOM, RIGHT }

sealed interface Instruction
data class Move(val steps: Int) : Instruction
object TurnLeft: Instruction
object TurnRight: Instruction

sealed interface Tile
object Wall : Tile
class Space<T>(val x: Int, val y: Int, top: () -> T, left: () -> T, bottom: () -> T, right: () -> T ) : Tile {
  val top by lazy(top)
  val left by lazy(left)
  val bottom by lazy(bottom)
  val right by lazy(right)

  override fun toString(): String = "$x -- $y"
}

enum class Face { TOP, FRONT, LEFT, BACK, RIGHT, BOTTOM }

data class Task(val pathToInput: String, val faceCrosser: FaceCrosser, val faceFinder: FaceFinder)