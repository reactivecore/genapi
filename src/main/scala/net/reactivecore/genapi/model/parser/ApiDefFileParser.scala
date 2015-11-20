package net.reactivecore.genapi.model.parser

import net.reactivecore.genapi.model._
import sbt.File

import scala.io.{Codec, Source}

class ApiDefFileParser {
  // TODO: Replace with Real-Scala parser

  import ApiDefFileParser._

  def parse(in: String): ApiDefFile = {
    parseAndTranslateLines(in.lines.toList, filename = None)
  }

  def parseFile(in: File): ApiDefFile = {
    implicit val codec = Codec.UTF8
    val content = Source.fromFile(in).getLines()
    parseAndTranslateLines(content.toList, Some(in.toString))
  }

  private[parser] def parseAndTranslateLines(lines: List[String], filename: Option[String]): ApiDefFile = {
    val parsedLines = parseLines(lines, filename)
    translate(parsedLines)
  }

  private[parser] def parseLine(line: String, position: Position): Line = {
    val trimmed = line.trim()
    trimmed match {
      case EmptyRegex() => Empty(position)
      case CommentRegEx(comment) => Comment(position, comment)
      case ControllerRegex(name, generator) =>
        ControllerCommand(position, name, generator, Nil)
      case ControllerWithArgs (name, generator, arguments) =>
        ControllerCommand(position, name, generator, WhiteSplit.split(arguments).toList)
      case RouteCommandEmpty (method, path, serviceCall) => RouteCommand(position, method, path, serviceCall, "")
      case RouteCommandWithArguments (method, path, serviceCall, argumentList) => RouteCommand(position, method, path, serviceCall, argumentList)
      case _ => throw new IllegalArgumentException(s"Could not parse line ${position}: $line")
    }
  }

  private[parser] def parseLines(lines: List[String], filename: Option[String]): List[Line] = {
    lines.zipWithIndex.map {
      case (line, i) => parseLine(line, Position(filename, i + 1))
    }
  }


  private[parser] def translate(lines: List[Line]): ApiDefFile = {
    lines.foldLeft(TranslationState()) { (state, line) =>
      line match {
        case cc: ControllerCommand =>
          val definition = ControllerDefinition(name = cc.name, generatorName = cc.generator, arguments = cc.arguments, commands = Vector.empty)(position = cc.position)
          state.withControllerDef(definition)
        case rc: RouteCommand =>
          val command = rc.parseCommand()
          state.withNewCommand(command)
        case _ =>
          // ignore
          state
      }
    }.result
  }
}

private[parser] object ApiDefFileParser {
  val Word = "(\\S+)"
  val White = "\\s+"
  val WhiteSplit = White.r

  val EmptyRegex = "^$".r
  val CommentRegEx = "^#(.*)$".r

  val ControllerRegex = s"^controller${White}${Word}${White}${Word}$$".r

  val ControllerWithArgs = s"^controller${White}${Word}${White}${Word}${White}(.*)$$".r
  // Routes without arguments (usually only sense ful for getters)

  val ServiceCallRegex = "([a-zA-Z_\\$]?[a-zA-Z_\\$\\.\\d]*[\\da-zA-Z_\\$])"

  val RouteCommandEmpty = s"^$Word$White$Word$White$ServiceCallRegex(?:\\s*\\(\\))?$$".r
  val RouteCommandWithArguments = s"^$Word$White$Word$White$ServiceCallRegex\\s*\\(([^\\)]+)\\)$$".r

  val PathParam = ":[^/]+".r
  val ParamWithType = "^([^,:\\(\\) ]+)\\s*:\\s*([^,:\\(\\) ]+)$".r
  val ParamWithoutType = "^([^,:\\)]+)$".r

  sealed trait Line {
    def position: Position
  }

  case class Empty(position: Position) extends Line

  case class Comment(position: Position, msg: String) extends Line

  case class ControllerCommand (position: Position, name: String, generator: String, arguments: List[String]) extends Line

  case class RouteCommand(position: Position, method: String, path: String, serviceCall: String, argumentList: String) extends Line {
    def parseCommand(): Command = {
      serviceCall.split("\\.").toList.reverse match {
        case methodName :: fullQualifiedClass if fullQualifiedClass.nonEmpty =>
          val className = fullQualifiedClass.reverse.mkString(".")
          val pathParams = extractPathParameters(path)
          Command(method, path, className, methodName, parseCommandParameters(pathParams))(position = position)
        case _ => throw new IllegalArgumentException(s"Could not parse service call ${serviceCall}")
      }
    }

    private def extractPathParameters(path: String): List[String] = {
      PathParam.findAllMatchIn(path).map(_.matched.stripPrefix(":")).toList
    }

    def parseCommandParameters(pathParams: List[String]): List[CommandParameter] = {
      val trimmed = argumentList.trim()
      if (trimmed.isEmpty){
        Nil
      } else {
        trimmed.split(",").toList.map(parameter => parseCommandParameter(parameter, pathParams))
      }
    }

    def parseCommandParameter(param: String, pathParams: List[String]): CommandParameter = {
      def build(name: String, parameterType: String = "String"): CommandParameter = {
        val paramSupply = if (pathParams.contains(name)){
          PathParameter
        } else if (parameterType.startsWith("@")){
          JsonParameter
        } else {
          QueryParameter
        }
        CommandParameter(name, parameterType.stripPrefix("@"), paramSupply)
      }
      param.trim() match {
        case ParamWithType(name, parameterType) => build(name, parameterType)
        case ParamWithoutType(name) => build(name)
        case _ =>
          throw new IllegalArgumentException(s"Could not parse parameter type $param on line $position")
      }
    }
  }

  case class TranslationState(
                    currentController: Option[ControllerDefinition] = None,
                    inConstruction: ApiDefFile = ApiDefFile(Vector.empty)) {

    def withControllerDef (controllerDefinition: ControllerDefinition): TranslationState = {
      currentController match {
        case Some(oldController) =>
          copy(currentController = Some(controllerDefinition), inConstruction = inConstruction.copy(controllerDefinitions = inConstruction.controllerDefinitions :+ oldController))
        case None =>
          // First controller definition
          copy(currentController = Some(controllerDefinition))
      }
    }

    def withNewCommand (command: Command): TranslationState = {
      val controller = currentController.getOrElse(throw new IllegalArgumentException(s"No controller set in ${command.position}"))
      copy(currentController = Some(controller.copy(
        commands = controller.commands :+ command
      )(position = controller.position)))
    }

    def result: ApiDefFile = {
      currentController match {
        case Some(controller) =>
          inConstruction.copy(controllerDefinitions = inConstruction.controllerDefinitions :+ controller)
        case None =>
          inConstruction
      }
    }
  }
}

