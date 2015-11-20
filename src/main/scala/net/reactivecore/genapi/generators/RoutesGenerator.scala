package net.reactivecore.genapi.generators

import java.nio.charset.Charset

import net.reactivecore.genapi.model._
import sbt.{IO, File}

class RoutesGenerator {
  val header = """
                 |# This file is auto-generated using genapi. All changes will be lost.
                 |
                 |""".stripMargin

  def generateRoutes(definition: ApiDefFile): String = {
    val content = (definition.controllerDefinitions.map { controllerDef =>
      s"# Controller ${controllerDef.name}\n" +
      controllerDef.commands.map { command =>
        formatControllerCall(command, controllerDef)
      }.mkString("\n")
    }.mkString("\n\n"))
    header + content
  }

  def formatControllerCall(command: Command, controllerDef: ControllerDefinition): String = {
    val paramList = command.params.filter(p => p.parameterSupply == QueryParameter || p.parameterSupply == PathParameter)
    val formattedParams = paramList.map { param =>
      param.parameterType match {
        case "String" => param.name
        case _ => param.name + ":" + param.parameterType
      }
    }.mkString(",")

    s"${command.httpMethod} ${command.path} generated.${controllerDef.name}.${command.actionName}($formattedParams)"
  }
}
