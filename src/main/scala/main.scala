import slick.driver.H2Driver.api._

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object Example extends App {

  //
  // A simple one table database
  //

  final case class Message(
    sender:  String,
    content: String,
    id:      Long = 0L)


  final class MessageTable(tag: Tag)
      extends Table[Message](tag, "message") {

    def id      = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def sender  = column[String]("sender")
    def content = column[String]("content")

    def * = (sender, content, id) <> (Message.tupled, Message.unapply)
  }

  lazy val messages = TableQuery[MessageTable]
  lazy val newMsg = messages returning messages.map(_.id)

  val db = Database.forConfig("example")

  // For convenience of testing in a main() application
  def exec[T](program: DBIO[T]): T = Await.result(db.run(program), 2 seconds)

  exec(messages.schema.create)

  //
  // Example 
  // Mixing non-db work inside a transcation
  //
  
  val action: DBIO[Long] = for {
    id <- newMsg += Message("Alice", "Hello World")
    content = s"Alice's first message has id $id"
    bobMsgId <- newMsg += Message("Bob", content)
  } yield bobMsgId

  println("Inserting a couple of messages...")
  println(exec(action.transactionally))

  println("Database content:")
  println(exec(messages.map(_.content).result))
    

}
