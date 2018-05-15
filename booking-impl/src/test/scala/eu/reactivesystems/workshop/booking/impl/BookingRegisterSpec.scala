package eu.reactivesystems.workshop.booking.impl

import java.time.LocalDate
import java.util.UUID

import scala.concurrent.Await
import scala.concurrent.duration._
import akka.Done
import akka.actor.ActorSystem
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.InvalidCommandException
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
import com.lightbend.lagom.scaladsl.testkit.PersistentEntityTestDriver
import com.typesafe.config.ConfigFactory
import eu.reactivesystems.workshop.booking.api.BookingRequest
import org.scalactic.{ConversionCheckedTripleEquals, TypeCheckedTripleEquals}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.Matchers
import org.scalatest.WordSpecLike

class BookingRegisterSpec extends WordSpecLike with Matchers with BeforeAndAfterAll
  with TypeCheckedTripleEquals {

  val system = ActorSystem("BookingRegisterSpec", JsonSerializerRegistry.actorSystemSetupFor(BookingSerializerRegistry))

  override def afterAll(): Unit = {
    Await.ready(system.terminate, 10.seconds)
  }

  "BookingRegister entity" must {
    "handle RequestBooking" in {
      val guestId = UUID.randomUUID()
      val date = LocalDate.now().plusWeeks(2)
      val driver = new PersistentEntityTestDriver(system, new BookingRegister, "register-1")
      val commandPayload = BookingRequest(guestId, date, 1, 1)
      val command = RequestBooking(commandPayload)
      val outcome = driver.run(command)
      val bookingId = outcome.replies.head.asInstanceOf[UUID]
      outcome.events should be(Seq(BookingRequested(bookingId, guestId, date, 1, 1)))
      outcome.state should ===(BookingRegisterState(BookingRegisterStatus.Listed, Map(bookingId.toString -> Booking(bookingId, guestId, date, 1, 1))))
      outcome.issues should be(empty)
    }
  }
}
