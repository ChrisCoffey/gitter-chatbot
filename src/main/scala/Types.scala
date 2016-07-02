package org.ensime.chatbot.types

import io.circe._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._

case class ChatbotConfig(
    baseURL: String,
    bearerToken: String,
    streamingUrl: String,
    rooms: Seq[String]
    )

case class User(
    id: String,
    username: String,
    displayName: String,
    userUrl: String,
    avatarUrlSmall: String,
    avatarUrlLarge: String
    )

case class Message(
    text: String
    )

