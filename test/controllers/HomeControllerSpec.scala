package controllers

import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.libs.json.Json
import play.api.mvc.ControllerComponents
import play.api.test.Helpers._
import play.api.test._
import play.api.Environment

import scala.util.Random

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 *
 * For more information, see https://www.playframework.com/documentation/latest/ScalaTestingWithScalaTest
 */
class HomeControllerSpec extends PlaySpec with GuiceOneAppPerTest with Injecting {
  val availableVariables: Array[String] = Array("customer_understands_products","customer_understands_technology","customer_is_hands_on",
    "customer_seems_fair","customer_shares_our_expectations","customer_is_old","customer_is_big","people_are_interested",
    "people_know_the_technology","people_are_senior","project_is_technically_challenging","project_is_large","project_has_clear_scope_and_focus",
    "project_has_optimistic_schedule","project_has_unknowns","project_tries_new_ways_or_technologies","contract_has_fixed_scope","contract_has_fixed_price",
    "result_customer_was_happy","result_people_were_happy","result_numbers_ended_good","result_things_went_as_predicted",
    "result_product_was_good","result_new_doors_were_opened","iterative","retrospectives","acceptance_testing","customer_as_product_owner",
    "ux_kickstart","multicompetence_team")

  "HomeController GET" should {

    "should return mutual information csv" in {
      val controller = new HomeController(inject[ControllerComponents], inject[Environment])
      val home = controller.mi("customer_understands_products").apply(FakeRequest(GET, "/dependencies"))

      status(home) mustBe OK
      contentType(home) mustBe Some("text/csv")
    }

    "should return 404 not found" in {
      val controller = inject[HomeController]
      val varName: String = Random.nextString(5)
      val home = controller.mi(varName).apply(FakeRequest(GET, "/dependencies"))

      status(home) mustBe NOT_FOUND
      contentAsJson(home) must equal(Json.obj(
        "msg" -> s"Variable named '$varName' is not valid.",
        "available_names" -> Json.toJson(availableVariables)
      ))
    }

    "should return available variable names" in {
      val controller = inject[HomeController]
      val home = controller.index().apply(FakeRequest(GET, "/"))

      status(home) mustBe OK
      contentAsJson(home) must equal(Json.toJson(availableVariables))
    }
  }
}
