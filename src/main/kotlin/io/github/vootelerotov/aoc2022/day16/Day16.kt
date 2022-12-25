package io.github.vootelerotov.aoc2022.day16

import io.github.vootelerotov.util.readResourceLines

fun main() {

  val lines = readResourceLines("aoc2022/day16/input-small.txt")

  val (startingValve, valves) = parseValves(lines)
  val (condensedStartingValve, condensedValves) = condenseValves(startingValve, valves)
  val minimumTimeToTurnOnAdjacentValve = condensedValves.flatMap { it.tunnels }.minOf { (_, distance) -> distance } + 1
  val potentialCalculator = potentialCalculator(condensedValves.toSet(), minimumTimeToTurnOnAdjacentValve)

  // 1st
  println(findCondensedSolution(State(condensedStartingValve, time = 30), potentialCalculator).guaranteedFlow)

  // 2nd
  val elephantCalc = potentialCalculatorWithElephant(condensedValves.toSet(), minimumTimeToTurnOnAdjacentValve)
  val startingState = StateWithElephant(condensedStartingValve to 26, condensedStartingValve to 26, 0, emptySet())
  findCondensedSolution(startingState, elephantCalc, time = 26).guaranteedFlow.let(::println)
}

fun findCondensedSolution(state: State, potentialCalc: (State) -> Int, bestSoFar: State = state): State {
  val potential = potentialCalc(state)
  bestSoFar.takeIf { it.guaranteedFlow > state.guaranteedFlow + potential }?.let { return bestSoFar }

  val currentBest = bestSoFar.takeIf { it.guaranteedFlow > state.guaranteedFlow } ?: state

  return condensedMoves(state).fold(currentBest) { latestBest, move ->
    findCondensedSolution(move, potentialCalc, latestBest)
  }
}

fun findCondensedSolution(
  state: StateWithElephant,
  potentialCalc: (StateWithElephant) -> Int,
  time: Int,
  visited: Set<CondensedValve> = emptySet(),
  bestSoFar: StateWithElephant = state
): StateWithElephant {

  val potential = potentialCalc(state)
  bestSoFar.takeIf { it.guaranteedFlow > state.guaranteedFlow + potential }?.let { return bestSoFar }

  val newVisited = visited + state.one.first + state.other.first
  val currentBest = bestSoFar.takeIf { it.guaranteedFlow > state.guaranteedFlow } ?: state

  return moves(state, time).fold(currentBest) { latestBest, move ->
    findCondensedSolution(move, potentialCalc, time - 1, newVisited, latestBest)
  }
}

fun condensedMoves(state: State): List<State> = state.let { (pos, flow, openValves, time) ->
  pos.tunnels.filterNot { (valve, _) -> valve in openValves }.mapNotNull { (valve, distance) ->
    (time - distance - 1).takeIf { it >= 0 }?.let { endTime ->
      State(valve, flow + (valve.flowRate * endTime), openValves.plus(valve), endTime)
    }
  }
}

fun moves(state: StateWithElephant, time: Time): List<StateWithElephant> {

  val (onePos, oneTime) = state.one
  val statesAfterMoves = if (oneTime == time) {
    onePos.tunnels.filterNot { (pos, _) -> state.openValves.contains(pos) }
      .mapNotNull { (pos, distance) ->
        (time - distance - 1).takeIf { it >= 0 }?.let { endTime ->
          state.copy(
            one = pos to endTime,
            guaranteedFlow = state.guaranteedFlow + pos.flowRate * endTime,
            openValves = state.openValves + pos
          )
        }
      }
  } else {
    listOf(state)
  }

  return statesAfterMoves.flatMap { stateAfterOne ->
    val (twoPos, twoTime) = stateAfterOne.other
    if (twoTime == time) {
      twoPos.tunnels.filterNot { (pos, _) -> stateAfterOne.openValves.contains(pos) }
        .mapNotNull { (pos, distance) ->
          (time - distance - 1).takeIf { it >= 0 }?.let { endTime ->
            stateAfterOne.copy(
              other = pos to endTime,
              guaranteedFlow = stateAfterOne.guaranteedFlow + pos.flowRate * endTime,
              openValves = stateAfterOne.openValves + pos
            )
          }
        }
    } else {
      listOf(stateAfterOne)
    }
  }
}

fun parseValves(lines: List<String>): Pair<Valve, Collection<Valve>> {
  lateinit var valves: Map<ID, Valve>
  valves = lines.map { line ->
    "Valve ([A-Z]+) has flow rate=(\\d+); tunnels? leads? to valves? (.+)".toRegex().toPattern().matcher(line)
      .let { matcher ->
        if (matcher.matches()) Valve(
          id = matcher.group(1),
          flowRate = matcher.group(2).toInt(),
          tunnels = { matcher.group(3).split(", ").map { valves[it]!! } }
        ) else throw IllegalArgumentException("Invalid line: $line")
      }
  }.associateBy(Valve::id)
  return valves["AA"]!! to valves.values
}

fun condenseValves(valve: Valve, valves: Collection<Valve>): Pair<CondensedValve, Collection<CondensedValve>> {
  lateinit var newValves: Map<ID, CondensedValve>
  newValves = valves.associateBy { it.id }.mapValues { (_, valve) ->
    CondensedValve(
      id = valve.id,
      flowRate = valve.flowRate,
      tunnels = { findConnectionToValvesWithFlow(valve).map { (id, time) -> newValves[id]!! to time } }
    )
  }
  return newValves[valve.id]!! to newValves.values.filter { it.flowRate > 0 }
}

fun findConnectionToValvesWithFlow(valve: Valve): List<Pair<ID, Time>> = bfsValve(listOf(valve to 0))

fun bfsValve(layer: List<Pair<Valve, Time>>, visited: Set<Valve> = emptySet()): List<Pair<ID, Time>> {
  if (layer.isEmpty()) {
    return listOf()
  }

  val newVisited = visited + layer.map { (valve, _) -> valve }

  val nextLayer = layer.flatMap { (valve, time) ->
    valve.tunnels.filterNot(newVisited::contains).map { nextValve -> nextValve to time + 1 }
  }
  return nextLayer.filter { (valve, _) -> valve.flowRate > 0 }.distinct().map { (valve, time) ->
    valve.id to time
  } + bfsValve(nextLayer, newVisited)
}

fun potentialCalculator(allValves: Set<CondensedValve>, minimumTimeToOpen: Int): (State) -> Int = { state ->
  val potentialFlows = allValves.minus(state.openValves).map { it.flowRate }.sortedDescending()
  val openTimesForValves = generateSequence(state.time - minimumTimeToOpen) { it - minimumTimeToOpen }
    .takeWhile { it > 0 }
    .toList()
  openTimesForValves.zip(potentialFlows).sumOf { (time, flow) -> time * flow }
}

fun potentialCalculatorWithElephant(
  allValves: Set<CondensedValve>,
  minimumTimeToOpen: Int
): (StateWithElephant) -> Int = { state ->
  val potentialFlows = allValves.minus(state.openValves).map { it.flowRate }.sortedDescending()
  val myOpenTimesForValves = generateSequence(state.one.let { (_, time) -> time }) { it - minimumTimeToOpen }
    .takeWhile { it > 0 }
    .toList()
  val elephantOpenTimesForValves = generateSequence(state.other.let { (_, time) -> time }) { it - minimumTimeToOpen }
    .takeWhile { it > 0 }
    .toList()

  (myOpenTimesForValves + elephantOpenTimesForValves).zip(potentialFlows).map { (time, flow) -> time * flow }.sum()
}

typealias ID = String
typealias FlowRate = Int
typealias Time = Int

class Valve(val id: ID, val flowRate: FlowRate, tunnels: () -> List<Valve>) {
  val tunnels: List<Valve> by lazy(tunnels)
}

class CondensedValve(val id: ID, val flowRate: FlowRate, tunnels: () -> List<Pair<CondensedValve, Time>>) {
  val tunnels: List<Pair<CondensedValve, Time>> by lazy(tunnels)

  override fun toString() = id
}

data class State(
  val position: CondensedValve,
  val guaranteedFlow: Int = 0,
  val openValves: Set<CondensedValve> = emptySet(),
  val time: Time
) {
  override fun toString(): String {
    return "State(position=${position.id}, guaranteedFlow=$guaranteedFlow, openValves=${openValves.map { it.id }}, time=$time)"
  }
}

data class StateWithElephant(
  val one: Pair<CondensedValve, Time>,
  val other: Pair<CondensedValve, Time>,
  val guaranteedFlow: Int,
  val openValves: Set<CondensedValve>
)
