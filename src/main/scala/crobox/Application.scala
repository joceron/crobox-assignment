package crobox

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import crobox.handlers.EventHandler
import crobox.rest.Server
import crobox.stores.SessionStore
import crobox.stores.domain.Event
import crobox.util.{FileUtils, LoggingSupport}
import io.circe.parser._

object Application extends IOApp with LoggingSupport[IO] {

  final val FILE_PATH = "data.json"

  def run(args: List[String]): IO[ExitCode] =
    for {
      _           <- logger.info("Attempting to load data from file...")
      fileLines   <- FileUtils.readFile[IO](FILE_PATH)
      _           <- logger.info(s"Data successfully loaded. Total lines: ${fileLines.length}")
      store        = SessionStore.inMemory[IO]
      parsedEvents = fileLines.map(decode[Event]).collect { case Right(value) => value }
      _           <- logger.info(s"Data parsed. Total parsed events: ${parsedEvents.length}")
      _           <- parsedEvents.traverse(store.put)
      handler     <- IO.delay(EventHandler[IO](store))
      _           <- new Server[IO](8080, "localhost", handler).serve()
    } yield ExitCode.Success

}
