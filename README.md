# Crobox Assignment

## Assumptions made

- For the second question, "What is the most viewed product on an overview page?", I assumed that "product" means "id 
  inside `overview_ids` field". So what I looked for was the most repeated value across all events
- The field `product_ids` on `addcart` events suggests that there might come several products, but then, the other field
  called `product_qty` suggests that there might only come one single product, on different quantities. I assumed the
  second case, and that's why on my model `product_ids` is a `String` and not a `List[String]`
- In the fourth question, for "Cart-to-Detail rate", I assumed "for every
  product with a `pageview` detail event, how many `addcart` events are?". I also considered "inside every session, for
  every product with a `pageview` detail event, how many `addcart` events are?" Because it was my decision to chose one,
  I went for the easy one (the first), but in case of it being the second option instead, we can discuss this on another
  technical-conversation-interview
- For the last question, "What is the average pageview duration?", I assumed that the duration of a `pageview` is the
  time difference between the timestamp of the `pageview` itself, and the next event for that session, if any. This
  means that if the session ended on a `pageview` event, I don't calculate how long that last `pageview` lasted

## How to run
- If you are using an IDE, simply import the project as an SBT project, and run `crobox.Application.scala`. It will
  start a server on port 8080
- If you prefer to use the command line:
  1. Go to the main folder of this project
  2. Verify that you have a JDK installed by running `javac -version`
  3. Run `sbt` and wait until the console has finished loading
  4. Run `run`
  5. To exit the console, type `exit`
  
Once the server is up and running, you can execute cURL commands, or whatever framework you prefer for doing HTTP
requests (like Postman) to `http://localhost/8080`. The endpoints are specified with a dsl on `crobox.rest.server`, and
they are:
- What is the average session duration? `GET /average/session`
- What is the most viewed product on an overview page? `GET /most-viewed`
- What product is added to the cart the most? `GET /most-added`
- What product is added to the cart the most? `GET /most-added`
- What is the average Cart-to-Detail rate? `GET /average/ratio`
- What is the average pageview duration? `GET /average/pageview`

## Libraries used

- Cats Effect: is always convenient to have if you have to build anything asynchronous
- http4s: servers and clients build on top of Fs2, another library for streams in Scala
- SLF4J: http4s needs it as a dependency, or it throws a warning on start-up
- circe: for encoding/decoding the models
- ScalaTest: for unit testing