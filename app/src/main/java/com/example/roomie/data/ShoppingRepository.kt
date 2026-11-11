package com.example.roomie.data

import com.example.roomie.data.api.AddShoppingItemBody
import com.example.roomie.data.api.BuyItemBody
import com.example.roomie.data.api.RetrofitInstance
import com.example.roomie.data.api.RoomieApiResponse
import com.example.roomie.data.api.ShoppingCategory
import com.example.roomie.data.api.ShoppingItemDto
import com.example.roomie.data.api.ShoppingListResponse
import com.example.roomie.data.api.UpdateShoppingItemBody
import com.example.roomie.data.api.apiCall
import com.example.roomie.data.api.fetchInto
import com.example.roomie.data.api.repositoryScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShoppingRepository @Inject constructor(private val retrofitInstance: RetrofitInstance) {

    private val scope = repositoryScope()
    private val api get() = retrofitInstance.shoppingApi

    private val _shoppingList = MutableStateFlow<RoomieApiResponse<ShoppingListResponse>>(RoomieApiResponse.Pending)

    /** Needed and recently bought items. Refreshed after every change below succeeds. */
    val shoppingList: StateFlow<RoomieApiResponse<ShoppingListResponse>> = _shoppingList.asStateFlow()

    /** Pipelines the response into [shoppingList]. */
    fun fetchShoppingList(houseId: Int, refresh: Boolean = false) {
        scope.fetchInto(_shoppingList, refresh) { api.getShoppingList(houseId) }
    }

    suspend fun addItem(houseId: Int, name: String, category: ShoppingCategory? = null): RoomieApiResponse<ShoppingItemDto> =
        changing(houseId) { api.addItem(houseId, AddShoppingItemBody(name.trim(), category)).item }

    suspend fun updateItem(houseId: Int, itemId: Int, body: UpdateShoppingItemBody): RoomieApiResponse<ShoppingItemDto> =
        changing(houseId) { api.updateItem(houseId, itemId, body).item }

    suspend fun deleteItem(houseId: Int, itemId: Int): RoomieApiResponse<Unit> =
        changing(houseId) { api.deleteItem(houseId, itemId) }

    suspend fun claimItem(houseId: Int, itemId: Int): RoomieApiResponse<ShoppingItemDto> =
        changing(houseId) { api.claimItem(houseId, itemId).item }

    suspend fun unclaimItem(houseId: Int, itemId: Int): RoomieApiResponse<ShoppingItemDto> =
        changing(houseId) { api.unclaimItem(houseId, itemId).item }

    suspend fun buyItem(houseId: Int, itemId: Int, priceCents: Int? = null): RoomieApiResponse<ShoppingItemDto> =
        changing(houseId) { api.buyItem(houseId, itemId, BuyItemBody(priceCents)).item }

    suspend fun unbuyItem(houseId: Int, itemId: Int): RoomieApiResponse<ShoppingItemDto> =
        changing(houseId) { api.unbuyItem(houseId, itemId).item }

    /** Moving an item can change both lists and the billable total, so refetch rather than patch. */
    private suspend fun <T> changing(houseId: Int, call: suspend () -> T): RoomieApiResponse<T> =
        apiCall(call).also { it.ifSuccess { fetchShoppingList(houseId, refresh = true) } }
}
