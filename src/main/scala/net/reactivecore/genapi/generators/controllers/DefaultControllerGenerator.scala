package net.reactivecore.genapi.generators.controllers

import net.reactivecore.genapi.model._
import sbt.File

class DefaultControllerGenerator extends ControllerGenerator {

  private class Builder (definition: ControllerDefinition) {
    val services: Map[String, String] = definition.commands.map(_.serviceClass).distinct.map (serviceClass => serviceClass -> (Utils.serviceVariableName(serviceClass))).toMap

    val baseClass = definition.arguments.headOption.getOrElse("DefaultGenApiControllerBase")

    val injectionVariables = services
      .map { case (className, name) => s"$name: $className"}
      .mkString(",")

    val header = s"""package generated
                    |
                    |// This file is auto-generated using genapi. All changes will be lost.
                    |
                    |import play.api.mvc._
                    |import javax.inject.Inject
                    |
                    |
                    |class ${definition.name} @Inject() ($injectionVariables) extends ${baseClass} {
                    |
                    |""".stripMargin

    def formatCall(in: Command): String = {
      val serviceVariable = services(in.serviceClass)

      Utils.stripEmptyNewlines(
        s"""def ${in.actionName}(${formatInArgumentList(in)}) = actionBuilder.async { implicit request =>
           |  catchErrors {
           |    ${decodeParams(in)}
           |    $serviceVariable.${in.serviceMethod}(${formatArgumentList(in)}).map (formatResult(_))
           |  }
           |}
      """.stripMargin)
    }

    def formatInArgumentList(in: Command): String = {
      in.params
        .filter(p => p.parameterSupply == QueryParameter || p.parameterSupply == PathParameter)
        .map { p => p.name + ":" + p.parameterType }
        .mkString(",")
    }

    def formatArgumentList(in: Command) = in.params.map(_.name).mkString(", ")

    def decodeParams(in: Command): String = {
      require(in.params.count(_.parameterSupply == JsonParameter) <= 1, s"Only one parameter can be a JSON parameter in ${in.position}")
      in.params.map(decodeParam).mkString("\n")
    }

    def decodeParam(commandParameter: CommandParameter): String = {
      commandParameter.parameterSupply match {
        case JsonParameter =>
         s"val ${commandParameter.name} = parseJsonInput[${commandParameter.parameterType}](request)"
        case _ =>
          // nothing to decode
          ""
      }
    }

    val footer =
      s"""
         |}
     """.stripMargin

    def full() =
      Utils.formatNesting(
        header +
        definition.commands.map(formatCall).mkString("\n\n") +
        footer
      )
  }





  override def generateController(definition: ControllerDefinition): String = {
    new Builder(definition).full()
  }

  override def needDependencyFile: Boolean = true

  override def generateDependency(): String =
    """package generated
      |// This file is auto-generated using genapi. All changes will be lost.
      |
      |import play.api.mvc._
      |import javax.inject.Inject
      |import play.api.libs.json._
      |import scala.concurrent.ExecutionContext
      |import scala.concurrent.Future
      |
      |class DefaultGenApiControllerBase extends Controller{
      |  implicit protected def ec: ExecutionContext = play.api.libs.concurrent.Execution.defaultContext
      |  import DefaultGenApiControllerBase._
      |
      |  protected def formatResult[T](result: T)(implicit writes: Writes[T]): Result = {
      |    Ok(writes.writes(result))
      |  }
      |
      |  protected val actionBuilder = Action
      |
      |  protected def formatResult(unit: Unit): Result = {
      |    Ok
      |  }
      |
      |  protected def formatResult(str: String): Result = {
      |    Ok(str)
      |  }
      |
      |  protected def formatResult(result: Result): Result = result
      |
      |  protected def parseJsonInput[T](in: Request[AnyContent])(implicit reads: Reads[T]): T = {
      |    val json = in.body.asJson.getOrElse (throw new ExpectedJsonException("Expected JSON input"))
      |    json.asOpt[T].getOrElse (throw new CouldNotParseJsonObjectException("Could not parse required JSON object"))
      |  }
      |
      |  protected val errorHandler: PartialFunction[Throwable, Future[Result]] = {
      |    case e: ExpectedJsonException => Future.successful(BadRequest(e.getMessage))
      |    case e: CouldNotParseJsonObjectException => Future.successful(BadRequest(e.getMessage))
      |  }
      |
      |  protected def catchErrors[T](in: => Future[Result]): Future[Result] = {
      |   (try {
      |     in
      |   } catch {
      |     errorHandler
      |   }).recoverWith(errorHandler)
      |  }
      |
      |}
      |
      |object DefaultGenApiControllerBase {
      |  /** JSON input expected. */
      |  class ExpectedJsonException(msg: String, cause: Throwable = null) extends IllegalArgumentException(msg, cause)
      |
      |  /** Object could not parsed into the requested type. */
      |  class CouldNotParseJsonObjectException(msg: String, cause: Throwable = null) extends IllegalArgumentException(msg, cause)
      |}
      |""".stripMargin
}
