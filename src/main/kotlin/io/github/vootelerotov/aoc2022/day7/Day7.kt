package io.github.vootelerotov.aoc2022.day7

import io.github.vootelerotov.util.head
import io.github.vootelerotov.util.readResourceLines

fun main() {
  val lines = readResourceLines("aoc2022/day7/input.txt")

  val terminalLines = lines.map(::refine)

  val (_, files) = parsePathFiles(terminalLines)
  val fileTree = convertToFileTree("/", files.toList())
  // println(toString(fileTree))

  // 1st
  directorySizes(fileTree).filter { it < 100_000 }.sum().let(::println)

  // 2nd
  val totalSpace = 70_000_000
  val currentFreeSpace = totalSpace - fileSize(fileTree)
  val requiredUnusedSpace = 30_000_000
  directorySizes(fileTree).filter { it + currentFreeSpace > requiredUnusedSpace }.minBy { it }.let(::println)

}

fun directorySizes(file: File): List<Int> = when (file) {
  is Dir -> file.files.flatMap(::directorySizes) + fileSize(file)
  is RegularFile -> emptyList()
}

fun fileSize(file: File): Int = when (file) {
  is Dir -> file.files.map(::fileSize).sum()
  is RegularFile -> file.size
}

// I realllly wanted to do this without mutable data structures, this is what I came up with
fun convertToFileTree(name: String, files: List<RegularFileWithPath>, parent: () -> File? = { null }): File {
  lateinit var dir: Dir
  dir = Dir(
    name,
    files
      .groupBy({ it.path.first() }, { it.copy(path = it.path.drop(1)) })
      .flatMap { (first, files) ->
        files.partition { it.path.isEmpty() }.let { (regularFiles, dirs) ->
          regularFiles.map {
            RegularFile(it.name, it.size) { dir }
          } + if (dirs.isEmpty()) emptyList() else listOf(convertToFileTree(first, dirs) { dir })
        }
      },
    parent
  )
  return dir
}

fun parsePathFiles(terminalLines: List<TerminalLine>) =
  terminalLines.fold(initialContext()) { context, terminalLine ->
    when (terminalLine) {
      is CmdLine -> terminalLine.let { (cmd) -> handleCmd(context, cmd) }
      is FileLine -> terminalLine.let { (rawLine) -> handleFile(context, rawLine) }
    }
  }

fun handleFile(context: PathContext, rawFile: RawFile) = when (rawFile) {
  is RawDir -> context
  is RawRegularFile -> context.copy(files = context.files.plusElement(simpleFileWithPath(context.currentPath, rawFile)))
}

fun simpleFileWithPath(currentPath: Path, rawRegularFile: RawRegularFile) =
  RegularFileWithPath(rawRegularFile.name, rawRegularFile.size, currentPath.plusElement(rawRegularFile.name))

private fun handleCmd(context: PathContext, cmd: Cmd): PathContext = when (cmd) {
  is Cd -> context.copy(currentPath = handleCdArg(context.currentPath, cmd.path))
  is Ls -> context
}

fun handleCdArg(currentPath: Path, path: String): Path = when (path) {
  "." -> currentPath
  ".." -> currentPath.dropLast(1)
  else -> currentPath.plusElement(path)
}

fun toString(file: File): String = when (file) {
  is Dir -> "- ${file.name}\n" + file.files.joinToString("\n") { toString(it).prependIndent("  ") }
  is RegularFile -> "- ${file.name} (${file.size})"
}

fun initialContext() = PathContext(listOf(), setOf())

fun refine(line: String): TerminalLine = head(line.split(" "))!!.let { (first, rest) ->
  when (first) {
    "$" -> refineCommand(rest)
    "dir" -> rest.let { (name) -> FileLine(RawDir(name)) }
    else -> rest.let { (name) -> refineRegularFile(name, first) }
  }
}

fun refineRegularFile(name: String, rawSize: String): FileLine = FileLine(RawRegularFile(name, rawSize.toInt()))

fun refineCommand(cmd: List<String>): CmdLine = head(cmd)!!.let { (first, rest) ->
  when (first) {
    "ls" -> CmdLine(Ls)
    "cd" -> rest.let { (argument) -> CmdLine(Cd(argument)) }
    else -> throw IllegalArgumentException("Unknown command $first")
  }
}

typealias Path = List<String>

data class PathContext(val currentPath: Path, val files: Set<RegularFileWithPath>)

data class RegularFileWithPath(val name: String, val size: Int, val path: Path)

sealed class File(val name: String, parent: () -> File?) {
  val parent: File? by lazy(parent)
}

class Dir(name: String, val files: List<File>, parent: () -> File?) : File(name, parent)
class RegularFile(name: String, val size: Int, parent: () -> File?) : File(name, parent)

sealed interface Cmd
data class Cd(val path: String) : Cmd
object Ls : Cmd

sealed interface RawFile
data class RawDir(val name: String) : RawFile
data class RawRegularFile(val name: String, val size: Int) : RawFile

sealed interface TerminalLine
data class FileLine(val file: RawFile) : TerminalLine
data class CmdLine(val cmd: Cmd) : TerminalLine
