package com.trustmesh.account

import com.trustmesh.db.LinkedAccounts
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*


@Serializable
data class PlaidTokenResponse(val linkToken: String)

@Serializable
data class PlaidExchangeRequest(val publicToken: String)

@Serializable
data class AccountDto(
    val id: String,
    val plaidAccountId: String,
    val institutionName: String,
    val currentBalance: Double,
    val availableBalance: Double,
    val lastSyncedAt: String
)

fun Route.accountRoutes() {
    route("/accounts") {
        authenticate("jwt") {
            get {
                val principal = call.principal<JWTPrincipal>()
                val userIdStr = principal?.subject ?: ""
                val accounts = if (userIdStr.isNotEmpty()) {
                    val userId = UUID.fromString(userIdStr)
                    transaction {
                        LinkedAccounts.select { LinkedAccounts.userId eq userId }
                            .map {
                                AccountDto(
                                    id = it[LinkedAccounts.id].toString(),
                                    plaidAccountId = it[LinkedAccounts.plaidAccountId],
                                    institutionName = it[LinkedAccounts.institutionName],
                                    currentBalance = it[LinkedAccounts.currentBalance].toDouble(),
                                    availableBalance = it[LinkedAccounts.availableBalance].toDouble(),
                                    lastSyncedAt = it[LinkedAccounts.lastSyncedAt].toString()
                                )
                            }
                    }
                } else emptyList()
                call.respond(accounts)
            }
            

            post("/plaid/link-token") {
                val linkToken = PlaidClient.createLinkToken()
                call.respond(PlaidTokenResponse(linkToken))
            }

            post("/plaid/exchange") {
                val req = call.receive<PlaidExchangeRequest>()
                val principal = call.principal<JWTPrincipal>()
                val userIdStr = principal?.subject ?: ""

                
                
                if (userIdStr.isNotEmpty()) {
                    val userId = UUID.fromString(userIdStr)
                    val accessToken = PlaidClient.exchangePublicToken(req.publicToken)
                    val balances = PlaidClient.getBalances(accessToken)

                    

                    transaction {
                        balances.forEach { bal ->
                            LinkedAccounts.insert {
                                it[id] = UUID.randomUUID()
                                it[LinkedAccounts.userId] = userId
                                it[plaidAccountId] = bal.accountId
                                it[institutionName] = bal.name
                                it[currentBalance] = BigDecimal.valueOf(bal.currentBalance)
                                it[availableBalance] = BigDecimal.valueOf(bal.availableBalance)
                            }
                        }
                    }
                }
                }
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}
}
}
