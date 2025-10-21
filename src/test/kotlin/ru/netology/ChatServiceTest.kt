package ru.netology

private typealias TestCase = Pair<String, () -> Unit>

object ChatServiceTestRunner {
    @JvmStatic
    fun main(args: Array<String>) {
        val failures = runAllTests()
        if (failures > 0) {
            throw AssertionError("$failures test(s) failed")
        }
    }

    private fun runAllTests(): Int {
        val tests = listOf<TestCase>(
            "sendMessage creates chat and stores message" to ::sendMessageCreatesChatAndStoresMessage,
            "getMessages returns ordered copies and marks as read" to ::getMessagesReturnsOrderedCopiesAndMarksAsRead,
            "deleteMessage removes empty chat" to ::deleteMessageRemovesEmptyChat,
            "editMessage updates text and resets read state" to ::editMessageUpdatesTextAndResetsReadState
        )

        var failures = 0
        tests.forEach { (name, test) ->
            ChatService.clear()
            try {
                test.invoke()
                println("[PASS] $name")
            } catch (error: Throwable) {
                failures += 1
                println("[FAIL] $name -> ${error.message ?: error::class.simpleName}")
            }
        }
        ChatService.clear()
        println("Executed ${tests.size} test(s). Failures: $failures")
        return failures
    }

    private fun sendMessageCreatesChatAndStoresMessage() {
        val message = ChatService.sendMessage(fromId = 1, toId = 2, text = "Hello")

        assertEquals(1, message.id, "Message id should start from 1")
        assertEquals("Hello", message.text, "Message text should match input")

        val chats = ChatService.getChats(userId = 1)
        assertEquals(1, chats.size, "Sender should have exactly one chat")
        val preview = chats.single()
        assertEquals(message.chatId, preview.chatId, "Preview chat id should match message chat")
        assertEquals("Hello", preview.lastMessage?.text, "Preview last message text should match")
        assertEquals(0, preview.unreadCount, "Sender should have no unread messages")
    }

    private fun getMessagesReturnsOrderedCopiesAndMarksAsRead() {
        val first = ChatService.sendMessage(1, 2, "First")
        ChatService.sendMessage(1, 2, "Second")
        ChatService.sendMessage(1, 2, "Third")

        assertEquals(1, ChatService.getUnreadChatsCount(2), "Recipient should have one unread chat before reading")

        val messages = ChatService.getMessages(
            chatId = first.chatId,
            userId = 2,
            limit = 2
        )

        assertEquals(listOf("First", "Second"), messages.map { it.text }, "Messages should be returned in id order respecting limit")
        assertTrue(messages.all { it.isRead }, "All returned messages should be marked as read")
        assertEquals(1, ChatService.getUnreadChatsCount(2), "One message should remain unread when limit excludes it")

        val remaining = ChatService.getMessages(
            chatId = first.chatId,
            userId = 2,
            lastMessageId = messages.last().id
        )

        assertEquals(listOf("Third"), remaining.map { it.text }, "Subsequent call should return remaining messages")
        assertEquals(0, ChatService.getUnreadChatsCount(2), "All messages should be read after fetching remaining ones")
    }

    private fun deleteMessageRemovesEmptyChat() {
        val message = ChatService.sendMessage(3, 4, "To delete")

        val deleted = ChatService.deleteMessage(chatId = message.chatId, messageId = message.id)

        assertTrue(deleted, "Message deletion should succeed")
        assertTrue(ChatService.getChats(3).isEmpty(), "Chat should be removed for sender when empty")
        assertTrue(ChatService.getChats(4).isEmpty(), "Chat should be removed for recipient when empty")
    }

    private fun editMessageUpdatesTextAndResetsReadState() {
        val message = ChatService.sendMessage(5, 6, "Original")

        ChatService.getMessages(chatId = message.chatId, userId = 6)
        assertEquals(0, ChatService.getUnreadChatsCount(6), "Unread chats should be zero after reading")

        val edited = ChatService.editMessage(
            chatId = message.chatId,
            messageId = message.id,
            userId = 5,
            newText = "Updated"
        )

        assertTrue(edited, "Editing own message should succeed")
        assertEquals(1, ChatService.getUnreadChatsCount(6), "Editing should reset unread state for recipient")

        val messages = ChatService.getMessages(chatId = message.chatId, userId = 6)
        assertEquals("Updated", messages.single().text, "Recipient should see updated text after edit")
    }

    private fun assertEquals(expected: Any?, actual: Any?, message: String) {
        if (expected != actual) {
            throw AssertionError("$message. Expected: $expected, Actual: $actual")
        }
    }

    private fun assertTrue(condition: Boolean, message: String) {
        if (!condition) {
            throw AssertionError(message)
        }
    }
}
