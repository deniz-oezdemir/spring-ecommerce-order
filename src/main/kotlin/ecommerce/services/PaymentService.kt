package ecommerce.services

import ecommerce.exception.PaymentFailedException
import ecommerce.infrastructure.StripeClient
import ecommerce.model.StripePaymentIntentResponse
import ecommerce.model.StripePaymentRequest
import ecommerce.util.logger
import org.springframework.stereotype.Service

@Service
class PaymentService(
    private val stripeClient: StripeClient
) {
    private val log = logger<PaymentService>()

    fun processPayment(
        amountInCents: Long,
        currency: String,
        paymentMethodId: String
    ): StripePaymentIntentResponse {
        val stripeRequest = StripePaymentRequest(
            amount = amountInCents,
            currency = currency,
            paymentMethod = paymentMethodId
        )

        log.info("Attempting payment of {} {} via Stripe", amountInCents, currency)

        try {
            val response = stripeClient.createPaymentIntent(stripeRequest)
            log.info("Payment successful for payment method {}", paymentMethodId)
            return response ?: throw PaymentFailedException("Stripe returned an empty response.")
        } catch (e: IllegalArgumentException) {
            log.warn("Payment failed for payment method {}: {}", paymentMethodId, e.message)
            throw PaymentFailedException(e.message ?: "Payment failed due to an unknown error.")
        }
    }
}
