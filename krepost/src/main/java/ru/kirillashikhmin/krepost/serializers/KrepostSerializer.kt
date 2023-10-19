package ru.kirillashikhmin.krepost.serializers

import kotlin.reflect.KType

interface KrepostSerializer {

    fun <T> serialize(data: T, type: KType) : String

    fun <T> deserialize(str: String, type: KType): T
}
