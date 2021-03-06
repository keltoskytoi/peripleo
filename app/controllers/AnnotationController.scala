package controllers

import controllers.common.JSONWrites._
import java.util.UUID
import models.core.Annotations
import play.api.db.slick._
import play.api.libs.json.Json

object AnnotationController extends AbstractController {

  def listAll(limit: Int, offset: Int) = loggingAction { implicit session =>
    jsonOk(Json.toJson(Annotations.listAll()), session.request)
  }
  
  def getAnnotation(id: UUID) = loggingAction { implicit session =>
    val annotation = Annotations.findByUUID(id)
    if (annotation.isDefined)
      jsonOk(Json.toJson(annotation.get), session.request)
    else
      NotFound(Json.parse("{ \"message\": \"Not found\" }"))
  }
  
}