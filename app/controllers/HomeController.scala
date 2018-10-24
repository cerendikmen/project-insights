package controllers

import javax.inject._
import play.api.libs.json.Json
import play.api.mvc._
import utilities.DataUtility
import scala.concurrent._
import ExecutionContext.Implicits.global


/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  /**
   * Create an Action to list variable names as JSON response.
   *
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index() = Action { implicit request: Request[AnyContent] =>
    lazy val res = DataUtility.getVariableList
    lazy val json = Json.toJson(res)
    Ok(json)
  }

  /**
    * Create an Action to return mutual information between variables and
    * the given variable as a parameter. If variable name is not valid returns
    * 404 not found error message.
    *
    * The configuration in the `routes` file means that this method
    * will be called when the application receives a `GET` request with
    * a path of `/dependencies` with a request parameter called variable.
    */
  def mi(variable: String) = Action {
    val availableVariables: Array[String] = DataUtility.getVariableList
    availableVariables.find(x=>x == variable) match {
      case Some(_) => {
        DataUtility.getMI(variable)
        Ok.sendFile(
          content = new java.io.File(DataUtility.CSV_FILE),
          inline = false
        )
      }
      case None => {
        lazy val errMessage = Json.obj(
          "msg" -> s"Variable named '$variable' is not valid.",
          "available_names" -> Json.toJson(availableVariables)
        )
        NotFound(errMessage)
      }
    }

  }
}
