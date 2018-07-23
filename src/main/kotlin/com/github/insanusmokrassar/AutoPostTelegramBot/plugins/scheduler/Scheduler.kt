package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.scheduler

import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.pluginLogger
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.publishers.Publisher
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.subscribe
import kotlinx.coroutines.experimental.*
import org.joda.time.DateTime

private typealias PostTimeToJob = Pair<PostIdPostTime, Job>

class Scheduler(
    private val schedulesTable: PostsSchedulesTable,
    private val publisher: Publisher
) {
    private var currentPlannedPostTimeAndJob: PostTimeToJob? = null
        @Synchronized
        set(value) {
            value ?.also {
                if (field ?.first ?.second ?.isAfter(value.first.second) == false) {
                    value.second.cancel()

                    return
                }
            }
            field ?.second ?.cancel()

            field = value
        }

    init {
        schedulesTable.postTimeRegisteredChannel.subscribe {
            updateJob(it)
        }
        schedulesTable.postTimeChangedChannel.subscribe {
            updateJob(it)
        }
        schedulesTable.postTimeRemovedChannel.subscribe {
            if (currentPlannedPostTimeAndJob ?.first ?.first == it) {
                schedulesTable.nearPost() ?.also {
                    updateJob(it)
                } ?:also {
                    currentPlannedPostTimeAndJob = null
                }
            }
        }
    }

    private fun updateJob(by: PostIdPostTime)  {
        try {
            currentPlannedPostTimeAndJob = by to createScheduledJob(by)
        } catch (e: Exception) {
            pluginLogger.throwing(
                Scheduler::class.java.simpleName,
                "update job",
                e
            )
        }
    }

    private fun createScheduledJob(by: PostIdPostTime): Job {
        return launch {
            delay(by.second.minus(DateTime.now().millis).millis)
            if (isActive) {
                currentPlannedPostTimeAndJob = null

                publisher.publishPost(by.first)

                schedulesTable.unregisterPost(by.first)

                schedulesTable.nearPost() ?.also {
                    updateJob(it)
                }
            }
        }
    }
}
