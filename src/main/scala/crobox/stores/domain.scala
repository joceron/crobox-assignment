package crobox.stores

import crobox.stores.domain.PageType.{Detail, OverView, PageType}
import io.circe._

object domain {

  implicit val decodePageType: Decoder[PageType] = Decoder.decodeString.emap { raw =>
    raw.toLowerCase match {
      case "detail"   => Right(Detail)
      case "overview" => Right(OverView)
      case _          => Left("page_type has no valid value")
    }
  }

  implicit val visitorEventDecoder: Decoder[Event] = (c: HCursor) =>
    (for {
      sessionId <- c.downField("session_id").as[String]
      viewCount <- c.downField("view_count").as[Int]
      visitorId <- c.downField("visitor_id").as[String]
      eventName <- c.downField("event_name").as[String]
      eventTs   <- c.downField("event_ts").as[Long]
      viewId    <- c.downField("view_id").as[String]
      eventId   <- c.downField("event_id").as[String]
    } yield eventName match {
      case "session"  =>
        for {
          deviceType     <- c.downField("device_type").as[String]
          visitorCreated <- c.downField("visitor_created").as[Long]
          regionCountry  <- c.downField("region_country").as[String]
        } yield Session(
            sessionId
          , viewCount
          , visitorId
          , eventName
          , eventTs
          , viewId
          , eventId
          , deviceType
          , visitorCreated
          , regionCountry
        )
      case "pageview" =>
        for {
          pageUrl     <- c.downField("page_url").as[String]
          pageType    <- c.downField("page_type").as[PageType]
          productIds  <- c.downField("product_ids").as[Option[String]]
          overviewIds <- c.downField("overview_ids").as[Option[String]]
        } yield {
          val pageIds =
            productIds
              .orElse(overviewIds)
              .map(_.split(",").toList)
              .getOrElse(List.empty)
          PageView(
              sessionId
            , viewCount
            , visitorId
            , eventName
            , eventTs
            , viewId
            , eventId
            , pageUrl
            , pageIds
            , pageType
          )
        }
      case "addcart"  =>
        for {
          productQty <- c.downField("product_qty").as[Int]
          productIds <- c.downField("product_ids").as[String]
        } yield AddCart(
            sessionId
          , viewCount
          , visitorId
          , eventName
          , eventTs
          , viewId
          , eventId
          , productQty
          , productIds
        )
      case "click"    =>
        for {
          productIds <- c.downField("product_ids").as[String]
        } yield Click(
            sessionId
          , viewCount
          , visitorId
          , eventName
          , eventTs
          , viewId
          , eventId
          , productIds.split(",").toList
        )
      case unknown    => throw new IllegalArgumentException(s"Unknown event_name value: in $unknown")
    }).flatten

  trait Event {
    val sessionId: String
    val viewCount: Int
    val visitorId: String
    val eventName: String
    val eventTs: Long
    val viewId: String
    val eventId: String
  }

  final case class Session(
        sessionId: String
      , viewCount: Int
      , visitorId: String
      , eventName: String
      , eventTs: Long
      , viewId: String
      , eventId: String
      , deviceType: String
      , visitorCreated: Long
      , regionCountry: String
  ) extends Event

  object PageType extends Enumeration {
    type PageType = Value
    val Detail, OverView = Value
  }

  final case class PageView(
        sessionId: String
      , viewCount: Int
      , visitorId: String
      , eventName: String
      , eventTs: Long
      , viewId: String
      , eventId: String
      , pageUrl: String
      , pageIds: List[String]
      , pageType: PageType
  ) extends Event

  final case class AddCart(
        sessionId: String
      , viewCount: Int
      , visitorId: String
      , eventName: String
      , eventTs: Long
      , viewId: String
      , eventId: String
      , productQty: Int
      , productIds: String
  ) extends Event

  final case class Click(
        sessionId: String
      , viewCount: Int
      , visitorId: String
      , eventName: String
      , eventTs: Long
      , viewId: String
      , eventId: String
      , productIds: List[String]
  ) extends Event

}
