package crobox.handlers

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import cats.implicits._
import crobox.stores.SessionStore
import crobox.stores.domain.PageType.OverView
import crobox.stores.domain._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

class EventHandlerSpec extends AnyWordSpec with Matchers {
  "EventHandler" should {

    val (productName1, productName2) = ("women/dresses/40", "women/underwear/3")

    val sessionTemplate  = Session(
        "73e4dc88-a474-4518-8b56-0029e11d0925"
      , 1
      , "895bfdda-14a6-4cc3-853b-1cd193a92cb2"
      , "session"
      , 10000
      , "3e029c2a-b7e5-4133-923e-1e7eb0ba7acb"
      , "9edc2509-8668-496c-b592-dc76f85438ec"
      , "TABLET"
      , 1625097600000L
      , "NL"
    )
    val pageViewTemplate = PageView(
        sessionTemplate.sessionId
      , sessionTemplate.viewCount
      , sessionTemplate.visitorId
      , sessionTemplate.eventName
      , sessionTemplate.eventTs
      , sessionTemplate.viewId
      , sessionTemplate.eventId
      , "https://shop.crobox.com/women/dresses/40"
      , List(productName1, productName2)
      , OverView
    )
    val addCartTemplate  = AddCart(
        sessionTemplate.sessionId
      , sessionTemplate.viewCount
      , sessionTemplate.visitorId
      , sessionTemplate.eventName
      , sessionTemplate.eventTs
      , sessionTemplate.viewId
      , sessionTemplate.eventId
      , 2
      , productName1
    )

    "calculate the average session duration" in {
      val store   = SessionStore.inMemory[IO]
      val handler = EventHandler.apply[IO](store)

      val data2    = sessionTemplate.copy(eventTs = 30000)
      val data3    = sessionTemplate.copy(visitorId = "d561f49a-d747-4519-a407-1b46a82adcac")
      val data4    = data3.copy(eventTs = 20000)
      val testData = List(sessionTemplate, data2, data3, data4)

      val setup = for {
        _      <- testData.traverse(store.put).void
        result <- handler.averageSession
      } yield result shouldBe FiniteDuration(15, TimeUnit.SECONDS)

      setup.unsafeRunSync()
    }

    "calculate the most viewed product on an overview page" in {
      val store   = SessionStore.inMemory[IO]
      val handler = EventHandler.apply[IO](store)

      val data2    = pageViewTemplate.copy(pageIds = List(productName2))
      val testData = List(pageViewTemplate, data2)

      val setup = for {
        _      <- testData.traverse(store.put).void
        result <- handler.mostViewedProduct
      } yield result shouldBe productName2

      setup.unsafeRunSync()
    }

    "calculate which product is added to the cart the most" in {
      val store   = SessionStore.inMemory[IO]
      val handler = EventHandler.apply[IO](store)

      val data2    = addCartTemplate.copy(productQty = 1, productIds = productName2)
      val data3    = addCartTemplate.copy(productQty = 1, productIds = "women/underwear/10")
      val data4    = addCartTemplate.copy(productQty = 1, productIds = productName2)
      val data5    = addCartTemplate.copy(productQty = 1, productIds = productName2)
      val testData = List(addCartTemplate, data2, data3, data4, data5)

      val setup = for {
        _      <- testData.traverse(store.put).void
        result <- handler.mostAddedToCart
      } yield result shouldBe productName2

      setup.unsafeRunSync()
    }

    "calculate what is the average cart-to-detail rate" in {
      val store   = SessionStore.inMemory[IO]
      val handler = EventHandler.apply[IO](store)

      val data2    = addCartTemplate.copy(productQty = 1, productIds = productName2)
      val data3    = addCartTemplate.copy(productQty = 1, productIds = "women/underwear/10")
      val data4    = addCartTemplate.copy(productQty = 1, productIds = productName2)
      val data5    = addCartTemplate.copy(productQty = 1, productIds = productName2)
      val testData = List(addCartTemplate, data2, data3, data4, data5)

      val setup = for {
        _      <- testData.traverse(store.put).void
        result <- handler.mostAddedToCart
      } yield result shouldBe productName2

      setup.unsafeRunSync()
    }

    "calculate the average pageView duration" in {
      val store   = SessionStore.inMemory[IO]
      val handler = EventHandler.apply[IO](store)

      val data  = pageViewTemplate.copy(eventTs = 10000)
      val data2 = pageViewTemplate.copy(eventTs = 20000)
      val data3 = pageViewTemplate.copy(visitorId = "d561f49a-d747-4519-a407-1b46a82adcac", eventTs = 30000)
      val data4 = pageViewTemplate.copy(visitorId = "d561f49a-d747-4519-a407-1b46a82adcac", eventTs = 50000)

      val testData = List(data, data2, sessionTemplate, data3, data4)

      val setup = for {
        _      <- testData.traverse(store.put).void
        result <- handler.averageViewDuration
      } yield result shouldBe FiniteDuration(15, TimeUnit.SECONDS)

      setup.unsafeRunSync()
    }
  }

}
