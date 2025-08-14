package ecommerce.exception

class NotFoundException(message: String = "The requested resource was not found.") : RuntimeException(message)

class OperationFailedException(message: String = "The operation could not be completed.") : RuntimeException(message)

class AuthorizationException(message: String = "Authorization failed. Please check your credentials.") : RuntimeException(message)

class ForbiddenException(message: String = "You do not have permission to access this resource.") : RuntimeException(message)

class InvalidCartItemQuantityException(quantity: Int) :
    RuntimeException("Quantity must be non-negative, got $quantity")

class InvalidOptionNameException(message: String = "The provided option name is invalid.") : RuntimeException(message)

class InvalidOptionQuantityException(message: String = "The provided option quantity is invalid.") : RuntimeException(message)

class InsufficientStockException(message: String = "There is not enough stock to complete the order.") : RuntimeException(message)

class MissingProductIdException(message: String = "A Product ID is required for this operation.") : RuntimeException(message)

class PaymentFailedException(message: String = "Payment processing failed.") : RuntimeException(message)
