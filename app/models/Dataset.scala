package models

import play.api.Play.current
import play.api.db.slick.Config.driver.simple._
import scala.slick.lifted.Tag
import java.sql.Date
import scala.slick.lifted.Query
import play.api.Logger

/** Dataset model entity **/
case class Dataset(
    
  /** Internal ID in the API **/
  id: String,
    
  /** dcterms:title **/
  title: String, 
    
  /** dcterms:publisher **/
  publisher: String, 
    
  /** dcterms:license **/
  license: String, 
    
  /** time the dataset was created in the system **/
  created: Date,
    
  /** time the dataset was last modified in the system **/
  modified: Date,
    
  /** URL of the VoID file (unless imported via file upload) **/
  voidURI: Option[String], 
    
  /** dcterms:description **/
  description: Option[String],
    
  /** foaf:homepage **/
  homepage: Option[String],
  
  /** The ID of the dataset this dataset is part of (if any) **/
  isPartOf: Option[String],
    
  /** void:dataDump **/
  datadump: Option[String],
  
  /** The start of the date interval this dataset encompasses (optional) **/ 
  temporalBoundsStart: Option[Int],
  
  /** The end of the date interval this dataset encompasses (optional).
    *
    * If the dataset is dated (i.e. if it has a temporalBoundsStart value)
    * this value MUST be set. In case the thing is dated with a datestamp
    * rather than an interval, temporalBoundsEnd must be the same as
    * temporalBoundsStart
    */   
  temporalBoundsEnd: Option[Int],
  
  /** The full temporal profile and time histogram for the dataset **/
  temporalProfile: Option[String])
    
/** Dataset DB table **/
class Datasets(tag: Tag) extends Table[Dataset](tag, "datasets") {

  def id = column[String]("id", O.PrimaryKey)
  
  def title = column[String]("title", O.NotNull)
  
  def publisher = column[String]("publisher", O.NotNull)
  
  def license = column[String]("license", O.NotNull)
  
  def created = column[Date]("created", O.NotNull)
  
  def modified = column[Date]("modified", O.NotNull)
  
  def voidURI = column[String]("void_uri", O.Nullable)
  
  def description = column[String]("description", O.Nullable)
  
  def homepage = column[String]("homepage", O.Nullable)
  
  def isPartOfId = column[String]("is_part_of", O.Nullable)
  
  def datadump = column[String]("datadump", O.Nullable)
  
  def temporalBoundsStart = column[Int]("temporal_bounds_start", O.Nullable)

  def temporalBoundsEnd = column[Int]("temporal_bounds_end", O.Nullable)
  
  def temporalProfile = column[String]("temporal_profile", O.Nullable, O.DBType("text"))
  
  def * = (id, title, publisher, license, created, modified, voidURI.?, description.?, 
    homepage.?, isPartOfId.?, datadump.?, temporalBoundsStart.?, temporalBoundsEnd.?, temporalProfile.?) <> (Dataset.tupled, Dataset.unapply)
  
  /** Foreign key constraints **/
    
  def isPartOfFk = foreignKey("is_part_of_dataset_fk", isPartOfId, Datasets.query)(_.id)
  
}

/** Queries **/
object Datasets {
  
  private[models] val query = TableQuery[Datasets]
  
  def create()(implicit s: Session) = query.ddl.create
  
  def insert(dataset: Dataset)(implicit s: Session) = query.insert(dataset)
  
  def insertAll(dataset: Seq[Dataset])(implicit s: Session) = query.insertAll(dataset:_*)
  
  def update(dataset: Dataset)(implicit s: Session) = query.where(_.id === dataset.id).update(dataset)
 
  def countAll(topLevelOnly: Boolean = true)(implicit s: Session): Int = {
    if (topLevelOnly)
      Query(query.where(_.isPartOfId.isNull).length).first
    else
      Query(query.length).first
  }

  def listAll(topLevelOnly: Boolean = true, offset: Int = 0, limit: Int = Int.MaxValue)(implicit s: Session): Page[Dataset] = {
    val total = countAll(topLevelOnly)
    val result = 
      if (topLevelOnly)
        query.where(_.isPartOfId.isNull).drop(offset).take(limit).list
      else
        query.drop(offset).take(limit).list
        
    Page(result, offset, limit, total)    
  }
      
  def findById(id: String)(implicit s: Session): Option[Dataset] =
    query.where(_.id === id).firstOption
  
  def listSubsets(id: String)(implicit s: Session): Seq[Dataset] =
    query.where(_.isPartOfId === id).list    
    
  private[models] def walkSubsets(parentId: String)(implicit s: Session): Seq[Dataset] = {
    val subsets = query.where(_.isPartOfId === parentId).list
    if (subsets.isEmpty)
      subsets
    else
      subsets.flatMap(dataset => dataset +: walkSubsets(dataset.id))    
  }
  
  def getParentHierarchy(dataset: Dataset)(implicit s: Session): Seq[Dataset] = {
    val superset = dataset.isPartOf.flatMap(parentId => query.where(_.id === parentId).firstOption)
    if (superset.isDefined)
      getParentHierarchy(superset.get) :+ superset.get
    else
      Seq.empty[Dataset]
  }
    
  def delete(id: String)(implicit s: Session) =
    query.where(_.id === id).delete
 
}
