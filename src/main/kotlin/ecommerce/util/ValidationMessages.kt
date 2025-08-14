package ecommerce.util

object ValidationMessages {
    const val NAME_REQUIRED = "Product name cannot be blank"
    const val PRICE_REQUIRED = "Price cannot be null"
    const val PRODUCT_NAME_SIZE = "The product name must contain between 1 and 15 characters"
    const val NAME_PATTERN = "Invalid characters in product name."
    const val PRICE_POSITIVE = "Price must be greater than zero"
    const val IMAGE_REQUIRED = "Image URL cannot be blank"
    const val IMAGE_FORMAT = "Invalid imageUrl, should start with http:// or https://"
    const val EMAIL_BLANK = "Email cannot be blank"
    const val EMAIL_INVALID = "Email format is invalid"
    const val PASSWORD_BLANK = "Password cannot be blank"
    const val MEMBER_NAME_REQUIRED = "Name cannot be blank"
    const val OPTION_REQUIRED = "Option must be at least 1"
    const val OPTION_NAME_SIZE = "The Option name must contain between 1 and 50 characters"
    const val OPTION_NAME_REQUIRED = "Product name cannot be blank"
    const val OPTION_NAME_PATTERN = "Invalid characters in product name."
    const val OPTION_QUANTITY_SIZE = "The Option quantity must be between 1 and 99,999,999"
    const val OPTION_PRODUCT_ID_REQUIRED = "Product ID cannot be blank"
    const val QUANTITY_POSITIVE = "Quantity must be a positive number"
}
