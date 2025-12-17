package de.dkb.api.codeChallenge.infrastructure.persistence.mapper

import de.dkb.api.codeChallenge.domain.model.NotificationCategory
import de.dkb.api.codeChallenge.domain.model.NotificationType
import de.dkb.api.codeChallenge.domain.model.valueobject.CategoryId
import de.dkb.api.codeChallenge.infrastructure.persistence.jpa.entity.NotificationCategoryEntity
import de.dkb.api.codeChallenge.infrastructure.persistence.jpa.entity.NotificationTypeEntity

/**
 * Mapper for converting between NotificationCategory domain model and JPA entity
 */
object NotificationCategoryMapper {

    fun toDomain(entity: NotificationCategoryEntity): NotificationCategory = NotificationCategory(
        id = CategoryId(entity.id),
        name = entity.name,
        types = entity.types.map { typeEntity ->
            NotificationType(
                code = typeEntity.code,
                categoryId = CategoryId(entity.id),
                addedAt = typeEntity.addedAt,
            )
        }.toSet(),
    )

    fun toDomainList(entities: List<NotificationCategoryEntity>): List<NotificationCategory> = entities.map { toDomain(it) }

    fun toEntity(domain: NotificationCategory): NotificationCategoryEntity {
        val entity = NotificationCategoryEntity(
            id = domain.id.value,
            name = domain.name,
        )

        entity.types = domain.types.map { type ->
            val typeEntity = NotificationTypeEntity(
                code = type.code,
                addedAt = type.addedAt,
            )
            typeEntity.category = entity
            typeEntity
        }.toMutableSet()

        return entity
    }
}
