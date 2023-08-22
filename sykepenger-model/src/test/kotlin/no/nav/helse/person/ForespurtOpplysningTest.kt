package no.nav.helse.person

import java.util.UUID
import no.nav.helse.april
import no.nav.helse.februar
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.PersonObserver.ForespurtOpplysning.Companion.toJsonMap
import no.nav.helse.person.inntekt.Refusjonsopplysning
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ForespurtOpplysningTest {

    @Test
    fun `serialiserer ForespurtOpplysning med Inntekt riktig`() {

        val forespurteOpplysninger = listOf(
            PersonObserver.Inntekt(PersonObserver.Inntektsforslag(
                beregningsmåneder = listOf(
                    januar(2018),
                    februar(2018),
                    mars(2018)
                ),
                PersonObserver.Inntektsdata(1.januar, PersonObserver.Inntektsopplysningstype.INNTEKTSMELDING, 31000.0)
            )
            ),
            PersonObserver.Arbeidsgiverperiode,
            PersonObserver.Refusjon(emptyList())
        )

        val expectedJson = forespurteOpplysningerMap()
        assertEquals(expectedJson, forespurteOpplysninger.toJsonMap())
    }

    @Test
    fun `serialiserer ForespurtOpplysning med FastsattInntekt riktig`() {

        val forespurteOpplysninger = listOf(
            PersonObserver.FastsattInntekt(30000.månedlig),
            PersonObserver.Arbeidsgiverperiode,
            PersonObserver.Refusjon(listOf(
                Refusjonsopplysning(UUID.randomUUID(), fom = 1.januar, tom = 31.mars, 30000.månedlig),
                Refusjonsopplysning(UUID.randomUUID(), fom = 1.april, tom = null, INGEN)
            ))
        )

        val expectedJson = forespurteOpplysningerMedFastsattInntektMap()
        assertEquals(expectedJson, forespurteOpplysninger.toJsonMap())
    }

    private fun forespurteOpplysningerMedFastsattInntektMap() = listOf(
        mapOf(
            "opplysningstype" to "FastsattInntekt",
            "fastsattInntekt" to 30000.0,
        ),
        mapOf(
            "opplysningstype" to "Arbeidsgiverperiode"
        ),
        mapOf(
            "opplysningstype" to "Refusjon",
            "forslag" to listOf(
                mapOf("fom" to 1.januar, "tom" to 31.mars, "beløp" to 30000.0),
                mapOf("fom" to 1.april, "tom" to null, "beløp" to 0.0)
            ),
        )
    )

    private fun forespurteOpplysningerMap() = listOf(
        mapOf(
            "opplysningstype" to "Inntekt",
            "forslag" to mapOf(
                "beregningsmåneder" to listOf(
                    januar(2018),
                    februar(2018),
                    mars(2018),
                ),
                "forrigeInntekt" to mapOf(
                        "skjæringstidspunkt" to 1.januar,
                        "kilde" to PersonObserver.Inntektsopplysningstype.INNTEKTSMELDING.name,
                        "beløp" to 31000.0
                )
            ),
        ),
        mapOf(
            "opplysningstype" to "Arbeidsgiverperiode"
        ),
        mapOf(
            "opplysningstype" to "Refusjon",
            "forslag" to emptyList<Refusjonsopplysning>(),
        )
    )
}