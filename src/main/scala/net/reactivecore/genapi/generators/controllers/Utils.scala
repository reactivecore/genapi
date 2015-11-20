package net.reactivecore.genapi.generators.controllers

object Utils {
  /** Gets a valid service name for a service class. */
  def serviceVariableName(serviceClass: String): String = {
    val classItSelf = serviceClass.split("\\.").toList.reverse.head
    classItSelf.charAt(0).toLower + classItSelf.drop(1)
  }

  def stripEmptyNewlines(in: String): String = {
    in.lines.filterNot(_.trim.isEmpty).mkString("\n")
  }

  def formatNesting(in: String): String = {
    // Note: it doesn't look into comments, but for our generated code it should be fine.
    var depth = 0
    in.lines.map { line =>
      val cleaned = line.trim()
      val addSub = cleaned.count(_ == '{') - cleaned.count(_ == '}')
      val result = if (cleaned.startsWith("}")){
        depth += addSub
        ("  " * depth) + cleaned
      } else {
        val result = ("  " * depth) + cleaned
        depth += addSub
        result
      }
      result
    }.mkString("\n")
  }
}
