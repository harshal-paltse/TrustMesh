package com.trustmesh

import com.trustmesh.payment.RazorpayVerifier
import org.junit.Test
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RazorpayVerificationTest {

    private val testSecret = "sample_mock_secret_for_unit_tests"

    private fun generateExpectedSignature(orderId: String, paymentId: String, secret: String): String {
        val payload = "$orderId|$paymentId"
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        val hash = mac.doFinal(payload.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    @Test
    fun testValidRazorpaySignaturePasses() {
        val orderId = "order_EKwxwpP1omqPpL"
        val paymentId = "pay_29BgBnoIOdSFBm"
        val signature = generateExpectedSignature(orderId, paymentId, testSecret)

        val isValid = RazorpayVerifier.verifySignature(
            orderId = orderId,
            paymentId = paymentId,
            signature = signature,
            secret = testSecret
        )

        assertTrue(isValid, "Authentic HMAC-SHA256 Razorpay signature must be verified successfully.")
    }

    @Test
    fun testTamperedSignatureFails() {
        val orderId = "order_EKwxwpP1omqPpL"
        val paymentId = "pay_29BgBnoIOdSFBm"
        val forgedSignature = "0000000000000000000000000000000000000000000000000000000000000000"

        val isValid = RazorpayVerifier.verifySignature(
            orderId = orderId,
            paymentId = paymentId,
            signature = forgedSignature,
            secret = testSecret
        )

        assertFalse(isValid, "Forged signature must be rejected immediately.")
    }

    @Test
    fun testTamperedPaymentIdFails() {
        val orderId = "order_EKwxwpP1omqPpL"
        val originalPaymentId = "pay_29BgBnoIOdSFBm"
        val tamperedPaymentId = "pay_99999999999999" // Attacker alters payment ID
        val signature = generateExpectedSignature(orderId, originalPaymentId, testSecret)

        val isValid = RazorpayVerifier.verifySignature(
            orderId = orderId,
            paymentId = tamperedPaymentId,
            signature = signature,
            secret = testSecret
        )

        assertFalse(isValid, "Signature verification must fail if payment ID is manipulated.")
    }
}
