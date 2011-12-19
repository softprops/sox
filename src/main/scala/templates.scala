package sox

import sbt.{BuildReference, ThisBuild, BuildRef, Project,
            ProjectReference, Scope,
            LocalRootProject, LocalProject, ProjectRef,
            ThisProject, RootProject, Reference,
            ConfigKey}

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
        <div id="find"><input type="text" placeholder="type key name"/></div>
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

   private val SbtBuildId  = """default-(\S{6})""".r

  /* sbt generates project uris using the following
    https://github.com/harrah/xsbt/blob/v0.10.1/main/Build.scala#L32
    match this pattern and just use default instead
  */
  private def uniform(id: String) = id match {
    case SbtBuildId(_) => "default"
    case id => id
  }

  // sbt doesn't show this if config.name is "compile"
  private def configDisplay(config: ConfigKey): String = config.name + ":"

  private def projectRefDisplay(ref: ProjectReference) =
		ref match {
			case ThisProject => "self/self"
			case LocalRootProject => "self/root"
			case LocalProject(id) => "self/%s" format uniform(id)
			case RootProject(uri) => "ref/root"
			case ProjectRef(uri, id) => "ref/%s" format uniform(id)
		}

  private def refDisplay(ref: Reference): String =
		ref match {
			case pr: ProjectReference => projectRefDisplay(pr)
			case br: BuildReference => buildRefDisplay(br)
		}

	def buildRefDisplay(ref: BuildReference) =
		ref match {
			case ThisBuild => "self"
			case BuildRef(uri) => "ref"
		}

  /* Project.display https://github.com/harrah/xsbt/blob/v0.10.1/main/Project.scala#L192-200 */
  val projectPrefix = project.foldStrict(refDisplay, "*", ".")
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

  private def setting(key: Project.ScopedKey[_], ss: Seq[SoxSetting]) =
    <li>
      <h1><a href="#">{label(key)}</a></h1>

      <div class="content">
        <div class="desc">{desc(key)}</div>
        { for (s <- ss) yield <div class="provided">Provided by <span>{scopeDisplay(s.providedBy, s.key)}</span></div> }
      </div>
    </li>

  def apply(settings: Seq[SoxSetting]) = {
    println("starting")
    val s = System.currentTimeMillis
    val keySetting = settings.map(s=>s.key->s)
    val groupedAndSortedByLabel = keySetting.groupBy(p=>label(p._1)).toSeq.sortBy(_._1)
    val renderedSettings = groupedAndSortedByLabel.map(p=>setting(p._2.head._1, p._2.map(_._2).distinct)) // distinct is used to not show multiple '*/*:<setting>'
    println("ended %s" format(System.currentTimeMillis - s))
    DefaultLayout(xml.NodeSeq fromSeq renderedSettings.toSeq match {
      case nil if (nil.isEmpty) => <li>These sox were ill stitched. Try another configuration.</li>
      case sx => sx
    }).toString
  }
}
