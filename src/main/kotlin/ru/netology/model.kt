package ru.netology

data class Message(
    val id: Int,
    val chatId: Int,
    val authorId: Int,
    val recipientId: Int,
    val text: String,
    val timestamp: Long,
    var isRead: Boolean = false,
    var isDeleted: Boolean = false
)

data class Chat(
    val id: Int,
    val participants: Set<Int>,
    val messages: MutableList<Message> = mutableListOf()
) {
    fun containsUser(userId: Int): Boolean = participants.contains(userId)

    fun interlocutorIdFor(userId: Int): Int? =
        participants.firstOrNull { it != userId } ?: participants.firstOrNull()

    fun hasUnreadMessagesFor(userId: Int): Boolean = messages.any { message ->
        !message.isDeleted && message.recipientId == userId && !message.isRead
    }

    fun activeMessages(): List<Message> = messages.filterNot(Message::isDeleted)
}

data class ChatPreview(
    val chatId: Int,
    val interlocutorId: Int?,
    val lastMessage: Message?,
    val unreadCount: Int
)
