package no.nav.helse.testhelpers

import org.junit.jupiter.api.Assertions
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.reflect.KClass

@OptIn(ExperimentalContracts::class)
fun <T: Any> assertNotNull(value: T?) {
    contract {
        returns() implies (value != null)
    }
    Assertions.assertNotNull(value)
}

@OptIn(ExperimentalContracts::class)
inline fun <reified T: Any> assertInstanceOf(actual: Any) {
    contract {
        returns() implies (actual is T)
    }
    Assertions.assertInstanceOf(T::class.java, actual)
}
