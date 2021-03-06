package index.places

import scala.collection.JavaConversions._
import index.{ IndexBase, Index, IndexFields }
import org.apache.lucene.index.Term
import org.apache.lucene.search.{ BooleanClause, BooleanQuery, MatchAllDocsQuery, TermQuery, TopScoreDocCollector }
import play.api.Logger
import org.apache.lucene.search.PhraseQuery
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.util.Version
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.spatial.query.SpatialArgs
import org.apache.lucene.spatial.query.SpatialOperation
import models.Page

trait PlaceReader extends IndexBase {
  
  def listAllPlaceNetworks(offset: Int = 0, limit: Int = 20): Seq[IndexedPlaceNetwork] = {
    val searcherAndTaxonomy = placeSearcherManager.acquire()
    val searcher = searcherAndTaxonomy.searcher
    
    try {
      val topDocs = searcher.search(new MatchAllDocsQuery(), offset + limit)
    
      topDocs.scoreDocs.drop(offset)
        .map(scoreDoc => new IndexedPlaceNetwork(searcher.doc(scoreDoc.doc))).toSeq
    } finally {
      placeSearcherManager.release(searcherAndTaxonomy)
    }
  }
 
  /** List all places (with filter options).
    * @param gazetteer the gazetteer
    * @param bbox bounding box - minLong, maxLong, minLat, maxLat
    * @param offset page offset
    * @param limit page size
    */
  def listAllPlaces(gazetteer: String, bbox: Option[(Double, Double, Double, Double)], offset: Int = 0, limit: Int = 20): Page[IndexedPlace] = {    
    val query = new TermQuery(new Term(IndexFields.SOURCE_DATASET, gazetteer))
    
    val bboxFilter = bbox.map(bounds => {
      val shape = Index.spatialCtx.makeRectangle(bounds._1, bounds._2, bounds._3, bounds._4)
      Index.rptStrategy.makeFilter(new SpatialArgs(SpatialOperation.Intersects, shape))
    })
 
    val searcherAndTaxonomy = placeSearcherManager.acquire()
    val searcher = searcherAndTaxonomy.searcher
    // val collector = TopScoreDocCollector.create(offset + limit, true)
    
    try {
      val topDocs = 
        if (bboxFilter.isDefined)
          searcher.search(query, bboxFilter.get, offset + limit)
        else
          searcher.search(query, offset + limit)
    
      val total = topDocs.totalHits //.getTotalHits
      val results = topDocs.scoreDocs.drop(offset)
        .map(scoreDoc => new IndexedPlaceNetwork(searcher.doc(scoreDoc.doc))).toSeq
        .map(_.places.filter(_.sourceGazetteer == gazetteer).head)
      
      Page(results, offset, limit, total)
    } finally {
      placeSearcherManager.release(searcherAndTaxonomy)
    }
  }
  
  def findPlaceByURI(uri: String): Option[IndexedPlace] =
    findNetworkByPlaceURI(uri).flatMap(_.getPlace(uri))
    
  def findNetworkByPlaceURI(uri: String): Option[IndexedPlaceNetwork] = {
    val q = new TermQuery(new Term(IndexFields.ID, Index.normalizeURI(uri)))
    
    val searcherAndTaxonomy = placeSearcherManager.acquire()
    val searcher = searcherAndTaxonomy.searcher
    try {
      val topDocs = searcher.search(q, 1)
    
      topDocs.scoreDocs.map(scoreDoc => new IndexedPlaceNetwork(searcher.doc(scoreDoc.doc))).headOption
    } finally {
      placeSearcherManager.release(searcherAndTaxonomy)
    }
  }

  // TODO is the result size ever > 1? Shouldn't be! 
  def findNetworkByCloseMatch(uri: String): Seq[IndexedPlaceNetwork] = {
    val q = new TermQuery(new Term(IndexFields.PLACE_MATCH, Index.normalizeURI(uri)))
    
    val searcherAndTaxonomy = placeSearcherManager.acquire()
    val searcher = searcherAndTaxonomy.searcher
    val numHits = Math.max(1, numPlaceNetworks) // Has to be minimum 1, but can never exceed size of index
    try {
      val topDocs = searcher.search(q, numHits)
    
      topDocs.scoreDocs.map(scoreDoc => new IndexedPlaceNetwork(searcher.doc(scoreDoc.doc)))
    } finally {
      placeSearcherManager.release(searcherAndTaxonomy)
    }
  }
  
  def findPlaceByAnyURI(uri: String): Option[IndexedPlaceNetwork] = {
    val normalizedURI = Index.normalizeURI(uri)
    
    val q = new BooleanQuery()
    q.add(new TermQuery(new Term(IndexFields.ID, normalizedURI)), BooleanClause.Occur.SHOULD)
    q.add(new TermQuery(new Term(IndexFields.PLACE_MATCH, normalizedURI)), BooleanClause.Occur.SHOULD)
    
    val searcherAndTaxonomy = placeSearcherManager.acquire()
    val searcher = searcherAndTaxonomy.searcher
    try {
      val topDocs = searcher.search(q, 1)
    
      topDocs.scoreDocs.map(scoreDoc => new IndexedPlaceNetwork(searcher.doc(scoreDoc.doc))).headOption
    } finally {
      placeSearcherManager.release(searcherAndTaxonomy)
    }
  }
    
}