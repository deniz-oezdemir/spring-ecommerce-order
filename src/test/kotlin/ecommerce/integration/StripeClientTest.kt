package ecommerce.integration

import ecommerce.infrastructure.StripeClient
import ecommerce.model.StripePaymentRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class StripeClientTest {

    @Autowired
    private lateinit var stripeClient: StripeClient

    @Test
    fun `should return a successful payment intent from Stripe`() {
        val request =
            StripePaymentRequest(
                amount = 1000,
                currency = "usd",
                paymentMethod = "pm_card_visa",
            )

        val responseJson = stripeClient.createPaymentIntent(request)

        assertThat(responseJson).isNotNull()
        assertThat(responseJson?.id).startsWith("pi_")
    }

    @Test
    fun `should throw an exception for a declined card`() {
        val request =
            StripePaymentRequest(
                amount = 1000,
                currency = "usd",
                paymentMethod = "pm_card_visa_chargeDeclined",
            )

        val exception =
            assertThrows<IllegalArgumentException> {
                stripeClient.createPaymentIntent(request)
            }

        assertThat(exception.message).contains("Your card was declined.")
    }

    @Test
    fun `should throw exception when amount is not positive`() {
        val request = StripePaymentRequest(
            amount = 0,
            currency = "usd",
            paymentMethod = "pm_card_visa"
        )

        val exception = assertThrows<IllegalArgumentException> {
            stripeClient.createPaymentIntent(request)
        }
        assertThat(exception.message).isEqualTo("Amount must be positive.")
    }

    @Test
    fun `should throw exception when currency is blank`() {
        val request = StripePaymentRequest(
            amount = 1000,
            currency = "  ",
            paymentMethod = "pm_card_visa"
        )

        val exception = assertThrows<IllegalArgumentException> {
            stripeClient.createPaymentIntent(request)
        }
        assertThat(exception.message).isEqualTo("Currency must not be blank.")
    }

    @Test
    fun `should throw exception when payment method is blank`() {
        val request = StripePaymentRequest(
            amount = 1000,
            currency = "usd",
            paymentMethod = ""
        )

        val exception = assertThrows<IllegalArgumentException> {
            stripeClient.createPaymentIntent(request)
        }
        assertThat(exception.message).isEqualTo("Payment method must not be blank.")
    }

    @Test
    fun `should throw exception for insufficient funds`() {
        val request = StripePaymentRequest(
            amount = 1000,
            currency = "usd",
            paymentMethod = "pm_card_visa_chargeDeclined_insufficientFunds"
        )

        val exception = assertThrows<IllegalArgumentException> {
            stripeClient.createPaymentIntent(request)
        }

        assertThat(exception.message).contains("insufficientFunds")
    }

    @Test
    fun `should throw exception for a lost card`() {
        val request = StripePaymentRequest(
            amount = 1000,
            currency = "usd",
            paymentMethod = "pm_card_visa_chargeDeclined_lostCard"
        )

        val exception = assertThrows<IllegalArgumentException> {
            stripeClient.createPaymentIntent(request)
        }

        assertThat(exception.message).contains("lostCard")
    }

    @Test
    fun `should throw exception for an incorrect CVC`() {
        val request = StripePaymentRequest(
            amount = 1000,
            currency = "usd",
            paymentMethod = "pm_card_visa_chargeDeclined_incorrectCvc"
        )

        val exception = assertThrows<IllegalArgumentException> {
            stripeClient.createPaymentIntent(request)
        }

        assertThat(exception.message).contains("incorrectCvc")
    }
}
