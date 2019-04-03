package net

import cats.MonadError
import cats.effect.Sync
import cats.implicits._
import io.circe._
import org.http4s._
import org.http4s.circe._
import org.http4s.client.{Client, UnexpectedStatus}
import java.util.UUID

trait User[F[_]] {
  def get(id: UUID): F[Option[User.ApplicationUser]]
}
object User {
  final case class ApplicationUser(id: UUID, name: String, hobbies: List[String])
  object ApplicationUser {
    implicit val decoder: Decoder[ApplicationUser] = new Decoder[ApplicationUser] {
      final def apply(c: HCursor): Decoder.Result[ApplicationUser] =
        for {
          id      <- c.downField("id").as[UUID]
          name    <- c.downField("name").as[String]
          hobbies <- c.downField("hobbies").as[List[String]]
        } yield {
          ApplicationUser(id, name, hobbies)
        }
    }
  }
  
  final case class UserError(uri: Uri, id: UUID, t: Throwable)
    extends RuntimeException(
      s"""
         | Failed to perform HTTP Request to: $uri
         | for user id: $id
       """.stripMargin
    , t)
  
  def create[F[_]](httpClient: Client[F], base: Uri)(
    implicit F : Sync[F]
  ): User[F] = new User[F] {
    
    private implicit val applicationUserDecoder: EntityDecoder[F, ApplicationUser] =
      jsonOf[F, ApplicationUser]
    
    override def get(id: UUID): F[Option[ApplicationUser]] = {
      
      val uri: Uri = base / "users" / id.toString
      
      val req: Request[F] = Request[F](method = Method.GET, uri = uri)
      
      val response: F[Option[ApplicationUser]] = 
        httpClient.fetch[Option[ApplicationUser]](req) {
          case Status.Ok(body)    => body.as[ApplicationUser].map(Some(_))
          case Status.NotFound(_) => F.pure(None)
          case unexpectedStatus   => F.raiseError(UserError(uri, id, UnexpectedStatus(unexpectedStatus.status)))
        }

      MonadError[F, Throwable].adaptError(response) {
        case ue @ UserError(_,_,_) => ue
        case t                     => UserError(uri, id, t)
      }
    }
  }
}