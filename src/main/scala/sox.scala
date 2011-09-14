package sox

import sbt._
import sbt.Keys._
import Project.Initialize

trait SettingFilter extends (SoxSetting => Boolean)

object AllSettings extends SettingFilter {
  def apply(s: SoxSetting) = true
}

object NoSettings extends SettingFilter {
  def apply(s: SoxSetting) = false
}

object Keys {
   val stitch = InputKey[Unit]("stitch",
                              "Compiles a list of sbt Settings and stitches them together into a browsable document")
   val soxDirectory = SettingKey[java.io.File]("sox-directory","Directory where sox docs will be written to")
   val preview  = TaskKey[Unit]("preview", "Generates a sox doc and launches in a browser")
   val filter = SettingKey[SettingFilter]("filter", "Filters which Settings get included")
}

case class SoxSetting(key: sbt.Project.ScopedKey[_],
                      providedBy: Scope,
                      deps: Iterable[sbt.Project.ScopedKey[_]],
                      rdeps: Iterable[sbt.Project.ScopedKey[_]],
                      delegates: Iterable[sbt.Project.ScopedKey[_]],
                      related: Iterable[sbt.Project.ScopedKey[_]])

object Plugin extends sbt.Plugin {
  import sbt.Keys._
  import sox.Keys._

  val Sox = config("sox") extend(Runtime)

  import complete._
  import complete.DefaultParsers._
  import Project.ScopedKey

  def options: Seq[Setting[_]] = inConfig(Sox)(Seq(
    soxDirectory <<= (crossTarget, configuration) { (outDir, conf) => outDir / (Defaults.prefix(conf.name) + "sox") },
    filter := AllSettings,
    stitch <<= InputTask(_ => (Space ~> ID).?) {
      (scope: TaskKey[Option[String]]) =>
        (scope, state, configuration, soxDirectory, filter, streams) map { (scope, state, config, sd, sf, out) =>

          val extracted: Extracted = Project.extract(state)
          val sessionSettings = extracted.session.original
          import extracted._

          val defined = sessionSettings sortBy(_.key.key.label) flatMap {  s =>
            val key = s.key
            val scope = key.scope
            val scoped = ScopedKey(scope, key.key)
            structure.data.definingScope(scope, key.key) match {
			         case Some(providedBy) =>

                 val cMap: Project.CompiledMap/*Map[ScopedKey[_], Compiled[_]]*/ =
                 Project.compiled(structure.settings, true/*actual*/)(structure.delegates, structure.scopeLocal)

		             val related: Iterable[Project.ScopedKey[_]] = cMap.keys.filter(k => k.key == key && k.scope != scope)

                 val depends: Iterable[sbt.Project.ScopedKey[_]] = cMap.get(scoped) match {
                   case Some(c) => c.dependencies.toSet
                   case None => Set.empty
                 }

		             val reverse: Iterable[sbt.Project.ScopedKey[_]] = Project.reverseDependencies(cMap, scoped)

                 Some(SoxSetting(key, providedBy, depends, reverse, Project.delegates(structure, scope, key.key), related))

			         case None => None
		       }
         }

         val to = new java.io.File(sd, "sox.html")
         IO.write(to, Template(defined.filter(sf)))
         Seq("sox.css", "jquery.min.js", "sox.js") foreach { r =>
           val f = new java.io.File(sd, r)
           IO.delete(f)
           IO.transfer(this.getClass().getResourceAsStream("/%s" format r), f)
         }
         out.log.info("Wrote sox docs")

      }
    }
  ))
}
