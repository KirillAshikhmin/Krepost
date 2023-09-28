package ru.kirillashikhmin.krepost.serializator

interface KrepostSerializer {

    fun serialize(data: Any) : String

    fun <T: Any> deserialize(str: String): T
}