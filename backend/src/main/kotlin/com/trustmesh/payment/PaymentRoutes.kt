package com.trustmesh.payment

import com.trustmesh.db.*
import com.trustmesh.ledger.LedgerService
import com.trustmesh.transaction.TransactionEventBus
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.MessageDigest
import java.time.LocalDateTime
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

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

object RazorpayVerifier {
    fun verifySignature(orderId: String, paymentId: String, signature: String, secret: String): Boolean {
        val payload = "$orderId|$paymentId"
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        val hash = mac.doFinal(payload.toByteArray(Charsets.UTF_8))
        val computedSignature = hash.joinToString("") { "%02x".format(it) }
        return MessageDigest.isEqual(
            computedSignature.toByteArray(Charsets.UTF_8),
            signature.toByteArray(Charsets.UTF_8)
        )
    }
}

fun Route.paymentRoutes() {
    val keyId = System.getenv("RAZORPAY_KEY_ID") ?: ""
    val keySecret = System.getenv("RAZORPAY_KEY_SECRET") ?: ""
    val httpClient = HttpClient.newHttpClient()

    route("/payments") {
        authenticate("jwt") {
            post("/create-order") {
                val principal = call.principal<JWTPrincipal>()
                val userIdStr = principal?.subject ?: ""
                if (userIdStr.isEmpty()) {
                    call.respond(HttpStatusCode.Unauthorized, "Missing authenticated user context")
                    return@post
                }
                val userId = UUID.fromString(userIdStr)

                val req = call.receive<CreateOrderRequest>()
                if (req.amountInRupees <= 0.0) {
                    call.respond(HttpStatusCode.BadRequest, "Order amount must be greater than zero")
                    return@post
                }

                val amountInPaise = (req.amountInRupees * 100).toLong()
                val receipt = "tm_${UUID.randomUUID().toString().take(10)}"

                val authHeader = "Basic " + Base64.getEncoder().encodeToString("$keyId:$keySecret".toByteArray(Charsets.UTF_8))
                val payloadJson = buildJsonObject {
                    put("amount", amountInPaise)
                    put("currency", "INR")
                    put("receipt", receipt)
                    put("payment_capture", 1)
                }.toString()

                val httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.razorpay.com/v1/orders"))
                    .header("Authorization", authHeader)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payloadJson))
                    .build()

                val httpResponse = try {
                    httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString())
                } catch (e: Exception) {
                    call.application.environment.log.error("Razorpay API connection failure", e)
                    call.respond(HttpStatusCode.BadGateway, "Unable to connect to Razorpay payment gateway")
                    return@post
                }

                if (httpResponse.statusCode() !in 200..299) {
                    call.application.environment.log.error("Razorpay order creation failed: ${httpResponse.body()}")
                    call.respond(HttpStatusCode.BadGateway, "Razorpay rejected order request: ${httpResponse.body()}")
                    return@post
                }

                val responseJson = Json.parseToJsonElement(httpResponse.body()).jsonObject
                val orderId = responseJson["id"]?.jsonPrimitive?.content ?: ""

                if (orderId.isEmpty()) {
                    call.respond(HttpStatusCode.InternalServerError, "Failed to parse order ID from Razorpay")
                    return@post
                }

                // Register payment order in database to prevent double-spending & replay attacks
                val agentUuid = req.agentId?.let { try { UUID.fromString(it) } catch (e: Exception) { null } }
                val escrowUuid = req.escrowId?.let { try { UUID.fromString(it) } catch (e: Exception) { null } }

                transaction {
                    PaymentOrders.insert {
                        it[id] = UUID.randomUUID()
                        it[PaymentOrders.userId] = userId
                        it[razorpayOrderId] = orderId
                        it[amount] = BigDecimal.valueOf(req.amountInRupees)
                        it[currency] = "INR"
                        it[status] = "CREATED"
                        it[agentId] = agentUuid
                        it[escrowId] = escrowUuid
                    }
                }

                call.respond(
                    HttpStatusCode.OK,
                    CreateOrderResponse(
                        orderId = orderId,
                        amountInPaise = amountInPaise,
                        currency = "INR",
                        keyId = keyId
                    )
                )
            }

            post("/verify") {
                val principal = call.principal<JWTPrincipal>()
                val userIdStr = principal?.subject ?: ""
                if (userIdStr.isEmpty()) {
                    call.respond(HttpStatusCode.Unauthorized, "Missing authenticated user context")
                    return@post
                }
                val userId = UUID.fromString(userIdStr)

                val req = call.receive<VerifyPaymentRequest>()

                // 1. Validate cryptographic HMAC-SHA256 signature
                val isSignatureValid = RazorpayVerifier.verifySignature(
                    orderId = req.razorpayOrderId,
                    paymentId = req.razorpayPaymentId,
                    signature = req.razorpaySignature,
                    secret = keySecret
                )

                if (!isSignatureValid) {
                    call.application.environment.log.warn("Tampered Razorpay payment signature detected for order ${req.razorpayOrderId}")
                    call.respond(HttpStatusCode.BadRequest, "Cryptographic payment verification failed: Invalid signature")
                    return@post
                }

                // 2. Anti-replay & order settlement within database transaction
                var settledAmount = BigDecimal.ZERO
                var associatedAgentId: UUID? = null
                var associatedEscrowId: UUID? = null

                val settlementSuccess = transaction {
                    val orderRow = PaymentOrders.select { PaymentOrders.razorpayOrderId eq req.razorpayOrderId }
                        .forUpdate()
                        .singleOrNull()

                    if (orderRow == null) {
                        return@transaction false
                    }

                    val currentStatus = orderRow[PaymentOrders.status]
                    if (currentStatus == "PAID") {
                        // Anti-replay: order was already processed
                        return@transaction false
                    }

                    settledAmount = orderRow[PaymentOrders.amount]
                    associatedAgentId = orderRow[PaymentOrders.agentId]
                    associatedEscrowId = orderRow[PaymentOrders.escrowId]

                    // Mark order as PAID
                    PaymentOrders.update({ PaymentOrders.razorpayOrderId eq req.razorpayOrderId }) {
                        it[status] = "PAID"
                        it[razorpayPaymentId] = req.razorpayPaymentId
                        it[updatedAt] = LocalDateTime.now()
                    }

                    // If linked to an escrow hold, release it
                    if (associatedEscrowId != null) {
                        val escrow = EscrowItems.select { EscrowItems.id eq associatedEscrowId }.singleOrNull()
                        if (escrow != null) {
                            val txId = escrow[EscrowItems.transactionId]
                            EscrowItems.update({ EscrowItems.id eq associatedEscrowId }) {
                                it[state] = "APPROVED"
                                it[resolvedAt] = LocalDateTime.now()
                            }
                            Transactions.update({ Transactions.id eq txId }) {
                                it[status] = "RELEASED"
                            }
                        }
                    }

                    // Update or credit user's Razorpay wallet cache in LinkedAccounts
                    val existingAccount = LinkedAccounts.select {
                        (LinkedAccounts.userId eq userId) and (LinkedAccounts.institutionName eq "Razorpay Digital Reserve")
                    }.singleOrNull()

                    if (existingAccount != null) {
                        val newCurrent = existingAccount[LinkedAccounts.currentBalance] + settledAmount
                        val newAvailable = existingAccount[LinkedAccounts.availableBalance] + settledAmount
                        LinkedAccounts.update({ LinkedAccounts.id eq existingAccount[LinkedAccounts.id] }) {
                            it[currentBalance] = newCurrent
                            it[availableBalance] = newAvailable
                            it[lastSyncedAt] = LocalDateTime.now()
                        }
                    } else {
                        LinkedAccounts.insert {
                            it[id] = UUID.randomUUID()
                            it[LinkedAccounts.userId] = userId
                            it[plaidAccountId] = "rzp_${userId.toString().take(8)}"
                            it[institutionName] = "Razorpay Digital Reserve"
                            it[currentBalance] = settledAmount
                            it[availableBalance] = settledAmount
                        }
                    }

                    true
                }

                if (!settlementSuccess) {
                    call.respond(HttpStatusCode.Conflict, "Order not found or has already been settled (Anti-Replay)")
                    return@post
                }

                // 3. Append to sequential SHA-256 Ledger for immutable auditability
                val fallbackAgentId = associatedAgentId ?: transaction {
                    Agents.select { Agents.ownerId eq userId }.firstOrNull()?.get(Agents.id)
                } ?: UUID.fromString("11111111-1111-1111-1111-111111111111")

                LedgerService.appendEntry(
                    agentId = fallbackAgentId,
                    intent = "Razorpay Financial Top-Up & Spend Clearance",
                    action = "Verified deposit of ₹$settledAmount via Razorpay (Payment: ${req.razorpayPaymentId}, Order: ${req.razorpayOrderId})",
                    outcome = "RELEASED"
                )

                // 4. Dispatch WebSocket event for real-time mobile sync
                application.launch {
                    TransactionEventBus.post("PAYMENT_SETTLED|${req.razorpayPaymentId}|$settledAmount")
                }

                call.respond(
                    HttpStatusCode.OK,
                    VerifyPaymentResponse(
                        status = "SUCCESS",
                        paymentId = req.razorpayPaymentId
                    )
                )
            }
        }
    }
}
