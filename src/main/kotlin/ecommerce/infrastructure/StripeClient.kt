package ecommerce.infrastructure

import ecommerce.config.StripeProperties
import ecommerce.model.StripePaymentIntentResponse
import ecommerce.model.StripePaymentRequest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestClientResponseException

@Component
class StripeClient(
    private val stripeProperties: StripeProperties,
) {
    private val restClient = RestClient.create()

    fun createPaymentIntent(req: StripePaymentRequest): StripePaymentIntentResponse {
        require(req.amount > 0) { "Amount must be positive." }
        require(req.currency.isNotBlank()) { "Currency must not be blank." }
        require(req.paymentMethod.isNotBlank()) { "Payment method must not be blank." }

        val body =
            listOf(
                "amount=${req.amount}",
                "currency=${req.currency}",
                "payment_method=${req.paymentMethod}",
                "confirm=true",
                "automatic_payment_methods[enabled]=true",
                "automatic_payment_methods[allow_redirects]=never",
            ).joinToString("&")

        try {
            val response = restClient.post()
                .uri("https://api.stripe.com/v1/payment_intents")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${stripeProperties.secretKey}")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .toEntity(StripePaymentIntentResponse::class.java)

            return response.body!!
        } catch (e: RestClientException) {
            val errorBody = if (e is RestClientResponseException) {
                e.responseBodyAsString
            } else {
                e.message
            }
            throw IllegalArgumentException("Stripe API error: $errorBody")
        }
    }
}
