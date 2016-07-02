package org.ensime.chatbot.config

import org.ensime.chatbot.types._

object Urls{

    def messageURL(c: ChatbotConfig, room: String): String = 
        c.baseURL + "/v1/rooms/" + room + "/chatMessages" 

    def roomStream(c: ChatbotConfig, r: String): String = 
        c.streamingUrl + "/v1/rooms/" + r + "/chatMessages"
}

object Rooms {

    lazy val allRooms: Seq[String] = Seq("57770666c2f0db084a2107a4")

}
