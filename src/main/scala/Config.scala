package org.ensime.chatbot.config

import org.ensime.chatbot.types._

object Urls{

    def messageURL(c: ChatbotConfig, room: String): String = 
        c.baseURL + "/v1/rooms/" + room + "/chatMessages" 

    def roomStream(c: ChatbotConfig, r: String): String = 
        c.streamingUrl + "/v1/rooms/" + r + "/chatMessages"

    def currentUser(c: ChatbotConfig): String = 
        c.baseURL + "/v1/user"
}

object Rooms {

    lazy val allRooms: Seq[String] = roomHelp.keys.toSeq
    
    val roomHelp = Map (
        "57770666c2f0db084a2107a4" -> List[(String, String)] (
            "*help*" -> "http://ensime.github.io/getting_help/",
            "*error*" -> "http://ensime.github.io/getting_help/"
            )
        )
}
