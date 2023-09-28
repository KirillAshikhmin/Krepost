package ru.kirillashikhmin.krepost

sealed class ValidateResult {
    object Ok : ValidateResult()
    class Empty(val message: String?) : ValidateResult()
    class Error(val message: String?) : ValidateResult()

}
