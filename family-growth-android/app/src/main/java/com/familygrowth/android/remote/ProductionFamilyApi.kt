package com.familygrowth.android.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.math.BigDecimal
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.util.UUID

data class RemoteExchangePreview(val id:String,val sourceAmount:BigDecimal,val fee:BigDecimal,val targetAmount:BigDecimal,val notice:String)
data class RemoteWithdrawalQuote(val id:String,val moneyAmount:BigDecimal,val gross:BigDecimal,val fee:BigDecimal,val net:BigDecimal,val notice:String)
data class RemoteWithdrawal(val id:String,val gross:BigDecimal,val fee:BigDecimal,val net:BigDecimal,val status:String,val requestedAt:String)
data class RemoteRewardProduct(val id:String,val title:String,val coinCost:Int)
data class RemoteRewardOrder(val id:String,val title:String,val coinCost:Int,val status:String)
data class RemoteWish(val id:String,val title:String,val target:BigDecimal,val allocated:BigDecimal)
data class RemoteFund(val id:String,val name:String)
data class RemoteFundPosition(val nav:BigDecimal,val shares:BigDecimal)
data class RemoteFundTradePreview(val id:String,val side:String,val gross:BigDecimal,val fee:BigDecimal,val net:BigDecimal,val shares:BigDecimal,val nav:BigDecimal,val notice:String)
data class RemoteUsageReport(val period:String,val appMinutes:Int,val learningMinutes:Int,val submittedTasks:Int,val approvedTasks:Int)
data class RemoteRetentionPolicy(val usageDetailDays:Int,val version:Long)
data class RemoteRetentionRun(val usageEventsDeleted:Int,val allowancesRedacted:Int,val confirmationsCleared:Int)

/** Production-only adapter for the Stage 5–7/17/9 APIs. It never falls back to local success. */
class ProductionFamilyApi(private val sessions:MemorySessionStore) {
    private fun session()=sessions.get()
    suspend fun gift(amount:BigDecimal):RemoteResult<Unit> = parent("/children/{child}/gift-money","POST",JSONObject().put("amount",amount).put("note","家长在 Android 端登记压岁钱"),true){Unit}
    suspend fun exchangePreview(amount:BigDecimal):RemoteResult<RemoteExchangePreview> = parent("/children/{child}/exchange-previews","POST",JSONObject().put("direction","MONEY_TO_COIN").put("sourceAmount",amount),decode=::decodeExchangePreview)
    suspend fun confirmExchange(preview:String):RemoteResult<Unit> = parent("/exchange-previews/$preview/confirm","POST",null,true){Unit}
    suspend fun withdrawalQuote(amount:BigDecimal):RemoteResult<RemoteWithdrawalQuote> = parent("/children/{child}/withdrawal-quotes","POST",JSONObject().put("moneyAmount",amount),true){root->
        val d=root.getJSONObject("data");RemoteWithdrawalQuote(d.getString("id"),d.decimal("moneyAmount"),d.decimal("grossPayout"),d.decimal("feeAmount"),d.decimal("netPayout"),d.getString("notice"))
    }
    suspend fun requestWithdrawal(quote:String):RemoteResult<RemoteWithdrawal> = parent("/children/{child}/withdrawal-requests","POST",JSONObject().put("quoteId",quote),true){decodeWithdrawal(it.getJSONObject("data"))}
    suspend fun withdrawals():RemoteResult<List<RemoteWithdrawal>> = parent("/children/{child}/withdrawal-requests","GET",null){root->root.getJSONArray("data").objects(::decodeWithdrawal)}
    suspend fun approveWithdrawal(id:String):RemoteResult<RemoteWithdrawal> = parent("/withdrawal-requests/$id/approve","POST",null,true){decodeWithdrawal(it.getJSONObject("data"))}
    suspend fun rejectWithdrawal(id:String):RemoteResult<RemoteWithdrawal> = parent("/withdrawal-requests/$id/reject","POST",null,true){decodeWithdrawal(it.getJSONObject("data"))}
    suspend fun cancelWithdrawalRequest(id:String):RemoteResult<RemoteWithdrawal> = child("/withdrawal-requests/$id/cancel","POST",null,true){decodeWithdrawal(it.getJSONObject("data"))}
    suspend fun markWithdrawalPaid(id:String):RemoteResult<RemoteWithdrawal> = parent("/withdrawal-requests/$id/paid","POST",null,true){decodeWithdrawal(it.getJSONObject("data"))}
    suspend fun products():RemoteResult<List<RemoteRewardProduct>> = parent("/reward-products?activeOnly=true","GET",null){root->root.getJSONArray("data").objects{d->RemoteRewardProduct(d.getString("id"),d.getString("title"),d.getInt("coinCost"))}}
    suspend fun createProduct(title:String,price:Int):RemoteResult<RemoteRewardProduct> = parent("/reward-products","POST",JSONObject().put("title",title).put("coinCost",price).put("stockCount",1000000).put("active",true)){root->val d=root.getJSONObject("data");RemoteRewardProduct(d.getString("id"),d.getString("title"),d.getInt("coinCost"))}
    suspend fun orderReward(product:String):RemoteResult<RemoteRewardOrder> = child("/children/{child}/reward-orders","POST",JSONObject().put("productId",product),true){decodeRewardOrder(it.getJSONObject("data"))}
    suspend fun rewardOrders():RemoteResult<List<RemoteRewardOrder>> = parent("/children/{child}/reward-orders","GET",null){root->root.getJSONArray("data").objects(::decodeRewardOrder)}
    suspend fun reviewReward(order:String,approved:Boolean):RemoteResult<RemoteRewardOrder> = parent("/reward-orders/$order/review","POST",JSONObject().put("approved",approved),true){decodeRewardOrder(it.getJSONObject("data"))}
    suspend fun savingDeposit(amount:BigDecimal):RemoteResult<Unit> = parent("/children/{child}/saving/transfers","POST",JSONObject().put("direction","DEPOSIT").put("amount",amount),true){Unit}
    suspend fun savingBalance():RemoteResult<BigDecimal> = parent("/children/{child}/saving","GET",null){it.getJSONObject("data").decimal("balance")}
    suspend fun wishes():RemoteResult<List<RemoteWish>> = parent("/children/{child}/wishes","GET",null){root->root.getJSONArray("data").objects{d->RemoteWish(d.getString("id"),d.getString("title"),d.decimal("targetAmount"),d.decimal("allocatedAmount"))}}
    suspend fun createWish(title:String,target:BigDecimal):RemoteResult<RemoteWish> = parent("/children/{child}/wishes","POST",JSONObject().put("title",title).put("targetAmount",target)){root->val d=root.getJSONObject("data");RemoteWish(d.getString("id"),d.getString("title"),d.decimal("targetAmount"),d.decimal("allocatedAmount"))}
    suspend fun allocateWish(id:String,amount:BigDecimal):RemoteResult<RemoteWish> = parent("/wishes/$id/allocation","POST",JSONObject().put("amount",amount),true){root->val d=root.getJSONObject("data");RemoteWish(d.getString("id"),d.getString("title"),d.decimal("targetAmount"),d.decimal("allocatedAmount"))}
    suspend fun funds():RemoteResult<List<RemoteFund>> = parent("/funds","GET",null){root->root.getJSONArray("data").objects{d->RemoteFund(d.getString("id"),d.getString("name"))}}
    suspend fun createFund():RemoteResult<RemoteFund> = parent("/funds","POST",JSONObject().put("name","家庭成长模拟基金").put("riskLabel","纯模拟，可涨可跌")){root->val d=root.getJSONObject("data");RemoteFund(d.getString("id"),d.getString("name"))}
    suspend fun updateNav(fund:String,nav:BigDecimal):RemoteResult<Unit> = parent("/funds/$fund/nav","POST",JSONObject().put("navDate",LocalDate.now().toString()).put("nav",nav)){Unit}
    suspend fun configureFundFees(fund:String):RemoteResult<Unit> = parent("/funds/$fund/fee-rules","POST",JSONObject().put("buyFeeRate","0.000000").put("sellFeeRate","0.000000")){Unit}
    suspend fun tradePreview(fund:String,side:String,input:BigDecimal):RemoteResult<RemoteFundTradePreview> = parent("/children/{child}/funds/$fund/trade-previews","POST",JSONObject().put("side",side).put("inputAmount",input)){root->val d=root.getJSONObject("data");RemoteFundTradePreview(d.getString("id"),d.getString("side"),d.decimal("grossMoney"),d.decimal("feeAmount"),d.decimal("netMoney"),d.decimal("shares"),d.decimal("nav"),d.getString("notice"))}
    suspend fun confirmTrade(preview:String):RemoteResult<Unit> = parent("/fund-trade-previews/$preview/confirm","POST",null,true){Unit}
    suspend fun fundPosition(fund:String):RemoteResult<RemoteFundPosition> = parent("/children/{child}/funds/$fund/position","GET",null){root->val d=root.getJSONObject("data");RemoteFundPosition(d.decimal("nav"),d.decimal("shares"))}
    suspend fun todayReport():RemoteResult<RemoteUsageReport> = parent("/children/{child}/reports/today","GET",null){root->val d=root.getJSONObject("data");RemoteUsageReport(d.getString("date"),d.getInt("appMinutes"),d.getInt("learningMinutes"),d.getInt("submittedTasks"),d.getInt("approvedTasks"))}
    suspend fun monthlyReport():RemoteResult<RemoteUsageReport> = parent("/children/{child}/reports/monthly?month=${LocalDate.now().toString().substring(0,7)}","GET",null){root->val d=root.getJSONObject("data");RemoteUsageReport(d.getString("month"),d.getInt("appMinutes"),d.getInt("learningMinutes"),d.getInt("submittedTasks"),d.getInt("approvedTasks"))}
    suspend fun retentionPolicy():RemoteResult<RemoteRetentionPolicy> = parent("/children/{child}/data-rights/retention-policy","GET",null){root->val d=root.getJSONObject("data");RemoteRetentionPolicy(d.getInt("usageDetailDays"),d.getLong("version"))}
    suspend fun updateRetentionPolicy(days:Int,version:Long):RemoteResult<RemoteRetentionPolicy> = parent("/children/{child}/data-rights/retention-policy","PUT",JSONObject().put("usageDetailDays",days).put("expectedVersion",version).put("reason","家长在 Android 隐私中心调整使用明细保留期")){root->val d=root.getJSONObject("data");RemoteRetentionPolicy(d.getInt("usageDetailDays"),d.getLong("version"))}
    suspend fun runRetention():RemoteResult<RemoteRetentionRun> = parent("/children/{child}/data-rights/retention-runs","POST",null){root->val d=root.getJSONObject("data");RemoteRetentionRun(d.getInt("usageEventsDeleted"),d.getInt("allowancesRedacted"),d.getInt("confirmationsCleared"))}

    private suspend fun <T> parent(path:String,method:String,body:JSONObject?,key:Boolean=false,decode:(JSONObject)->T):RemoteResult<T>{val s=session()?:return RemoteResult.Failure("请先连接家庭服务");return request(s,path,method,s.parentToken,body,key,decode)}
    private suspend fun <T> child(path:String,method:String,body:JSONObject?,key:Boolean=false,decode:(JSONObject)->T):RemoteResult<T>{val s=session()?:return RemoteResult.Failure("请先连接家庭服务");return request(s,path,method,s.childToken,body,key,decode)}
    private suspend fun <T> request(s:RemoteSession,path:String,method:String,token:String,body:JSONObject?,key:Boolean,decode:(JSONObject)->T):RemoteResult<T> = withContext(Dispatchers.IO){
        try { val target=path.replace("{child}",s.childId);val c=(URL(s.baseUrl+"/api/v1/families/${s.familyId}"+target).openConnection() as HttpURLConnection).apply{requestMethod=method;connectTimeout=8_000;readTimeout=10_000;setRequestProperty("Accept","application/json");setRequestProperty("Authorization","Bearer $token");if(key)setRequestProperty("Idempotency-Key",UUID.randomUUID().toString());if(body!=null){doOutput=true;setRequestProperty("Content-Type","application/json");outputStream.use{it.write(body.toString().toByteArray())}}};val code=c.responseCode;if(code==401){c.disconnect();sessions.clear();return@withContext RemoteResult.Unauthorized};val stream=if(code in 200..299)c.inputStream else c.errorStream;val text=stream?.bufferedReader()?.use{it.readText()}.orEmpty();c.disconnect();if(code !in 200..299)return@withContext remoteFailureForStatus(code);RemoteResult.Ok(decode(JSONObject(text))) } catch(_:IOException){RemoteResult.Failure("无法连接家庭服务，本次操作没有记为成功",RemoteFailureKind.RETRYABLE)} catch(_:Exception){RemoteResult.Failure("家庭服务返回了无法识别的数据")}
    }
    private fun decodeWithdrawal(d:JSONObject)=RemoteWithdrawal(d.getString("id"),d.decimal("grossPayout"),d.decimal("feeAmount"),d.decimal("netPayout"),d.getString("status"),d.getString("requestedAt"))
    private fun decodeRewardOrder(d:JSONObject)=RemoteRewardOrder(d.getString("id"),d.getString("productTitle"),d.getInt("coinCost"),d.getString("status"))
}

private fun JSONObject.decimal(name:String)=getString(name).toBigDecimal()
private fun <T> JSONArray.objects(decode:(JSONObject)->T)=buildList{for(i in 0 until length())add(decode(getJSONObject(i)))}
internal fun decodeExchangePreview(root:JSONObject):RemoteExchangePreview{val d=root.getJSONObject("data");return remoteExchangePreview(d.getString("id"),d.getString("sourceAmount"),d.getString("sourceFee"),d.getString("targetAmount"),d.getString("educationNotice"))}
internal fun remoteExchangePreview(id:String,source:String,fee:String,target:String,notice:String)=RemoteExchangePreview(id,source.toBigDecimal(),fee.toBigDecimal(),target.toBigDecimal(),notice)
