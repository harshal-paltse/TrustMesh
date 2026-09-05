package com.trustmesh.db

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object Users : Table("users") {
    val id = uuid("id")
    val email = varchar("email", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val displayName = varchar("display_name", 255)
    val biometricEnabled = bool("biometric_enabled").default(false)
    val createdAt = datetime("created_at").defaultExpression(org.jetbrains.exposed.sql.javatime.CurrentDateTime)
    override val primaryKey = PrimaryKey(id)
}

object ActiveSessions : Table("active_sessions") {
    val id = uuid("id")
    val userId = uuid("user_id").references(Users.id)
    val refreshTokenHash = varchar("refresh_token_hash", 255)
    val deviceInfo = varchar("device_info", 255).nullable()
    val expiresAt = datetime("expires_at")
    val createdAt = datetime("created_at").defaultExpression(org.jetbrains.exposed.sql.javatime.CurrentDateTime)
    override val primaryKey = PrimaryKey(id)
}

object Agents : Table("agents") {
    val id = uuid("id")
    val ownerId = uuid("owner_id").references(Users.id)
    val name = varchar("name", 255)
    val intentStatement = text("intent_statement")
    val categoryScope = text("category_scope") // Comma-separated categories
    val spendEnvelopeLimit = decimal("spend_envelope_limit", 12, 2)
    val spendEnvelopeWindow = varchar("spend_envelope_window", 50)
    val currentUtilization = decimal("current_utilization", 12, 2).default(0.0.toBigDecimal())
    val escalationRules = text("escalation_rules") // JSON array string
    val status = varchar("status", 50)
    val createdAt = datetime("created_at").defaultExpression(org.jetbrains.exposed.sql.javatime.CurrentDateTime)
    override val primaryKey = PrimaryKey(id)
}

object Transactions : Table("transactions") {
    val id = uuid("id")
    val agentId = uuid("agent_id").references(Agents.id)
    val merchantName = varchar("merchant_name", 255)
    val merchantCategory = varchar("merchant_category", 100)
    val amount = decimal("amount", 12, 2)
    val status = varchar("status", 50)
    val negotiationDetail = text("negotiation_detail")
    val createdAt = datetime("created_at").defaultExpression(org.jetbrains.exposed.sql.javatime.CurrentDateTime)
    override val primaryKey = PrimaryKey(id)
}

object EscrowItems : Table("escrow_items") {
    val id = uuid("id")
    val transactionId = uuid("transaction_id").references(Transactions.id)
    val state = varchar("state", 50)
    val conditionType = varchar("condition_type", 100)
    val conditionThreshold = decimal("condition_threshold", 12, 2)
    val createdAt = datetime("created_at").defaultExpression(org.jetbrains.exposed.sql.javatime.CurrentDateTime)
    val resolvedAt = datetime("resolved_at").nullable()
    override val primaryKey = PrimaryKey(id)
}

object LedgerEntries : Table("ledger_entries") {
    val id = uuid("id")
    val agentId = uuid("agent_id").references(Agents.id)
    val timestamp = datetime("timestamp").defaultExpression(org.jetbrains.exposed.sql.javatime.CurrentDateTime)
    val statedIntentSnapshot = text("stated_intent_snapshot")
    val actionTaken = text("action_taken")
    val outcome = varchar("outcome", 50)
    val entryHash = varchar("entry_hash", 64)
    val previousHash = varchar("previous_hash", 64)
    override val primaryKey = PrimaryKey(id)
}

object LinkedAccounts : Table("linked_accounts") {
    val id = uuid("id")
    val userId = uuid("user_id").references(Users.id)
    val plaidAccountId = varchar("plaid_account_id", 255)
    val institutionName = varchar("institution_name", 255)
    val currentBalance = decimal("current_balance", 12, 2)
    val availableBalance = decimal("available_balance", 12, 2)
    val lastSyncedAt = datetime("last_synced_at").defaultExpression(org.jetbrains.exposed.sql.javatime.CurrentDateTime)
    override val primaryKey = PrimaryKey(id)
}

object Merchants : Table("merchants") {
    val id = uuid("id")
    val name = varchar("name", 255)
    val category = varchar("category", 100)
    val externalReputationScore = decimal("external_reputation_score", 4, 2)
    val internalTrustScore = decimal("internal_trust_score", 4, 2)
    override val primaryKey = PrimaryKey(id)
}

object PaymentOrders : Table("payment_orders") {
    val id = uuid("id")
    val userId = uuid("user_id").references(Users.id)
    val razorpayOrderId = varchar("razorpay_order_id", 100).uniqueIndex()
    val amount = decimal("amount", 12, 2)
    val currency = varchar("currency", 10).default("INR")
    val status = varchar("status", 50).default("CREATED") // CREATED, PAID, FAILED
    val razorpayPaymentId = varchar("razorpay_payment_id", 100).nullable()
    val agentId = uuid("agent_id").references(Agents.id).nullable()
    val escrowId = uuid("escrow_id").references(EscrowItems.id).nullable()
    val createdAt = datetime("created_at").defaultExpression(org.jetbrains.exposed.sql.javatime.CurrentDateTime)
    val updatedAt = datetime("updated_at").nullable()
    override val primaryKey = PrimaryKey(id)
}
