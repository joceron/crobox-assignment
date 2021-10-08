package crobox.util

import cats.effect.{Async, Resource}

import scala.io.Source

object FileUtils {

  def readFile[F[_]](fileName: String)(implicit F: Async[F]): F[List[String]] =
    Resource
      .make(F.delay(getClass.getResourceAsStream(s"/$fileName")))(stream => F.delay(stream.close()))
      .use(stream =>
        Resource
          .make(F.delay(Source.fromInputStream(stream)))(source => F.delay(source.close()))
          .use(source => F.delay(source.getLines().toList))
      )

}
