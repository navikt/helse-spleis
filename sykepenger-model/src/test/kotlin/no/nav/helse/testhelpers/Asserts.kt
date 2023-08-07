package no.nav.helse.testhelpers

import org.junit.jupiter.api.Assertions
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
fun <T: Any> assertNotNull(value: T?) {
    contract {
        returns() implies (value != null)
    }
    Assertions.assertNotNull(value)
}
