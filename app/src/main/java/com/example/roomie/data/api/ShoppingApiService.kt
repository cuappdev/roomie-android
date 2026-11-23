package com.example.roomie.data.api

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ShoppingApiService {

    /** The whole Shopping screen: Needed, Bought, and the "Split $X as a bill" total. */
    @GET("houses/{houseId}/shopping-items")
    suspend fun getShoppingList(
        @Path("houseId") houseId: Int,
        @Query("category") category: ShoppingCategory? = null,
        @Query("status") status: ShoppingStatus? = null,
    ): ShoppingListResponse

    @POST("houses/{houseId}/shopping-items")
    suspend fun addItem(@Path("houseId") houseId: Int, @Body body: AddShoppingItemBody): ShoppingItemResponse

    /** Fails once the item is on a bill. */
    @PATCH("houses/{houseId}/shopping-items/{itemId}")
    suspend fun updateItem(
        @Path("houseId") houseId: Int,
        @Path("itemId") itemId: Int,
        @Body body: UpdateShoppingItemBody,
    ): ShoppingItemResponse

    @DELETE("houses/{houseId}/shopping-items/{itemId}")
    suspend fun deleteItem(@Path("houseId") houseId: Int, @Path("itemId") itemId: Int)

    /** "I'll get it". */
    @POST("houses/{houseId}/shopping-items/{itemId}/claim")
    suspend fun claimItem(@Path("houseId") houseId: Int, @Path("itemId") itemId: Int): ShoppingItemResponse

    @DELETE("houses/{houseId}/shopping-items/{itemId}/claim")
    suspend fun unclaimItem(@Path("houseId") houseId: Int, @Path("itemId") itemId: Int): ShoppingItemResponse

    /** Marks the item bought and clears its claim. The price can be added later with [updateItem]. */
    @POST("houses/{houseId}/shopping-items/{itemId}/buy")
    suspend fun buyItem(
        @Path("houseId") houseId: Int,
        @Path("itemId") itemId: Int,
        @Body body: BuyItemBody,
    ): ShoppingItemResponse

    /** Also clears the price. */
    @DELETE("houses/{houseId}/shopping-items/{itemId}/buy")
    suspend fun unbuyItem(@Path("houseId") houseId: Int, @Path("itemId") itemId: Int): ShoppingItemResponse
}

data class ShoppingListResponse(
    val needed: List<ShoppingItemDto>,
    /** Bought in the last 14 days or not yet billed, newest first. */
    val bought: List<ShoppingItemDto>,
    val billable: BillableDto,
)

/** Items you bought and priced that aren't on a bill yet. */
data class BillableDto(val itemIds: List<Int>, val totalCents: Int)

data class ShoppingItemResponse(val item: ShoppingItemDto)

data class AddShoppingItemBody(
    val name: String,
    /** The server defaults to [ShoppingCategory.GROCERIES]. */
    val category: ShoppingCategory? = null,
)

data class UpdateShoppingItemBody(
    val name: String? = null,
    val category: ShoppingCategory? = null,
    val priceCents: Int? = null,
)

data class BuyItemBody(val priceCents: Int? = null)
