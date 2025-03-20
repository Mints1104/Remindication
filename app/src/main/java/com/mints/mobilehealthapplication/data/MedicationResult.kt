package com.mints.mobilehealthapplication.data

data class MedicationResult(
    val openfda: OpenFDA?,
    val dosage_and_administration: List<String>?,
    val warnings: List<String>?,
    val indications_and_usage: List<String>?,
    val active_ingredient: List<String>?,
    val stop_use: List<String>?,
    val do_not_use: List<String>?,
    val ask_doctor: List<String>?,
    val pregnancy_or_breast_feeding: List<String>?,
    val storage_and_handling: List<String>?,
    val inactive_ingredient: List<String>?
)