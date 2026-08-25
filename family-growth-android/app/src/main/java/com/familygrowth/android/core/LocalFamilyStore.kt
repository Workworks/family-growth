package com.familygrowth.android.core

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal

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
            put("xpReward", task.xpReward); put("status", task.status.name)
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
    }

    private fun decode(text: String): FamilyLocalState {
        val root = JSONObject(text)
        val wallet = root.optJSONObject("wallet") ?: JSONObject()
        val fund = root.optJSONObject("fund") ?: JSONObject()
        val usage = root.optJSONObject("usage") ?: JSONObject()
        return FamilyLocalState(
            tasks = root.optJSONArray("tasks").mapObjects { value -> LocalGrowthTask(
                id = value.getString("id"), title = value.getString("title"), minutes = value.getInt("minutes"),
                moneyReward = value.decimal("moneyReward"), coinReward = value.getInt("coinReward"),
                xpReward = value.getInt("xpReward"), status = TaskStatus.valueOf(value.getString("status")),
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
            savings = root.optJSONArray("savings").mapObjects { value -> LocalSavingGoal(value.getString("id"), value.getString("title"), value.decimal("target"), value.decimal("saved", "0.00")) },
            wishes = root.optJSONArray("wishes").mapObjects { value -> LocalWish(value.getString("id"), value.getString("title"), value.decimal("target")) },
            fund = LocalFundPosition(fund.decimal("nav", "1.0000"), fund.decimal("shares", "0.0000")),
            usage = UsagePolicy(
                dailyLimitMinutes = usage.optInt("dailyLimitMinutes", 20), sessionLimitMinutes = usage.optInt("sessionLimitMinutes", 10),
                usedMinutes = usage.optInt("usedMinutes", 0), usageDate = usage.optString("usageDate", java.time.LocalDate.now().toString()),
            ),
        )
    }

    private fun JSONObject.decimal(key: String, default: String? = null): BigDecimal =
        optString(key, default ?: "").toBigDecimalOrNull() ?: throw IllegalStateException("本地金额字段损坏：$key")

    private inline fun <T> JSONArray?.mapObjects(block: (JSONObject) -> T): List<T> {
        if (this == null) return emptyList()
        return buildList { for (index in 0 until length()) add(block(getJSONObject(index))) }
    }

    companion object {
        private const val KEY_STATE = "state"
        private const val KEY_PIN = "parent_pin_bcrypt"
    }
}
