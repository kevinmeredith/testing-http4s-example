package net

import cats.data.{Kleisli, OptionT}
import cats.effect.IO
import io.circe.Json
import io.circe.testing.instances.arbitraryJson
import java.util.UUID

import net.User.{ApplicationUser, UserError}
import org.http4s._
import org.http4s.circe._
import org.http4s.client.Client
import org.scalacheck.{Arbitrary, Gen}
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

class UserSpec extends Specification with ScalaCheck {

  private def testResponse(status: Status, json: Json): Client[IO] = Client.fromHttpService[IO] {
    Kleisli {
      _ => OptionT.liftF(Response[IO](status = status).withBody(json))
    }
  }

  private val TestUri = Uri.uri("for.testing.only")

  private def appUserJson(user: ApplicationUser): Json =
    Json.obj(
      ("id", Json.fromString(user.id.toString)),
      ("name", Json.fromString(user.name)),
      ("hobbies", Json.fromValues(user.hobbies.map(Json.fromString)))
    )
  
  private implicit val arbApplicationUser: Arbitrary[ApplicationUser] =
    Arbitrary {
      for {
        id      <- Gen.uuid
        name    <- Gen.alphaNumStr
        hobbies <- Gen.listOf(Gen.alphaNumStr)       
      } yield ApplicationUser(id, name, hobbies)
    }
  
  "User#get" >> {
    "should return None on HTTP-404" >> prop {
      (id: UUID, json: Json) => {
        val client: Client[IO] = testResponse(Status.NotFound, json)
        val resp: IO[Option[ApplicationUser]] = User.create(client, TestUri).get(id)
        resp.unsafeRunSync() ==== None
      }
    }
    "should return Some(user) on HTTP-200 w/ expected JSON" >> prop {
      (id: UUID, appUser: ApplicationUser) => {
        val client: Client[IO] = testResponse(Status.Ok, appUserJson(appUser))
        val resp: IO[Option[ApplicationUser]] = User.create(client, TestUri).get(id)
        resp.unsafeRunSync() ==== Some(appUser)
      }
    }
    "should raise error on HTTP-200 w/ non-decoding JSON" >> {

      val nonDecodingResponse: Gen[Json] =
        arbitraryJson.arbitrary.suchThat(_.as[ApplicationUser].isLeft)
      
      prop {
        (id: UUID, json: Json) => {
          val client: Client[IO] = testResponse(Status.Ok, json)
          val resp: IO[Option[ApplicationUser]] = User.create(client, TestUri).get(id)
          resp.attempt.unsafeRunSync() match {
            case Left(UserError(_, _, _)) => ok
            case unexpected => ko(unexpected.toString)
          }
        }
      }.setGen2(nonDecodingResponse)
    }  
    
    
    "should raise error on HTTP Response's Status that's not 200 or 404." >> {
      
      implicit val badStatus: Arbitrary[Status] = Arbitrary {
        Gen.oneOf(Status.registered.toList.filter {
          case Status.Ok | Status.NotFound => false
          case _ => true
        })
      }
      
      prop {
        (id: UUID, json: Json, st: Status) => {
          val client: Client[IO] = testResponse(Status.Ok, json)
          val resp: IO[Option[ApplicationUser]] = User.create(client, TestUri).get(id)
          resp.attempt.unsafeRunSync() match {
            case Left(UserError(_, _, _)) => ok
            case unexpected => ko(unexpected.toString)
          }
        }
      }
    }    
  }
}
