package com.familygrowth.android.core

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.time.LocalDate

class LocalFamilyStore(context: Context) {
    private val preferences = context.getSharedPreferences("family_growth_local_v1", Context.MODE_PRIVATE)

    fun load(): Result<FamilyLocalState> = runCatching {
        preferences.getString(KEY_STATE, null)?.let(::decode) ?: FamilyLocalState()
    }

    fun save(state: FamilyLocalState): Result<Unit> = runCatching {
        check(preferences.edit().putString(KEY_STATE, encode(state).toString()).commit()) { "本地数据保存失败" }
    }

    fun hasPin(): Boolean = !preferences.getString(KEY_PIN, null).isNullOrBlank()
    fun pinHash(): String? = preferences.getString(KEY_PIN, null)
    fun savePin(hash: String): Boolean = preferences.edit().putString(KEY_PIN, hash).commit()

    private fun encode(state: FamilyLocalState) = JSONObject().apply {
        put("tasks", JSONArray().apply { state.tasks.forEach { task -> put(JSONObject().apply {
            put("id", task.id); put("title", task.title); put("minutes", task.minutes)
            put("moneyReward", task.moneyReward.toPlainString()); put("coinReward", task.coinReward)
            put("xpReward", task.xpReward); put("status", task.status.name); put("source", task.source.name)
            task.sourceVideoId?.let { put("sourceVideoId", it) }
        }) } })
        put("wallet", JSONObject().apply {
            put("money", state.wallet.money.toPlainString()); put("coin", state.wallet.coin); put("xp", state.wallet.xp)
        })
        put("ledger", JSONArray().apply { state.ledger.forEach { entry -> put(JSONObject().apply {
            put("id", entry.id); put("type", entry.type); put("description", entry.description)
            put("moneyDelta", entry.moneyDelta.toPlainString()); put("coinDelta", entry.coinDelta)
            put("xpDelta", entry.xpDelta); put("createdAt", entry.createdAt)
        }) } })
        put("withdrawals", JSONArray().apply { state.withdrawals.forEach { request -> put(JSONObject().apply {
            put("id", request.id); put("gross", request.gross.toPlainString()); put("fee", request.fee.toPlainString())
            put("net", request.net.toPlainString()); put("status", request.status.name); put("createdAt", request.createdAt)
        }) } })
        put("rewards", JSONArray().apply { state.rewards.forEach { reward -> put(JSONObject().apply {
            put("id", reward.id); put("title", reward.title); put("coinPrice", reward.coinPrice)
        }) } })
        put("rewardInterestIds", JSONArray().apply { state.rewardInterestIds.forEach { put(it) } })
        put("learningProgress", JSONArray().apply { state.learningProgress.forEach { progress -> put(JSONObject().apply {
            put("videoId", progress.videoId); put("watchedSeconds", progress.watchedSeconds); put("completed", progress.completed)
        }) } })
        put("savings", JSONArray().apply { state.savings.forEach { goal -> put(JSONObject().apply {
            put("id", goal.id); put("title", goal.title); put("target", goal.target.toPlainString()); put("saved", goal.saved.toPlainString())
        }) } })
        put("wishes", JSONArray().apply { state.wishes.forEach { wish -> put(JSONObject().apply {
            put("id", wish.id); put("title", wish.title); put("target", wish.target.toPlainString())
        }) } })
        put("fund", JSONObject().apply { put("nav", state.fund.nav.toPlainString()); put("shares", state.fund.shares.toPlainString()) })
        put("usage", JSONObject().apply {
            put("dailyLimitMinutes", state.usage.dailyLimitMinutes); put("sessionLimitMinutes", state.usage.sessionLimitMinutes)
            put("usedMinutes", state.usage.usedMinutes); put("usageDate", state.usage.usageDate)
        })
        put("learningRewardPolicy", JSONObject().apply {
            put("money", state.learningRewardPolicy.money.toPlainString())
            put("coin", state.learningRewardPolicy.coin)
            put("xp", state.learningRewardPolicy.xp)
        })
        put("experience", JSONObject().apply {
            put("birthDate", state.experience.birthDate)
            put("recommendedStage", state.experience.recommendedStage.name)
            state.experience.stageOverride?.let { put("stageOverride", it.name) }
            put("effectiveStage", state.experience.effectiveStage.name)
            state.experience.recommendedPrimaryBand?.let { put("recommendedPrimaryBand", it.name) }
            state.experience.primaryBandOverride?.let { put("primaryBandOverride", it.name) }
            state.experience.effectivePrimaryBand?.let { put("effectivePrimaryBand", it.name) }
            put("overrideReason", state.experience.overrideReason)
            put("hapticsEnabled", state.experience.hapticsEnabled)
            put("version", state.experience.version)
            put("source", state.experience.source.name)
        })
    }

    private fun decode(text: String): FamilyLocalState {
        val root = JSONObject(text)
        val wallet = root.optJSONObject("wallet") ?: JSONObject()
        val fund = root.optJSONObject("fund") ?: JSONObject()
        val usage = root.optJSONObject("usage") ?: JSONObject()
        val learningReward = root.optJSONObject("learningRewardPolicy") ?: JSONObject()
        val experience = root.optJSONObject("experience")
        return FamilyLocalState(
            tasks = root.optJSONArray("tasks").mapObjects { value -> LocalGrowthTask(
                id = value.getString("id"), title = value.getString("title"), minutes = value.getInt("minutes"),
                moneyReward = value.decimal("moneyReward"), coinReward = value.getInt("coinReward"),
                xpReward = value.getInt("xpReward"), status = TaskStatus.valueOf(value.getString("status")),
                source = TaskSource.valueOf(value.optString("source", TaskSource.FAMILY.name)),
                sourceVideoId = value.optString("sourceVideoId").takeIf(String::isNotBlank),
            ) },
            wallet = WalletSnapshot(wallet.decimal("money", "0.00"), wallet.optInt("coin"), wallet.optInt("xp")),
            ledger = root.optJSONArray("ledger").mapObjects { value -> LocalLedgerEntry(
                id = value.getString("id"), type = value.getString("type"), description = value.getString("description"),
                moneyDelta = value.decimal("moneyDelta", "0.00"), coinDelta = value.optInt("coinDelta"),
                xpDelta = value.optInt("xpDelta"), createdAt = value.optLong("createdAt"),
            ) },
            withdrawals = root.optJSONArray("withdrawals").mapObjects { value -> LocalWithdrawalRequest(
                id = value.getString("id"), gross = value.decimal("gross"), fee = value.decimal("fee"),
                net = value.decimal("net"), status = WithdrawalStatus.valueOf(value.getString("status")),
                createdAt = value.optLong("createdAt"),
            ) },
            rewards = root.optJSONArray("rewards").mapObjects { value -> LocalRewardItem(value.getString("id"), value.getString("title"), value.getInt("coinPrice")) },
            rewardInterestIds = root.optJSONArray("rewardInterestIds").mapStrings(),
            learningProgress = root.optJSONArray("learningProgress").mapObjects { value -> LocalLearningProgress(
                videoId = value.getString("videoId"),
                watchedSeconds = value.optInt("watchedSeconds"),
                completed = value.optBoolean("completed"),
            ) },
            learningRewardPolicy = LearningRewardPolicy(
                money = learningReward.decimal("money", "0.00"),
                coin = learningReward.optInt("coin", 2),
                xp = learningReward.optInt("xp", 5),
            ),
            savings = root.optJSONArray("savings").mapObjects { value -> LocalSavingGoal(value.getString("id"), value.getString("title"), value.decimal("target"), value.decimal("saved", "0.00")) },
            wishes = root.optJSONArray("wishes").mapObjects { value -> LocalWish(value.getString("id"), value.getString("title"), value.decimal("target")) },
            fund = LocalFundPosition(fund.decimal("nav", "1.0000"), fund.decimal("shares", "0.0000")),
            usage = UsagePolicy(
                dailyLimitMinutes = usage.optInt("dailyLimitMinutes", 20), sessionLimitMinutes = usage.optInt("sessionLimitMinutes", 10),
                usedMinutes = usage.optInt("usedMinutes", 0), usageDate = usage.optString("usageDate", java.time.LocalDate.now().toString()),
            ),
            experience = experience?.let { value ->
                val birthDate = value.optString("birthDate", LocalDate.now().minusYears(4).toString())
                val recommended = runCatching { SchoolStage.valueOf(value.optString("recommendedStage")) }
                    .getOrElse { ChildExperiencePolicy.recommendedStage(LocalDate.parse(birthDate)) }
                val override = value.optString("stageOverride").takeIf(String::isNotBlank)?.let { SchoolStage.valueOf(it) }
                val recommendedBand = value.optString("recommendedPrimaryBand").takeIf(String::isNotBlank)?.let { PrimaryGradeBand.valueOf(it) }
                val bandOverride = value.optString("primaryBandOverride").takeIf(String::isNotBlank)?.let { PrimaryGradeBand.valueOf(it) }
                ChildExperienceSettings(
                    birthDate = birthDate,
                    recommendedStage = recommended,
                    stageOverride = override,
                    effectiveStage = runCatching { SchoolStage.valueOf(value.optString("effectiveStage")) }.getOrDefault(override ?: recommended),
                    recommendedPrimaryBand = recommendedBand,
                    primaryBandOverride = bandOverride,
                    effectivePrimaryBand = value.optString("effectivePrimaryBand").takeIf(String::isNotBlank)?.let { PrimaryGradeBand.valueOf(it) },
                    overrideReason = value.optString("overrideReason"),
                    hapticsEnabled = value.optBoolean("hapticsEnabled", true),
                    version = value.optLong("version", 0),
                    source = runCatching { ExperienceSource.valueOf(value.optString("source")) }.getOrDefault(ExperienceSource.LOCAL),
                )
            } ?: ChildExperiencePolicy.localSettings(LocalDate.now().minusYears(4), null, null, "", true),
        )
    }

    private fun JSONObject.decimal(key: String, default: String? = null): BigDecimal =
        optString(key, default ?: "").toBigDecimalOrNull() ?: throw IllegalStateException("本地金额字段损坏：$key")

    private inline fun <T> JSONArray?.mapObjects(block: (JSONObject) -> T): List<T> {
        if (this == null) return emptyList()
        return buildList { for (index in 0 until length()) add(block(getJSONObject(index))) }
    }

    private fun JSONArray?.mapStrings(): List<String> {
        if (this == null) return emptyList()
        return buildList { for (index in 0 until length()) add(getString(index)) }
    }

    companion object {
        private const val KEY_STATE = "state"
        private const val KEY_PIN = "parent_pin_bcrypt"
    }
}
