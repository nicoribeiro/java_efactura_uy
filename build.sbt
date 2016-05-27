name := """java_efactura_uy_play"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  javaJdbc,
  cache,
  javaWs,
    "com.google.code.gson" % "gson" % "2.2",
    "org.apache.cxf" % "cxf-rt-frontend-jaxws" % "3.1.1",
    "org.apache.cxf" % "cxf-rt-transports-http" % "3.1.1",
    "org.apache.cxf" % "cxf-rt-ws-policy" % "3.1.1",
    "org.apache.cxf" % "cxf-rt-ws-security" % "3.1.1",
    "org.slf4j" % "slf4j-simple" % "1.7.12",
    "org.json" % "json" % "20140107",
	"net.sf.flexjson" % "flexjson" % "2.1",
    "com.itextpdf" % "itextpdf" % "5.5.6",
	"com.google.zxing" % "core" % "3.2.1"
	
)

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.1.7"
libraryDependencies += "ch.qos.logback" % "logback-core" % "1.1.7"
libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.21"
libraryDependencies += "org.slf4j" % "jcl-over-slf4j" % "1.7.21"
libraryDependencies += "javassist" % "javassist" % "3.12.1.GA"

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator


fork in run := true