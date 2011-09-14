package sox

import sbt.{Project, Scope, ConfigKey}

object DefaultLayout {
  def apply(body: xml.NodeSeq, title: String ="sox", header: String = "sox") =
    <html>
      <head>
        <title>{title}</title>
        <link href="http://fonts.googleapis.com/css?family=Cabin|Copse" rel="stylesheet" type="text/css"/>
        <link href="sox.css" rel="stylesheet" type="text/css"/>
        <script src="jquery.min.js" type="text/javascript"></script>
      </head>
      <body>
        <div class="sox">
          <h1>{header}</h1>
          <div id="explain">Documentation for your Sbt settings</div>
        </div>
        <ul id="settings">
          { body }
        </ul>
        <script src="sox.js" type="text/javascript"></script>
      </body>
     </html>
}

/* S(coped)uri
 * extracted from https://github.com/harrah/xsbt/blob/v0.10.1/main/Scope.scala#L101-111 */
case class Suri(scope: Scope, key: String) {
  import scope.{project, config, task, extra}

  // sbt doesn't show this if config.name is "compile"
  private def configDisplay(config: ConfigKey): String = config.name + ":"

  /* Project.display https://github.com/harrah/xsbt/blob/v0.10.1/main/Project.scala#L192-200 */
  val projectPrefix = project.foldStrict(Project.display, "*", ".")
	val configPrefix = config.foldStrict(configDisplay, "*:", ".:")
	val taskPostfix = task.foldStrict(x => ("for " + x.label) :: Nil, Nil, Nil)

	val extraPostfix = extra.foldStrict(_.entries.map( _.toString ).toList, Nil, Nil)
	val extras = taskPostfix ::: extraPostfix

	val postfix = if(extras.isEmpty) "" else extras.mkString("(", ", ", ")")

	val uri = "%s/%s%s%s" format(projectPrefix, configPrefix, key, postfix)
}

object Template {

  private def scopeDisplay(sc: Scope, k: Project.ScopedKey[_]) =
    Suri(sc, k.key.label).uri

  private def desc(k: Project.ScopedKey[_]) =
    k.key.description.getOrElse("")

  private def label(k: Project.ScopedKey[_]) =
    "%s%s" format(Suri(k.scope, k.key.label).configPrefix, k.key.label)

  private def setting(s: SoxSetting) =
    <li>
      <h1><a href="#">{label(s.key)}</a></h1>
      <div class="content">
        <div>{desc(s.key)}</div>
        <div>Provided by <span>{scopeDisplay(s.providedBy, s.key)}</span></div>
      </div>
    </li>

  def apply(settings: Seq[SoxSetting]) =
    DefaultLayout(settings.map(setting)).toString
}