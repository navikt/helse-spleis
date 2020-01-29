package no.nav.helse.serde.reflection

import no.nav.helse.serde.reflection.ReflectClass.Companion.getReflectClass
import no.nav.helse.serde.reflection.ReflectInstance.Companion.getReflectInstance
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible

@Suppress("UNCHECKED_CAST")
internal class ReflectClass private constructor(
    private val kClass: KClass<*>
) {
    internal operator fun <R> get(instance: Any, property: String): R =
        kClass.memberProperties
            .single { it.name == property }
            .also {
                it.isAccessible = true
            }.call(instance) as R

    internal fun getEnumValue(property: String): Enum<*> =
        (kClass as KClass<Enum<*>>).java.enumConstants.single { it.name == property }

    internal fun add(instance: Any, property: String, value: Any) {
        get<MutableList<Any>>(instance, property).add(value)
    }

    internal fun getInstance(vararg args: Any?) =
        kClass.primaryConstructor?.also { it.isAccessible = true }?.call(*args)
            ?: throw RuntimeException("No primary constructor")

    internal fun getNestedClass(nestedClassName: String) =
        getNestedClasses().single { it.simpleName == nestedClassName }.let(::ReflectClass)

    private fun getNestedClasses(kClass: KClass<*> = this.kClass): List<KClass<*>> =
        kClass.nestedClasses.fold(emptyList()) { nestedClasses, nestedClass ->
            nestedClasses + nestedClass + getNestedClasses(nestedClass)
        }

    internal companion object {
        internal inline fun <reified T> getNestedClass(nestedClassName: String) =
            ReflectClass(T::class).getNestedClass(nestedClassName)

        internal fun getReflectClass(kClass: KClass<*>) = ReflectClass(kClass)
    }
}

@Suppress("UNCHECKED_CAST")
internal class ReflectInstance private constructor(
    private val reflectClass: ReflectClass,
    private val instance: Any
) {
    internal operator fun <R> get(property: String): R =
        reflectClass[instance, property]

    internal operator fun get(nestedClassName: String, property: String): List<ReflectInstance> {
        val nestedClass = reflectClass.getNestedClass(nestedClassName)
        return get<List<Any>>(property).map { ReflectInstance(nestedClass, it) }
    }

    internal fun add(property: String, value: ReflectInstance) {
        reflectClass.add(instance, property, value.instance)
    }

    internal companion object {
        internal fun getReflectInstance(instance: Any) =
            ReflectInstance(getReflectClass(instance::class), instance)

        internal fun ReflectClass.getReflectInstance(vararg args: Any?) =
                ReflectInstance(this, getInstance(*args))
    }
}

internal operator fun <T : Any, R> T.get(property: String): R =
    getReflectInstance(this)[property]

internal operator fun <T : Any> T.get(nestedClassName: String, property: String) =
    getReflectInstance(this)[nestedClassName, property]

internal fun <T : Any> T.add(property: String, value: ReflectInstance) =
    getReflectInstance(this).add(property, value)
