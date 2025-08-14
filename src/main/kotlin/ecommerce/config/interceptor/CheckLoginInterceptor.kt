package ecommerce.config.interceptor

import ecommerce.annotation.IgnoreCheckLogin
import ecommerce.entities.Member
import ecommerce.exception.AuthorizationException
import ecommerce.infrastructure.JwtTokenProvider
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor

@Component
class CheckLoginInterceptor(private val jwtTokenProvider: JwtTokenProvider) : HandlerInterceptor {
    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        if (request.method == HttpMethod.OPTIONS.name()) {
            return true
        }

        if (handler is HandlerMethod) {
            if (handler.hasMethodAnnotation(IgnoreCheckLogin::class.java) ||
                handler.beanType.isAnnotationPresent(IgnoreCheckLogin::class.java)
            ) {
                return true
            }
        }
        val token = extractToken(request)
        val (email, role) = checkAndExtractAdminCredentials(token)
        request.setAttribute("email", email)
        request.setAttribute("role", role)

        return true
    }

    private fun extractToken(request: HttpServletRequest): String {
        val header = request.getHeader("Authorization") ?: throw AuthorizationException("Unauthorized")
        return header.removePrefix("Bearer ").trim()
    }

    private fun checkAndExtractAdminCredentials(token: String): Pair<String, Member.Role> {
        if (!jwtTokenProvider.validateToken(token)) {
            throw AuthorizationException("Invalid or expired token")
        }
        return jwtTokenProvider.getPayload(token)
    }
}
