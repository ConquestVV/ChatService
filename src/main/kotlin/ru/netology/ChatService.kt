package ru.netology

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

object ChatService {
    private val chatIdSequence = AtomicInteger(1)
    private val messageIdSequence = AtomicInteger(1)
    private val chats: MutableMap<Int, Chat> = ConcurrentHashMap()

    fun sendMessage(fromId: Int, toId: Int, text: String): Message {
        require(text.isNotBlank()) { "Message text must not be blank" }
        val participants = setOf(fromId, toId)
        val chat = chats.values.firstOrNull { existing ->
            existing.participants == participants
        } ?: createChat(participants)
        val message = Message(
            id = messageIdSequence.getAndIncrement(),
            chatId = chat.id,
            authorId = fromId,
            recipientId = toId,
            text = text.trim(),
            timestamp = System.currentTimeMillis()
        )
        chat.messages += message
        return message.copy()
    }

    fun getChats(userId: Int): List<ChatPreview> = chats.values
        .filter { chat -> chat.containsUser(userId) }
        .map { chat ->
            val lastMessage = chat.activeMessages().maxByOrNull { it.timestamp }
            val unreadCount = chat.messages.count { message ->
                !message.isDeleted && !message.isRead && message.recipientId == userId
            }
            ChatPreview(
                chatId = chat.id,
                interlocutorId = chat.interlocutorIdFor(userId),
                lastMessage = lastMessage?.copy(),
                unreadCount = unreadCount
            )
        }
        .sortedByDescending { preview -> preview.lastMessage?.timestamp ?: Long.MIN_VALUE }

    fun getUnreadChatsCount(userId: Int): Int = chats.values.count { chat ->
        chat.hasUnreadMessagesFor(userId)
    }

    fun getMessages(chatId: Int, userId: Int, lastMessageId: Int = 0, limit: Int = Int.MAX_VALUE): List<Message> {
        require(limit > 0) { "Limit must be positive" }
        val chat = chats[chatId] ?: throw NoSuchElementException("Chat $chatId not found")
        if (!chat.containsUser(userId)) {
            throw IllegalAccessException("User $userId is not a participant of chat $chatId")
        }
        val messages = chat.messages
            .filter { message -> !message.isDeleted }
            .filter { message -> message.id > lastMessageId }
            .sortedBy { message -> message.id }
            .take(limit)

        messages.filter { it.recipientId == userId && !it.isRead }
            .forEach { message -> message.isRead = true }

        return messages.map(Message::copy)
    }

    fun markMessageAsRead(chatId: Int, messageId: Int, userId: Int): Boolean {
        val chat = chats[chatId] ?: return false
        if (!chat.containsUser(userId)) return false
        val message = chat.messages.firstOrNull { it.id == messageId && !it.isDeleted } ?: return false
        if (message.recipientId != userId) return false
        if (message.isRead) return false
        message.isRead = true
        return true
    }

    fun deleteMessage(chatId: Int, messageId: Int): Boolean {
        val chat = chats[chatId] ?: return false
        val message = chat.messages.firstOrNull { it.id == messageId } ?: return false
        if (message.isDeleted) return false
        message.isDeleted = true
        cleanupChatIfEmpty(chat)
        return true
    }

    fun deleteChat(chatId: Int): Boolean {
        val removed = chats.remove(chatId) != null
        return removed
    }

    fun editMessage(chatId: Int, messageId: Int, userId: Int, newText: String): Boolean {
        require(newText.isNotBlank()) { "Message text must not be blank" }
        val chat = chats[chatId] ?: return false
        val message = chat.messages.firstOrNull { it.id == messageId && !it.isDeleted } ?: return false
        if (message.authorId != userId) return false
        chat.messages.replaceAll { existing ->
            if (existing.id == messageId) {
                existing.copy(text = newText.trim(), timestamp = System.currentTimeMillis()).also {
                    it.isRead = false
                    it.isDeleted = false
                }
            } else {
                existing
            }
        }
        return true
    }

    private fun createChat(participants: Set<Int>): Chat {
        val chat = Chat(
            id = chatIdSequence.getAndIncrement(),
            participants = participants
        )
        chats[chat.id] = chat
        return chat
    }

    private fun cleanupChatIfEmpty(chat: Chat) {
        if (chat.messages.none { !it.isDeleted }) {
            chats.remove(chat.id)
        }
    }

    internal fun clear() {
        chats.clear()
        chatIdSequence.set(1)
        messageIdSequence.set(1)
    }
}
