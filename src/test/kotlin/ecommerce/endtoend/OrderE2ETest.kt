package ecommerce.endtoend

import ecommerce.config.DatabaseSeeder
import ecommerce.entities.Member
import ecommerce.entities.Option
import ecommerce.infrastructure.StripeClient
import ecommerce.model.OrderResponseDTO
import ecommerce.model.PaymentRequestDTO
import ecommerce.model.StripePaymentIntentResponse
import ecommerce.model.StripePaymentRequest
import ecommerce.repositories.CartItemRepository
import ecommerce.repositories.MemberRepository
import ecommerce.repositories.OptionRepository
import ecommerce.repositories.OrderRepository
import io.restassured.RestAssured
import io.restassured.common.mapper.TypeRef
import io.restassured.http.ContentType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpStatus
import org.springframework.test.annotation.DirtiesContext

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class OrderE2ETest {

    @Autowired
    private lateinit var stripeClient: StripeClient

    @TestConfiguration
    open class MockConfig {
        @Bean
        @Primary
        open fun mockStripeClient(): StripeClient {
            return mock(StripeClient::class.java)
        }
    }

    @Autowired
    private lateinit var databaseSeeder: DatabaseSeeder
    @Autowired
    private lateinit var optionRepository: OptionRepository
    @Autowired
    private lateinit var memberRepository: MemberRepository
    @Autowired
    private lateinit var orderRepository: OrderRepository
    @Autowired
    private lateinit var cartItemRepository: CartItemRepository

    private lateinit var token: String
    private lateinit var member: Member
    private lateinit var optionToPurchase: Option

    @AfterEach
    fun cleanup() {
        databaseSeeder.cleanup()
        databaseSeeder.seed()
    }

    @BeforeEach
    fun setup() {
        val loginPayload = mapOf("email" to "user1@example.com", "password" to "pass")
        val response =
            RestAssured.given()
                .contentType(ContentType.JSON)
                .body(loginPayload)
                .post("/api/members/login")
                .then().extract()

        token = response.body().jsonPath().getString("accessToken")
        member = memberRepository.findByEmail("user1@example.com")!!
        optionToPurchase = optionRepository.findAll().find { it.product?.name == "Car" && it.name == "Red Color" }!!
    }

    @Test
    fun `should create order and return details on successful payment`() {
        val initialStock = optionToPurchase.quantity
        val request = PaymentRequestDTO(
            optionId = optionToPurchase.id!!,
            quantity = 1,
            paymentMethod = "pm_card_visa",
        )

        val mockStripeResponse = StripePaymentIntentResponse(id = "pi_mock_success_123")
        given(stripeClient.createPaymentIntent(any<StripePaymentRequest>())).willReturn(mockStripeResponse)

        val orderResponse =
            RestAssured.given()
                .header("Authorization", "Bearer $token")
                .contentType(ContentType.JSON)
                .body(request)
                .post("/api/orders")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract().`as`(OrderResponseDTO::class.java)


        assertThat(orderResponse.orderId).isNotNull()
        assertThat(orderResponse.stripePaymentId).isEqualTo("pi_mock_success_123")
        val updatedOption = optionRepository.findById(optionToPurchase.id!!).get()
        assertThat(updatedOption.quantity).isEqualTo(initialStock - 1)
    }

    @Test
    fun `should not create an order when payment fails`() {
        val initialOrderCount = orderRepository.count()
        val request = PaymentRequestDTO(
            optionId = optionToPurchase.id!!,
            quantity = 1,
            paymentMethod = "pm_card_visa_chargeDeclined",
        )

        given(stripeClient.createPaymentIntent(any<StripePaymentRequest>()))
            .willThrow(IllegalArgumentException("Your card was declined."))

        RestAssured.given()
            .header("Authorization", "Bearer $token")
            .contentType(ContentType.JSON)
            .body(request)
            .post("/api/orders")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())

        assertThat(orderRepository.count()).isEqualTo(initialOrderCount)
    }

    @Test
    fun `should return 409 conflict when stock is insufficient`() {
        val initialStock = optionToPurchase.quantity
        val requestedQuantity = (initialStock + 1).toInt()
        val initialOrderCount = orderRepository.count()
        val request = PaymentRequestDTO(
            optionId = optionToPurchase.id!!,
            quantity = requestedQuantity.toLong(),
            paymentMethod = "pm_card_visa",
        )

        val errorResponse =
            RestAssured.given()
                .header("Authorization", "Bearer $token")
                .contentType(ContentType.JSON)
                .body(request)
                .post("/api/orders")
                .then()
                .statusCode(HttpStatus.CONFLICT.value())
                .extract().body().jsonPath()

        assertThat(errorResponse.getString("message")).isEqualTo("Not enough stock")
        val updatedOption = optionRepository.findById(optionToPurchase.id!!).get()
        assertThat(updatedOption.quantity).isEqualTo(initialStock)
        assertThat(orderRepository.count()).isEqualTo(initialOrderCount)
    }

    @Test
    fun `should return all orders for the authenticated user`() {
        // Arrange: Create an order for the user first.
        val creationRequest = PaymentRequestDTO(
            optionId = optionToPurchase.id!!,
            quantity = 1,
            paymentMethod = "pm_card_visa",
        )

        val mockStripeResponse = StripePaymentIntentResponse(id = "pi_mock_another_test")
        given(stripeClient.createPaymentIntent(any<StripePaymentRequest>())).willReturn(mockStripeResponse)

        RestAssured.given()
            .header("Authorization", "Bearer $token")
            .contentType(ContentType.JSON)
            .body(creationRequest)
            .post("/api/orders")
            .then()
            .statusCode(HttpStatus.OK.value())

        val orders =
            RestAssured.given()
                .header("Authorization", "Bearer $token")
                .get("/api/orders")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract().body().`as`(object : TypeRef<List<OrderResponseDTO>>() {})

        assertThat(orders).hasSize(1)
        val fetchedOrder = orders[0]
        assertThat(fetchedOrder.totalAmount).isEqualTo(optionToPurchase.product!!.price)
        assertThat(fetchedOrder.items[0].productName).isEqualTo("Car")
        assertThat(fetchedOrder.stripePaymentId).isEqualTo("pi_mock_another_test")
    }
}
