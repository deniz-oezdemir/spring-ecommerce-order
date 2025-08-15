package ecommerce.endtoend

import ecommerce.config.DatabaseSeeder
import ecommerce.model.ActiveMemberDTO
import ecommerce.model.OptionDTO
import ecommerce.model.ProductRequestDTO
import ecommerce.model.ProductResponseDTO
import ecommerce.model.TopProductDTO
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import java.time.LocalDateTime
import kotlin.jvm.java

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class AdminE2ETest {
    lateinit var token: String
    lateinit var product: ProductResponseDTO

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

        product =
            RestAssured.given()
                .auth().oauth2(token)
                .contentType(ContentType.JSON)
                .get("/api/products/2")
                .then().log().all()
                .extract().`as`(ProductResponseDTO::class.java)
    }

    @Test
    @DisplayName("GET /admin/top-products returns 200 and product list")
    fun getTopProducts() {
        val products =
            RestAssured.given()
                .header("Authorization", "Bearer $token")
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .get("/admin/top-products")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .body()
                .jsonPath()
                .getList("$.data", TopProductDTO::class.java)

        assertThat(products).isNotNull()
        assertThat(products).allSatisfy {
            assertThat(it.name).isNotBlank()
            assertThat(it.count).isGreaterThan(0)
            assertThat(it.mostRecentAddedAt).isBefore(LocalDateTime.now().plusMinutes(1))
        }
    }

    @Test
    @DisplayName("GET /admin/active-members returns 200 and member list")
    fun getActiveMembers() {
        val members =
            RestAssured.given()
                .header("Authorization", "Bearer $token")
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .get("/admin/active-members")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .body()
                .jsonPath()
                .getList("", ActiveMemberDTO::class.java)

        assertThat(members).isNotEmpty
        assertThat(members).allSatisfy {
            assertThat(it.id).isPositive()
            assertThat(it.email).contains("@")
        }
    }

    @Test
    @DisplayName("GET /admin/top-products fails for non-admin token")
    fun getTopProductsUnauthorized() {
        val nonAdminToken = loginAsUser()

        RestAssured.given()
            .header("Authorization", "Bearer $nonAdminToken")
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .get("/admin/top-products")
            .then()
            .statusCode(HttpStatus.FORBIDDEN.value())
    }

    private fun loginAsUser(): String {
        val loginPayload =
            mapOf(
                "email" to "user1@example.com",
                "password" to "pass",
            )

        val response =
            RestAssured.given()
                .contentType(ContentType.JSON)
                .body(loginPayload)
                .post("/api/members/login")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()

        return response.body().jsonPath().getString("accessToken")
    }

    @Test
    fun `Should create new option for existing product`() {
        // Create option for the product
        val optionDTO =
            OptionDTO(
                id = null,
                name = "Test Option",
                quantity = 10,
                productId = product.id,
            )

        val response =
            RestAssured.given()
                .auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body(optionDTO)
                .post("/admin/options")
                .then().log().all()

        // Check status code
        response.statusCode(HttpStatus.OK.value())
    }

    @Test
    fun `Should return error when option name exceeds 50 characters`() {
        val longName = "a".repeat(51)
        val optionDTO =
            OptionDTO(
                id = null,
                name = longName,
                quantity = 10,
                productId = product.id,
            )

        val response =
            RestAssured.given()
                .auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body(optionDTO)
                .post("/admin/options")
                .then().log().all()
                .extract()

        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value())
    }

    @Test
    fun `Should return error when option quantity is less than 1`() {
        val optionDTO =
            OptionDTO(
                id = null,
                name = "Test Option",
                quantity = 0,
                productId = product.id,
            )

        val response =
            RestAssured.given()
                .auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body(optionDTO)
                .post("/admin/options")
                .then().log().all()
                .extract()

        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value())
    }

    @Test
    fun `Should return error when option quantity is greater than or equal to 100_000_000`() {
        val optionDTO =
            OptionDTO(
                id = null,
                name = "Test Option",
                quantity = 100_000_000,
                productId = product.id,
            )

        val response =
            RestAssured.given()
                .auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body(optionDTO)
                .post("/admin/options")
                .then().log().all()
                .extract()

        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value())
        assertThat(response.body().jsonPath().getString("message"))
            .contains("must be between 1 and 99999999")
    }

    @Test
    fun `Should return error when creating duplicate option name for same product`() {
        val product =
            RestAssured.given()
                .auth().oauth2(token)
                .contentType(ContentType.JSON)
                .get("/api/products/2")
                .then().log().all()
                .extract().`as`(ProductResponseDTO::class.java)

        val optionDTO =
            OptionDTO(
                id = null,
                name = "Test Option",
                quantity = 10,
                productId = product.id,
            )

        // Create first option
        RestAssured.given()
            .auth().oauth2(token)
            .contentType(ContentType.JSON)
            .body(optionDTO)
            .post("/admin/options")

        // Try to create duplicate option
        val response =
            RestAssured.given()
                .auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body(optionDTO)
                .post("/admin/options")
                .then().log().all()
                .extract()

        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value())
        assertThat(response.body().jsonPath().getString("message"))
            .contains("Option with name 'Test Option' already exists")
    }

    @Test
    fun `Should return error when creating option without productId`() {
        val optionDTO =
            OptionDTO(
                id = null,
                name = "Orphan Option",
                quantity = 5,
                productId = null,
            )

        val response =
            RestAssured.given()
                .auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body(optionDTO)
                .post("/admin/options")
                .then().log().all()
                .extract()

        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value())
        assertThat(response.body().jsonPath().getString("message"))
            .containsIgnoringCase("productId is required")
    }

    private fun createTestProduct(): Long {
        val productDTO =
            ProductRequestDTO(
                name = "Test Product",
                price = 99.99,
                imageUrl = "http://example.com/image.jpg",
                options =
                    setOf(
                        OptionDTO(
                            name = "Deniz",
                            quantity = 1,
                            productId = null,
                            id = null,
                        ),
                    ),
            )

        return RestAssured.given()
            .auth().oauth2(token)
            .contentType(ContentType.JSON)
            .body(productDTO)
            .post("/api/products")
            .then().extract().jsonPath().getLong("id")
    }
}
