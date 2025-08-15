package ecommerce.endtoend

import ecommerce.model.CartItemRequestDTO
import ecommerce.model.CartItemResponseDTO
import ecommerce.model.PageResponseDTO
import ecommerce.model.ProductResponseDTO
import io.restassured.RestAssured
import io.restassured.common.mapper.TypeRef
import io.restassured.http.ContentType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import java.time.LocalDateTime

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class CartItemE2ETest {
    lateinit var token: String
    private val productId: Long = 1L
    private val request get() = CartItemRequestDTO(productId = productId, quantity = 2)

    @BeforeEach
    fun setup() {
        val loginPayload =
            mapOf(
                "email" to "sebas@sebas.com",
                "password" to "123456",
            )

        val response =
            RestAssured.given()
                .contentType(ContentType.JSON)
                .body(loginPayload)
                .post("/api/members/login")
                .then().extract()

        token = response.body().jsonPath().getString("accessToken")
        assertThat(token).isNotBlank()
    }

    @Test
    fun `add cart item`() {
        val cartItem = addCartItemAndReturn()

        assertThat(cartItem.product).isInstanceOf(ProductResponseDTO::class.java)
        assertThat(cartItem.product.id).isEqualTo(productId)
        assertThat(cartItem.quantity).isEqualTo(2)
        assertThat(cartItem.addedAt).isBefore(LocalDateTime.now().plusMinutes(1))
    }

    @Test
    fun `get cart items`() {
        val addedItem = addCartItemAndReturn()

        val items =
            RestAssured.given()
                .header("Authorization", "Bearer $token")
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .get("/api/carts")
                .then().log().all()
                .statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getList("", CartItemResponseDTO::class.java)

        assertThat(items).hasSize(1)

        val cartItem = items.first()
        with(cartItem) {
            assertThat(quantity).isEqualTo(addedItem.quantity)
            assertThat(product.id).isEqualTo(productId)
            assertThat(addedAt).isBefore(LocalDateTime.now().plusMinutes(1))
            assertThat(addedAt).isAfter(LocalDateTime.now().minusDays(1))
        }
    }

    @Test
    fun `delete cart item`() {
//        addCartItem()
        val cartItem = addCartItemAndReturn()
        val products =
            RestAssured.given()
                .header("Authorization", "Bearer $token")
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .get("/api/products")
                .then().log().all()
                .statusCode(HttpStatus.OK.value())
                .extract().body().`as`(object : TypeRef<PageResponseDTO<ProductResponseDTO>>() {})

        RestAssured.given()
            .header("Authorization", "Bearer $token")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(request)
            .delete("/api/carts")
            .then().log().all()
            .statusCode(HttpStatus.NO_CONTENT.value())

        val items =
            RestAssured.given()
                .header("Authorization", "Bearer $token")
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .get("/api/carts")
                .then().extract().body().jsonPath().getList("", CartItemResponseDTO::class.java)

        assertThat(items).isEmpty()
    }

    private fun addCartItemAndReturn(): CartItemResponseDTO {
        return RestAssured.given()
            .header("Authorization", "Bearer $token")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(request)
            .post("/api/carts")
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .`as`(CartItemResponseDTO::class.java)
    }
}
