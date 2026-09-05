package com.trustmesh.data.repository

import com.trustmesh.data.local.AccountDao
import com.trustmesh.data.remote.CreateOrderRequest
import com.trustmesh.data.remote.TrustMeshApi
import com.trustmesh.data.remote.VerifyPaymentRequest
import com.trustmesh.domain.model.PaymentOrder
import com.trustmesh.domain.repository.PaymentRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaymentRepositoryImpl @Inject constructor(
    private val api: TrustMeshApi,
    private val accountDao: AccountDao
) : PaymentRepository {

    override suspend fun createPaymentOrder(
        amountInRupees: Double,
        agentId: String?,
        escrowId: String?
    ): Result<PaymentOrder> {
        return try {
            val response = api.createPaymentOrder(
                CreateOrderRequest(
                    amountInRupees = amountInRupees,
                    agentId = agentId,
                    escrowId = escrowId
                )
            )
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                Result.success(
                    PaymentOrder(
                        orderId = body.orderId,
                        amountInPaise = body.amountInPaise,
                        currency = body.currency,
                        keyId = body.keyId
                    )
                )
            } else {
                Result.failure(Exception("Failed to create Razorpay payment order: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun verifyPayment(
        razorpayOrderId: String,
        razorpayPaymentId: String,
        razorpaySignature: String,
        agentId: String?,
        escrowId: String?
    ): Result<String> {
        return try {
            val response = api.verifyPayment(
                VerifyPaymentRequest(
                    razorpayOrderId = razorpayOrderId,
                    razorpayPaymentId = razorpayPaymentId,
                    razorpaySignature = razorpaySignature,
                    agentId = agentId,
                    escrowId = escrowId
                )
            )
            if (response.isSuccessful && response.body() != null) {
                // Refresh local accounts to display updated balances
                try {
                    val accountsResp = api.getLinkedAccounts()
                    if (accountsResp.isSuccessful && accountsResp.body() != null) {
                        accountDao.insertLinkedAccounts(accountsResp.body()!!)
                    }
                } catch (_: Exception) {
                    // Cache refresh can fail silently in offline/transient modes
                }
                Result.success(response.body()!!.paymentId)
            } else {
                Result.failure(Exception("Payment signature verification failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
