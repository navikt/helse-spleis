package no.nav.helse.serde.reflection

import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible

internal class Reflect(
    private val kClass: KClass<*>,
    private val instance: Any
) {
    internal inline fun <reified T> get(): T {
        return instance as T
    }

    internal inline operator fun <reified R> get(name: String): R =
        kClass.memberProperties
            .single { it.name == name }
            .also {
                it.isAccessible = true
            }.call(instance) as R

    internal operator fun get(nestedClassName: String, name: String): List<Reflect> {
        val nestedClass = getNestedClass(nestedClassName)
        return get<List<Any>>(name).map { Reflect(nestedClass, it) }
    }

    internal operator fun set(property: String, value: Any?) {
        kClass.memberProperties
            .single { it.name == property }
            .also {
                it.isAccessible = true
            }.call(instance, value)
    }

    fun add(s: String, instance: Reflect) {
        get<MutableList<Any>>(s).add(instance.instance)
    }

    internal fun getNestedClass(nestedClassName: String): KClass<*> =
        getNestedClasses().single { it.simpleName == nestedClassName }

    private fun getNestedClasses(kClass: KClass<*> = this.kClass): List<KClass<*>> =
        kClass.nestedClasses.fold(emptyList()) { nestedClasses, nestedClass ->
            nestedClasses + nestedClass + getNestedClasses(nestedClass)
        }

    internal companion object {
        internal fun getInstance(kClass: KClass<*>, vararg args: Any?) =
            Reflect(kClass, kClass.primaryConstructor?.also { it.isAccessible = true }?.call(*args) ?: throw RuntimeException("No primary constructor"))
    }
}

internal inline operator fun <reified T : Any, reified R> T.get(name: String): R =
    Reflect(T::class, this)[name]

internal inline operator fun <reified T : Any> T.get(nestedClassName: String, name: String) =
    Reflect(T::class, this)[nestedClassName, name]
