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

    lazy val allRooms: Seq[String] = rulesForRoom.keys.toSeq
    
    val rulesForRoom = Map (
        //Ensime Server
        "54b58225db8155e6700e9eb0" -> List[(String, String)] (
            ".* error.*" -> "http://ensime.github.io/getting_help/",
            ".* troubleshooting.*" -> "http://ensime.github.io/getting_help/"
            ),
        //Ensime Vim
        "55e0d9070fc9f982beaef2e3" -> List[(String, String)] (
            ".* error.*" -> "http://ensime.github.io/editors/vim/troubleshooting/",
            ".* troubleshooting.*" -> "http://ensime.github.io/getting_help/"
            )
        )
}
