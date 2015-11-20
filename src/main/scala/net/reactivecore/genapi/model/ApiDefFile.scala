package net.reactivecore.genapi.model

case class Position (file: Option[String] = None, line: Int = 0) {
  override def toString = s"${file.getOrElse("<unknown>")}:$line"
}

case class ApiDefFile (controllerDefinitions: Vector[ControllerDefinition])

case class ControllerDefinition (name: String,
                                  generatorName: String,
                                  arguments: List[String] = Nil,
                                  commands: Vector[Command] = Vector.empty
)(val position: Position = Position())

sealed trait ParameterSupply

/** Parameter is supplied as a query parameter, used when no other values match.*/
case object QueryParameter extends ParameterSupply
/** Parameter is supplied using the path (via colon-Syntax). This is auto detected*/
case object PathParameter extends ParameterSupply
/** Parameter is supplied as POST/PUT-Element using JSON. Detected using @ Notation */
case object JsonParameter extends ParameterSupply

case class CommandParameter (name: String, parameterType: String, parameterSupply: ParameterSupply)

case class Command (
  httpMethod: String,
  path: String,
  serviceClass: String,
  serviceMethod: String,
  params: List[CommandParameter]
)(val position: Position = Position()) {


  /** Makes a hopefully scala-compatible action name for this command. */
  def actionName: String = {
    "action_" + httpMethod.toLowerCase() +
    toCamelCase(path
      .replace("/", "_")
      .replace(":", "_"))
  }

  private def toCamelCase (in: String): String = {
    val builder = new StringBuilder()
    var upperCase = false
    in.indices.foreach { i =>
      val c = in.charAt(i)
      c match {
        case '_' =>
          upperCase = true
        case x =>
          if (upperCase) {
            upperCase = false
            builder.append(x.toUpper)
          } else builder.append(x)
      }
    }
    builder.toString()
  }
}

