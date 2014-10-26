package global

import index.Index
import java.io.{ File, FileInputStream }
import java.util.zip.GZIPInputStream
import models._
import org.pelagios.api.gazetteer.Place
import play.api.Play
import play.api.Play.current
import play.api.{ Application, GlobalSettings, Logger }
import play.api.db.slick._
import scala.slick.jdbc.meta.MTable

object Global extends GlobalSettings {
  
  private val GAZETTER_DATA_DIR = "gazetteer"
       
  lazy val index = {
    val idx = Index.open("index")
    
    if (idx.numPlaceNetworks == 0) {
      Logger.info("Building place index...")
      
      // Grab the list of gazetteers from the application properties file
      def getPropertyAsList(name: String): Seq[String] = 
        Play.current.configuration.getString(name).map(_.split(",").toSeq).getOrElse(Seq.empty[String]).map(_.trim)
      
      val gazetteers = getPropertyAsList("api.gazetteer.names").zip(getPropertyAsList("api.gazetteer.files"))
      
      // Helper function to insert a single dump file into the index (returning number of total places, distinct places and URI prefixes)
      def insertDumpfile(sourceGazetteer: String, file: File): (Int, Int, Seq[String]) = {
        val (is, filename)  = if (file.getName.endsWith(".gz"))
          (new GZIPInputStream(new FileInputStream(file)), file.getName.substring(0, file.getName.lastIndexOf('.')))
        else
          (new FileInputStream(file), file.getName)

        idx.addPlaceStream(is, filename, sourceGazetteer)
      }
      
      // Build the place index
      DB.withSession { implicit session: Session =>
        gazetteers.foreach { case (name, dump) => {
          Logger.info("Loading gazetteer " + name  + ": " + dump)
        
          val file = new File(GAZETTER_DATA_DIR, dump)
          val (totalPlaces, distinctPlaces, uriPrefixes) =
            if (file.isDirectory) {
              file.listFiles.foldLeft((0, 0, Seq.empty[String])) { case ((totalPlaces, distinctPlaces, uriPrefixes), nextFile) => {
                Logger.info("Loading partial gazetteer file: " + nextFile.getName)
                val (newPlaces, newDistinctPlaces, prefixes) = insertDumpfile(name, nextFile)
                Logger.info("Inserted " + (totalPlaces + newPlaces) + " places")
                (totalPlaces + newPlaces, distinctPlaces + newDistinctPlaces, (uriPrefixes ++ prefixes).distinct)
              }}
            } else {
              insertDumpfile(name, file)
            }
          
          idx.refresh()
          
          // Insert gazetteer meta in to DB
          Gazetteers.insert(Gazetteer(name, totalPlaces, distinctPlaces), uriPrefixes)
        }}
      }
    }

    idx
  }

  override def onStart(app: Application): Unit = {
    // Initializes the database schema
    DB.withSession { implicit session: Session =>
      if (MTable.getTables("datasets").list.isEmpty) {
        Logger.info("DB table 'datasets' does not exist - creating")
        Datasets.create
      }
            
      if (MTable.getTables("annotated_things").list.isEmpty) {
        Logger.info("DB table 'annotated_things' does not exist - creating")
        AnnotatedThings.create
      }
      
      if (MTable.getTables("images").list.isEmpty) {
        Logger.info("DB table 'images' does not exist - creating")
        Images.create
      }  
       
      if (MTable.getTables("annotations").list.isEmpty) {
        Logger.info("DB table 'annotations' does not exist - creating")
        Annotations.create
      }
      
      if (MTable.getTables("dataset_dumpfiles").list.isEmpty) {
		 Logger.info("DB table 'dataset_dumpfiles' does not exist - creating")
		 DatasetDumpfiles.create
	  }

      if (MTable.getTables("gazetteers").list.isEmpty) {
        Logger.info("DB table 'gazetteers' does not exist - creating")
        Gazetteers.create
      }
      
      if (MTable.getTables("places_by_dataset").list.isEmpty && MTable.getTables("places_by_annotated_thing").list.isEmpty) {
        Logger.info("DB Tables 'places_by_dataset' and 'places_by_annotated_thing' do not exist - creating")
        AggregatedView.create
      }
      
      if (MTable.getTables("tags").list.isEmpty) {
        Logger.info("DB table 'tags' does not exist - creating")
        Tags.create
      }    
      
      if (MTable.getTables("access_log").list.isEmpty) {
        Logger.info("DB table 'access_log' does not exit - creating")
        AccessLog.create
      }  
    }
    
    index.buildSpellchecker()
  }  
  
  override def onStop(app: Application): Unit = index.close()

}
