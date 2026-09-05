package com.trustmesh.data.remote

import com.trustmesh.domain.model.Category
import com.trustmesh.domain.model.EscalationRule
import com.trustmesh.domain.model.WindowType
import kotlinx.serialization.Serializable

@Serializable
data class AuthRequest(
    val email: String,
    val password: String
)

@Serializable
data class SignupRequest(
    val email: String,
    val password: String,
    val displayName: String
)

@Serializable
data class AuthResponse(
    val token: String,
    val refreshToken: String,
    val user: UserDto
)

@Serializable
data class UserDto(
    val id: String,
    val email: String,
    val displayName: String,
    val biometricEnabled: Boolean,
    val createdAt: String
)

@Serializable
data class AgentDto(
    val id: String,
    val ownerId: String,
    val name: String,
    val intentStatement: String,
    val categoryScope: List<Category>,
    val limitAmount: Double,
    val windowType: WindowType,
    val currentUtilization: Double,
    val escalationRules: List<EscalationRule>,
    val status: String,
    val createdAt: String
)

@Serializable
data class CreateAgentRequest(
    val name: String,
    val intentStatement: String,
    val categoryScope: List<Category>,
    val limitAmount: Double,
    val windowType: WindowType,
    val escalationRules: List<EscalationRule>
)

@Serializable
data class UpdateEnvelopeRequest(
    val limitAmount: Double,
    val windowType: WindowType
)

@Serializable
data class StatusRequest(
    val status: String
)

@Serializable
data class EscrowActionRequest(
    val action: String // "APPROVE" or "DENY"
)

@Serializable
data class PlaidTokenResponse(
    val linkToken: String
)

@Serializable
data class PlaidExchangeRequest(
    val publicToken: String
)

@Serializable
data class SessionResponse(
    val sessions: List<String>
)

@Serializable
data class TransactionRequest(
    val agentId: String,
    val merchantName: String,
    val merchantCategory: String,
    val amount: Double,
    val negotiationDetail: String
)

@Serializable
data class GoogleAuthRequest(
    val idToken: String
)

@Serializable
data class CreateOrderRequest(
    val amountInRupees: Double,
    val agentId: String? = null,
    val escrowId: String? = null
)

@Serializable
data class CreateOrderResponse(
    val orderId: String,
    val amountInPaise: Long,
    val currency: String,
    val keyId: String
)

@Serializable
data class VerifyPaymentRequest(
    val razorpayOrderId: String,
    val razorpayPaymentId: String,
    val razorpaySignature: String,
    val agentId: String? = null,
    val escrowId: String? = null
)

@Serializable
data class VerifyPaymentResponse(
    val status: String,
    val paymentId: String
)

