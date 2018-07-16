package com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.rating

import com.github.insanusmokrassar.IObjectK.exceptions.ReadException
import com.github.insanusmokrassar.IObjectKRealisations.toIObject
import com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.CallbackQueryReceivers.CallbackQueryReceiver
import com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.rating.database.PostsLikesTable
import com.github.insanusmokrassar.TimingPostsTelegramBot.utils.extensions.queryAnswer
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.CallbackQuery

const val dislike = "\uD83D\uDC4E"

internal fun makeDislikeText(dislikes: Int) = "$dislike $dislikes"

private const val dislikeIdentifier = "dislike"

fun makeDislikeInline(postId: Int): String = "$dislikeIdentifier: $postId"
fun extractDislikeInline(from: String): Int? = try {
    from.toIObject().get<String>(dislikeIdentifier).toInt()
} catch (e: ReadException) {
    null
}

class DislikeReceiver(bot: TelegramBot) : CallbackQueryReceiver(bot) {
    override fun invoke(
        query: CallbackQuery,
        bot: TelegramBot?
    ) {
        extractDislikeInline(
            query.data()
        )?.let {
            postId ->

            PostsLikesTable.userDislikePost(
                query.from().id().toLong(),
                postId
            )

            bot ?.queryAnswer(
                query.id(),
                "Voted"
            )
        }
    }
}