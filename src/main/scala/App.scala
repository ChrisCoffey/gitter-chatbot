package org.ensime.chatbot

import java.time._

import scala.collection.mutable.{Set => MSet}
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.{Duration => D}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps
import scala.io.Source

import cats.data.Xor
import io.circe._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._

import org.ensime.chatbot.config.{Rooms,Urls, Welcome}
import org.ensime.chatbot.types._

object Main {
    type Result[A] = Either.RightProjection[String, A]
    private def getEnvVar(s: String): Result[String] = 
        sys.env.get(s)
            .fold[Either[String, String]](Left(s"Could not find Env Variable $s "))(value => Right(value))
            .right

    def main(args: Array[String]): Unit = {
        configure() match {
            case Left(err) => 
                System.out.println(err)
                System.exit(1)
            case Right(conf) =>
                implicit val c = conf
                Chatbot.helpAllThePeople()
        }    
    }


    private def configure() = 
        for {
           base             <-  getEnvVar("G_BASE_URL")
           bearer           <-  getEnvVar("G_TOKEN")
           streamingUrl     <-  getEnvVar("G_STREAM_URL")
        } yield ChatbotConfig (
            base, 
            bearer,
            streamingUrl,
            Rooms.allRooms)
}

object Chatbot {

    private val knownUsers: Map[String, MSet[String]] = Rooms.allRooms.map(r => r -> MSet.empty[String]).toMap

    def helpAllThePeople()(implicit c: ChatbotConfig) = {
        Gitter.getMyUserInfo match {
            case Xor.Right(myself) =>
                implicit val me = myself
                val fs = Rooms.allRooms.map{r => 
                        val roomUsers = Gitter.roomUsers(r)
                        knownUsers(r) ++= roomUsers.map(_.id)
                        Future { Gitter.processRoomMessages(r)(sendHelpfulMessage) }
                    }
                Await.ready(Future.sequence(fs), D.Inf)
            case Xor.Left(err) => 
                System.out.println(err)
                System.out.println("Problems requesting User. Exiting.")
        }

        System.out.println("No more help for you")
    }

    private def sendHelpfulMessage(msg: Message, room: String)(implicit c: ChatbotConfig, me: User) = {
        msg.fromUser match {
            case None =>
                System.out.println("Skipping Chatbot message.")
            case Some(u) if u.id == me.id =>
                System.out.println("Skipping Chatbot message.")
            case Some(u) =>
                if(msg.mentions.nonEmpty || firstMessage(room, msg)){
                    val helpMessage = determineHelpMessage(room, msg)
                    helpMessage.foreach(x => Gitter.sendMessage(room, x))
                }
        }
    }

    private def determineHelpMessage(r: String, msg: Message): Option[String] = {
        val x = for {
            users <- knownUsers.get(r)
            sender <- msg.fromUser
            senderId = sender.id
            roomRules <- Rooms.rulesForRoom.get(r)
            helpMsg = maybeMessageRule(msg.text, roomRules)
        } yield {
            if(!users.contains(senderId)){
                users += senderId
                Some( Welcome.toEnsime )
            } else {
                helpMsg
            }
        }
        x.flatten
    }

    private def firstMessage(r: String, msg: Message): Boolean = 
        knownUsers.get(r).exists(users => 
                users.contains(msg.fromUser.fold("Fake")(u=> u.id)))

    private def maybeMessageRule(text: String, rules: List[(String, String)]): Option[String] = 
        rules.filter( assoc => text.matches(assoc._1)) match {
            case Nil => 
                None
            case matches => 
                Some(matches.map(_._2).distinct.mkString("Please read: ", " and ", ""))
        }
}

object Gitter {
    import scalaj.http._

    def getMyUserInfo(implicit c: ChatbotConfig): Xor[Error,User] = {
        val rawBody = Http(Urls.currentUser(c)) 
            .header("Authorization",s"Bearer ${c.bearerToken}")
            .header("Accept", "application/json")
            .asString
            .body

        decode[List[User]](rawBody).map(_.head)
    }

    def processRoomMessages( r: String)(f: (Message, String) => Unit)(implicit c: ChatbotConfig): Unit  = {
        Http(Urls.roomStream(c, r)) 
            .timeout(connTimeoutMs = 1000, readTimeoutMs = 30000000)
            .header("Authorization",s"Bearer ${c.bearerToken}")
            .header("Accept", "application/json")
            .execute{input =>
                val bs = Source.fromInputStream(input).getLines()
                    bs.foreach{m =>
                        decode[Message](m) match {
                            case Xor.Right(mv) =>
                                f(mv, r)
                            case _ => 
                                System.out.println("skipping")
                        }
                    }
            }.body
    
    }

    def roomUsers(room: String)(implicit c: ChatbotConfig): Seq[User] = {
        val rawBody = Http(Urls.roomUsers(c, room))
            .header("Authorization",s"Bearer ${c.bearerToken}")
            .header("Accept", "application/json")
            .asString
            .body

        decode[List[User]](rawBody) match {
            case Xor.Left(err) => 
                Seq.empty
            case Xor.Right(users) =>
                users
        }
    }

    def sendMessage( room: String, text: String)(implicit c: ChatbotConfig) : Unit = {
        val message = Message(text)
        System.out.println(message)
        Http(Urls.messageURL(c, room))
            .postData(message.asJson.noSpaces)
            .header("Authorization",s"Bearer ${c.bearerToken}")
            .header("content-type", "application/json")
            .header("Accept", "application/json")
            .asString 
    }

}
