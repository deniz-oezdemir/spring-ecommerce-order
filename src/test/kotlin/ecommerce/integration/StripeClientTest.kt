package ecommerce.integration

import ecommerce.config.StripeProperties
import ecommerce.infrastructure.StripeClient
import ecommerce.model.StripePaymentIntentResponse
import ecommerce.model.StripePaymentRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException

@ExtendWith(MockitoExtension::class)
class StripeClientTest {

    @Mock
    private lateinit var mockRestClient: RestClient
    @Mock
    private lateinit var mockRequestBodyUriSpec: RestClient.RequestBodyUriSpec
    @Mock
    private lateinit var mockRequestBodySpec: RestClient.RequestBodySpec
    @Mock
    private lateinit var mockResponseSpec: RestClient.ResponseSpec

    private lateinit var mockedRestClientStatic: MockedStatic<RestClient>
    private lateinit var stripeClient: StripeClient

    @BeforeEach
    fun setup() {
        mockedRestClientStatic = Mockito.mockStatic(RestClient::class.java)
        mockedRestClientStatic.`when`<RestClient> { RestClient.create() }.thenReturn(mockRestClient)

        val stripeProperties = StripeProperties(secretKey = "test_key")
        stripeClient = StripeClient(stripeProperties)
    }

    @AfterEach
    fun teardown() {
        mockedRestClientStatic.close()
    }

    @Test
    fun `should return a successful payment intent from Stripe`() {
        val request = StripePaymentRequest(amount = 1000, currency = "usd", paymentMethod = "pm_card_visa")
        val mockResponseEntity = ResponseEntity.ok(StripePaymentIntentResponse(id = "pi_mock_123"))

        // Manually define the behavior for each step in the fluent API chain
        whenever(mockRestClient.post()).thenReturn(mockRequestBodyUriSpec)
        whenever(mockRequestBodyUriSpec.uri(any<String>())).thenReturn(mockRequestBodySpec)
        whenever(mockRequestBodySpec.header(any(), any())).thenReturn(mockRequestBodySpec)
        whenever(mockRequestBodySpec.contentType(any())).thenReturn(mockRequestBodySpec)
        whenever(mockRequestBodySpec.body(any<String>())).thenReturn(mockRequestBodySpec)
        whenever(mockRequestBodySpec.retrieve()).thenReturn(mockResponseSpec)
        whenever(mockResponseSpec.toEntity(StripePaymentIntentResponse::class.java)).thenReturn(mockResponseEntity)

        val response = stripeClient.createPaymentIntent(request)

        assertThat(response).isNotNull()
        assertThat(response?.id).startsWith("pi_")
    }

    @Test
    fun `should throw an exception for a declined card`() {
        val request = StripePaymentRequest(amount = 1000, currency = "usd", paymentMethod = "pm_card_visa_chargeDeclined")
        val errorJson = """{"error": {"message": "Your card was declined."}}"""
        val mockException = Mockito.mock(RestClientResponseException::class.java)
        whenever(mockException.responseBodyAsString).thenReturn(errorJson)

        whenever(mockRestClient.post()).thenReturn(mockRequestBodyUriSpec)
        whenever(mockRequestBodyUriSpec.uri(any<String>())).thenReturn(mockRequestBodySpec)
        whenever(mockRequestBodySpec.header(any(), any())).thenReturn(mockRequestBodySpec)
        whenever(mockRequestBodySpec.contentType(any())).thenReturn(mockRequestBodySpec)
        whenever(mockRequestBodySpec.body(any<String>())).thenReturn(mockRequestBodySpec)
        whenever(mockRequestBodySpec.retrieve()).thenThrow(mockException)

        val exception = assertThrows<IllegalArgumentException> {
            stripeClient.createPaymentIntent(request)
        }
        assertThat(exception.message).contains("Your card was declined.")
    }

    @Test
    fun `should throw exception when amount is not positive`() {
        val request = StripePaymentRequest(amount = 0, currency = "usd", paymentMethod = "pm_card_visa")
        val exception = assertThrows<IllegalArgumentException> { stripeClient.createPaymentIntent(request) }
        assertThat(exception.message).isEqualTo("Amount must be positive.")
    }

    @Test
    fun `should throw exception when currency is blank`() {
        val request = StripePaymentRequest(amount = 1000, currency = "  ", paymentMethod = "pm_card_visa")
        val exception = assertThrows<IllegalArgumentException> { stripeClient.createPaymentIntent(request) }
        assertThat(exception.message).isEqualTo("Currency must not be blank.")
    }

    @Test
    fun `should throw exception when payment method is blank`() {
        val request = StripePaymentRequest(amount = 1000, currency = "usd", paymentMethod = "")
        val exception = assertThrows<IllegalArgumentException> { stripeClient.createPaymentIntent(request) }
        assertThat(exception.message).isEqualTo("Payment method must not be blank.")
    }

    @Test
    fun `should throw exception for insufficient funds`() {
        val request = StripePaymentRequest(amount = 1000, currency = "usd", paymentMethod = "pm_card_visa_chargeDeclined_insufficientFunds")
        val errorJson = """{"error": {"decline_code": "insufficient_funds"}}"""
        val mockException = Mockito.mock(RestClientResponseException::class.java)
        whenever(mockException.responseBodyAsString).thenReturn(errorJson)

        whenever(mockRestClient.post()).thenReturn(mockRequestBodyUriSpec)
        whenever(mockRequestBodyUriSpec.uri(any<String>())).thenReturn(mockRequestBodySpec)
        whenever(mockRequestBodySpec.header(any(), any())).thenReturn(mockRequestBodySpec)
        whenever(mockRequestBodySpec.contentType(any())).thenReturn(mockRequestBodySpec)
        whenever(mockRequestBodySpec.body(any<String>())).thenReturn(mockRequestBodySpec)
        whenever(mockRequestBodySpec.retrieve()).thenThrow(mockException)

        val exception = assertThrows<IllegalArgumentException> {
            stripeClient.createPaymentIntent(request)
        }
        assertThat(exception.message).contains("insufficient_funds")
    }

    @Test
    fun `should throw exception for a lost card`() {
        val request = StripePaymentRequest(amount = 1000, currency = "usd", paymentMethod = "pm_card_visa_chargeDeclined_lostCard")
        val errorJson = """{"error": {"decline_code": "lost_card"}}"""
        val mockException = Mockito.mock(RestClientResponseException::class.java)
        whenever(mockException.responseBodyAsString).thenReturn(errorJson)

        whenever(mockRestClient.post()).thenReturn(mockRequestBodyUriSpec)
        whenever(mockRequestBodyUriSpec.uri(any<String>())).thenReturn(mockRequestBodySpec)
        whenever(mockRequestBodySpec.header(any(), any())).thenReturn(mockRequestBodySpec)
        whenever(mockRequestBodySpec.contentType(any())).thenReturn(mockRequestBodySpec)
        whenever(mockRequestBodySpec.body(any<String>())).thenReturn(mockRequestBodySpec)
        whenever(mockRequestBodySpec.retrieve()).thenThrow(mockException)

        val exception = assertThrows<IllegalArgumentException> {
            stripeClient.createPaymentIntent(request)
        }
        assertThat(exception.message).contains("lost_card")
    }

    @Test
    fun `should throw exception for an incorrect CVC`() {
        val request = StripePaymentRequest(amount = 1000, currency = "usd", paymentMethod = "pm_card_visa_chargeDeclined_incorrectCvc")
        val errorJson = """{"error": {"decline_code": "incorrect_cvc"}}"""
        val mockException = Mockito.mock(RestClientResponseException::class.java)
        whenever(mockException.responseBodyAsString).thenReturn(errorJson)

        whenever(mockRestClient.post()).thenReturn(mockRequestBodyUriSpec)
        whenever(mockRequestBodyUriSpec.uri(any<String>())).thenReturn(mockRequestBodySpec)
        whenever(mockRequestBodySpec.header(any(), any())).thenReturn(mockRequestBodySpec)
        whenever(mockRequestBodySpec.contentType(any())).thenReturn(mockRequestBodySpec)
        whenever(mockRequestBodySpec.body(any<String>())).thenReturn(mockRequestBodySpec)
        whenever(mockRequestBodySpec.retrieve()).thenThrow(mockException)

        val exception = assertThrows<IllegalArgumentException> {
            stripeClient.createPaymentIntent(request)
        }
        assertThat(exception.message).contains("incorrect_cvc")
    }
}
