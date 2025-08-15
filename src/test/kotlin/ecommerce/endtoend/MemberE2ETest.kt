package ecommerce.endtoend

import ecommerce.model.MemberDTO
import ecommerce.model.TokenResponseDTO
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class MemberE2ETest {
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
    fun createMember() {
        val accessToken =
            RestAssured
                .given().log().all()
                .body(MemberDTO(name = NAME, email = EMAIL, password = PASSWORD))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .`when`().post("/api/members/register")
                .then().log().all().extract().`as`(TokenResponseDTO::class.java).accessToken

        assertThat(accessToken).isNotNull()

        val member =
            RestAssured
                .given().log().all()
                .header("Authorization", "Bearer $accessToken")
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .`when`().get("/api/members/me/token")
                .then().log().all()
                .statusCode(HttpStatus.OK.value()).extract().`as`(MemberDTO::class.java)

        assertThat(member.email).isEqualTo(EMAIL)
    }

    companion object {
        private const val NAME = "Sebastian"
        private const val EMAIL = "email@email.com"
        private const val PASSWORD = "1234"
    }
}
