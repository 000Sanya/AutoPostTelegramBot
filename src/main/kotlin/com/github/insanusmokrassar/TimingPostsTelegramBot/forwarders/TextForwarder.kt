package com.github.insanusmokrassar.TimingPostsTelegramBot.forwarders

import com.github.insanusmokrassar.TimingPostsTelegramBot.models.PostMessage
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.request.ParseMode
import com.pengrad.telegrambot.request.SendMessage

class TextForwarder : Forwarder {
    override fun canForward(message: PostMessage): Boolean {
        return message.message ?. text() != null
    }

    override fun forward(bot: TelegramBot, targetChatId: Long, vararg messages: PostMessage): List<Int> {
        return messages.mapNotNull {
            it.message
        }.map {
            SendMessage(
                targetChatId,
                it.text()
            ).parseMode(
                ParseMode.Markdown
            )
        }.mapNotNull {
            bot.execute(it).message() ?.messageId()
        }
    }
}