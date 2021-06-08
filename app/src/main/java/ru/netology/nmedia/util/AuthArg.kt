package ru.netology.nmedia.util

import android.os.Bundle
import ru.netology.nmedia.dto.Token
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

object AuthArg: ReadWriteProperty<Bundle, Token?> {

    override fun setValue(thisRef: Bundle, property: KProperty<*>, value: Token?) {
        thisRef.putParcelable(property.name, value)
    }

    override fun getValue(thisRef: Bundle, property: KProperty<*>): Token? =
        thisRef.getParcelable(property.name)
}