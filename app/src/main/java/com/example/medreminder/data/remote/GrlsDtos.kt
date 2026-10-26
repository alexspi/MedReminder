package com.example.medreminder.data.remote

import com.google.gson.annotations.SerializedName

data class GrlsSearchResponse(
    @SerializedName("generated_at") val generatedAt: String?,
    @SerializedName("query") val query: String?,
    @SerializedName("items") val items: List<GrlsRecordDto>?,
)

data class GrlsRecordDto(
    @SerializedName("id") val id: Long?,
    @SerializedName("status") val status: String?,
    @SerializedName("registration_number") val registrationNumber: String?,
    @SerializedName("registration_date") val registrationDate: String?,
    @SerializedName("expiry_date") val expiryDate: String?,
    @SerializedName("holder") val holder: GrlsHolderDto?,
    @SerializedName("trade_name") val tradeName: String?,
    @SerializedName("inn_name") val innName: String?,
    @SerializedName("dosage_forms") val dosageForms: String?,
    @SerializedName("pharm_group") val pharmGroup: String?,
    @SerializedName("vital") val vital: String?,
)

data class GrlsHolderDto(
    @SerializedName("name") val name: String?,
    @SerializedName("country") val country: String?,
)
