package com.mints.mobilehealthapplication.data

import com.google.gson.annotations.SerializedName

data class RxNormResponse(
    @SerializedName("approximateGroup")
    val approximateGroup: ApproximateGroup
)

data class ApproximateGroup(
    @SerializedName("candidate")
    val candidates: List<Candidate>
)

data class Candidate(
    @SerializedName("rxcui")
    val rxcui: String,
    @SerializedName("name")
    val name: String?,
    @SerializedName("score")
    val score: Double
)