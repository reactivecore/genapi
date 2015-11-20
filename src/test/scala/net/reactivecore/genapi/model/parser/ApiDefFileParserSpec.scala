package net.reactivecore.genapi.model.parser

import net.reactivecore.genapi.SpecBase
import net.reactivecore.genapi.model._

class ApiDefFileParserSpec extends SpecBase {

  trait Env {
    val parser = new ApiDefFileParser()
  }

  val simple =
    """
      |# This is a comment
      |
      |controller MyApi default
      |
      | GET /get_result Service1.myCommand
    """.stripMargin

  it should "parse a simple file" in new Env {
    val parsed = parser.parse(simple)
    parsed.controllerDefinitions.size shouldBe 1
    val controller1 = parsed.controllerDefinitions.head
    controller1.generatorName shouldBe "default"
    controller1.name shouldBe "MyApi"
    controller1.commands.size shouldBe 1

    val command1 = controller1.commands.head
    command1.httpMethod shouldBe "GET"
    command1.path shouldBe "/get_result"
    command1.serviceClass shouldBe "Service1"
    command1.serviceMethod shouldBe "myCommand"
    command1.params shouldBe empty
  }

  val withControllerArguments =
    """
      |# This is a comment
      |
      |controller MyApi default Arg1 Arg2 Arg3
      |
      | GET /get_result Service1.myCommand
    """.stripMargin

  it should "parse a file with controller arguments" in new Env {
    val parsed = parser.parse(withControllerArguments)
    parsed.controllerDefinitions.size shouldBe 1
    val controller1 = parsed.controllerDefinitions.head
    controller1.generatorName shouldBe "default"
    controller1.name shouldBe "MyApi"
    controller1.commands.size shouldBe 1
    controller1.arguments shouldBe List("Arg1", "Arg2", "Arg3")
  }


  val withPathParameters =
    """|# This is a comment
       |
       |controller MyApi default
       |
       | GET /get_result/:path Service1.myCommand(path)
       | POST /post_result/:path Service1.myCommand (path:com.example.model.Data)
       |
       |""".stripMargin

  it should "parse path parameters" in new Env {
    val parsed = parser.parse(withPathParameters)
    parsed.controllerDefinitions.size shouldBe 1
    val controller1 = parsed.controllerDefinitions.head
    controller1.commands.size shouldBe 2
    controller1.commands(0) shouldBe Command ("GET", "/get_result/:path", "Service1", "myCommand", List(CommandParameter("path", "String", PathParameter)))()
    controller1.commands(1) shouldBe Command ("POST", "/post_result/:path", "Service1", "myCommand", List(CommandParameter("path", "com.example.model.Data", PathParameter)))()
  }

  val withJsonParameters =
    """|# This is a comment
      |
      |controller MyApi default
      |
      | POST /post_result Service1.myCommand (data:@com.example.model.Data)
      |
      |""".stripMargin

  it should "parse json parameters" in new Env {
    val parsed = parser.parse(withJsonParameters)
    parsed.controllerDefinitions.size shouldBe 1
    val controller1 = parsed.controllerDefinitions.head
    controller1.commands.size shouldBe 1
    controller1.commands(0) shouldBe Command ("POST", "/post_result", "Service1", "myCommand", List(CommandParameter("data", "com.example.model.Data", JsonParameter)))()
  }

  val complex =
    """|# This is a comment
      |
      |controller MyApi default
      |
      | GET /get_result Service1.myCommand
      | POST /post_result Service1.myCommand (data : com.example.model.Data)
      |
      | # Another comment
      |controller OtherController authenticated
      |# Comment in between
      | PUT /post_result com.example.Service2.myCommand (str, data : com.example.model.Data)
      |
      |controller ThirdController complex
      | PUT /all_types/:path Service3.myCommand(param1,param2: Int, path: UUID, data: @com.example.model.Data)
      |""".stripMargin

  it should "parse a more complex case" in new Env {
    val parsed = parser.parse(complex)
    parsed.controllerDefinitions.size shouldBe 3
    val controller1 = parsed.controllerDefinitions.head
    controller1.generatorName shouldBe "default"
    controller1.name shouldBe "MyApi"

    controller1.commands.size shouldBe 2
    controller1.commands(0) shouldBe Command ("GET", "/get_result", "Service1", "myCommand", List.empty)()
    controller1.commands(1) shouldBe Command ("POST", "/post_result", "Service1", "myCommand", List (CommandParameter("data", "com.example.model.Data", QueryParameter)))()

    val controller2 = parsed.controllerDefinitions(1)
    controller2.generatorName shouldBe "authenticated"
    controller2.name shouldBe "OtherController"
    controller2.commands.size shouldBe 1
    controller2.commands(0) shouldBe Command ("PUT", "/post_result", "com.example.Service2", "myCommand", List(CommandParameter("str", "String", QueryParameter), CommandParameter("data", "com.example.model.Data", QueryParameter)))()

    val controller3 = parsed.controllerDefinitions(2)
    controller3.generatorName shouldBe "complex"
    controller3.commands.size shouldBe 1
    controller3.commands(0) shouldBe Command ("PUT", "/all_types/:path", "Service3", "myCommand",
      List(
        CommandParameter("param1", "String", QueryParameter),
        CommandParameter("param2", "Int", QueryParameter),
        CommandParameter("path", "UUID", PathParameter),
        CommandParameter("data", "com.example.model.Data", JsonParameter)
      )
    )()
  }

  it should "contain well tested parsers" in {
    "MyClass.cmmand" should (fullyMatch regex ApiDefFileParser.ServiceCallRegex)
    "# A comment" should (fullyMatch regex ApiDefFileParser.CommentRegEx)
    "controller MyController MyGenerator" should (fullyMatch regex ApiDefFileParser.ControllerRegex)
    "controller 24m2l35132± wd'`%%" should (fullyMatch regex ApiDefFileParser.ControllerRegex)
    "controller MyController MyGenerator Arg1 Arg2" should (fullyMatch regex ApiDefFileParser.ControllerWithArgs)


    "GET" should (fullyMatch regex ApiDefFileParser.Word)
    "/get_result" should (fullyMatch regex ApiDefFileParser.Word)
    "Service1.MyCommand" should (fullyMatch regex ApiDefFileParser.ServiceCallRegex)
    "GET /get_result Service1.myCommand" should (fullyMatch regex ApiDefFileParser.RouteCommandEmpty)
    "GET /get_result Service1.myCommand ()" should (fullyMatch regex ApiDefFileParser.RouteCommandEmpty)
    "GET /get_result Service1.myCommand()" should (fullyMatch regex ApiDefFileParser.RouteCommandEmpty)
    "GET /get_result Service1.myCommand(alpha,beta)" should (fullyMatch regex ApiDefFileParser.RouteCommandWithArguments)
    "GET /get_result Service1.myCommand(alpha:String,beta:net.reactivecore.model.ComplexObject)" should (fullyMatch regex ApiDefFileParser.RouteCommandWithArguments)
    "POST /post_result Service1.myCommand (data : net.reactivecore.model.Data)" should (fullyMatch regex ApiDefFileParser.RouteCommandWithArguments)
    "data" should (fullyMatch regex ApiDefFileParser.ParamWithoutType)
    "data : String" should (fullyMatch regex ApiDefFileParser.ParamWithType)
    "data : com.example.model.Data" should (fullyMatch regex ApiDefFileParser.ParamWithType)

    ApiDefFileParser.PathParam.findAllMatchIn("/data/:param1/:param2/lala").map(_.matched).toList shouldBe List(":param1", ":param2")

  }
}
