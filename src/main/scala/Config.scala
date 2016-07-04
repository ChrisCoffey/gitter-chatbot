package org.ensime.chatbot.config

import org.ensime.chatbot.types._

object Urls{

    def messageURL(c: ChatbotConfig, room: String): String = 
        c.baseURL + "/v1/rooms/" + room + "/chatMessages" 

    def roomStream(c: ChatbotConfig, room: String): String = 
        c.streamingUrl + "/v1/rooms/" + room + "/chatMessages"

    def currentUser(c: ChatbotConfig): String = 
        c.baseURL + "/v1/user"

    def roomUsers(c: ChatbotConfig, room: String): String = 
        c.baseURL + "/v1/rooms/" + room + "/users?limit=500"

    def roomUsersWithSkip(c: ChatbotConfig, room: String, skip: Int ) = 
        roomUsers(c,room) + s"&skip=$skip"
}

object Rooms {

    lazy val allRooms: Seq[String] = rulesForRoom.keys.toSeq
    
    val rulesForRoom = Map (
        //Ensime Server
        "54b58225db8155e6700e9eb0" -> List[Rule] (
            Rule(".*@\\w*.* error.*|error", "http://ensime.github.io/getting_help/"),
            Rule(".*@\\w*.* troubleshooting.*|troubleshooting", "http://ensime.github.io/getting_help/"),
            Rule("@ensimebot help @\\w*.*|help", "http://ensime.github.io/getting_help/")
            ),
        //Ensime Vim
        "55e0d9070fc9f982beaef2e3" -> List[Rule] (
            Rule(".*@\\w*.* error.*|error", "http://ensime.github.io/editors/vim/troubleshooting/"),
            Rule(".*@\\w*.* troubleshooting.*|troubleshooting", "http://ensime.github.io/editors/vim/troubleshooting/"),
            Rule("@ensimebot help @\\w*.*|help", "http://ensime.github.io/getting_help/")
            ),
        //Ensime Emacs
        "558fcd0315522ed4b3e2f65d" -> List[Rule] (
            Rule(".*@\\w*.* error.*|error", "http://ensime.github.io/editors/emacs/troubleshooting/"),
            Rule(".*@\\w*.* troubleshooting.*|troubleshooting", "http://ensime.github.io/editors/emacs/troubleshooting/"),
            Rule("@ensimebot help @\\w*.*|help", "http://ensime.github.io/getting_help/")
            )
        )
}

object Welcome {
    val toEnsime = "Welcome to the Ensime gitter room. Make sure you've read http://ensime.github.io/getting_started/  "
}
