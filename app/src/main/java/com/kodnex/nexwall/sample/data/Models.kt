package com.kodnex.nexwall.sample.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Category(
    val id: Int,
    val name: String,
    val slug: String = "",
    @SerialName("cover_image_url") val coverImageUrl: String? = null,
    @SerialName("wallpaper_count") val wallpaperCount: Int = 0,
    @SerialName("is_premium") val isPremium: Boolean = false,
)

@Serializable
data class CategoryRef(
    val id: Int,
    val name: String,
    val slug: String = "",
    @SerialName("is_premium") val isPremium: Boolean = false,
)

@Serializable
data class Wallpaper(
    val id: Int,
    @SerialName("category_id") val categoryId: Int? = null,
    @SerialName("image_url") val imageUrl: String,
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null,
    @SerialName("is_premium") val isPremium: Boolean = false,
    /** "image" or "live". Live (video) wallpapers need the Ultra plan. */
    val type: String = "image",
    val resolution: String? = null,
    @SerialName("file_size") val fileSize: Long? = null,
    val category: CategoryRef? = null,
) {
    val previewUrl: String get() = thumbnailUrl ?: imageUrl
}

@Serializable
data class CategoriesResponse(
    val data: List<Category> = emptyList(),
    val plan: String? = null,
    @SerialName("remaining_requests_today") val remainingRequestsToday: Int? = null,
)

@Serializable
data class WallpapersResponse(
    val data: List<Wallpaper> = emptyList(),
    @SerialName("current_page") val currentPage: Int = 1,
    @SerialName("last_page") val lastPage: Int = 1,
    @SerialName("per_page") val perPage: Int = 0,
    val total: Int = 0,
    val plan: String? = null,
    @SerialName("remaining_requests_today") val remainingRequestsToday: Int? = null,
)
