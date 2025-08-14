package ecommerce.advice

import ecommerce.exception.AuthorizationException
import ecommerce.exception.ForbiddenException
import ecommerce.exception.InsufficientStockException
import ecommerce.exception.InvalidCartItemQuantityException
import ecommerce.exception.InvalidOptionNameException
import ecommerce.exception.InvalidOptionQuantityException
import ecommerce.exception.MissingProductIdException
import ecommerce.exception.NotFoundException
import ecommerce.exception.OperationFailedException
import ecommerce.exception.PaymentFailedException
import ecommerce.util.logger
import org.springframework.dao.DataAccessException
import org.springframework.dao.DuplicateKeyException
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice(annotations = [RestController::class])
class ApiErrorControllerAdvice {
    private val log = logger<ApiErrorControllerAdvice>()

    private fun buildResponse(error: ApiError): ResponseEntity<ApiError> = ResponseEntity.status(error.status).body(error)

    @ExceptionHandler(NotFoundException::class, NoSuchElementException::class, EmptyResultDataAccessException::class)
    fun handleNotFoundExceptions(e: RuntimeException): ResponseEntity<ApiError> {
        log.warn("Resource not found", e)
        val apiError = ApiError.notFound(e.message)
        return buildResponse(apiError)
    }

    @ExceptionHandler(OperationFailedException::class, InvalidCartItemQuantityException::class, InvalidOptionNameException::class, InvalidOptionQuantityException::class, MissingProductIdException::class)
    fun handleBadRequestExceptions(e: RuntimeException): ResponseEntity<ApiError> {
        log.warn("Bad request", e)
        val apiError = ApiError.badRequest("Invalid Request", e.message)
        return buildResponse(apiError)
    }

    @ExceptionHandler(AuthorizationException::class)
    fun handleAuthorizationException(e: AuthorizationException): ResponseEntity<ApiError> {
        log.warn("Authorization failed", e)
        val apiError = ApiError.unauthorized(e.message)
        return buildResponse(apiError)
    }

    @ExceptionHandler(ForbiddenException::class)
    fun handleForbiddenException(e: ForbiddenException): ResponseEntity<ApiError> {
        log.warn("Access forbidden", e)
        val apiError = ApiError.forbidden(e.message)
        return buildResponse(apiError)
    }

    @ExceptionHandler(InsufficientStockException::class, DuplicateKeyException::class)
    fun handleConflictExceptions(e: RuntimeException): ResponseEntity<ApiError> {
        log.warn("Conflict detected", e)
        val apiError = ApiError.conflict("Conflict", e.message)
        return buildResponse(apiError)
    }

    @ExceptionHandler(DataAccessException::class)
    fun handleDataAccessException(e: DataAccessException): ResponseEntity<ApiError> {
        log.error("A database error occurred", e)
        val apiError = ApiError.internalError("Database Error", e.message)
        return buildResponse(apiError)
    }

    @ExceptionHandler(PaymentFailedException::class)
    fun handlePaymentFailedException(e: PaymentFailedException): ResponseEntity<ApiError> {
        log.warn("Payment processing failed", e)
        val apiError = ApiError.badRequest("Payment Failed", e.message)
        return buildResponse(apiError)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(e: MethodArgumentNotValidException): ResponseEntity<ApiError> {
        log.warn("Validation failed for request", e)
        val errors = e.bindingResult.fieldErrors.associate {
            it.field to (it.defaultMessage ?: "Validation error")
        }
        val apiError = ApiError.badRequest(
            error = "Validation failed",
            message = "One or more fields are invalid",
            details = errors,
        )
        return buildResponse(apiError)
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(e: Exception): ResponseEntity<ApiError> {
        log.error("An unhandled exception occurred", e)
        val apiError = ApiError.internalError("Internal Server Error", e.message)
        return buildResponse(apiError)
    }
}
