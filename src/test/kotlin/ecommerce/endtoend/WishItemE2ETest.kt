package ecommerce.endtoend

import ecommerce.config.DatabaseSeeder
import ecommerce.model.PageResponseDTO
import ecommerce.model.ProductResponseDTO
import ecommerce.model.WishItemRequestDTO
import ecommerce.model.WishItemResponseDTO
import io.restassured.RestAssured
import io.restassured.common.mapper.TypeRef
import io.restassured.http.ContentType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import java.time.LocalDateTime

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class WishItemE2ETest {
    lateinit var token: String
    private val productId: Long = 1L
    private val request get() = WishItemRequestDTO(productId = productId)

    @Autowired
    private lateinit var databaseSeeder: DatabaseSeeder

    @AfterEach
    fun cleanup() {
        databaseSeeder.cleanup()
        databaseSeeder.seed()
    }

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
    fun `add wish item`() {
        val wishItem = addWishItemAndReturn()

        assertThat(wishItem.product).isInstanceOf(ProductResponseDTO::class.java)
        assertThat(wishItem.product.id).isEqualTo(productId)
        assertThat(wishItem.addedAt).isBefore(LocalDateTime.now().plusMinutes(1))
    }

    @Test
    fun `get wish items`() {
        addWishItemAndReturn()

        val items =
            RestAssured.given()
                .header("Authorization", "Bearer $token")
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .get("/api/wish-items")
                .then().log().all()
                .statusCode(HttpStatus.OK.value())
                .extract().body().`as`(object : TypeRef<PageResponseDTO<WishItemResponseDTO>>() {})

        assertThat(items.content).hasSize(1)

        val wishItem = items.content.first()
        with(wishItem) {
            assertThat(product.id).isEqualTo(productId)
            assertThat(addedAt).isBefore(LocalDateTime.now().plusMinutes(1))
            assertThat(addedAt).isAfter(LocalDateTime.now().minusDays(1))
        }
    }

    @Test
    fun `delete wish item`() {
        addWishItemAndReturn()

        RestAssured.given()
            .header("Authorization", "Bearer $token")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(request)
            .delete("/api/wish-items")
            .then().log().all()
            .statusCode(HttpStatus.NO_CONTENT.value())

        val items =
            RestAssured.given()
                .header("Authorization", "Bearer $token")
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .get("/api/wish-items")
                .then().extract().body().`as`(object : TypeRef<PageResponseDTO<ProductResponseDTO>>() {})

        assertThat(items.content).isEmpty()
    }

    private fun addWishItemAndReturn(): WishItemResponseDTO {
        return RestAssured.given()
            .header("Authorization", "Bearer $token")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(request)
            .post("/api/wish-items")
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .`as`(WishItemResponseDTO::class.java)
    }
}
