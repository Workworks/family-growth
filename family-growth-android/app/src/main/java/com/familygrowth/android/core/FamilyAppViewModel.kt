package com.familygrowth.android.core

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.familygrowth.android.BuildConfig
import com.familygrowth.android.remote.*
import kotlinx.coroutines.launch
import org.mindrot.jbcrypt.BCrypt
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class FamilyAppViewModel(application: Application) : AndroidViewModel(application) {
    private val store = LocalFamilyStore(application)
    private val sessionStore = MemorySessionStore()
    private val reliableSync = ReliableSyncStateStore(application)
    private val remote = RemoteFamilyRepository(HttpFamilyApiTransport(), sessionStore, BuildConfig.DEBUG, reliableSync)
    private val production = ProductionFamilyApi(sessionStore)
    private val learningOutbox = LearningOutbox(EncryptedLearningOutboxStore(application))
    private var remoteCompletionByTask: Map<String, String> = emptyMap()
    private val pendingUsage = mutableListOf<PendingUsage>()
    private var usageFlushRunning = false
    private var learningFlushRunning = false
    private var teachingActionRunningInternal = false

    var state by mutableStateOf(FamilyLocalState())
        private set
    var mode by mutableStateOf(AppMode.CHILD)
        private set
    var section by mutableStateOf(AppSection.TODAY)
        private set
    var message by mutableStateOf<String?>(null)
        private set
    var hasParentPin by mutableStateOf(store.hasPin())
        private set
    var sessionUsedMinutes by mutableIntStateOf(0)
        private set
    var connectionState by mutableStateOf<ConnectionState>(ConnectionState.Disconnected)
        private set
    var educationSources by mutableStateOf<List<RemoteEducationSource>>(emptyList())
        private set
    var childEducationCatalog by mutableStateOf<List<RemoteChildEducationSource>>(emptyList())
        private set
    var learningAssignments by mutableStateOf<List<RemoteLearningAssignment>>(emptyList())
        private set
    var juniorLearningPlan by mutableStateOf<RemoteJuniorPlan?>(null)
        private set
    var juniorLearningReport by mutableStateOf<RemoteJuniorLearningReport?>(null)
        private set
    var seniorModuleConfiguration by mutableStateOf<RemoteSeniorModuleConfiguration?>(null)
        private set
    var seniorGoals by mutableStateOf<List<RemoteSeniorGoal>>(emptyList())
        private set
    var seniorReflections by mutableStateOf<List<RemoteSeniorReflection>>(emptyList())
        private set
    var seniorLearningReport by mutableStateOf<RemoteSeniorLearningReport?>(null)
        private set
    var remoteUsageAccess by mutableStateOf<RemoteUsageAccess?>(null)
        private set
    var erasurePreview by mutableStateOf<RemoteErasurePreview?>(null)
        private set
    var lastExportPath by mutableStateOf<String?>(null)
        private set
    var stageTransitionPreview by mutableStateOf<RemoteStageTransitionPreview?>(null)
        private set
    var learningSupportByAssignment by mutableStateOf<Map<String,List<RemoteSupportEvent>>>(emptyMap())
        private set
    var primaryLearningReport by mutableStateOf<RemotePrimaryLearningReport?>(null)
        private set
    var teachingCourses by mutableStateOf<List<RemoteCourseSummary>>(emptyList())
        private set
    var pendingLearningActions by mutableStateOf(learningOutbox.snapshot())
        private set
    var teachingActionRunning by mutableStateOf(false)
        private set
    var pendingExchangePreview by mutableStateOf<RemoteExchangePreview?>(null)
        private set
    var pendingWithdrawalQuote by mutableStateOf<RemoteWithdrawalQuote?>(null)
        private set
    var pendingFundTrade by mutableStateOf<RemoteFundTradePreview?>(null)
        private set
    var todayUsageReport by mutableStateOf<RemoteUsageReport?>(null)
        private set
    var monthlyUsageReport by mutableStateOf<RemoteUsageReport?>(null)
        private set
    var rewardOrders by mutableStateOf<List<RemoteRewardOrder>>(emptyList())
        private set
    var rewardBudget by mutableStateOf<RemoteRewardBudget?>(null)
        private set
    var exchangeControl by mutableStateOf<RemoteExchangeControl?>(null)
        private set
    var exchangeApprovals by mutableStateOf<List<RemoteExchangeApproval>>(emptyList())
        private set
    var savingBalance by mutableStateOf<BigDecimal?>(null)
        private set
    var retentionPolicy by mutableStateOf<RemoteRetentionPolicy?>(null)
        private set
    var growthPlans by mutableStateOf<List<RemoteGrowthPlan>>(emptyList())
        private set
    var growthMilestones by mutableStateOf<List<RemoteGrowthMilestone>>(emptyList())
        private set
    var growthReport by mutableStateOf<RemoteGrowthReport?>(null)
        private set
    var familyChildren by mutableStateOf<List<RemoteFamilyChild>>(emptyList())
        private set
    var familyMembers by mutableStateOf<List<RemoteFamilyMember>>(emptyList())
        private set
    var pairedDevices by mutableStateOf<List<RemotePairedDevice>>(emptyList())
        private set
    var familyNotifications by mutableStateOf<List<RemoteFamilyNotification>>(emptyList())
        private set
    var latestCollaborationCode by mutableStateOf<RemoteOneTimeCode?>(null)
        private set
    var isFamilyOwner by mutableStateOf(false)
        private set

    val isChildLocked: Boolean get() = mode == AppMode.CHILD &&
        (remoteUsageAccess?.allowed==false || state.usage.usedMinutes >= state.usage.dailyLimitMinutes || sessionUsedMinutes >= state.usage.sessionLimitMinutes)

    init {
        store.load().onSuccess { state = it }.onFailure { message = "本地数据读取失败，已进入空白安全状态" }
        learningOutbox.initializationError?.let { message = "加密学习记录无法读取，请由家长检查应用数据" }
    }

    fun selectSection(value: AppSection) {
        section = if (value in ChildExperiencePolicy.sectionsFor(mode)) value else AppSection.TODAY
    }
    fun enterChild() { mode = AppMode.CHILD; section = AppSection.TODAY; sessionUsedMinutes = 0 }

    fun setParentPin(pin: String, confirm: String): Boolean {
        if (!pin.matches(Regex("^\\d{4,8}$"))) return fail("PIN 需为 4–8 位数字")
        if (pin != confirm) return fail("两次 PIN 不一致")
        val saved = store.savePin(BCrypt.hashpw(pin, BCrypt.gensalt(10)))
        if (!saved) return fail("PIN 保存失败")
        hasParentPin = true
        mode = AppMode.PARENT
        section = AppSection.TODAY
        message = "家长 PIN 已设置，仅保护本机基础版"
        return true
    }

    fun verifyParentPin(pin: String): Boolean {
        val hash = store.pinHash() ?: return fail("请先设置家长 PIN")
        val valid = runCatching { BCrypt.checkpw(pin, hash) }.getOrDefault(false)
        if (!valid) return fail("PIN 不正确")
        mode = AppMode.PARENT
        section = AppSection.TODAY
        message = "已进入家长模式"
        return true
    }

    fun recordUsageMinute() {
        if (mode != AppMode.CHILD || isChildLocked) return
        sessionUsedMinutes += 1
        mutate { LocalFamilyEngine.recordUsageMinute(it) }
        if (remote.hasSession()) {
            pendingUsage += PendingUsage(UUID.randomUUID().toString(), Instant.now())
            flushUsage()
        }
    }

    fun addTask(title: String, minutes: Int, rewardMoney: BigDecimal, coin: Int, xp: Int) =
        mutate { LocalFamilyEngine.addTask(it, title, minutes, rewardMoney, coin, xp) }
    fun submitTask(id: String) {
        if (!remote.hasSession()) return mutate("任务已提交，等待家长审核") { LocalFamilyEngine.submitTask(it, id) }
        viewModelScope.launch { handleRemote(remote.submitTask(id), "任务已提交，等待家长回应") }
    }
    fun approveTask(id: String) {
        val completion = remoteCompletionByTask[id]
        if (!remote.hasSession() || completion == null) return mutate("审核通过，奖励已进入钱包流水") { LocalFamilyEngine.approveTask(it, id) }
        viewModelScope.launch { handleRemote(remote.approveTask(completion), "家长已确认，稳定奖励已进入服务端账本") }
    }
    fun depositGift(amount: BigDecimal) { if(!remote.hasSession()) return mutate("本机演示账本已记录；连接服务后不会自动上传") { LocalFamilyEngine.depositGiftMoney(it, amount) }; productionAction({production.gift(amount)},"压岁钱已进入服务端账本") }
    fun exchange(amount: BigDecimal) { if(!remote.hasSession()) return mutate("本机演示兑换已完成") { LocalFamilyEngine.exchangeMoneyToCoin(it, amount) }; viewModelScope.launch{when(val r=production.exchangePreview(amount,mode==AppMode.CHILD)){is RemoteResult.Ok->{pendingExchangePreview=r.value;message="请核对兑换金额、费用和到账 Coin"};RemoteResult.Unauthorized->handleRemote(RemoteResult.Unauthorized,"");is RemoteResult.Failure->message=r.message}} }
    fun confirmExchange(){val preview=pendingExchangePreview?:return;viewModelScope.launch{if(mode==AppMode.CHILD&&exchangeControl?.childRequiresApproval==true){when(val r=production.requestExchangeApproval(preview.id)){is RemoteResult.Ok->{pendingExchangePreview=null;syncRewardGovernance(true);message="已经请家长看看，等待回应时不会扣除 Money"};RemoteResult.Unauthorized->handleRemote(RemoteResult.Unauthorized,"");is RemoteResult.Failure->message=r.message}}else when(val r=production.confirmExchange(preview.id,mode==AppMode.CHILD)){is RemoteResult.Ok->{pendingExchangePreview=null;refreshProduction("兑换已确认，服务端账本已更新")};RemoteResult.Unauthorized->handleRemote(RemoteResult.Unauthorized,"");is RemoteResult.Failure->message=r.message}}}
    fun cancelExchange(){pendingExchangePreview=null;message="已取消兑换，没有扣款"}
    fun withdraw(amount: BigDecimal) { if(!remote.hasSession()) return mutate("本机演示申请已提交") { LocalFamilyEngine.requestWithdrawal(it, amount) };viewModelScope.launch{when(val r=production.withdrawalQuote(amount)){is RemoteResult.Ok->{pendingWithdrawalQuote=r.value;message="请核对线下兑现费用和预计到账"};RemoteResult.Unauthorized->handleRemote(RemoteResult.Unauthorized,"");is RemoteResult.Failure->message=r.message}} }
    fun confirmWithdrawal(){val quote=pendingWithdrawalQuote?:return;viewModelScope.launch{when(val r=production.requestWithdrawal(quote.id)){is RemoteResult.Ok->{pendingWithdrawalQuote=null;syncProductionState();message="零钱回收申请已提交，尚未扣款"};RemoteResult.Unauthorized->handleRemote(RemoteResult.Unauthorized,"");is RemoteResult.Failure->message=r.message}}}
    fun cancelWithdrawal(){pendingWithdrawalQuote=null;message="已取消申请，没有扣款"}
    fun approveWithdrawal(id: String) { if(!remote.hasSession()) return mutate("本机演示申请已确认") { LocalFamilyEngine.approveWithdrawal(it, id) };productionAction({production.approveWithdrawal(id)},"申请已批准并冻结账本额度；线下支付后还需标记已支付") }
    fun rejectWithdrawal(id:String){productionAction({production.rejectWithdrawal(id)},"申请已拒绝，没有扣款")}
    fun cancelWithdrawalRequest(id:String){productionAction({production.cancelWithdrawalRequest(id)},"申请已取消，没有扣款")}
    fun markWithdrawalPaid(id:String){productionAction({production.markWithdrawalPaid(id)},"已确认线下支付，扣款和费用已进入服务端流水")}
    fun addReward(title: String, price: Int) {if(!remote.hasSession())return mutate { LocalFamilyEngine.addReward(it, title, price) };viewModelScope.launch{when(val r=production.createProduct(title,price)){is RemoteResult.Ok->{syncProductionState();message="奖励商品已保存到家庭服务"};RemoteResult.Unauthorized->handleRemote(RemoteResult.Unauthorized,"");is RemoteResult.Failure->message=r.message}}}
    fun redeemReward(id: String) {if(!remote.hasSession())return mutate("本机演示兑换已记录") { LocalFamilyEngine.redeemReward(it, id) };productionAction({production.orderReward(id)},"奖励申请已提交，等待家长审核后才扣 Coin")}
    fun reviewRewardOrder(id:String,approved:Boolean){productionAction({production.reviewReward(id,approved)},if(approved)"奖励申请已批准，Coin 已进入服务端流水" else "奖励申请已拒绝，没有扣 Coin")}
    fun fulfillRewardOrder(id:String){productionAction({production.fulfillReward(id,"家庭已经完成这份现实奖励约定")},"已经记录为兑现；这一步不会再次扣除 Coin")}
    fun configureRewardGovernance(daily:BigDecimal,weekly:BigDecimal,monthly:BigDecimal,moneyDaily:BigDecimal,moneyMonthly:BigDecimal){viewModelScope.launch{when(val b=production.configureRewardBudget(daily,weekly,monthly,"CONVERT_TO_COIN")){is RemoteResult.Ok->when(val e=production.configureExchangeControl(moneyDaily,moneyMonthly,moneyDaily,moneyMonthly,true)){is RemoteResult.Ok->{syncRewardGovernance(false);message="家庭奖励约定已保存：超额转 Coin，孩子兑换先请家长确认"};RemoteResult.Unauthorized->handleRemote(RemoteResult.Unauthorized,"");is RemoteResult.Failure->message=e.message};RemoteResult.Unauthorized->handleRemote(RemoteResult.Unauthorized,"");is RemoteResult.Failure->message=b.message}}}
    fun reviewExchangeApproval(id:String,approved:Boolean){productionAction({production.reviewExchangeApproval(id,approved)},if(approved)"本次兑换已批准并写入服务端账本" else "本次兑换已婉拒，没有扣款")}
    fun toggleRewardInterest(id: String) {
        val selecting = id !in state.rewardInterestIds
        mutate(if (selecting) "已经记下你想要它，可以和家长说一说" else "已经先不选这个奖励") {
            LocalFamilyEngine.toggleRewardInterest(it, id)
        }
    }
    fun recordLearningSecond(videoId: String) {
        val wasCompleted = state.learningProgress.any { it.videoId == videoId && it.completed }
        mutate { LocalFamilyEngine.recordLearningPlayback(it, videoId, 1) }
        val isCompleted = state.learningProgress.any { it.videoId == videoId && it.completed }
        if (!wasCompleted && isCompleted) message = "这节看完了，等家长确认"
    }
    fun addSaving(title: String, target: BigDecimal) = mutate("储蓄目标名称仅保存在本机；资金变动由服务端账本负责") { LocalFamilyEngine.addSavingGoal(it, title, target) }
    fun saveToGoal(id: String, amount: BigDecimal) {if(!remote.hasSession())return mutate("本机演示已存入目标") { LocalFamilyEngine.saveToGoal(it, id, amount) };productionAction({production.savingDeposit(amount)},"储蓄转入已进入服务端守恒流水")}
    fun addWish(title: String, target: BigDecimal) {if(!remote.hasSession())return mutate { LocalFamilyEngine.addWish(it, title, target) };viewModelScope.launch{when(val r=production.createWish(title,target)){is RemoteResult.Ok->{syncProductionState();message="愿望已保存到家庭服务"};RemoteResult.Unauthorized->handleRemote(RemoteResult.Unauthorized,"");is RemoteResult.Failure->message=r.message}}}
    fun allocateWish(id:String,amount:BigDecimal){if(!remote.hasSession())return failUnit("愿望分配需要连接家庭服务，避免本机余额与账本不一致");productionAction({production.allocateWish(id,amount)},"愿望分配已进入服务端守恒流水")}
    fun buyFund(amount: BigDecimal) {if(!remote.hasSession())return mutate("本机纯模拟份额已购买") { LocalFamilyEngine.buyFund(it, amount) };fundTrade("BUY",amount,"模拟买入预览已生成")}
    fun sellFund() {if(!remote.hasSession())return mutate("本机纯模拟份额已赎回") { LocalFamilyEngine.sellAllFund(it) };val shares=state.fund.shares;if(shares<=BigDecimal.ZERO)return failUnit("当前没有模拟持仓");fundTrade("SELL",shares,"模拟赎回预览已生成")}
    fun updateNav(nav: BigDecimal) {if(!remote.hasSession())return mutate("本机教学 NAV 已更新") { LocalFamilyEngine.updateFundNav(it, nav) };viewModelScope.launch{val fund=ensureFund()?:return@launch;when(val r=production.updateNav(fund.id,nav)){is RemoteResult.Ok->{syncProductionState();message="教学 NAV 已保存到家庭服务"};RemoteResult.Unauthorized->handleRemote(RemoteResult.Unauthorized,"");is RemoteResult.Failure->message=r.message}}}
    fun confirmFundTrade(){val preview=pendingFundTrade?:return;viewModelScope.launch{when(val r=production.confirmTrade(preview.id)){is RemoteResult.Ok->{pendingFundTrade=null;refreshProduction("纯模拟交易已确认并进入服务端账本")};RemoteResult.Unauthorized->handleRemote(RemoteResult.Unauthorized,"");is RemoteResult.Failure->message=r.message}}}
    fun cancelFundTrade(){pendingFundTrade=null;message="已取消模拟交易，没有账本变化"}
    fun updateRetentionPolicy(days:Int){val policy=retentionPolicy?:return failUnit("请先连接并同步家庭服务");viewModelScope.launch{when(val r=production.updateRetentionPolicy(days,policy.version)){is RemoteResult.Ok->{retentionPolicy=r.value;message="使用明细保留期已保存；账本和最小审计不受影响"};RemoteResult.Unauthorized->handleRemote(RemoteResult.Unauthorized,"");is RemoteResult.Failure->message=r.message}}}
    fun runRetentionNow(){viewModelScope.launch{when(val r=production.runRetention()){is RemoteResult.Ok->message="保留策略已执行：删除 ${r.value.usageEventsDeleted} 条过期使用明细，脱敏 ${r.value.allowancesRedacted} 条临时原因";RemoteResult.Unauthorized->handleRemote(RemoteResult.Unauthorized,"");is RemoteResult.Failure->message=r.message}}}
    fun addGrowthPlan(title:String,target:String){viewModelScope.launch{when(val r=production.createGrowthPlan(title,"HABITS",target)){is RemoteResult.Ok->{syncGrowthArchive();message="成长计划已保存"};RemoteResult.Unauthorized->handleRemote(RemoteResult.Unauthorized,"");is RemoteResult.Failure->message=r.message}}}
    fun completeGrowthPlan(plan:RemoteGrowthPlan){viewModelScope.launch{when(val r=production.transitionGrowthPlan(plan,"COMPLETED")){is RemoteResult.Ok->{syncGrowthArchive();message="成长计划已完成，历史记录仍会保留"};RemoteResult.Unauthorized->handleRemote(RemoteResult.Unauthorized,"");is RemoteResult.Failure->message=r.message}}}
    fun addGrowthMilestone(title:String,observation:String,category:String="OTHER"){viewModelScope.launch{when(val r=production.createGrowthMilestone(growthPlans.firstOrNull{it.status=="ACTIVE"}?.id,title,observation,category)){is RemoteResult.Ok->{syncGrowthArchive();message="今天的真实变化已记录"};RemoteResult.Unauthorized->handleRemote(RemoteResult.Unauthorized,"");is RemoteResult.Failure->message=r.message}}}
    fun uploadGrowthPhoto(milestoneId:String,bytes:ByteArray,mime:String,altText:String){viewModelScope.launch{when(val r=production.uploadGrowthArtifact(milestoneId,bytes,mime,altText)){is RemoteResult.Ok->{syncGrowthArchive();message="照片已安全加入成长档案"};RemoteResult.Unauthorized->handleRemote(RemoteResult.Unauthorized,"");is RemoteResult.Failure->message=r.message}}}
    fun updateUsage(daily: Int, session: Int) {
        if(!remote.hasSession())return mutate("本机防沉迷规则已更新；连接服务后可统一设置休息时段") { LocalFamilyEngine.updateUsagePolicy(it, daily, session) }
        viewModelScope.launch { when(val result=remote.configureUsage(daily,session,"21:30:00","06:30:00")) {
            is RemoteResult.Ok->{mutate("防沉迷规则已同步：休息时段 21:30–06:30"){LocalFamilyEngine.updateUsagePolicy(it,daily,session)};syncUsageAccess()}
            RemoteResult.Unauthorized->handleRemote(RemoteResult.Unauthorized,"")
            is RemoteResult.Failure->message=result.message
        } }
    }
    fun grantTemporaryUsage(minutes:Int,reason:String){if(reason.isBlank())return failUnit("请写明临时放行原因");viewModelScope.launch{when(val result=remote.createUsageAllowance(minutes,reason.trim())){is RemoteResult.Ok->{syncUsageAccess();message="已临时允许 $minutes 分钟，到时自动结束"};RemoteResult.Unauthorized->handleRemote(RemoteResult.Unauthorized,"");is RemoteResult.Failure->message=result.message}}}
    fun exportChildData(){viewModelScope.launch{when(val result=remote.exportChildData()){is RemoteResult.Ok->{val dir=getApplication<Application>().getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS)?:getApplication<Application>().filesDir;val file=java.io.File(dir,"family-growth-child-export-${System.currentTimeMillis()}.json");runCatching{file.writeText(result.value.json,Charsets.UTF_8)}.onSuccess{lastExportPath=file.absolutePath;message="儿童数据已导出到应用文档目录"}.onFailure{message="导出已生成，但设备文件保存失败"}};RemoteResult.Unauthorized->handleRemote(RemoteResult.Unauthorized,"");is RemoteResult.Failure->message=result.message}}}
    fun previewChildErasure(){viewModelScope.launch{when(val result=remote.erasurePreview()){is RemoteResult.Ok->{erasurePreview=result.value;message="删除预览已生成，十分钟内需 PIN 再确认"};RemoteResult.Unauthorized->handleRemote(RemoteResult.Unauthorized,"");is RemoteResult.Failure->message=result.message}}}
    fun confirmChildErasure(pin:String){val preview=erasurePreview?:return failUnit("请先生成删除预览");if(pin.length!=6)return failUnit("请输入 6 位服务端 PIN");viewModelScope.launch{when(val result=remote.confirmErasure(preview,pin)){is RemoteResult.Ok->{remote.disconnect();erasurePreview=null;remoteUsageAccess=null;lastExportPath=null;state=FamilyLocalState();store.save(state);connectionState=ConnectionState.Disconnected;mode=AppMode.PARENT;message="儿童直接标识和自由文本已删除；账本与最小审计已去标识化保留"};RemoteResult.Unauthorized->handleRemote(RemoteResult.Unauthorized,"");is RemoteResult.Failure->message=result.message}}}
    fun updateLearningReward(money: BigDecimal, coin: Int, xp: Int) {
        if (!remote.hasSession()) return mutate("本机自主学习奖励已保存；连接服务后需再次确认服务端规则") {
            LocalFamilyEngine.updateLearningRewardPolicy(it, money, coin, xp)
        }
        viewModelScope.launch {
            when (val result = remote.updateLearningReward(money.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString(), coin.toLong(), xp.toLong())) {
                is RemoteResult.Ok -> mutate("自主学习奖励已保存到家庭服务；只影响新加入课程") {
                    LocalFamilyEngine.updateLearningRewardPolicy(it, money, coin, xp)
                }
                RemoteResult.Unauthorized -> handleRemote(RemoteResult.Unauthorized, "")
                is RemoteResult.Failure -> message = result.message
            }
        }
    }
    fun updateExperience(birthDate: String, stageOverride: SchoolStage?, primaryBandOverride: PrimaryGradeBand?, overrideReason: String, hapticsEnabled: Boolean) {
        val parsed = runCatching { java.time.LocalDate.parse(birthDate) }.getOrElse { return failUnit("请填写 YYYY-MM-DD 格式的出生日期") }
        if (remote.hasSession()) {
            val intended=stageOverride?:ChildExperiencePolicy.recommendedStage(parsed)
            if(stageTransitionPreview?.newStage!=intended.name)return failUnit("配置已变化，请重新查看学段迁移预览")
            viewModelScope.launch {
                handleRemote(remote.updateExperience(parsed, stageOverride, primaryBandOverride, overrideReason, hapticsEnabled,
                    state.experience.version), "学习阶段已保存到家庭服务")
                stageTransitionPreview=null
            }
        } else {
            mutate("本机学习阶段已更新；连接服务后以服务端配置为准") {
                it.copy(experience = ChildExperiencePolicy.localSettings(parsed, stageOverride, primaryBandOverride, overrideReason,
                    hapticsEnabled, version = it.experience.version + 1))
            }
        }
    }
    fun previewExperienceTransition(birthDate:String,stageOverride:SchoolStage?){runCatching{LocalDate.parse(birthDate)}.getOrElse{return failUnit("请填写 YYYY-MM-DD 格式的出生日期")};viewModelScope.launch{when(val result=remote.stageTransitionPreview(birthDate,stageOverride?.name)){is RemoteResult.Ok->{stageTransitionPreview=result.value;message="迁移预览已生成，请核对后再保存"};RemoteResult.Unauthorized->handleRemote(RemoteResult.Unauthorized,"");is RemoteResult.Failure->message=result.message}}}
    fun addEducationSource(title: String, url: String, stages: List<SchoolStage>, usageNote: String) {
        if (title.isBlank() || url.isBlank() || stages.isEmpty() || usageNote.isBlank()) return failUnit("请完整填写来源、网址、学段和免费使用说明")
        viewModelScope.launch {
            when (val result = remote.createEducationSource(title.trim(), url.trim(), stages, usageNote.trim())) {
                is RemoteResult.Ok -> { educationSources = result.value; message = "来源已保存，请读取栏目后再批准" }
                RemoteResult.Unauthorized -> handleRemote(RemoteResult.Unauthorized, "")
                is RemoteResult.Failure -> message = result.message
            }
        }
    }
    fun refreshEducationSource(id: String) = changeEducationSource(id, "refresh", "栏目读取完成；请确认后批准")
    fun approveEducationSource(id: String) = changeEducationSource(id, "approve", "来源已批准，孩子目录会动态更新")
    fun withdrawEducationSource(id: String) = changeEducationSource(id, "withdraw", "来源已撤回，不再显示给孩子")
    fun requestParentForResource() { message = "这个栏目需要家长一起打开" }
    fun attemptLearningActivity(assignmentId: String, activityId: String, response: String) {
        enqueueLearning(LearningActionType.ATTEMPT, assignmentId, activityId=activityId, response=response)
    }
    fun requestLearningHelp(assignmentId: String, activityId: String, message: String) {
        enqueueLearning(LearningActionType.HELP, assignmentId, activityId=activityId, note=message)
    }
    fun completeLearningVideo(assignmentId: String, activityId: String, playedSeconds: Int, durationSeconds: Int) {
        enqueueLearning(LearningActionType.ATTEMPT, assignmentId, activityId=activityId, response="VIEWED",
            playedSeconds=playedSeconds, durationSeconds=durationSeconds)
    }
    fun submitLearningAssignment(assignmentId: String, version: Long) {
        enqueueLearning(LearningActionType.SUBMIT, assignmentId, expectedVersion=version)
    }
    fun reviewLearningAssignment(assignmentId: String, approve: Boolean, note: String, version: Long) {
        enqueueLearning(LearningActionType.REVIEW, assignmentId, expectedVersion=version,
            decision=if (approve) "APPROVE" else "REWORK", note=note)
    }
    fun scheduleLearningRevisit(assignmentId:String, sourceEventId:String) {
        viewModelScope.launch {
            val revisitAt=Instant.now().plus(java.time.Duration.ofDays(2)).toString()
            when(val result=remote.scheduleLearningRevisit(assignmentId,sourceEventId,"OTHER","先一起重读要求，再用实物或例子试一次。",revisitAt)) {
                is RemoteResult.Ok -> { learningSupportByAssignment=learningSupportByAssignment+(assignmentId to result.value); message="已安排两天后温和再练" }
                RemoteResult.Unauthorized -> handleRemote(RemoteResult.Unauthorized,"")
                is RemoteResult.Failure -> message=result.message
            }
        }
    }
    fun moveJuniorLearning(assignmentId:String,direction:String) {
        val plan=juniorLearningPlan ?: return failUnit("学习计划还没有同步完成")
        viewModelScope.launch {
            when(val result=remote.moveJuniorLearning(assignmentId,direction,plan.revision)) {
                is RemoteResult.Ok -> { applyJuniorPlan(result.value); message="计划顺序已保存" }
                RemoteResult.Unauthorized -> handleRemote(RemoteResult.Unauthorized,"")
                is RemoteResult.Failure -> {
                    message=result.message
                    if(result.kind==RemoteFailureKind.CONFLICT) syncJuniorPlan()
                }
            }
        }
    }
    fun retryLearningSync() { viewModelScope.launch { reconcileAndFlushLearning() } }
    fun discardLearningAction(key: String) {
        learningOutbox.remove(key).onSuccess { updateLearningOutbox(); message = "这条本地操作已由家长移除" }
            .onFailure { message = it.message ?: "本地学习记录无法更新" }
    }
    fun createTeachingCourse(courseTitle:String, lessonTitle:String, lessonSummary:String, activityType:String,
                             activityTitle:String, instruction:String, contentRef:String?) {
        val stage = state.experience.effectiveStage
        if (stage == SchoolStage.PARENT_ONLY) return failUnit("0–2 岁仅由家长记录成长，不创建儿童课程")
        if (teachingActionRunningInternal) return
        if (courseTitle.isBlank() || lessonTitle.isBlank() || lessonSummary.isBlank() || activityTitle.isBlank() || instruction.isBlank())
            return failUnit("请完整填写课程、课节和活动")
        updateTeachingActionState(true)
        viewModelScope.launch {
            try {
                val draft = RemoteTeachingCourseDraft(stage=stage.name, courseTitle=courseTitle.trim(),
                    lessonTitle=lessonTitle.trim(), lessonSummary=lessonSummary.trim(), activityType=activityType,
                    activityTitle=activityTitle.trim(), instruction=instruction.trim(), contentRef=contentRef,
                    expectedMinutes=if(activityType=="SHORT_VIDEO") 3 else 8,
                    rightsBasis=if(activityType=="SHORT_VIDEO") "应用内原创审核视频" else "家庭原创学习活动",
                    chapterTitle=if(stage==SchoolStage.JUNIOR_MIDDLE) lessonTitle.trim() else null,
                    knowledgePoints=if(stage==SchoolStage.JUNIOR_MIDDLE) listOf(activityTitle.trim()) else emptyList(),
                    learningGoal=if(stage==SchoolStage.JUNIOR_MIDDLE) lessonSummary.trim() else null,
                    safetyNote=if(stage==SchoolStage.JUNIOR_MIDDLE) "只使用家庭常见安全材料；不确定时先请家长确认。" else null)
                when (val result = remote.createTeachingCourse(draft)) {
                    is RemoteResult.Ok -> { message = "课程草稿已保存"; syncTeachingCourses() }
                    RemoteResult.Unauthorized -> handleRemote(RemoteResult.Unauthorized, "")
                    is RemoteResult.Failure -> message = result.message
                }
            } finally {
                updateTeachingActionState(false)
            }
        }
    }
    fun createKindergartenTeachingCourse(template: KindergartenCourseTemplate) {
        if (state.experience.effectiveStage != SchoolStage.KINDERGARTEN)
            return failUnit("当前学段不是幼儿园，请刷新家长配置")
        if (teachingActionRunningInternal) return
        updateTeachingActionState(true)
        viewModelScope.launch {
            try {
                val draft = RemoteTeachingCourseDraft(stage=SchoolStage.KINDERGARTEN.name,
                    courseTitle=template.courseTitle, lessonTitle=template.lessonTitle,
                    lessonSummary=template.lessonSummary, activityType=template.activityType,
                    activityTitle=template.activityTitle, instruction=template.childAction,
                    expectedMinutes=template.expectedMinutes, rightsBasis=template.rightsBasis,
                    kindergartenAgeBand=template.ageBand.apiValue,
                    kindergartenDomains=listOf(template.domain.apiValue))
                when (val result = remote.createTeachingCourse(draft)) {
                    is RemoteResult.Ok -> { message = "亲子活动草稿已保存"; syncTeachingCourses() }
                    RemoteResult.Unauthorized -> handleRemote(RemoteResult.Unauthorized, "")
                    is RemoteResult.Failure -> message = result.message
                }
            } finally {
                updateTeachingActionState(false)
            }
        }
    }
    fun createPrimaryTeachingCourse(template: PrimaryCourseTemplate) {
        if (state.experience.effectiveStage != SchoolStage.PRIMARY) return failUnit("当前学段不是小学，请刷新家长配置")
        if (teachingActionRunningInternal) return
        updateTeachingActionState(true)
        viewModelScope.launch {
            try {
                val draft = RemoteTeachingCourseDraft(stage=SchoolStage.PRIMARY.name, subject=template.subject.apiValue,
                    courseTitle=template.courseTitle, lessonTitle=template.lessonTitle,
                    lessonSummary="${template.goal}；${template.realityExit}", activityType=template.activityType,
                    activityTitle=template.activityTitle, instruction=template.instruction,
                    expectedMinutes=template.expectedMinutes, rightsBasis=template.rightsBasis)
                when (val result = remote.createTeachingCourse(draft)) {
                    is RemoteResult.Ok -> { message="小学原创活动草稿已保存"; syncTeachingCourses() }
                    RemoteResult.Unauthorized -> handleRemote(RemoteResult.Unauthorized, "")
                    is RemoteResult.Failure -> message=result.message
                }
            } finally { updateTeachingActionState(false) }
        }
    }
    fun createJuniorTeachingCourse(template:JuniorCourseTemplate) {
        if(state.experience.effectiveStage!=SchoolStage.JUNIOR_MIDDLE) return failUnit("当前学段不是初中，请刷新家长配置")
        if(teachingActionRunningInternal)return
        updateTeachingActionState(true)
        viewModelScope.launch {
            try {
                val draft=RemoteTeachingCourseDraft(stage=SchoolStage.JUNIOR_MIDDLE.name,subject=template.subject.apiValue,
                    courseTitle=template.courseTitle,lessonTitle=template.lessonTitle,lessonSummary=template.learningGoal,
                    activityType="OFFLINE_PRACTICE",activityTitle=template.activityTitle,instruction=template.instruction,
                    expectedMinutes=template.expectedMinutes,rightsBasis=template.rightsBasis,
                    chapterTitle=template.chapterTitle,knowledgePoints=template.knowledgePoints,
                    learningGoal=template.learningGoal,safetyNote=template.safetyNote)
                when(val result=remote.createTeachingCourse(draft)) {
                    is RemoteResult.Ok->{message="初中原创活动草稿已保存";syncTeachingCourses()}
                    RemoteResult.Unauthorized->handleRemote(RemoteResult.Unauthorized,"")
                    is RemoteResult.Failure->message=result.message
                }
            } finally { updateTeachingActionState(false) }
        }
    }
    fun createSeniorTeachingCourse(template:SeniorCourseTemplate) {
        if(state.experience.effectiveStage!=SchoolStage.SENIOR_HIGH) return failUnit("当前学段不是高中，请刷新家长配置")
        if(teachingActionRunningInternal)return
        updateTeachingActionState(true)
        viewModelScope.launch {
            try {
                val draft=RemoteTeachingCourseDraft(stage=SchoolStage.SENIOR_HIGH.name,subject=template.subject.apiValue,
                    courseTitle=template.courseTitle,lessonTitle=template.lessonTitle,lessonSummary=template.inquiryQuestion,
                    activityType="OFFLINE_PRACTICE",activityTitle="形成研究证据",instruction=template.instruction,
                    expectedMinutes=template.expectedMinutes,rightsBasis=template.rightsBasis,
                    seniorModuleType=template.subject.defaultModule,topicTitle=template.topicTitle,
                    inquiryQuestion=template.inquiryQuestion,expectedEvidence=template.expectedEvidence,
                    seniorSafetyNote=template.safetyNote)
                when(val result=remote.createTeachingCourse(draft)) {
                    is RemoteResult.Ok->{message="高中原创研究活动草稿已保存";syncTeachingCourses()}
                    RemoteResult.Unauthorized->handleRemote(RemoteResult.Unauthorized,"")
                    is RemoteResult.Failure->message=result.message
                }
            } finally { updateTeachingActionState(false) }
        }
    }
    fun updateSeniorModules(selections:List<RemoteSeniorModule>) {
        val current=seniorModuleConfiguration ?: return failUnit("模块配置尚未同步")
        viewModelScope.launch { when(val result=remote.updateSeniorModules(selections,current.revision)) {
            is RemoteResult.Ok->{seniorModuleConfiguration=result.value;message="高中课程模块已保存"}
            RemoteResult.Unauthorized->handleRemote(RemoteResult.Unauthorized,"")
            is RemoteResult.Failure->{message=result.message;if(result.kind==RemoteFailureKind.CONFLICT)syncSeniorLearning()}
        } }
    }
    fun createSeniorGoal(module:RemoteSeniorModule,assignmentId:String?,title:String,evidence:String,next:String) {
        if(listOf(title,evidence,next).any{it.isBlank()})return failUnit("请写清目标、证据和下一步")
        viewModelScope.launch { when(val result=remote.createSeniorGoal(module,assignmentId,LocalDate.now().with(java.time.DayOfWeek.MONDAY).toString(),title.trim(),evidence.trim(),next.trim())) {
            is RemoteResult.Ok->{syncSeniorLearning();message="本周目标已保存"}
            RemoteResult.Unauthorized->handleRemote(RemoteResult.Unauthorized,"")
            is RemoteResult.Failure->message=result.message
        } }
    }
    fun archiveSeniorGoal(goal:RemoteSeniorGoal) { viewModelScope.launch { when(val result=remote.archiveSeniorGoal(goal)) {
        is RemoteResult.Ok->{syncSeniorLearning();message="目标已归档，学习事实仍保留"}
        RemoteResult.Unauthorized->handleRemote(RemoteResult.Unauthorized,"")
        is RemoteResult.Failure->{message=result.message;if(result.kind==RemoteFailureKind.CONFLICT)syncSeniorLearning()}
    } } }
    fun createSeniorReflection(goal:RemoteSeniorGoal?,assignmentId:String?,evidence:String,strategy:String,next:String,support:Boolean) {
        if(evidence.isBlank()||next.isBlank())return failUnit("请写下证据和下一步")
        viewModelScope.launch { when(val result=remote.createSeniorReflection(goal?.id,assignmentId,evidence.trim(),strategy,next.trim(),support)) {
            is RemoteResult.Ok->{syncSeniorLearning();message=if(support)"复盘已保存，也已记下需要支持" else "复盘已保存"}
            RemoteResult.Unauthorized->handleRemote(RemoteResult.Unauthorized,"")
            is RemoteResult.Failure->message=result.message
        } }
    }
    fun publishTeachingCourse(versionId:String) = runTeachingAction {
        when (val result=remote.publishTeachingVersion(versionId)) {
            is RemoteResult.Ok -> { message="课程已发布，可以布置给孩子"; syncTeachingCourses() }
            RemoteResult.Unauthorized -> handleRemote(RemoteResult.Unauthorized, "")
            is RemoteResult.Failure -> message=result.message
        }
    }
    fun withdrawTeachingCourse(versionId:String) = runTeachingAction {
        when(val result=remote.withdrawTeachingVersion(versionId)) {
            is RemoteResult.Ok -> { message="课程已撤回；历史学习记录仍保留"; syncTeachingCourses() }
            RemoteResult.Unauthorized -> handleRemote(RemoteResult.Unauthorized,"")
            is RemoteResult.Failure -> message=result.message
        }
    }
    fun assignTeachingCourse(versionId:String) = runTeachingAction {
        when (val result=remote.assignTeachingVersion(versionId)) {
            is RemoteResult.Ok -> { replaceLearning(result.value); message="课节已布置给孩子"; syncTeachingCourses() }
            RemoteResult.Unauthorized -> handleRemote(RemoteResult.Unauthorized, "")
            is RemoteResult.Failure -> message=result.message
        }
    }
    fun clearMessage() { message = null }

    fun connectService(baseUrl: String, familyId: String, parentId: String, childId: String, pin: String) {
        if (connectionState == ConnectionState.Connecting) return
        connectionState = ConnectionState.Connecting
        viewModelScope.launch {
            val result = remote.connect(baseUrl, familyId, parentId, childId, pin)
            handleRemote(result, "已连接并同步家庭服务")
            if (result is RemoteResult.Ok) { syncEducationResources(); syncProductionState(); reconcileAndFlushLearning(refreshFirst=false) }
        }
    }
    fun pairParentDevice(baseUrl:String,code:String,deviceName:String){
        if(connectionState==ConnectionState.Connecting)return
        connectionState=ConnectionState.Connecting
        viewModelScope.launch{val result=remote.pairParent(baseUrl,code,deviceName);handleRemote(result,"设备已配对，并已选择第一个孩子");if(result is RemoteResult.Ok){syncEducationResources();syncProductionState();reconcileAndFlushLearning(refreshFirst=false)}}
    }
    fun switchFamilyChild(childId:String){
        if(connectionState !is ConnectionState.Connected)return
        connectionState=ConnectionState.Connecting
        viewModelScope.launch{val result=remote.switchChild(childId);handleRemote(result,"已切换孩子，历史记录保持不变");if(result is RemoteResult.Ok){syncEducationResources();syncProductionState();reconcileAndFlushLearning(refreshFirst=false)}}
    }
    fun createParentPairing(){viewModelScope.launch{when(val r=production.createParentPairing()){is RemoteResult.Ok->{latestCollaborationCode=r.value;message="家长设备配对码仅显示一次，5 分钟内有效"};RemoteResult.Unauthorized->handleRemote(RemoteResult.Unauthorized,"");is RemoteResult.Failure->message=r.message}}}
    fun createChildPairing(childId:String){viewModelScope.launch{when(val r=production.createChildPairing(childId)){is RemoteResult.Ok->{latestCollaborationCode=r.value;message="孩子设备配对码仅显示一次，5 分钟内有效"};RemoteResult.Unauthorized->handleRemote(RemoteResult.Unauthorized,"");is RemoteResult.Failure->message=r.message}}}
    fun inviteGuardian(name:String){if(name.isBlank())return failUnit("请输入家长称呼");viewModelScope.launch{when(val r=production.inviteGuardian(name.trim())){is RemoteResult.Ok->{latestCollaborationCode=r.value;message="邀请代码仅显示一次，10 分钟内有效"};RemoteResult.Unauthorized->handleRemote(RemoteResult.Unauthorized,"");is RemoteResult.Failure->message=r.message}}}
    fun revokePairedDevice(id:String){viewModelScope.launch{when(val r=production.revokeDevice(id)){is RemoteResult.Ok->{syncCollaboration();message="设备已撤销，对应会话已失效"};RemoteResult.Unauthorized->handleRemote(RemoteResult.Unauthorized,"");is RemoteResult.Failure->message=r.message}}}
    fun resetFamilyMemberPin(id:String,pin:String){if(pin.length!=6)return failUnit("请输入 6 位新 PIN");viewModelScope.launch{when(val r=production.resetMemberPin(id,pin)){is RemoteResult.Ok->{syncCollaboration();message="成员 PIN 已重置，旧会话已失效"};RemoteResult.Unauthorized->handleRemote(RemoteResult.Unauthorized,"");is RemoteResult.Failure->message=r.message}}}
    fun revokeFamilyMember(id:String){viewModelScope.launch{when(val r=production.revokeMember(id)){is RemoteResult.Ok->{syncCollaboration();message="家庭成员已撤销，旧会话已失效"};RemoteResult.Unauthorized->handleRemote(RemoteResult.Unauthorized,"");is RemoteResult.Failure->message=r.message}}}
    fun readFamilyNotification(id:String){viewModelScope.launch{when(val r=production.readNotification(id)){is RemoteResult.Ok->{syncCollaboration()};RemoteResult.Unauthorized->handleRemote(RemoteResult.Unauthorized,"");is RemoteResult.Failure->message=r.message}}}
    fun clearCollaborationCode(){latestCollaborationCode=null}
    fun refreshService() {
        if (connectionState !is ConnectionState.Connected) connectionState = ConnectionState.Connecting
        viewModelScope.launch {
            val result = remote.refresh()
            handleRemote(result, "已同步最新数据")
            if (result is RemoteResult.Ok) { syncEducationResources(); syncProductionState(); reconcileAndFlushLearning(refreshFirst=false); flushUsageNow() }
        }
    }
    fun disconnectService() { remote.disconnect(); remoteCompletionByTask = emptyMap(); educationSources = emptyList(); childEducationCatalog = emptyList(); learningAssignments = emptyList(); juniorLearningPlan=null; juniorLearningReport=null; seniorModuleConfiguration=null; seniorGoals=emptyList(); seniorReflections=emptyList(); seniorLearningReport=null;remoteUsageAccess=null;erasurePreview=null;stageTransitionPreview=null; learningSupportByAssignment=emptyMap(); primaryLearningReport=null; teachingCourses = emptyList();growthPlans=emptyList();growthMilestones=emptyList();growthReport=null;familyChildren=emptyList();familyMembers=emptyList();pairedDevices=emptyList();familyNotifications=emptyList();latestCollaborationCode=null;isFamilyOwner=false; connectionState = ConnectionState.Disconnected; message = if(pendingLearningActions.isEmpty()) "已断开；服务端 Token 已从内存清除" else "已断开；Token 已清除，待同步学习记录仍加密保留" }

    private fun changeEducationSource(id: String, action: String, success: String) {
        viewModelScope.launch {
            when (val result = remote.educationSourceAction(id, action)) {
                is RemoteResult.Ok -> {
                    educationSources = result.value
                    syncEducationResources()
                    val changed = result.value.firstOrNull { it.id == id }
                    message = if (action == "refresh" && changed?.refreshStatus == "FAILED")
                        "栏目读取失败，已保留上一次成功结果：${changed.refreshError}" else success
                }
                RemoteResult.Unauthorized -> handleRemote(RemoteResult.Unauthorized, "")
                is RemoteResult.Failure -> message = result.message
            }
        }
    }

    private fun enqueueLearning(type:LearningActionType, assignmentId:String, activityId:String="", response:String="",
                                playedSeconds:Int?=null, durationSeconds:Int?=null, expectedVersion:Long?=null,
                                decision:String="", note:String="") {
        val scope = remote.learningScope() ?: return failUnit("请家长先连接家庭服务")
        val action = PendingLearningAction(familyId=scope.first, childId=scope.second, type=type,
            assignmentId=assignmentId, activityId=activityId, responseText=response,
            playedSeconds=playedSeconds, durationSeconds=durationSeconds, expectedVersion=expectedVersion,
            decision=decision, note=note)
        learningOutbox.enqueue(action).onSuccess {
            updateLearningOutbox()
            message = "这一步已安全保存，正在同步"
            flushLearningOutbox()
        }.onFailure { message = it.message ?: "学习记录无法安全保存" }
    }

    private fun updateLearningOutbox() { pendingLearningActions = learningOutbox.snapshot() }

    private fun flushLearningOutbox() {
        if (learningFlushRunning) return
        viewModelScope.launch { flushLearningOutboxNow() }
    }

    private suspend fun flushLearningOutboxNow() {
        if (learningFlushRunning) return
        learningFlushRunning = true
        try {
            while (remote.hasSession()) {
                val scope = remote.learningScope() ?: return
                val action = learningOutbox.snapshot().firstOrNull {
                    it.state == LearningActionState.PENDING && it.familyId == scope.first && it.childId == scope.second
                } ?: return
                when (val result = remote.executeLearning(action)) {
                    is RemoteResult.Ok -> {
                        val removed = learningOutbox.remove(action.idempotencyKey)
                        if (removed.isFailure) {
                            message = removed.exceptionOrNull()?.message ?: "同步成功，但本地队列无法安全更新"
                            updateLearningOutbox()
                            return
                        }
                        updateLearningOutbox()
                        replaceLearning(result.value)
                        message = when {
                            action.type == LearningActionType.REVIEW && action.decision == "APPROVE" -> "已确认孩子的努力"
                            action.type == LearningActionType.REVIEW -> "已温和地告诉孩子再试哪一步"
                            action.type == LearningActionType.HELP -> "已经告诉家长：这里需要一起看看"
                            action.type == LearningActionType.SUBMIT -> "已经交给家长看了"
                            result.value.activities.firstOrNull { it.id == action.activityId }?.checkedCorrect == false -> "再看一看，可以慢慢试"
                            action.responseText == "VIEWED" -> "已记录观看；这不代表全部学会"
                            else -> "这一步已经同步"
                        }
                    }
                    RemoteResult.Unauthorized -> {
                        handleRemote(RemoteResult.Unauthorized, "")
                        updateLearningOutbox()
                        return
                    }
                    is RemoteResult.Failure -> {
                        val needsReview = result.kind != RemoteFailureKind.RETRYABLE
                        val marked = learningOutbox.markFailure(action.idempotencyKey, result.message, needsReview)
                        if (marked.isFailure) {
                            message = marked.exceptionOrNull()?.message ?: "本地学习记录无法安全更新"
                            updateLearningOutbox()
                            return
                        }
                        updateLearningOutbox()
                        if (result.kind == RemoteFailureKind.CONFLICT && syncLearningAssignments()) {
                            val reconciliation = learningOutbox.reconcile(learningAssignments)
                            if (reconciliation.isFailure) {
                                message = reconciliation.exceptionOrNull()?.message ?: "本地学习记录无法安全合并"
                                updateLearningOutbox()
                                return
                            }
                            updateLearningOutbox()
                            val reconciled = learningOutbox.snapshot().firstOrNull { it.idempotencyKey == action.idempotencyKey }
                            if (reconciled == null || reconciled.state == LearningActionState.PENDING) continue
                        }
                        message = if (needsReview) "有一条学习记录需要家长刷新处理" else "网络暂不可用，这一步已加密保留"
                        return
                    }
                }
            }
        } finally { learningFlushRunning = false }
    }

    private suspend fun reconcileAndFlushLearning(refreshFirst:Boolean=true) {
        if (refreshFirst && !syncLearningAssignments()) return
        val reconciliation = learningOutbox.reconcile(learningAssignments)
        if (reconciliation.isFailure) {
            message = reconciliation.exceptionOrNull()?.message ?: "本地学习记录无法安全合并"
            updateLearningOutbox()
            return
        }
        updateLearningOutbox()
        flushLearningOutboxNow()
    }

    private suspend fun syncEducationResources() {
        when (val sources = remote.educationSources()) {
            is RemoteResult.Ok -> educationSources = sources.value
            RemoteResult.Unauthorized -> { handleRemote(RemoteResult.Unauthorized, ""); return }
            is RemoteResult.Failure -> message = sources.message
        }
        when (val catalog = remote.childEducationCatalog()) {
            is RemoteResult.Ok -> childEducationCatalog = catalog.value
            RemoteResult.Unauthorized -> handleRemote(RemoteResult.Unauthorized, "")
            is RemoteResult.Failure -> message = catalog.message
        }
        syncLearningAssignments()
        syncUsageAccess()
        if(state.experience.effectiveStage==SchoolStage.JUNIOR_MIDDLE) {
            syncJuniorPlan()
            when(val report=remote.juniorLearningReport()) {
                is RemoteResult.Ok->juniorLearningReport=report.value
                RemoteResult.Unauthorized->handleRemote(RemoteResult.Unauthorized,"")
                is RemoteResult.Failure->Unit
            }
        } else { juniorLearningPlan=null; juniorLearningReport=null }
        if(state.experience.effectiveStage==SchoolStage.PRIMARY) when(val report=remote.primaryLearningReport()) {
            is RemoteResult.Ok -> primaryLearningReport=report.value
            RemoteResult.Unauthorized -> handleRemote(RemoteResult.Unauthorized,"")
            is RemoteResult.Failure -> Unit
        } else primaryLearningReport=null
        if(state.experience.effectiveStage==SchoolStage.SENIOR_HIGH) syncSeniorLearning()
        else { seniorModuleConfiguration=null;seniorGoals=emptyList();seniorReflections=emptyList();seniorLearningReport=null }
        syncTeachingCourses()
    }

    private suspend fun syncUsageAccess(){when(val result=remote.usageAccess()){is RemoteResult.Ok->remoteUsageAccess=result.value;RemoteResult.Unauthorized->handleRemote(RemoteResult.Unauthorized,"");is RemoteResult.Failure->Unit}}

    private fun productionAction(call:suspend()->RemoteResult<*>,success:String){viewModelScope.launch{when(val result=call()){is RemoteResult.Ok->refreshProduction(success);RemoteResult.Unauthorized->handleRemote(RemoteResult.Unauthorized,"");is RemoteResult.Failure->message=result.message}}}
    private suspend fun refreshProduction(success:String){when(val result=remote.refresh()){is RemoteResult.Ok->{handleRemote(result,success);syncProductionState()};RemoteResult.Unauthorized->handleRemote(RemoteResult.Unauthorized,"");is RemoteResult.Failure->message=result.message}}
    private suspend fun syncProductionState(){
        when(val r=production.products()){is RemoteResult.Ok->state=state.copy(rewards=r.value.map{LocalRewardItem(it.id,it.title,it.coinCost)});RemoteResult.Unauthorized->{handleRemote(RemoteResult.Unauthorized,"");return};is RemoteResult.Failure->Unit}
        when(val r=production.wishes()){is RemoteResult.Ok->state=state.copy(wishes=r.value.map{LocalWish(it.id,it.title,it.target)});RemoteResult.Unauthorized->{handleRemote(RemoteResult.Unauthorized,"");return};is RemoteResult.Failure->Unit}
        when(val r=production.withdrawals()){is RemoteResult.Ok->state=state.copy(withdrawals=r.value.map{LocalWithdrawalRequest(it.id,it.gross,it.fee,it.net,when(it.status){"APPROVED"->WithdrawalStatus.APPROVED;"PAID"->WithdrawalStatus.PAID;"REJECTED"->WithdrawalStatus.REJECTED;"CANCELLED"->WithdrawalStatus.CANCELLED;else->WithdrawalStatus.PENDING})});RemoteResult.Unauthorized->{handleRemote(RemoteResult.Unauthorized,"");return};is RemoteResult.Failure->Unit}
        when(val r=production.rewardOrders()){is RemoteResult.Ok->rewardOrders=r.value;RemoteResult.Unauthorized->{handleRemote(RemoteResult.Unauthorized,"");return};is RemoteResult.Failure->Unit}
        syncRewardGovernance(mode==AppMode.CHILD)
        when(val r=production.savingBalance()){is RemoteResult.Ok->savingBalance=r.value;RemoteResult.Unauthorized->{handleRemote(RemoteResult.Unauthorized,"");return};is RemoteResult.Failure->Unit}
        when(val funds=production.funds()){is RemoteResult.Ok->{funds.value.firstOrNull()?.let{f->when(val p=production.fundPosition(f.id)){is RemoteResult.Ok->state=state.copy(fund=LocalFundPosition(p.value.nav,p.value.shares));else->Unit}}};else->Unit}
        when(val r=production.todayReport()){is RemoteResult.Ok->todayUsageReport=r.value;else->Unit}
        when(val r=production.monthlyReport()){is RemoteResult.Ok->monthlyUsageReport=r.value;else->Unit}
        when(val r=production.retentionPolicy()){is RemoteResult.Ok->retentionPolicy=r.value;else->Unit}
        syncGrowthArchive()
        syncCollaboration()
        store.save(state)
    }
    private suspend fun syncCollaboration(){
        when(val r=remote.children()){is RemoteResult.Ok->familyChildren=r.value;RemoteResult.Unauthorized->{handleRemote(RemoteResult.Unauthorized,"");return};is RemoteResult.Failure->Unit}
        when(val r=production.familyMembers()){is RemoteResult.Ok->{familyMembers=r.value;isFamilyOwner=r.value.any{it.id==remote.currentParentId()&&it.role=="OWNER"&&it.status=="ACTIVE"}};else->Unit}
        if(isFamilyOwner)when(val r=production.pairedDevices()){is RemoteResult.Ok->pairedDevices=r.value;else->Unit}else pairedDevices=emptyList()
        when(val r=production.familyNotifications()){is RemoteResult.Ok->familyNotifications=r.value;else->Unit}
    }
    private suspend fun syncRewardGovernance(asChild:Boolean){if(!asChild)when(val r=production.rewardBudgetSummary()){is RemoteResult.Ok->rewardBudget=r.value;else->rewardBudget=null};when(val r=production.activeExchangeControl(asChild)){is RemoteResult.Ok->exchangeControl=r.value;else->exchangeControl=null};when(val r=production.exchangeApprovals(asChild)){is RemoteResult.Ok->exchangeApprovals=r.value;else->Unit}}
    private suspend fun syncGrowthArchive(){
        when(val r=production.growthPlans()){is RemoteResult.Ok->growthPlans=r.value;RemoteResult.Unauthorized->{handleRemote(RemoteResult.Unauthorized,"");return};is RemoteResult.Failure->Unit}
        when(val r=production.growthMilestones()){is RemoteResult.Ok->growthMilestones=r.value;RemoteResult.Unauthorized->{handleRemote(RemoteResult.Unauthorized,"");return};is RemoteResult.Failure->Unit}
        when(val r=production.growthReport()){is RemoteResult.Ok->growthReport=r.value;else->Unit}
    }
    private suspend fun ensureFund():RemoteFund?{when(val existing=production.funds()){is RemoteResult.Ok->existing.value.firstOrNull()?.let{return it};RemoteResult.Unauthorized->{handleRemote(RemoteResult.Unauthorized,"");return null};is RemoteResult.Failure->{message=existing.message;return null}};return when(val made=production.createFund()){is RemoteResult.Ok->{when(val nav=production.updateNav(made.value.id,BigDecimal.ONE.setScale(6))){is RemoteResult.Failure->{message=nav.message;return null};RemoteResult.Unauthorized->{handleRemote(RemoteResult.Unauthorized,"");return null};else->Unit};when(val fees=production.configureFundFees(made.value.id)){is RemoteResult.Failure->{message=fees.message;return null};RemoteResult.Unauthorized->{handleRemote(RemoteResult.Unauthorized,"");return null};else->Unit};made.value};RemoteResult.Unauthorized->{handleRemote(RemoteResult.Unauthorized,"");null};is RemoteResult.Failure->{message=made.message;null}}}
    private fun fundTrade(side:String,input:BigDecimal,success:String){viewModelScope.launch{val fund=ensureFund()?:return@launch;when(val preview=production.tradePreview(fund.id,side,input)){is RemoteResult.Ok->{pendingFundTrade=preview.value;message="$success；请先核对 NAV、费用和份额"};RemoteResult.Unauthorized->handleRemote(RemoteResult.Unauthorized,"");is RemoteResult.Failure->message=preview.message}}}

    private suspend fun syncSeniorLearning() {
        when(val result=remote.seniorModules()) { is RemoteResult.Ok->seniorModuleConfiguration=result.value;RemoteResult.Unauthorized->handleRemote(RemoteResult.Unauthorized,"");is RemoteResult.Failure->Unit }
        when(val result=remote.seniorGoals()) { is RemoteResult.Ok->seniorGoals=result.value;RemoteResult.Unauthorized->handleRemote(RemoteResult.Unauthorized,"");is RemoteResult.Failure->Unit }
        when(val result=remote.seniorReflections()) { is RemoteResult.Ok->seniorReflections=result.value;RemoteResult.Unauthorized->handleRemote(RemoteResult.Unauthorized,"");is RemoteResult.Failure->Unit }
        when(val result=remote.seniorReport()) { is RemoteResult.Ok->seniorLearningReport=result.value;RemoteResult.Unauthorized->handleRemote(RemoteResult.Unauthorized,"");is RemoteResult.Failure->Unit }
    }

    private suspend fun syncLearningAssignments():Boolean {
        return when (val result = remote.learningAssignments()) {
            is RemoteResult.Ok -> { learningAssignments = result.value; syncLearningSupport(result.value); true }
            RemoteResult.Unauthorized -> { handleRemote(RemoteResult.Unauthorized, ""); false }
            is RemoteResult.Failure -> { message = result.message; false }
        }
    }

    private suspend fun syncJuniorPlan() {
        when(val result=remote.juniorLearningPlan()) {
            is RemoteResult.Ok -> applyJuniorPlan(result.value)
            RemoteResult.Unauthorized -> handleRemote(RemoteResult.Unauthorized,"")
            is RemoteResult.Failure -> message=result.message
        }
    }

    private fun applyJuniorPlan(plan:RemoteJuniorPlan) {
        juniorLearningPlan=plan
        val order=plan.items.mapIndexed { index,item -> item.assignmentId to index }.toMap()
        learningAssignments=learningAssignments.sortedWith(compareBy<RemoteLearningAssignment> {
            order[it.id] ?: Int.MAX_VALUE
        }.thenBy { if(it.status=="COMPLETED") 1 else 0 }.thenBy { it.id })
    }

    private suspend fun syncLearningSupport(assignments:List<RemoteLearningAssignment>) {
        val next=mutableMapOf<String,List<RemoteSupportEvent>>()
        assignments.forEach { assignment ->
            when(val result=remote.learningSupportEvents(assignment.id)) {
                is RemoteResult.Ok -> if(result.value.isNotEmpty()) next[assignment.id]=result.value
                RemoteResult.Unauthorized -> return
                is RemoteResult.Failure -> Unit
            }
        }
        learningSupportByAssignment=next
    }

    private suspend fun syncTeachingCourses() {
        when (val result = remote.teachingCourses()) {
            is RemoteResult.Ok -> teachingCourses = result.value
            RemoteResult.Unauthorized -> handleRemote(RemoteResult.Unauthorized, "")
            is RemoteResult.Failure -> message = result.message
        }
    }

    private fun runTeachingAction(block: suspend () -> Unit) {
        if (teachingActionRunningInternal) return
        updateTeachingActionState(true)
        viewModelScope.launch {
            try { block() } finally { updateTeachingActionState(false) }
        }
    }

    private fun updateTeachingActionState(running:Boolean) {
        teachingActionRunningInternal = running
        teachingActionRunning = running
    }

    private fun replaceLearning(updated: RemoteLearningAssignment) {
        learningAssignments = (learningAssignments.filterNot { it.id == updated.id } + updated)
            .sortedBy { if (it.status == "COMPLETED") 1 else 0 }
    }

    private fun handleRemote(result: RemoteResult<RemoteSnapshot>, success: String) {
        when (result) {
            is RemoteResult.Ok -> {
                val snapshot = result.value
                remoteCompletionByTask = snapshot.tasks.mapNotNull { task -> task.completionId?.let { task.id to it } }.toMap()
                state = state.copy(
                    tasks = snapshot.tasks.map { task -> LocalGrowthTask(
                        id = task.id, title = task.title, minutes = task.minutes,
                        moneyReward = BigDecimal.ZERO.setScale(2), coinReward = 10, xpReward = 10,
                        status = when (task.status) { "SUBMITTED" -> TaskStatus.SUBMITTED; "APPROVED" -> TaskStatus.APPROVED; else -> TaskStatus.TODO },
                    ) },
                    wallet = state.wallet.copy(money = snapshot.money, coin = snapshot.coin),
                    experience = snapshot.experience?.toLocal() ?: state.experience,
                )
                connectionState = ConnectionState.Connected(snapshot)
                message = success
            }
            RemoteResult.Unauthorized -> { remoteCompletionByTask = emptyMap(); connectionState = ConnectionState.Expired; message = "登录已过期，请由家长重新连接" }
            is RemoteResult.Failure -> { if (connectionState !is ConnectionState.Connected) connectionState = ConnectionState.Error(result.message); message = result.message }
        }
    }

    private fun flushUsage() {
        if (usageFlushRunning) return
        viewModelScope.launch { flushUsageNow() }
    }

    private suspend fun flushUsageNow() {
        if (usageFlushRunning) return
        usageFlushRunning = true
        try {
            while (pendingUsage.isNotEmpty() && remote.hasSession()) {
                val event = pendingUsage.first()
                when (val result = remote.recordUsage(event.key, event.occurredAt)) {
                    is RemoteResult.Ok -> { pendingUsage.removeAt(0); syncUsageAccess() }
                    RemoteResult.Unauthorized -> { handleRemote(RemoteResult.Unauthorized, ""); return }
                    is RemoteResult.Failure -> { message = result.message; return }
                }
            }
        } finally {
            usageFlushRunning = false
        }
    }

    private fun mutate(success: String? = null, operation: (FamilyLocalState) -> FamilyLocalState) {
        runCatching { operation(state) }
            .onSuccess { updated ->
                store.save(updated).onSuccess {
                    state = updated
                    if (success != null) message = success
                }.onFailure { message = it.message ?: "本地保存失败" }
            }
            .onFailure { message = it.message ?: "操作失败" }
    }

    private fun fail(text: String): Boolean { message = text; return false }
    private fun failUnit(text: String) { message = text }
    private data class PendingUsage(val key: String, val occurredAt: Instant)
}

private fun RemoteExperienceProfile.toLocal() = ChildExperienceSettings(
    birthDate = birthDate,
    recommendedStage = SchoolStage.valueOf(recommendedStage),
    stageOverride = stageOverride?.let(SchoolStage::valueOf),
    effectiveStage = SchoolStage.valueOf(effectiveStage),
    recommendedPrimaryBand = recommendedPrimaryBand?.let(PrimaryGradeBand::valueOf),
    primaryBandOverride = primaryBandOverride?.let(PrimaryGradeBand::valueOf),
    effectivePrimaryBand = effectivePrimaryBand?.let(PrimaryGradeBand::valueOf),
    overrideReason = overrideReason,
    hapticsEnabled = hapticsEnabled,
    version = version,
    source = ExperienceSource.SERVER,
)
