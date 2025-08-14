package ecommerce.model

import ecommerce.util.ValidationMessages
import jakarta.validation.constraints.Positive

class CartItemRequestDTO(
    val productId: Long,
    @field:Positive(message = ValidationMessages.QUANTITY_POSITIVE)
    val quantity: Int,
)
