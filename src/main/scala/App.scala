package org.ensime.chatbot

import java.time._

import scala.collection.mutable.{Map => MMap}
import scala.language.postfixOps
import scala.io.Source

import cats.data.Xor
import io.circe._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._

import org.ensime.chatbot.config.{Rooms,Urls}
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
                System.err.println(err)
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

    private val recentlyHelped = MMap.empty[String, LocalDateTime]

    def helpAllThePeople()(implicit c: ChatbotConfig) = {
        Gitter.getMyUserInfo match {
            case Xor.Right(myself) =>
                implicit val me = myself
                Rooms.allRooms.map(r => Gitter.processRoomMessages(r)(sendHelpfulMessage))
            case Xor.Left(err) => 
                System.err.println(err)
                System.err.println("Problems requesting User. Exiting.")
        }



        println("No more help for you")
    }

    private def sendHelpfulMessage(msg: Message, room: String)(implicit c: ChatbotConfig, me: User) = {
        if(msg.fromUser.exists(u => u.id == me.id)){
            System.err.println("Skipping Chatbot message.")
        } else {
            determineHelpMessage(msg.text, room).foreach(x => Gitter.sendMessage(room, x))
        }
    }

    private def determineHelpMessage(r: String, text: String): Option[String] = {
        Rooms.roomHelp.get(r).flatMap{ls =>
                ls.filter( assoc => text.matches(assoc._1)) match {
                    case Nil => 
                        None
                    case matches => 
                        Some(matches.map(_._2).mkString("Please read these: ", " or ", ""))
                }
            }
    }

    private def updateHelpCache(id: String) = synchronized {
        recentlyHelped.get(id) match {
            case None => 
                recentlyHelped + (id -> LocalDateTime.now())
            case Some(x) =>
                val now = LocalDateTime.now()
                val toRemove = recentlyHelped.filter(kvp => kvp._2.isBefore(now.plusHours(1))).map(_._1)
                recentlyHelped -- toRemove
        } 
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

        println(rawBody)
        decode[List[User]](rawBody).map(_.head)
    }

    def processRoomMessages( r: String)(f: (Message, String) => Unit)(implicit c: ChatbotConfig)  = {
        val inputStream = Http(Urls.roomStream(c, r)) 
            .timeout(connTimeoutMs = 1000, readTimeoutMs = 30000)
            .header("Authorization",s"Bearer ${c.bearerToken}")
            .header("Accept", "application/json")
            .execute{input =>
                val bs = Source.fromInputStream(input).getLines()
                    bs.foreach{m =>
                        decode[Message](m) match {
                            case Xor.Right(mv) =>
                                f(mv, r)
                            case _ => println("skipping")
                        }
                    }
            }.body
    
    }

    def sendMessage( room: String, text: String)(implicit c: ChatbotConfig) : Unit = {
        val message = Message(text)
        Http(Urls.messageURL(c, room))
            .postData(message.asJson.noSpaces)
            .header("Authorization",s"Bearer ${c.bearerToken}")
            .header("content-type", "application/json")
            .header("Accept", "application/json").asString 
    }

}
