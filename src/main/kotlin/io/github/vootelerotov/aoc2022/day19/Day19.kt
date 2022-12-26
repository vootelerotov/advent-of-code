package io.github.vootelerotov.aoc2022.day19

import io.github.vootelerotov.aoc2022.day19.Material.*
import io.github.vootelerotov.util.readResourceLines
import java.util.stream.Collectors

fun main() {
  val lines = readResourceLines("aoc2022/day19/input-small.txt")

  val blueprints = lines.map(::parseBluePrint)

  val initialState = State(0, Materials(), Robots(ore = 1))
  // 1st
  blueprints.map { it.id to factory(it) }.parallelStream().map { (id, factory) ->
    findMostObsidian(initialState, factory, 24).let { (state, _) -> state to id }
  }.collect(Collectors.toList()).sumOf { (state, id) -> state.materials.geode * id }.let(::println)

  // 2nd
  blueprints.take(3).map(::factory).parallelStream().map { factory ->
    findMostObsidian(initialState, factory, 32)
  }.collect(Collectors.toList()).map { (state, _) -> state.materials.geode }.reduce(Int::times).let(::println)

}

fun findMostObsidian(
  state: State,
  factory: Factory,
  timeLimit: Int,
  cache: MutableMap<State, State> = mutableMapOf(),
  upgrader: Upgrader = Upgrader(),
  knownBestState: State = state,

  ): Triple<State, MutableMap<State, State>, Upgrader> {
  if (state.time == timeLimit) return Triple(state, cache.also { it[state] = state }, upgrader)

  cache[state]?.let { return Triple(it, cache, upgrader) }

  val (upgradedState, newUpgrader) = upgrader.upgrade(state)

  cache[upgradedState]?.let { return Triple(it, cache, upgrader) }

  if (knownBestState.materials.geode > upgradedState.materials.geode + potential(upgradedState.robots, timeLimit - upgradedState.time)) {
    return Triple(knownBestState, cache.also { it[upgradedState] = upgradedState }, newUpgrader)
  }

  val (mostGeodeReachable, newCache, newestUpgrader) =
    nextPossibleStates(state, factory, timeLimit).fold(Triple(knownBestState, cache, newUpgrader)) { (bestState, cache, betterCache), newState ->
      findMostObsidian(newState, factory, timeLimit, cache, betterCache, bestState).let { (newBestState, newCache, newerUpdater) ->
        Triple(listOf(newBestState, bestState).maxBy { it.materials.geode }, newCache, newerUpdater)
      }
    }
  return Triple(mostGeodeReachable, newCache.also { it[state] = mostGeodeReachable }, newestUpgrader)
}

fun atLeastAsManyMaterials(one: State, other: State): Boolean = (one.materials to other.materials).let { (one, other) ->
  one.ore >= other.ore && one.clay >= other.clay && one.geode >= other.geode && one.obsidian >= other.obsidian
}

fun atLeastAsManyRobots(one: State, other: State): Boolean = (one.robots to other.robots).let { (one, other) ->
  one.ore >= other.ore && one.clay >= other.clay && one.geode >= other.geode && one.obsidian >= other.obsidian
}

fun nextPossibleStates(state: State, factory: Factory, timeLimit: Int): List<State> =
  state.let { (initialTime, initialMaterials, initialRobots) ->
    val time = initialTime + 1
    val minedMaterials = mineMaterials(initialRobots)
    val configurations = factory(initialMaterials, timeLimit - initialTime, initialRobots)
    return configurations.map { (robots, materials) ->
      State(time, materials + minedMaterials, robots + initialRobots)
    }
  }

fun factory(bluePrint: BluePrint): Factory = { materials: Materials, timeLeft: Int, robots: Robots ->
  val maximumCosts = Material.values().map(bluePrint::get).let {
    Materials(
      ore = it.maxOf { bluePrint -> bluePrint[ORE] },
      clay = it.maxOf { bluePrint -> bluePrint[CLAY] },
      obsidian = it.maxOf { bluePrint -> bluePrint[OBSIDIAN] },
      geode = Int.MAX_VALUE
    )
  }
  Material.values()
    .filter { material -> material == GEODE || materials[material] <= maximumCosts[material] * timeLeft }
    .filter { material -> robots[material] < maximumCosts[material] }
    .mapNotNull { material ->
      val robotCost = bluePrint[material]
      (materials - robotCost)?.let { robots(material) to it }
    } + (Robots() to materials)
}


private fun mineMaterials(initialRobots: Robots) = Materials(
  ore = initialRobots.ore,
  clay = initialRobots.clay,
  obsidian = initialRobots.obsidian,
  geode = initialRobots.geode,
)

fun parseBluePrint(line: String): BluePrint {
  val (header, rawCosts) = line.split(": ")
  val id = parseId(header)
  val (ore, clay, obs, geode) = rawCosts.split(".").filterNot(String::isBlank).map { it.trim() }.map(::parseCosts)
  return BluePrint(id, ore, clay, obs, geode)
}

fun parseCosts(rawCost: String): Materials = rawCost.split("robot costs").let { (_, rawMaterials) ->
  rawMaterials.split("and").map { it.trim() }.map(::parseMaterialCost)
}.let { it.toMap() }.let {
  Materials(
    ore = it.getOrDefault(ORE, 0),
    clay = it.getOrDefault(CLAY, 0),
    obsidian = it.getOrDefault(OBSIDIAN, 0),
    geode = it.getOrDefault(GEODE, 0)
  )
}

fun parseMaterialCost(rawMaterialCost: String): Pair<Material, Int> =
  rawMaterialCost.split(" ").let { (rawAmount, rawMaterial) ->
    parseMaterial(rawMaterial) to rawAmount.toInt()
  }

fun parseMaterial(rawMaterial: String): Material = when (rawMaterial) {
  "ore" -> ORE
  "clay" -> CLAY
  "obsidian" -> OBSIDIAN
  "geode" -> GEODE
  else -> throw IllegalArgumentException("Unknown material: $rawMaterial")
}

fun parseId(header: String): Int = header.removePrefix("Blueprint ").toInt()

fun robots(material: Material) = when (material) {
  ORE -> Robots(ore = 1)
  CLAY -> Robots(clay = 1)
  OBSIDIAN -> Robots(obsidian = 1)
  GEODE -> Robots(geode = 1)
}

enum class Material { ORE, CLAY, OBSIDIAN, GEODE }
data class Materials(val ore: Int = 0, val clay: Int = 0, val obsidian: Int = 0, val geode: Int = 0) {
  operator fun get(material: Material) = when (material) {
    ORE -> ore
    CLAY -> clay
    OBSIDIAN -> obsidian
    GEODE -> geode
  }

  operator fun plus(other: Materials) =
    Materials(ore + other.ore, clay + other.clay, obsidian + other.obsidian, geode + other.geode)

  operator fun minus(other: Materials) =
    Materials(ore - other.ore, clay - other.clay, obsidian - other.obsidian, geode - other.geode)
      .takeIf { it.ore >= 0 && it.clay >= 0 && it.obsidian >= 0 && it.geode >= 0 }

  override fun toString(): String {
    return "ore: $ore, clay: $clay, obsidian: $obsidian, geode: $geode"
  }
}

fun potential(robots: Robots, timeLeft: Int) = robots.geode * timeLeft + ((timeLeft + 1) * timeLeft)/2

data class Robots(val ore: Int = 0, val clay: Int = 0, val obsidian: Int = 0, val geode: Int = 0) {
  operator fun plus(other: Robots) =
    Robots(ore + other.ore, clay + other.clay, obsidian + other.obsidian, geode + other.geode)

  operator fun minus(other: Robots) =
    Robots(ore - other.ore, clay - other.clay, obsidian - other.obsidian, geode - other.geode)
      .takeIf { it.ore >= 0 && it.clay >= 0 && it.obsidian >= 0 && it.geode >= 0 }

  override fun toString(): String {
    return "ore: $ore, clay: $clay, obsidian: $obsidian, geode: $geode"
  }

  operator fun get(material: Material) = when (material) {
    ORE -> ore
    CLAY -> clay
    OBSIDIAN -> obsidian
    GEODE -> geode
  }
}

data class BluePrint(
  val id: Int,
  val oreRobot: Materials,
  val clayRobot: Materials,
  val obsidianRobot: Materials,
  val geodeRobot: Materials
) {
  operator fun get(material: Material) = when (material) {
    ORE -> oreRobot
    CLAY -> clayRobot
    OBSIDIAN -> obsidianRobot
    GEODE -> geodeRobot
  }
}
typealias Factory = (Materials, Int, Robots) -> List<Pair<Robots, Materials>>

data class State(val time: Int, val materials: Materials, val robots: Robots)

class Upgrader {

  private val cacheByRobots = mutableMapOf<Int, MutableMap<Robots, MutableList<State>>>()
  private val cacheByMaterials = mutableMapOf<Int, MutableMap<Materials, MutableList<State>>>()
  private val cacheByRobotsAndMaterials = mutableMapOf<Robots, MutableMap<Materials, State>>()

  fun upgrade(state: State): Pair<State, Upgrader> {
    getBetterByRobots(state)?.let { return it to this }
    getBetterByMaterials(state)?.let { return it to this }
    getBetterByTime(state)?.let { return it to this }

    return state to withState(state)
  }

  private fun withState(state: State): Upgrader {
    cacheByMaterials.getOrPut(state.time) { mutableMapOf() }.getOrPut(state.materials) { mutableListOf() }
      .let { it.also { it.removeIf { elem -> atLeastAsManyRobots(state, elem) } }.add(state) }
    cacheByRobotsAndMaterials.getOrPut(state.robots) { mutableMapOf() }[state.materials] = state
    cacheByRobots.getOrPut(state.time) { mutableMapOf() }.getOrPut(state.robots) { mutableListOf() }
      .let { it.also { it.removeIf { elem -> atLeastAsManyMaterials(state, elem) } }.add(state) }
    return this
  }

  private fun getBetterByTime(state: State): State? =
    cacheByRobotsAndMaterials[state.robots]?.get(state.materials)?.let {
      if (it.time < state.time) it else null
    }

  private fun getBetterByMaterials(state: State): State? =
    cacheByMaterials[state.time]?.get(state.materials)
      ?.firstOrNull { candidate -> atLeastAsManyRobots(candidate, state) }

  private fun getBetterByRobots(state: State): State? =
    cacheByRobots[state.time]?.get(state.robots)?.firstOrNull { candidate -> atLeastAsManyMaterials(candidate, state) }

}