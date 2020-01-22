package no.nav.helse.serde.reflection

import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

internal inline fun <reified T: Any, reified R> T.getProp(name: String): R =
    T::class.memberProperties
        .single { it.name == name }
        .also {
            it.isAccessible = true
        }.get(this) as R
