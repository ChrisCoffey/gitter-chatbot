package org.ensime.chatbot

import scala.language.postfixOps

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
                Chatbot.helpAllThePeople
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

    def helpAllThePeople()(implicit c: ChatbotConfig) = {
        val roomStreams = Rooms.allRooms.map(Gitter.roomMessages)

        println("No more help for you")
    }

    //Note I need to filter my messages w/ a few regexes
    private def sendHelpfulMessage(room: String)(implicit c: ChatbotConfig) = {
        Gitter.sendMessage(room, "You have been helped by the great chatbot.") 
    }

}

object Gitter {
    import scalaj.http._
    import scala.io.Source
    import cats.data.Xor._
    import io.circe._
    import io.circe.generic.auto._
    import io.circe.parser._
    import io.circe.syntax._

    def roomMessages( r: String)(implicit c: ChatbotConfig)  = {
        val inputStream = Http(Urls.roomStream(c, r)) 
            .timeout(connTimeoutMs = 1000, readTimeoutMs = 30000)
            .header("Authorization",s"Bearer ${c.bearerToken}")
            .header("Accept", "application/json")
            .execute{input =>
                val bs = Source.fromInputStream(input).getLines()
                    bs.foreach{m =>
                        decode[Message](m) match {
                            case Right(mv) =>
                                if (mv.text != "You have been helped by the great chatbot.")
                                    Gitter.sendMessage(Rooms.allRooms.head, "You have been helped by the great chatbot.") 
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
