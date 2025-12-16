package de.dkb.api.codeChallenge.domain.model.valueobject

/**
 * Value object representing a notification category identifier.
 * Examples: "CATEGORY_A", "CATEGORY_B"
 */
data class CategoryId(val value: String) {
    init {
        require(value.isNotBlank()) { "CategoryId cannot be blank" }
    }

    override fun toString(): String = value
}
