package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsMessagesTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.transactionCompletedChannel
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.commonLogger
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.database.PostsLikesMessagesTable
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.database.PostsLikesTable
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.receivers.*
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.subscribeChecking
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.toTable
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.makeLinkToMessage
import com.github.insanusmokrassar.TelegramBotAPI.bot.RequestsExecutor
import com.github.insanusmokrassar.TelegramBotAPI.requests.DeleteMessage
import com.github.insanusmokrassar.TelegramBotAPI.requests.edit.text.EditChatMessageText
import com.github.insanusmokrassar.TelegramBotAPI.requests.send.SendMessage
import com.github.insanusmokrassar.TelegramBotAPI.types.ChatId
import com.github.insanusmokrassar.TelegramBotAPI.types.ChatIdentifier
import com.github.insanusmokrassar.TelegramBotAPI.types.ParseMode.MarkdownParseMode
import com.github.insanusmokrassar.TelegramBotAPI.types.buttons.InlineKeyboardButtons.*
import com.github.insanusmokrassar.TelegramBotAPI.types.buttons.InlineKeyboardMarkup
import com.github.insanusmokrassar.TelegramBotAPI.utils.extensions.executeAsync
import com.github.insanusmokrassar.TelegramBotAPI.utils.matrix
import com.github.insanusmokrassar.TelegramBotAPI.utils.row
import java.lang.ref.WeakReference

suspend fun disableLikesForPost(
    postId: Int,
    executor: RequestsExecutor,
    sourceChatId: ChatIdentifier,
    postsLikesMessagesTable: PostsLikesMessagesTable
) {
    postsLikesMessagesTable.messageIdByPostId(postId) ?.let {
        messageId ->

        executor.execute(
            DeleteMessage(
                sourceChatId,
                messageId
            )
        )

        postsLikesMessagesTable.disableLikes(postId)
    }
}

class RegisteredRefresher(
    sourceChatId: ChatId,
    executor: RequestsExecutor,
    postsLikesTable: PostsLikesTable,
    postsLikesMessagesTable: PostsLikesMessagesTable
) {
    private val botWR = WeakReference(executor)

    init {
        postsLikesTable.ratingsChannel.subscribeChecking(
            {
                commonLogger.throwing(
                    "RegisteredRefresher",
                    "updateMessageId",
                    it
                )
                true
            }
        ) {
            refreshRegisteredMessage(
                sourceChatId,
                botWR.get() ?: return@subscribeChecking false,
                it.first,
                postsLikesTable,
                postsLikesMessagesTable,
                it.second
            )
            true
        }

        transactionCompletedChannel.subscribeChecking {
            refreshRegisteredMessage(
                sourceChatId,
                botWR.get() ?: return@subscribeChecking false,
                it,
                postsLikesTable,
                postsLikesMessagesTable
            )
            true
        }

        PostsTable.postRemovedChannel.subscribeChecking(
            {
                commonLogger.throwing(
                    "RegisteredRefresher",
                    "remove registered post-message link",
                    it
                )
                true
            }
        ) {
            disableLikesForPost(
                it,
                botWR.get() ?: return@subscribeChecking false,
                sourceChatId,
                postsLikesMessagesTable
            )
            true
        }
    }
}

internal fun refreshRegisteredMessage(
    chatId: ChatId,
    executor: RequestsExecutor,
    postId: Int,
    postsLikesTable: PostsLikesTable,
    postsLikesMessagesTable: PostsLikesMessagesTable,
    postRating: Int = postsLikesTable.getPostRating(postId),
    username: String? = null
) {
    val dislikeButton = CallbackDataInlineKeyboardButton(
        makeDislikeText(
            postsLikesTable.postDislikes(postId)
        ),
        makeDislikeInline(postId)
    )
    val likeButton = CallbackDataInlineKeyboardButton(
        makeLikeText(
            postsLikesTable.postLikes(postId)
        ),
        makeLikeInline(postId)
    )

    val buttons = matrix<InlineKeyboardButton> {
        row {
            add(dislikeButton)
            add(likeButton)
        }
    }.let { base ->
        username ?.let { chatUsername ->
            PostsMessagesTable.getMessagesOfPost(
                postId
            ).map {
                makeLinkToMessage(
                    chatUsername,
                    it.messageId
                )
            }.mapIndexed { index, s ->
                URLInlineKeyboardButton(
                    (index + 1).toString(),
                    s
                )
            }.toTable(4)
        } ?.let {
            base + it
        } ?: base
    }

    val markup = InlineKeyboardMarkup(buttons)

    val message = "Rating: $postRating"

    val registeredMessageId = postsLikesMessagesTable.messageIdByPostId(postId)

    if (registeredMessageId == null) {
        SendMessage(
            chatId,
            message,
            replyMarkup = markup,
            replyToMessageId = PostsMessagesTable.getMessagesOfPost(postId).firstOrNull() ?.messageId ?: return,
            parseMode = MarkdownParseMode
        ).let {
            executor.executeAsync(
                it,
                onSuccess = { response ->
                    if (!postsLikesMessagesTable.enableLikes(postId, response.messageId)) {
                        executor.executeAsync(
                            DeleteMessage(
                                chatId,
                                response.messageId
                            )
                        )
                    }
                }
            )
        }
    } else {
        EditChatMessageText(
            chatId,
            registeredMessageId,
            message,
            replyMarkup = markup
        ).let {
            executor.executeAsync(it)
        }
    }
}
