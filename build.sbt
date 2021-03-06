name := "peripleo"

version := "0.0.1"

play.Project.playScalaSettings

resolvers += "Open Source Geospatial Foundation Repository" at "http://download.osgeo.org/webdav/geotools/"

libraryDependencies ++= Seq(jdbc, cache)

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-slick" % "0.6.0.1",
  "org.apache.lucene" % "lucene-analyzers-common" % "5.3.1",
  "org.apache.lucene" % "lucene-queryparser" % "5.3.1",
  "org.apache.lucene" % "lucene-facet" % "5.3.1",
  "org.apache.lucene" % "lucene-spatial" % "5.3.1",
  "org.apache.lucene" % "lucene-suggest" % "5.3.1",
  "org.apache.lucene" % "lucene-highlighter" % "5.3.1",
  "com.vividsolutions" % "jts" % "1.13",
  "com.spatial4j" % "spatial4j" % "0.5",
  "org.geotools" % "gt-geojson" % "14.0",
  "org.xerial" % "sqlite-jdbc" % "3.7.2",
  "postgresql" % "postgresql" % "9.1-901.jdbc4"
)

/** Transient dependencies required by Scalagios
  *
  * TODO: remove once Scalagios is included as managed dependency!
  */
libraryDependencies ++= Seq(
  "org.openrdf.sesame" % "sesame-rio-n3" % "2.7.5",
  "org.openrdf.sesame" % "sesame-rio-rdfxml" % "2.7.5",
  "org.slf4j" % "slf4j-simple" % "1.7.7"
)

requireJs ++= Seq("peripleo-ui.js", "placeAdjacencyNetwork.js")

requireJsShim += "build.js"
