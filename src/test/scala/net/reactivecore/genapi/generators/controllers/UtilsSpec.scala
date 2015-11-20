package net.reactivecore.genapi.generators.controllers

import net.reactivecore.genapi.SpecBase

class UtilsSpec extends SpecBase {

  "serviceVariableName" should "generate nice variable names" in {
    Utils.serviceVariableName("UserCustomerService") shouldBe "userCustomerService"
    Utils.serviceVariableName("com.example.project1.UserCustomerService") shouldBe "userCustomerService"
  }

  "stripEmptyNewlines" should "strip empty new lines" in {
    Utils.stripEmptyNewlines(
      """
        |Hello
        |
        |
        |This is a
        |
        |newline""".stripMargin) shouldBe
    """Hello
      |This is a
      |newline""".stripMargin
  }

  "formatNesting" should "format the nesting" in {
    val formatted = Utils.formatNesting(
    """
      |def abc(): Int {
      |var x = 3
      |if (x == 4){
      |  x = 5
      |}
      |x
      |}""".stripMargin
    )
    formatted shouldBe
    """
      |def abc(): Int {
      |  var x = 3
      |  if (x == 4){
      |    x = 5
      |  }
      |  x
      |}""".stripMargin
  }

}
