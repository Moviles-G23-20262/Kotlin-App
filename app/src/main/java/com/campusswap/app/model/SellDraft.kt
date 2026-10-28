package com.campusswap.app.model

import com.campusswap.app.model.domain.InputValidation

/** Borrador de una publicación mientras se llena el formulario de venta. */
data class SellDraft(
    /** Rutas de las fotos guardadas por ListingPhotoStore*/
    val photoPaths: List<String> = emptyList(),
    val title: String = "",
    val description: String = "",
    val category: Category = Category.TEXTBOOKS,
    val price: String = "",
    val course: Course? = null,
    val notAssociatedWithCourse: Boolean = false,
    val condition: Condition? = null,
) {
    val photoCount: Int get() = photoPaths.size
    val isProductInfoValid: Boolean
        get() = photoCount > 0 &&
            InputValidation.titleError(title) == null &&
            InputValidation.descriptionError(description) == null &&
            InputValidation.priceError(price) == null

    val isDirty: Boolean
        get() = title.isNotBlank() || description.isNotBlank() || photoCount > 0 ||
            course != null || condition != null || price.isNotBlank()

    val isCourseConditionValid: Boolean
        get() = (notAssociatedWithCourse || course != null) && condition != null
}
