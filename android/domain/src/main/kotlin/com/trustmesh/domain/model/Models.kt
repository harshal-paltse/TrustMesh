package com.trustmesh.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class Category {
    GROCERY, ELECTRONICS, TRAVEL, ENTERTAINMENT, HEALTH, FOOD, OTHER
}

@Serializable
enum class AgentStatus {
    ACTIVE, PAUSED, REVOKED
}

@Serializable
enum class WindowType {
    DAILY, WEEKLY, MONTHLY
}

@Serializable
enum class TransactionStatus {
    CREATED, NEGOTIATED, PENDING_CONDITION, RELEASED, CANCELLED, DISPUTED
}

@Serializable
enum class EscrowState {
    PENDING, APPROVED, DENIED
}

@Serializable
enum class EscrowConditionType {
    SPEND_LIMIT_EXCEEDED, CATEGORY_MISMATCH, TRUST_SCORE_LOW, MANUAL_GATED
}

@Serializable
data class User(
    val id: String,
    val email: String,
    val displayName: String,
    val biometricEnabled: Boolean,
    val createdAt: String
)

@Serializable
data class EscalationRule(
    val type: String, // e.g., "SINGLE_TRANSACTION_LIMIT"
    val threshold: Double,
    val description: String
)

@Serializable
data class SpendEnvelope(
    val amountLimit: Double,
    val windowType: WindowType,
    val currentUtilization: Double
)

@Serializable
data class Agent(
    val id: String,
    val ownerId: String,
    val name: String,
    val intentStatement: String,
    val categoryScope: List<Category>,
    val spendEnvelope: SpendEnvelope,
    val escalationRules: List<EscalationRule>,
    val status: AgentStatus,
    val createdAt: String
)

@Serializable
data class TrustScore(
    val agentId: String,
    val category: Category,
    val score: Double,
    val lastUpdated: String,
    val componentBreakdown: Map<String, Double>
)

@Serializable
data class Transaction(
    val id: String,
    val agentId: String,
    val merchantName: String,
    val merchantCategory: Category,
    val amount: Double,
    val status: TransactionStatus,
    val negotiationDetail: String,
    val createdAt: String
)

@Serializable
data class EscrowItem(
    val id: String,
    val transactionId: String,
    val state: EscrowState,
    val conditionType: EscrowConditionType,
    val conditionThreshold: Double,
    val createdAt: String,
    val resolvedAt: String? = null
)

@Serializable
data class LedgerEntry(
    val id: String,
    val agentId: String,
    val timestamp: String,
    val statedIntentSnapshot: String,
    val actionTaken: String,
    val outcome: String,
    val hash: String,
    val previousHash: String
)

@Serializable
data class Merchant(
    val id: String,
    val name: String,
    val category: Category,
    val externalReputationScore: Double,
    val internalTrustScore: Double
)

@Serializable
data class LinkedAccount(
    val id: String,
    val plaidAccountId: String,
    val institutionName: String,
    val currentBalance: Double,
    val availableBalance: Double,
    val lastSyncedAt: String
)

@Serializable
data class PaymentOrder(
    val orderId: String,
    val amountInPaise: Long,
    val currency: String,
    val keyId: String
)

