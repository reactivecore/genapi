package net.reactivecore.genapi

import net.reactivecore.genapi.generators.RoutesGenerator
import net.reactivecore.genapi.generators.controllers.DefaultControllerGenerator
import net.reactivecore.genapi.model.parser.ApiDefFileParser
import sbt.Keys._
import sbt._
import sbt.plugins.JvmPlugin

object GenApiPlugin extends AutoPlugin {

  object autoImport {
    lazy val genApiControllers = taskKey[Seq[File]]("Generate API Controllers")
    lazy val genApiRoutes = taskKey[Seq[File]]("Generate API route files")
  }

  import autoImport._

  override def requires = JvmPlugin

  override def projectSettings: Seq[Setting[_]] = Seq(
    sources in genApiControllers := Nil,
    sources in genApiControllers ++= {
      val dirs = (unmanagedResourceDirectories in Compile).value
      (dirs * "apidef.txt").get ++ (dirs * "*.apidef.txt").get
    },

    target in genApiControllers := crossTarget.value / "genapi",
    target in genApiRoutes := crossTarget.value / "genapi",

    genApiControllers  <<= (streams, sources in genApiControllers, target in genApiControllers).map(generateControllers),
    
    genApiRoutes <<= (streams, sources in genApiControllers, target in genApiRoutes).map(generateRoutesFile),

    watchSources in Defaults.ConfigGlobal <++= sources in genApiControllers,
    (sourceGenerators in Compile) <+= genApiControllers,
    // Generates the routes too late before they are being picked up by the Play Routes compiler
    // (resourceGenerators in Compile) <+= genApiRoutes,
    compile in Compile <<= (compile in Compile).dependsOn(genApiRoutes),

    // Routes are picked up by the routes plugin from the play framework here
    (unmanagedResourceDirectories in Compile) <+= target in genApiRoutes
  )

  def destinationDirectorySetting = Def.setting[File] {
    new File(sourceManaged.value, "genapi")
  }

  val controllerGenerators = Map(
    "default" -> new DefaultControllerGenerator()
  )

  def generateControllers (streams: TaskStreams, sources: Seq[File], targetDir: File): Seq[File] = {
    val log = streams.log

    val baseFiles = controllerGenerators.flatMap { case (name, generator) =>
      if (generator.needDependencyFile){
        val file = new File(targetDir, name + "_base.scala")
        if (!file.exists()){
          // TODO: this will require clean if plugin is updated.
          IO.write(file, generator.generateDependency())
        }
        Some(file)
      }  else None
    }

    val generatedControllers = sources.flatMap { definitionFile =>
      val parsed = new ApiDefFileParser().parseFile(definitionFile)
      // log.info(s"GenApi Controllers ${definitionFile} -> ${targetDir}")


      parsed.controllerDefinitions.map { controllerDef =>
        val generator = controllerGenerators.get(controllerDef.generatorName).getOrElse(throw new IllegalArgumentException(s"Unknown generator ${controllerDef.name}"))
        val targetFile = new File(targetDir, controllerDef.name + ".scala")
        onlyIfTargetIsOutdated(definitionFile, targetFile){
          log.info(s"  Generating ${targetFile}")
          IO.write(targetFile, generator.generateController(controllerDef))
        }
        targetFile
      }
    }
    (baseFiles ++ generatedControllers).toSeq
  }

  def generateRoutesFile (streams: TaskStreams, sources: Seq[File], targetDir: File): Seq[File] = {
    val log = streams.log

    sources.flatMap { definitionFile =>
      val targetFile = new File(targetDir, definitionFile.name.split("\\.").head + ".routes")
      // Note: we do not return the file if nothing changed (in contrast to the controller generator)
      // As this is not a real source generator, but fills the routes directory.
      onlyIfTargetIsOutdated(definitionFile, targetFile){
        val parsed = new ApiDefFileParser().parseFile(definitionFile)

        log.info(s"GenApi Routes --> ${targetFile}")
        IO.write(targetFile, new RoutesGenerator().generateRoutes(parsed))
        targetFile
      }
    }
  }

  /** Only execute f if the target file doesn't exists or is older than the src file.
    * TODO: use more sophisticated mechanisms from sbt.
    */
  private def onlyIfTargetIsOutdated[T](src: File, target: File)(f: => T): Option[T] = {
    if (src.newerThan(target)){
      Some(f)
    } else {
      None
    }
  }

}