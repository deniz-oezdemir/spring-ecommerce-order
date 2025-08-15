package ecommerce.endtoend

import ecommerce.model.OptionDTO
import ecommerce.model.PageResponseDTO
import ecommerce.model.ProductRequestDTO
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
import org.springframework.test.annotation.DirtiesContext

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class ProductE2ETest {
    lateinit var token: String

    @BeforeEach
    fun loginAndGetToken() {
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
        assertThat(token).isNotBlank
    }

    @Test
    fun getProducts() {
        val response =
            RestAssured.given().log().all()
                .auth().oauth2(token)
                .accept(ContentType.JSON)
                .`when`().get("/api/products")
                .then().log().all().extract()

        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value())
        val page = response.body().`as`(object : TypeRef<PageResponseDTO<ProductResponseDTO>>() {})
        assertThat(page.content).isNotEmpty()
        assertThat(page.content.size).isEqualTo(10)
    }

    @Test
    fun getProduct() {
        val productDTO =
            ProductRequestDTO(
                id = null,
                name = "TV",
                price = 99.99,
                imageUrl = "https://example.com/speaker.jpg",
                options =
                    setOf(
                        OptionDTO(
                            id = null,
                            name = "hi",
                            quantity = 99,
                            productId = null,
                        ),
                    ),
            )
        val id =
            RestAssured.given()
                .auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body(productDTO)
                .post("/api/products")
                .then().extract().jsonPath().getLong("id")

        val response =
            RestAssured.given()
                .get("/api/products/$id")
                .then().log().all().extract()

        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value())
        assertThat(response.body().jsonPath().getString("name")).isEqualTo("TV")
        assertThat(response.body().jsonPath().getFloat("price")).isEqualTo(99.99f)
    }

    @Test
    fun getProduct_notFound() {
        val response =
            RestAssured.given()
                .auth().oauth2(token)
                .get("/api/products/999999")
                .then().log().all().extract()

        assertThat(response.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value())
    }

    @Test
    fun createProduct() {
        val newProductDTO =
            ProductRequestDTO(
                id = null,
                name = "Monitor",
                price = 150.0,
                imageUrl = "https://example.com/speaker.jpg",
                options =
                    setOf(
                        OptionDTO(
                            id = null,
                            name = "hi",
                            quantity = 99,
                            productId = null,
                        ),
                    ),
            )

        val response =
            RestAssured.given().log().all()
                .auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body(newProductDTO)
                .`when`().post("/api/products")
                .then().log().all().extract()

        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value())
        assertThat(response.body().jsonPath().getString("name")).isEqualTo("Monitor")
        assertThat(response.body().jsonPath().getFloat("price")).isEqualTo(150.0f)
    }

    @Test
    fun `Should return error when product name use invalid characters`() {
        val newProductDTO =
            ProductRequestDTO(
                id = null,
                name = "!@#$%^&*()_+}{",
                price = 99.99,
                imageUrl = "https://example.com/speaker.jpg",
                options =
                    setOf(
                        OptionDTO(
                            id = null,
                            name = "hi",
                            quantity = 99,
                            productId = null,
                        ),
                    ),
            )

        val response =
            RestAssured.given().log().all()
                .auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body(newProductDTO)
                .`when`().post("/api/products")
                .then().log().all().extract()

        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value())
        assertThat(response.body().jsonPath().getInt("status")).isEqualTo(400)
        assertThat(
            response.body().jsonPath().getString("details.name"),
        ).isEqualTo("Invalid characters in product name.")
    }

    @Test
    fun `Should return error when product name is bigger than 15 characters`() {
        val newProductDTO =
            ProductRequestDTO(
                id = null,
                name = "SpeakersareLovemyDearDearDear",
                price = 99.99,
                imageUrl = "https://example.com/speaker.jpg",
                options =
                    setOf(
                        OptionDTO(
                            id = null,
                            name = "hi",
                            quantity = 99,
                            productId = null,
                        ),
                    ),
            )

        val response =
            RestAssured.given().log().all()
                .auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body(newProductDTO)
                .`when`().post("/api/products")
                .then().log().all().extract()

        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value())
        assertThat(response.body().jsonPath().getInt("status")).isEqualTo(400)
        assertThat(
            response.body().jsonPath().getString("details.name"),
        ).isEqualTo("The product name must contain between 1 and 15 characters")
    }

    @Test
    fun `Should return error when product name already exists`() {
        val dto =
            ProductRequestDTO(
                id = null,
                name = "Speaker",
                price = 99.99,
                imageUrl = "https://example.com/speaker.jpg",
                options =
                    setOf(
                        OptionDTO(
                            id = null,
                            name = "hi",
                            quantity = 99,
                            productId = null,
                        ),
                    ),
            )

        RestAssured.given().auth().oauth2(token)
            .contentType(ContentType.JSON).body(dto).post("/api/products")

        val duplicate =
            RestAssured.given()
                .contentType(ContentType.JSON)
                .auth().oauth2(token)
                .body(dto)
                .post("/api/products")
                .then().extract()

        assertThat(duplicate.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value())
        assertThat(duplicate.body().jsonPath().getString("message")).contains("already exists")
    }

    @Test
    fun `Should return error when product price is negative value`() {
        val newProductDTO =
            ProductRequestDTO(
                id = null,
                name = "Speaker",
                price = -99.99,
                imageUrl = "https://example.com/speaker.jpg",
                options =
                    setOf(
                        OptionDTO(
                            id = null,
                            name = "hi",
                            quantity = 99,
                            productId = null,
                        ),
                    ),
            )

        val response =
            RestAssured.given().log().all()
                .auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body(newProductDTO)
                .`when`().post("/api/products")
                .then().log().all().extract()

        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value())
        assertThat(response.body().jsonPath().getInt("status")).isEqualTo(400)
        assertThat(
            response.body().jsonPath().getString("details.price"),
        ).contains("must be greater than zero")
    }

    @Test
    fun updateProduct() {
        val created =
            ProductRequestDTO(
                id = null,
                name = "Speaker",
                price = 99.99,
                imageUrl = "https://example.com/speaker.jpg",
                options =
                    setOf(
                        OptionDTO(
                            id = null,
                            name = "hi",
                            quantity = 99,
                            productId = null,
                        ),
                    ),
            )
        val id =
            RestAssured.given()
                .auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body(created)
                .post("/api/products")
                .then().extract().jsonPath().getLong("id")

        val updated =
            ProductRequestDTO(
                id = null,
                name = "Gaming Mouse",
                price = 45.0,
                imageUrl = "https://example.com/speaker.jpg",
                options =
                    setOf(
                        OptionDTO(
                            id = null,
                            name = "ello",
                            quantity = 99,
                            productId = null,
                        ),
                    ),
            )

        val response =
            RestAssured.given().log().all()
                .auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body(updated)
                .put("/api/products/$id")
                .then().log().all().extract()

        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value())
        assertThat(response.body().jsonPath().getString("name")).isEqualTo("Gaming Mouse")
        assertThat(response.body().jsonPath().getFloat("price")).isEqualTo(45.0f)
    }

    @Test
    fun patchProduct() {
        val created =
            ProductRequestDTO(
                id = null,
                name = "Tv",
                price = 99.99,
                imageUrl = "https://example.com/speaker.jpg",
                options =
                    setOf(
                        OptionDTO(
                            id = null,
                            name = "hi",
                            quantity = 99,
                            productId = null,
                        ),
                    ),
            )
        val id =
            RestAssured.given()
                .auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body(created)
                .post("/api/products")
                .then().extract().jsonPath().getLong("id")

        val patch = mapOf("price" to 249.0)

        val response =
            RestAssured.given().log().all()
                .auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body(patch)
                .patch("/api/products/$id")
                .then().log().all().extract()

        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value())
        assertThat(response.body().jsonPath().getFloat("price")).isEqualTo(249.0f)
    }

    @Test
    fun deleteProduct() {
        val created =
            ProductRequestDTO(
                id = null,
                name = "Toilet",
                price = 99.99,
                imageUrl = "https://example.com/speaker.jpg",
                options =
                    setOf(
                        OptionDTO(
                            id = null,
                            name = "hi",
                            quantity = 99,
                            productId = null,
                        ),
                    ),
            )
        val id =
            RestAssured.given()
                .auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body(created)
                .post("/api/products")
                .then().extract().jsonPath().getLong("id")

        val deleteResponse =
            RestAssured.given()
                .auth().oauth2(token)
                .delete("/api/products/$id")
                .then().log().all().extract()

        assertThat(deleteResponse.statusCode()).isEqualTo(HttpStatus.NO_CONTENT.value())

        val getResponse =
            RestAssured.given()
                .auth().oauth2(token)
                .get("/api/products/$id")
                .then().log().all().extract()

        assertThat(getResponse.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value())
    }

    @Test
    fun `getProductOptions returns all options for a specific product`() {
        val productId = 1L

        val options =
            RestAssured.given()
                .get("/api/products/$productId/options")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getList("", OptionDTO::class.java)

        assertThat(options).hasSize(5)
        assertThat(options.map { it.name }).contains("Red Color", "Blue Color", "Black Color")
    }
}
