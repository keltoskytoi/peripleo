package controllers.pages

import models._
import play.api.db.slick._
import play.api.mvc.Controller

object AnnotatedThingPagesController extends Controller {
  
  def getAnnotatedThing(id: String) = DBAction { implicit session =>
    val thing = AnnotatedThings.findById(id)
    if (thing.isDefined) {
      val places = AggregatedView.countPlacesForThing(id)
      val datasetHierarchy = Datasets.findByIds(thing.get.dataset +: Datasets.getParentHierarchy(thing.get.dataset)).reverse
      Ok(views.html.annotatedThingDetails(thing.get, datasetHierarchy))
    } else {
      NotFound
    }
  }
  
}
