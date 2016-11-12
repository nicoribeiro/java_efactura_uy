name := """java_efactura_uy"""

version := "0.61"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.11.6"

// el excludeAll(ExclusionRule(organization = "org.apache.geronimo.specs")) es para que no incluya el paquete geronimo-javamail_1.4_spec-1.7.1.jar que es viejo y anda muy mal

libraryDependencies ++= Seq(
  javaJdbc,
  cache,
  javaWs,
    "com.google.code.gson" % "gson" % "2.2",
    "org.apache.cxf" % "cxf-rt-frontend-jaxws" % "3.1.7" excludeAll(ExclusionRule(organization = "org.apache.geronimo.specs")),
    "org.apache.cxf" % "cxf-rt-transports-http" % "3.1.7" excludeAll(ExclusionRule(organization = "org.apache.geronimo.specs")),
    "org.apache.cxf" % "cxf-rt-ws-policy" % "3.1.7" excludeAll(ExclusionRule(organization = "org.apache.geronimo.specs")),
    "org.apache.cxf" % "cxf-rt-ws-security" % "3.1.7" excludeAll(ExclusionRule(organization = "org.apache.geronimo.specs")),
    "org.slf4j" % "slf4j-simple" % "1.7.12",
    "org.json" % "json" % "20140107",
	"net.sf.flexjson" % "flexjson" % "2.1",
	"com.sun.mail" % "javax.mail" % "1.5.6",
    "com.itextpdf" % "itextpdf" % "5.5.6",
	"com.google.zxing" % "core" % "3.2.1",
	"org.hibernate" % "hibernate-entitymanager" % "4.3.11.Final",
	"postgresql" % "postgresql" % "8.2-507.jdbc4",
	"com.zaxxer" % "HikariCP-java6" % "2.3.7",
    "com.edulify" %% "play-hikaricp" % "2.0.6",
    "com.twilio.sdk" % "twilio-java-sdk" % "3.6.2",
    "play4jpa" %% "play4jpa" % "1.0-SNAPSHOT",
    //Para imprimir
    "org.apache.pdfbox" % "pdfbox" % "2.0.2"
)

resolvers += Resolver.url("Edulify Repository", url("https://edulify.github.io/modules/releases/"))(Resolver.ivyStylePatterns)


libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.1.7"
libraryDependencies += "ch.qos.logback" % "logback-core" % "1.1.7"
libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.21"
libraryDependencies += "org.slf4j" % "jcl-over-slf4j" % "1.7.21"
libraryDependencies += "javassist" % "javassist" % "3.12.1.GA"

libraryDependencies += "com.hazelcast" % "hazelcast" % "3.6.3"

libraryDependencies += "com.hazelcast" % "hazelcast-client" % "3.6.3"

libraryDependencies += "com.hazelcast" % "hazelcast-cloud" % "3.6.3"

libraryDependencies += filters

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator


fork in run := true
