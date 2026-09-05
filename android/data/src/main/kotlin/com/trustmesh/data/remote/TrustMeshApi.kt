package com.trustmesh.data.remote

import retrofit2.Response
import retrofit2.http.*

interface TrustMeshApi {

    @POST("auth/signup")
    suspend fun signup(@Body request: SignupRequest): Response<AuthResponse>

    @POST("auth/login")
    @Headers("No-Auth: true")
    suspend fun login(@Body request: AuthRequest): Response<AuthResponse>

    @POST("auth/logout")
    suspend fun logout(): Response<Unit>

    @PUT("auth/biometrics")
    suspend fun updateBiometrics(@Query("enabled") enabled: Boolean): Response<Unit>

    @GET("auth/sessions")
    suspend fun getActiveSessions(): Response<SessionResponse>

    @DELETE("auth/sessions/{id}")
    suspend fun revokeSession(@Path("id") sessionId: String): Response<Unit>

    @GET("agents")
    suspend fun getAgents(): Response<List<AgentDto>>

    @POST("agents")
    suspend fun createAgent(@Body request: CreateAgentRequest): Response<AgentDto>

    @PUT("agents/{id}/envelope")
    suspend fun updateEnvelope(
        @Path("id") agentId: String,
        @Body request: UpdateEnvelopeRequest
    ): Response<AgentDto>

    @PUT("agents/{id}/status")
    suspend fun updateStatus(
        @Path("id") agentId: String,
        @Body request: StatusRequest
    ): Response<AgentDto>

    @GET("transactions")
    suspend fun getTransactions(@Query("agentId") agentId: String? = null): Response<List<com.trustmesh.data.local.TransactionEntity>>

    @GET("transactions/escrow")
    suspend fun getEscrowItems(): Response<List<com.trustmesh.data.local.EscrowItemEntity>>

    @POST("transactions/escrow/{id}/action")
    suspend fun resolveEscrow(
        @Path("id") escrowId: String,
        @Body request: EscrowActionRequest
    ): Response<Unit>

    @GET("accounts")
    suspend fun getLinkedAccounts(): Response<List<com.trustmesh.data.local.LinkedAccountEntity>>

    @POST("accounts/plaid/link-token")
    suspend fun createPlaidLinkToken(): Response<PlaidTokenResponse>

    @POST("accounts/plaid/exchange")
    suspend fun exchangePlaidPublicToken(@Body request: PlaidExchangeRequest): Response<Unit>

    @GET("ledger")
    suspend fun getLedger(): Response<List<com.trustmesh.data.local.LedgerEntryEntity>>

    @GET("merchants/search")
    suspend fun searchMerchants(@Query("q") query: String): Response<List<com.trustmesh.data.local.MerchantEntity>>

    @POST("transactions/request")
    suspend fun requestTransaction(@Body request: TransactionRequest): Response<Unit>

    @POST("auth/google")
    @Headers("No-Auth: true")
    suspend fun googleLogin(@Body request: GoogleAuthRequest): Response<AuthResponse>

    @POST("payments/create-order")
    suspend fun createPaymentOrder(@Body request: CreateOrderRequest): Response<CreateOrderResponse>

    @POST("payments/verify")
    suspend fun verifyPayment(@Body request: VerifyPaymentRequest): Response<VerifyPaymentResponse>
}
