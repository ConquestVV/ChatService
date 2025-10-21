package ru.netology

fun main() {
    ChatService.sendMessage(fromId = 1, toId = 2, text = "Привет!")
    ChatService.sendMessage(fromId = 2, toId = 1, text = "Добрый день!")
    ChatService.sendMessage(fromId = 1, toId = 2, text = "Как дела?")

    val chats = ChatService.getChats(userId = 1)
    println("Чаты пользователя 1: $chats")

    val messages = ChatService.getMessages(chatId = chats.first().chatId, userId = 1)
    println("Сообщения в чате: $messages")

    val unreadCount = ChatService.getUnreadChatsCount(userId = 2)
    println("Количество чатов с непрочитанными сообщениями у пользователя 2: $unreadCount")
}
