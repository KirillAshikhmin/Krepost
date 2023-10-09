package ru.kirillashikhmin.krepost.serializator

import java.lang.reflect.Type

interface KrepostSerializer {

    fun serialize(data: Any) : String

    fun <T: Any> deserialize(str: String, type: Type): T
}