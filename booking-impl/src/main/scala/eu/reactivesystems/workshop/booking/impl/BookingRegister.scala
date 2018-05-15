package eu.reactivesystems.workshop.booking.impl

import java.time.LocalDate
import java.util.UUID

import akka.Done
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.ReplyType
import com.lightbend.lagom.scaladsl.persistence.{AggregateEvent, AggregateEventTag, AggregateEventTagger, PersistentEntity}
import eu.reactivesystems.workshop.booking.api.BookingRequest
import eu.reactivesystems.workshop.jsonformats.JsonFormats._
import play.api.libs.json.{Format, Json}

/**
  */
class BookingRegister extends PersistentEntity {

  override type State = BookingRegisterState
  override type Command = BookingRegisterCommand
  override type Event = BookingRegisterEvent

  override def initialState: BookingRegisterState = BookingRegisterState(BookingRegisterStatus.Listed, Map.empty)

  override def behavior: Behavior = {
    case BookingRegisterState(BookingRegisterStatus.Unlisted, _) => unlisted
    case BookingRegisterState(BookingRegisterStatus.Listed, _) => listed
  }

  private def unlisted = Actions()
    .onCommand[ListRoom.type, Done] {
        case (ListRoom, ctx, state) => ctx.thenPersist(RoomListed)(event => ctx.reply(Done))
      }
    .onEvent {
      case (RoomListed, state) => BookingRegisterState(BookingRegisterStatus.Listed, Map.empty)
    }

  private def listed = Actions()
    .onCommand[RequestBooking, UUID] {
      case (RequestBooking(bookingRequest), ctx, state) =>
        if (bookingRequest.startingDate isBefore LocalDate.now()) {
          ctx.invalidCommand("Booking date must be in the future")
          ctx.done
        } else {
          val bookingId: UUID = UUID.randomUUID()
          val event = BookingRequested(bookingId, bookingRequest.guest, bookingRequest.startingDate, bookingRequest.duration, bookingRequest.numberOfGuests)
          ctx.thenPersist(event)(event => ctx.reply(event.bookingId))
        }
    }
    .onEvent {
      case (event @ BookingRequested(bookingId, guestId, startingDate, duration, numberOfGuests), state) =>
        state.copy(requestedBookings =
        state.requestedBookings + (event.bookingId.toString -> Booking(event.bookingId, event.guest, event.startingDate, event.duration, event.numberOfGuests)))
    }

}


/**
  * The state.
  */
case class BookingRegisterState(status: BookingRegisterStatus.Status, requestedBookings: Map[String, Booking])

object BookingRegisterState {
  implicit val format: Format[BookingRegisterState] = Json.format
}

case class Booking(bookingId: UUID,
                   guest: UUID,
                   startingDate: LocalDate,
                   duration: Int,
                   numberOfGuests: Int)

object Booking {
  implicit val format: Format[Booking] = Json.format
}

/**
  * Status.
  */
object BookingRegisterStatus extends Enumeration {
  type Status = Value
  val Unlisted, Listed = Value

  implicit val format: Format[Status] = enumFormat(BookingRegisterStatus)
}

/**
  * A command.
  */
sealed trait BookingRegisterCommand

case class RequestBooking(request: BookingRequest) extends BookingRegisterCommand with ReplyType[UUID]

case class CancelBooking(bookingId: UUID) extends BookingRegisterCommand with ReplyType[Done]
case class WithdrawBooking(bookingId: UUID) extends BookingRegisterCommand with ReplyType[Done]
case class RejectBooking(bookingId: UUID) extends BookingRegisterCommand with ReplyType[Done]

case object ListRoom extends BookingRegisterCommand with ReplyType[Done] {
  implicit val format: Format[ListRoom.type] = singletonFormat(ListRoom)
}

case object UnlistRoom extends BookingRegisterCommand with ReplyType[Done] {
  implicit val format: Format[UnlistRoom.type] = singletonFormat(UnlistRoom)
}


object RequestBooking {
  implicit val format: Format[RequestBooking] = Json.format
}

object CancelBooking {
  implicit val format: Format[CancelBooking] = Json.format
}

object WithdrawBooking {
  implicit val format: Format[WithdrawBooking] = Json.format
}

object RejectBooking {
  implicit val format: Format[RejectBooking] = Json.format
}


/**
  * A persisted event.
  */
sealed trait BookingRegisterEvent extends AggregateEvent[BookingRegisterEvent] {
  override def aggregateTag: AggregateEventTagger[BookingRegisterEvent] = BookingRegisterEvent.Tag
}

case object RoomListed extends BookingRegisterEvent {
  implicit val format: Format[RoomListed.type] = singletonFormat(RoomListed)
}

case object RoomUnlisted extends BookingRegisterEvent {
  implicit val format: Format[RoomUnlisted.type] = singletonFormat(RoomUnlisted)
}

case class BookingRequested(bookingId: UUID,
                            guest: UUID,
                            startingDate: LocalDate,
                            duration: Int,
                            numberOfGuests: Int) extends BookingRegisterEvent

case class BookingCancelled(bookingId: UUID) extends BookingRegisterEvent
case class BookingWithdrawn(bookingId: UUID) extends BookingRegisterEvent
case class BookingRejected(bookingId: UUID) extends BookingRegisterEvent

object BookingRequested {
  implicit val format: Format[BookingRequested] = Json.format
}


object BookingRegisterEvent {
  val Tag = AggregateEventTag[BookingRegisterEvent]
}

