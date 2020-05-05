package no.nav.helse.serde.reflection

import no.nav.helse.serde.reflection.ReflectClass.Companion.getReflectClass
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

        internal fun getReflectClass(instance: Any) = ReflectClass(instance::class)
    }
}

@Suppress("UNCHECKED_CAST")
internal class ReflectInstance private constructor(
    private val reflectClass: ReflectClass,
    private val instance: Any
) {
    internal operator fun <R> get(property: String): R =
        reflectClass.get(instance, property)

    private operator fun get(nestedClassName: String, property: String): List<ReflectInstance> {
        val nestedClass = reflectClass.getNestedClass(nestedClassName)
        return get<List<Any>>(property).map { ReflectInstance(nestedClass, it) }
    }

    internal companion object {
        private fun getReflectInstance(instance: Any) =
            ReflectInstance(getReflectClass(instance), instance)

        internal operator fun <R> Any.get(property: String): R =
            getReflectInstance(this)[property]

        internal operator fun Any.get(nestedClassName: String, property: String) =
            getReflectInstance(this)[nestedClassName, property]
    }
}
