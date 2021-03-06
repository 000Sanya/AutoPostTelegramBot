package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.choosers

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.commonLogger
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.database.PostIdRatingPair
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.*
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.parseDateTimes
import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import java.util.*

private const val ascendSort = "ascend"
private const val descendSort = "descend"
private const val randomSort = "random"
private const val defaultSort = descendSort

private val commonRandom = Random()

private typealias InnerChooser = (List<PostIdRatingPair>, Int) -> Collection<Int>

private val commonInnerChoosers = mapOf<String, InnerChooser>(
    ascendSort to {
        pairs, count ->
        pairs.sortedBy { (_, rating) -> rating }.let {
            it.subList(
                0,
                if (it.size < count) {
                    it.size
                } else {
                    count
                }
            )
        }.map {
            (postId, _) ->
            postId
        }
    },
    descendSort to {
        pairs, count ->
        pairs.sortedByDescending { (_, rating) -> rating }.let {
            it.subList(
                0,
                if (it.size < count) {
                    it.size
                } else {
                    count
                }
            )
        }.map {
            (postId, _) ->
            postId
        }
    },
    randomSort to {
        pairs, count ->
        mutableSetOf<Int>().apply {
            val from = pairs.toMutableSet()
            while (size < count && size < from.size) {
                val chosen = from.elementAt(
                    commonRandom.nextInt(
                        from.size
                    )
                )
                from.remove(chosen)
                add(
                    chosen.first
                )
            }
        }
    }
)

interface SmartChooserBaseConfigItem {
    val minRate: Int?
    val maxRate: Int?
    val sort: String
    val count: Int
    val minAge: Long?
    val maxAge: Long?
    val otherwise: SmartChooserAdditionalConfigItem?
}

abstract class AbstractSmartChooserBaseConfigItem : SmartChooserBaseConfigItem {
    @Transient
    private val minAgeAsDateTime: DateTime? by lazy {
        minAge ?.let {
            DateTime.now().withZone(DateTimeZone.UTC).withMillis(it).withZone(DateTimeZone.getDefault())
        }
    }

    @Transient
    private val maxAgeAsDateTime: DateTime? by lazy {
        maxAge ?.let {
            DateTime.now().withZone(DateTimeZone.UTC).withMillis(it).withZone(DateTimeZone.getDefault())
        }
    }

    @Transient
    val chooser: InnerChooser?
        get() = commonInnerChoosers[sort]

    fun checkPostAge(postId: Int): Boolean {
        val postDateTime: DateTime = PostsTable.getPostCreationDateTime(postId) ?: return false
        val minIsOk = minAgeAsDateTime ?.let {
                minDateTime ->
            postDateTime.plus(minDateTime.millis).isBeforeNow
        } ?: true
        val maxIsOk = maxAgeAsDateTime ?.let {
                minDateTime ->
            postDateTime.plus(minDateTime.millis).isAfterNow
        } ?: true
        return minIsOk && maxIsOk
    }
}

@Serializable
data class SmartChooserAdditionalConfigItem(
    @Optional
    override val minRate: Int? = null,
    @Optional
    override val maxRate: Int? = null,
    @Optional
    override val sort: String = defaultSort,
    @Optional
    override val count: Int = 1,
    @Optional
    override val minAge: Long? = null,
    @Optional
    override val maxAge: Long? = null,
    @Optional
    override val otherwise: SmartChooserAdditionalConfigItem? = null
) : AbstractSmartChooserBaseConfigItem()

@Serializable
data class SmartChooserConfigItem (
    @Optional
    override val minRate: Int? = null,
    @Optional
    override val maxRate: Int? = null,
    @Optional
    val time: String? = null,
    @Optional
    val times: List<String>? = null,
    @Optional
    override val sort: String = defaultSort,
    @Optional
    override val count: Int = 1,
    @Optional
    override val minAge: Long? = null,
    @Optional
    override val maxAge: Long? = null,
    @Optional
    override val otherwise: SmartChooserAdditionalConfigItem? = null
) : AbstractSmartChooserBaseConfigItem() {
    @Transient
    private val timePairs: List<CalculatedPeriod> by lazy {
        (times ?.flatMap {
            it.parseDateTimes()
        } ?: time ?.parseDateTimes() ?: emptyList()).asPairs()
    }

    fun isActual(
        now: DateTime = DateTime.now()
    ): Boolean {
        return timePairs.firstOrNull {
                pair ->
            pair.isBetween(now)
        } != null
    }

    override fun toString(): String {
        val stringBuilder = StringBuilder()
        stringBuilder.append("Rating: ${minRate ?: "any low"} - ${maxRate ?: "any big"}\n")
        stringBuilder.append("Time:\n")
        timePairs.forEach {
                (from, to) ->
            stringBuilder.append("  ${from.dateTime} - ${to.dateTime}\n")
        }
        return stringBuilder.toString()
    }
}

@Serializable
class SmartChooser(
    @Optional
    val times: List<SmartChooserConfigItem> = emptyList()
) : RateChooser() {
    init {
        commonLogger.info("Smart chooser inited: ${times.joinToString(separator = "\n") { it.toString() }}")
    }

    override fun triggerChoose(): Collection<Int> {
        var actualItem: AbstractSmartChooserBaseConfigItem? = times.firstOrNull { it.isActual() }
        var resultList: Collection<Int> = emptyList()
        while (actualItem != null && resultList.isEmpty()) {
            val realItem = actualItem
            resultList = postsLikesTable ?.getRateRange(
                realItem.minRate,
                realItem.maxRate
            ) ?.filter { (postId, _) ->
                realItem.checkPostAge(postId)
            } ?.let { chosenList ->
                realItem.chooser ?.invoke(
                    chosenList,
                    realItem.count
                )
            } ?: emptyList()
            actualItem = realItem.otherwise
        }
        return resultList
    }
}