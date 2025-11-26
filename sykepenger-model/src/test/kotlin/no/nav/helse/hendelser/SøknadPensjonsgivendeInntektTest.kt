package no.nav.helse.hendelser

import java.time.Year
import no.nav.helse.hendelser.Søknad.PensjonsgivendeInntekt.Companion.harFlereTyperPensjonsgivendeInntekt
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class SøknadPensjonsgivendeInntektTest {

    @ParameterizedTest
    @CsvSource(
        value = [
            // format: år, næringsinntekt, lønnsinntekt, lønnsinntektBarePensjonsdel, næringsinntektFraFiskeFangstEllerFamiliebarnehage for hvert av de tre årene, og forventetutfall
            "2017, 30000, 0,        0,      0, 2016, 30000, 0,      0, 0, 2015, 30000,  0,      0, 0,       false",
            "2017, 30000, 30000,    0,      0, 2016, 30000, 30000,  0, 0, 2015, 30000,  30000,  0, 0,       true",
            "2017, 0,     30000,    0,      0, 2016, 0,     30000,  0, 0, 2015, 0,      30000,  0, 0,       false",
            "2017, 30000, 0,        0,      0, 2016, 30000, 0,      0, 0, 2015, 0,      30000,  0, 0,       true",
            "2017, 30000, 0,        30000,  0, 2016, 30000, 0,      0, 0, 2015, 30000,  0,      0, 0,       true",
            "2017, 30000, 0,        0,      0, 2016, 30000, 0,      0, 0, 2015, 0,      0,      0, 30000,   true",
        ]
    )
    fun `har flere pensjonsgivende inntekter`(
        år1: String, næringsinntekt1: Int, lønnsinntekt1: Int, lønnsinntektBarePensjonsdel1: Int, næringsinntektFraFiskeFangstEllerFamiliebarnehage1: Int,
        år2: String, næringsinntekt2: Int, lønnsinntekt2: Int, lønnsinntektBarePensjonsdel2: Int, næringsinntektFraFiskeFangstEllerFamiliebarnehage2: Int,
        år3: String, næringsinntekt3: Int, lønnsinntekt3: Int, lønnsinntektBarePensjonsdel3: Int, næringsinntektFraFiskeFangstEllerFamiliebarnehage3: Int,
        forventet: Boolean
    ) {
        val pensjonsgivendeInntekter = listOf(
            Søknad.PensjonsgivendeInntekt(inntektsår = Year.parse(år1), næringsinntekt = næringsinntekt1.årlig, lønnsinntekt = lønnsinntekt1.årlig, lønnsinntektBarePensjonsdel = lønnsinntektBarePensjonsdel1.årlig, næringsinntektFraFiskeFangstEllerFamiliebarnehage = næringsinntektFraFiskeFangstEllerFamiliebarnehage1.årlig, erFerdigLignet = true),
            Søknad.PensjonsgivendeInntekt(inntektsår = Year.parse(år2), næringsinntekt = næringsinntekt2.årlig, lønnsinntekt = lønnsinntekt2.årlig, lønnsinntektBarePensjonsdel = lønnsinntektBarePensjonsdel2.årlig, næringsinntektFraFiskeFangstEllerFamiliebarnehage = næringsinntektFraFiskeFangstEllerFamiliebarnehage2.årlig, erFerdigLignet = true),
            Søknad.PensjonsgivendeInntekt(inntektsår = Year.parse(år3), næringsinntekt = næringsinntekt3.årlig, lønnsinntekt = lønnsinntekt3.årlig, lønnsinntektBarePensjonsdel = lønnsinntektBarePensjonsdel3.årlig, næringsinntektFraFiskeFangstEllerFamiliebarnehage = næringsinntektFraFiskeFangstEllerFamiliebarnehage3.årlig, erFerdigLignet = true)
        )
        assertEquals(forventet, pensjonsgivendeInntekter.harFlereTyperPensjonsgivendeInntekt())
    }
}
