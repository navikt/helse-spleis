package no.nav.helse.person

import no.nav.helse.person.PersonObserver.ForespurtOpplysning.Companion.toJsonMap
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ForespurtOpplysningTest {

    @Test
    fun `serialiserer ForespurtOpplysning med Inntekt riktig`() {

        val forespurteOpplysninger = listOf(
            PersonObserver.Inntekt,
            PersonObserver.Arbeidsgiverperiode,
            PersonObserver.Refusjon
        )

        val expectedJson = forespurteOpplysningerMap()
        assertEquals(expectedJson, forespurteOpplysninger.toJsonMap())
    }

    private fun forespurteOpplysningerMap() = listOf(
        mapOf(
            "opplysningstype" to "Inntekt",
            "forslag" to mapOf(
                "forrigeInntekt" to null
            ),
        ),
        mapOf(
            "opplysningstype" to "Arbeidsgiverperiode"
        ),
        mapOf(
            "opplysningstype" to "Refusjon",
            "forslag" to emptyList<Any>(),
        )
    )
}
