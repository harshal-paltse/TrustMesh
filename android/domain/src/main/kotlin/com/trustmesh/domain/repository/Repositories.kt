package com.trustmesh.domain.repository

import com.trustmesh.domain.model.*
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun getSessionUser(): Flow<User?>
    suspend fun signup(email: String, password: String, displayName: String): Result<User>
    suspend fun login(email: String, password: String): Result<User>
    suspend fun googleLogin(idToken: String): Result<User>
    suspend fun logout(): Result<Unit>
    suspend fun setBiometricEnabled(enabled: Boolean): Result<Unit>
    suspend fun getActiveSessions(): Result<List<String>>
    suspend fun revokeSession(sessionId: String): Result<Unit>
}

interface AgentRepository {
    fun getAgents(): Flow<List<Agent>>
    fun getAgentById(id: String): Flow<Agent?>
    suspend fun createAgent(name: String, intent: String, categories: List<Category>, limit: Double, window: WindowType, rules: List<EscalationRule>): Result<Agent>
    suspend fun updateSpendEnvelope(agentId: String, limit: Double, window: WindowType): Result<Agent>
    suspend fun setAgentStatus(agentId: String, status: AgentStatus): Result<Agent>
    suspend fun syncAgents(): Result<Unit>
}

interface TransactionRepository {
    fun getTransactions(): Flow<List<Transaction>>
    fun getTransactionsForAgent(agentId: String): Flow<List<Transaction>>
    fun getEscrowItems(): Flow<List<EscrowItem>>
    suspend fun approveEscrow(escrowId: String): Result<Unit>
    suspend fun denyEscrow(escrowId: String): Result<Unit>
    suspend fun syncTransactions(): Result<Unit>
    suspend fun syncEscrowItems(): Result<Unit>
}

interface AccountRepository {
    fun getLinkedAccounts(): Flow<List<LinkedAccount>>
    suspend fun generatePlaidLinkToken(): Result<String>
    suspend fun exchangePlaidPublicToken(publicToken: String): Result<Unit>
    suspend fun syncAccounts(): Result<Unit>
}

interface LedgerRepository {
    fun getLedgerEntries(): Flow<List<LedgerEntry>>
    suspend fun syncLedger(): Result<Unit>
    suspend fun verifyLedgerChainIntegrity(): Result<Boolean>
}

interface MerchantRepository {
    fun getMerchants(): Flow<List<Merchant>>
    suspend fun searchMerchants(query: String): Result<List<Merchant>>
}

interface PaymentRepository {
    suspend fun createPaymentOrder(amountInRupees: Double, agentId: String? = null, escrowId: String? = null): Result<PaymentOrder>
    suspend fun verifyPayment(razorpayOrderId: String, razorpayPaymentId: String, razorpaySignature: String, agentId: String? = null, escrowId: String? = null): Result<String>
}

