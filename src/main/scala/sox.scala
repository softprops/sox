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

case class OnlyIn(config: String) extends SettingFilter {
  def apply(s: SoxSetting) =
    s.key.scope.config.foldStrict(_.name, "", "").equalsIgnoreCase(config)
}

// sbt generates one of these for every task
// non-informative
object IgnoreConfiguration extends SettingFilter {
  val Name = "configuration"
  def apply(s: SoxSetting) =
    !Name.equalsIgnoreCase(s.key.key.label)
}

object Keys {
   val stitch = InputKey[Unit](key("stitch"),
                              "Compiles a list of sbt Settings and stitches them together into a browsable document")
   val drawer = SettingKey[java.io.File](key("drawer"),"Directory where sox docs will be written to")
   val filter = SettingKey[SettingFilter](key("filter"),
                                          "Filters which Settings get included. Defaults to every setting except those named `configuration`")

  private def key(name: String) = "sox-%s" format name
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

  import complete._
  import complete.DefaultParsers._
  import Project.ScopedKey

  def soxSettings: Seq[Setting[_]] = soxSettingsIn(Compile) ++ soxSettingsIn(Test)

  def soxSettingsIn(c: Configuration): Seq[Setting[_]] = inConfig(c)(soxSettings0)

  def soxSettings0: Seq[Setting[_]] = Seq(
    drawer in stitch <<= (crossTarget, configuration) { (outDir, conf) => outDir / (
      Defaults.prefix(conf.name) + "sox")
    },
    filter in stitch := IgnoreConfiguration,
    stitch <<= InputTask(_ => (Space ~> ID).?) {
      (fscope: TaskKey[Option[String]]) =>
        (fscope, state, configuration, drawer in stitch, filter in stitch, streams) map { (fscope, state, config, sd, sf, out) =>

          val extracted: Extracted = Project.extract(state)
          val sessionSettings = extracted.session.original
          import extracted._

          println("start extracting")
          val es = System.currentTimeMillis
          val defined = sessionSettings sortBy(_.key.key.label) flatMap {  s =>
            val key = s.key
            val scope = key.scope
            val scoped = ScopedKey(scope, key.key)
            structure.data.definingScope(scope, key.key) match {
			        case Some(providedBy) =>
                val cMap:  Map[sbt.Project.ScopedKey[_],sbt.Project.Flattened] =
                  Project.flattenLocals(Project.compiled(
                    structure.settings, true/*actual*/)(
                    structure.delegates, structure.scopeLocal, Project.showFullKey/* display */))

		            val related: Iterable[Project.ScopedKey[_]] =
                  cMap.keys.filter(k => k.key == key && k.scope != scope)

                val depends: Iterable[sbt.Project.ScopedKey[_]] =
                  cMap.get(scoped) match {
                    case Some(c) => c.dependencies.toSet
                    case None => Set.empty
                  }

		            val reverse: Iterable[sbt.Project.ScopedKey[_]] =
                  Project.reverseDependencies(cMap, scoped)

                Some(SoxSetting(key, providedBy, depends, reverse,
                                Project.delegates(structure, scope, key.key),
                                related))

			        case None => None
		        }
         }
         println("done extracting %s" format(System.currentTimeMillis - es))

         val to = new java.io.File(sd, "index.html")
         IO.delete(to)
         (defined.filter(sf), fscope) match {
           case (sx, None) =>
             println("writing template")
             val s = System.currentTimeMillis
             IO.write(to, Template(sx))
             println("done writing template %s" format(
               System.currentTimeMillis - s))
           case (sx, Some(fc)) =>
             IO.write(to, Template(sx.filter(OnlyIn(fc))))
         }
         println("writing resources")
         val s = System.currentTimeMillis
         Seq("sox.css", "jquery.min.js", "sox.js") foreach { r =>
           val f = new java.io.File(sd, r)
           IO.delete(f)
           IO.transfer(this.getClass().getResourceAsStream("/%s" format r), f)
         }
         println("wrote resources in %s" format(System.currentTimeMillis - s))
         out.log.info("Wrote sox docs to %s" format to.getPath)

      }
    },
    (aggregate in stitch) := false
  )
}
