package eu.reactivesystems.workshop.booking.api


import java.util.UUID

import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{Descriptor, Service, ServiceCall}

/**
  * The booking service.
  */
trait BookingService extends Service {

  def healthCheck: ServiceCall[NotUsed, String]

  def requestBooking(roomId: UUID): ServiceCall[BookingRequest, UUID]

  def withdrawBooking(roomId: UUID, bookingId: UUID): ServiceCall[NotUsed, Done]

  def confirmBooking(roomId: UUID, bookingId: UUID): ServiceCall[NotUsed, Done]

  def rejectBooking(roomId: UUID, bookingId: UUID): ServiceCall[BookingRequest, Done]

  def cancelBooking(roomId: UUID, bookingId: UUID): ServiceCall[NotUsed, Done]

  def modifyBooking(roomId: UUID, bookingId: UUID): ServiceCall[NotUsed, Done]

  def listRoom(roomId: UUID): ServiceCall[NotUsed, Done]

  def unlistRoom(roomId: UUID): ServiceCall[NotUsed, Done]

  final override def descriptor: Descriptor = {
    import Service._

    named("booking")
      .withCalls(
        restCall(Method.GET, "/api/room/healthCheck", healthCheck),
        restCall(Method.POST, "/api/room/:roomId/bookings/", requestBooking _),
        restCall(Method.POST, "/api/room/:roomId/bookings/:bookingId/withdraw", withdrawBooking _),
        restCall(Method.DELETE, "/api/room/:roomId/bookings/:bookingId", cancelBooking _),
        restCall(Method.POST, "/api/room/:roomId/bookings/:bookingId/confirm", confirmBooking _),
        restCall(Method.PUT, "/api/room/:roomId/bookings/:bookingId", modifyBooking _),
        restCall(Method.POST, "/api/room/:roomId/bookings/:bookingId/reject", rejectBooking _),
        restCall(Method.POST, "/api/room/:roomId", listRoom _),
        restCall(Method.DELETE, "/api/room/:roomId", unlistRoom _)
      )
      .withAutoAcl(true)
  }

}
