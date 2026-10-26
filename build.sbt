name := """java_efactura_uy"""

version := "1.1"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.11.12"

// el excludeAll(ExclusionRule(organization = "org.apache.geronimo.specs")) es para que no incluya el paquete geronimo-javamail_1.4_spec-1.7.1.jar que es viejo y anda muy mal
// y el de "org.apache.geronimo.javamail" para que no entre geronimo-javamail_1.4_mail (lo trae wss4j desde CXF 3.5): el mail es com.sun.mail

libraryDependencies ++= Seq(
  javaJdbc,
  cache,
  javaWs,
    "com.google.code.gson" % "gson" % "2.2",
    "org.apache.cxf" % "cxf-rt-frontend-jaxws" % "3.5.11" excludeAll(ExclusionRule(organization = "org.apache.geronimo.specs"), ExclusionRule(organization = "org.apache.geronimo.javamail")),
    "org.apache.cxf" % "cxf-rt-transports-http" % "3.5.11" excludeAll(ExclusionRule(organization = "org.apache.geronimo.specs"), ExclusionRule(organization = "org.apache.geronimo.javamail")),
    "org.apache.cxf" % "cxf-rt-ws-policy" % "3.5.11" excludeAll(ExclusionRule(organization = "org.apache.geronimo.specs"), ExclusionRule(organization = "org.apache.geronimo.javamail")),
    "org.apache.cxf" % "cxf-rt-ws-security" % "3.5.11" excludeAll(ExclusionRule(organization = "org.apache.geronimo.specs"), ExclusionRule(organization = "org.apache.geronimo.javamail")),
    "org.slf4j" % "slf4j-simple" % "1.7.12",
    "org.json" % "json" % "20260814",
	"net.sf.flexjson" % "flexjson" % "2.1",
	"com.sun.mail" % "javax.mail" % "1.5.6",
    "com.itextpdf" % "itextpdf" % "5.5.6",
	"com.google.zxing" % "core" % "3.2.1",
	"org.hibernate" % "hibernate-entitymanager" % "4.3.11.Final",
	//DB 
	"org.postgresql" % "postgresql" % "42.2.16",
	//"mysql" % "mysql-connector-java" % "5.1.6",
	"com.zaxxer" % "HikariCP-java6" % "2.3.7",
    "com.edulify" %% "play-hikaricp" % "2.0.6",
    "play4jpa" %% "play4jpa" % "2.4.11",
    "org.apache.poi" % "poi" % "4.0.1",
  	"org.apache.poi" % "poi-ooxml" % "4.0.1"
)

resolvers += Resolver.url("Edulify Repository", url("https://edulify.github.io/modules/releases/"))(Resolver.ivyStylePatterns)


libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.1.7"
libraryDependencies += "ch.qos.logback" % "logback-core" % "1.1.7"
libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.21"
libraryDependencies += "org.slf4j" % "jcl-over-slf4j" % "1.7.21"
libraryDependencies += "javassist" % "javassist" % "3.12.1.GA"

// https://mvnrepository.com/artifact/com.google.guava/guava
libraryDependencies += "com.google.guava" % "guava" % "30.1-jre"


libraryDependencies += filters

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator


fork in run := true

// Version del build para GET /api/v1/version: version (la pone scripts/build.sh), commit y fecha de compilacion
resourceGenerators in Compile += Def.task {
  val archivo = (resourceManaged in Compile).value / "version.properties"
  val commit = scala.util.Try(Process("git rev-parse --short=7 HEAD", baseDirectory.value).!!.trim).getOrElse("desconocido")
  val fecha = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(new java.util.Date())
  IO.write(archivo, s"version=${version.value}\ncommit=$commit\ncompilado=$fecha\n")
  Seq(archivo)
}.taskValue
