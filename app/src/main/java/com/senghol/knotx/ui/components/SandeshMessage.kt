package com.senghol.knotx.ui.components

import kotlinx.serialization.Serializable


@Serializable
data class SandeshMessage(
    val message: Boolean = false,
    val video: Boolean = false,
    val audio: Boolean = false,
    val doc: Boolean = false,
    val map: Boolean = false,
    val contact: Boolean = false,
    val text: String? = null,  // Store text for messages
    val reply: ReplyMessage? = null // Store reply details
)
@kotlinx.serialization.Serializable
enum class MessageType {
    TEXT, IMAGE, VIDEO, AUDIO, DOCUMENT, CONTACT, MAP, REPLY
}


@Serializable
data class ReplyMessage(
    val message: Boolean = false,
    val video: Boolean = false,
    val text: String? = null  // Text for replied messages
)

