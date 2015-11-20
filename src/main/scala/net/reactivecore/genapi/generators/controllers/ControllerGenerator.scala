package net.reactivecore.genapi.generators.controllers

import java.nio.charset.Charset

import net.reactivecore.genapi.model.ControllerDefinition
import sbt.{IO, File}

import scala.io.Codec

trait ControllerGenerator {
  def generateController(definition: ControllerDefinition): String

  /** Generator needs a dependency file to be generated before. */
  def needDependencyFile: Boolean = false

  def generateDependency(): String = ""
}
