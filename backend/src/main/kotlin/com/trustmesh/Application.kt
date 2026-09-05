package com.trustmesh

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.server.websocket.*
import org.jetbrains.exposed.sql.Database
import redis.clients.jedis.JedisPool
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import com.trustmesh.auth.JwtProvider
import com.trustmesh.auth.authRoutes
import com.trustmesh.account.accountRoutes
import com.trustmesh.agent.agentRoutes
import com.trustmesh.transaction.transactionRoutes
import com.trustmesh.ledger.ledgerRoutes
import com.trustmesh.merchant.merchantRoutes
import com.trustmesh.payment.paymentRoutes

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) {
        json()
    }
    install(WebSockets)
    install(Authentication) {
        jwt("jwt") {
            verifier(JwtProvider.verifier)
            validate { credential ->
                if (credential.payload.subject != null) JWTPrincipal(credential.payload) else null
            }
        }
    }

    // Connect DB
    val dbHost = System.getenv("DB_HOST") ?: "localhost"
    val dbPort = System.getenv("DB_PORT") ?: "5432"
    val dbName = System.getenv("DB_NAME") ?: "trustmesh_db"
    val dbUser = System.getenv("DB_USER") ?: "trustmesh_user"
    val dbPassword = System.getenv("DB_PASSWORD") ?: "trustmesh_password"

    val hikariConfig = HikariConfig().apply {
        jdbcUrl = "jdbc:postgresql://$dbHost:$dbPort/$dbName"
        driverClassName = "org.postgresql.Driver"
        username = dbUser
        password = dbPassword
        maximumPoolSize = 3
    }
    
    try {
        val dataSource = HikariDataSource(hikariConfig)
        Database.connect(dataSource)
        log.info("Successfully connected to database. Creating schemas if not exist.")
        org.jetbrains.exposed.sql.transactions.transaction {
            org.jetbrains.exposed.sql.SchemaUtils.create(
                com.trustmesh.db.Users,
                com.trustmesh.db.ActiveSessions,
                com.trustmesh.db.Agents,
                com.trustmesh.db.Transactions,
                com.trustmesh.db.EscrowItems,
                com.trustmesh.db.LedgerEntries,
                com.trustmesh.db.LinkedAccounts,
                com.trustmesh.db.Merchants,
                com.trustmesh.db.PaymentOrders
            )
            
            // Seed default FinGuru profiles if empty
            val userCount = com.trustmesh.db.Users.selectAll().count()
            if (userCount == 0L) {
                val testUserId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000000")
                com.trustmesh.db.Users.insert {
                    it[id] = testUserId
                    it[email] = "test@trustmesh.in"
                    it[passwordHash] = "$2a$10$8.z8pCym1Sj9vG6.H6fJee.C6d/Cg0.x7P8c7eFpW/8hEaH7d1a2S"
                    it[displayName] = "Harshal"
                    it[biometricEnabled] = true
                }
                
                val finGuruAgentId = java.util.UUID.fromString("11111111-1111-1111-1111-111111111111")
                com.trustmesh.db.Agents.insert {
                    it[id] = finGuruAgentId
                    it[ownerId] = testUserId
                    it[name] = "FinGuru"
                    it[intentStatement] = "Automate portfolio management, buy items within constraints, and rebalance index allocations"
                    it[categoryScope] = "ELECTRONICS,OTHER"
                    it[spendEnvelopeLimit] = java.math.BigDecimal.valueOf(50000.00)
                    it[spendEnvelopeWindow] = "WEEKLY"
                    it[currentUtilization] = java.math.BigDecimal.valueOf(12450.00)
                    it[escalationRules] = """[{"type":"SINGLE_TRANSACTION_LIMIT","threshold":25000.0,"description":"Require authorization above ₹25,000"}]"""
                    it[status] = "ACTIVE"
                }

                com.trustmesh.db.Transactions.insert {
                    it[id] = java.util.UUID.randomUUID()
                    it[agentId] = finGuruAgentId
                    it[merchantName] = "Reliance Digital"
                    it[merchantCategory] = "ELECTRONICS"
                    it[amount] = java.math.BigDecimal.valueOf(8450.00)
                    it[status] = "RELEASED"
                    it[negotiationDetail] = "Negotiated 12% discount on bulk options analysis monitor. Initial quote: ₹9600. Accepted final offer: ₹8450."
                }

                com.trustmesh.db.Transactions.insert {
                    it[id] = java.util.UUID.randomUUID()
                    it[agentId] = finGuruAgentId
                    it[merchantName] = "Tata Neu"
                    it[merchantCategory] = "OTHER"
                    it[amount] = java.math.BigDecimal.valueOf(4000.00)
                    it[status] = "RELEASED"
                    it[negotiationDetail] = "Negotiated subscription bundle fee. Original quote: ₹4600. Accepted final offer: ₹4000. Saved ₹600."
                }

                com.trustmesh.ledger.LedgerService.appendEntry(
                    agentId = finGuruAgentId,
                    intent = "Automate portfolio management and buy items within constraints",
                    action = "Purchased options display screen at Reliance Digital for ₹8,450.00",
                    outcome = "RELEASED"
                )

                com.trustmesh.ledger.LedgerService.appendEntry(
                    agentId = finGuruAgentId,
                    intent = "Automate portfolio management and buy items within constraints",
                    action = "Purchased options premium sub at Tata Neu for ₹4,000.00",
                    outcome = "RELEASED"
                )
            }
        }
    } catch (e: Exception) {
        log.error("Failed to connect to database.", e)
    }

    // Connect Redis
    val redisHost = System.getenv("REDIS_HOST") ?: "localhost"
    val redisPort = (System.getenv("REDIS_PORT") ?: "6379").toInt()
    val redisPool = try {
        JedisPool(redisHost, redisPort).also {
            it.resource.use { jedis ->
                log.info("Successfully pinged Redis: ${jedis.ping()}")
            }
        }
    } catch (e: Exception) {
        log.error("Failed to connect to Redis.", e)
        null
    }

    routing {
        route("/api/v1") {
            get("/health") {
                val redisStatus = try {
                    redisPool?.resource?.use { jedis ->
                        jedis.ping() == "PONG"
                    } ?: false
                } catch (e: Exception) {
                    false
                }
                call.respond(
                    mapOf(
                        "status" to "UP",
                        "postgres" to "CONNECTED",
                        "redis" to if (redisStatus) "CONNECTED" else "DISCONNECTED"
                    )
                )
            }
            authRoutes()
            accountRoutes()
            agentRoutes()
            transactionRoutes()
            ledgerRoutes()
            merchantRoutes()
            paymentRoutes()
        }
    }
}
