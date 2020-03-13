package no.nav.helse.person

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

/**
 * Denne testen skal fange opp endringer i tilstandstyper som vil kreve at koden i Speil oppdateres.
 *
 * Hvis denne testen brekker er det stor sjanse for at noen™ må endre koden i Speil.
 */
internal class TilstandTypeTest {
    @Test
    fun `tilstandene matcher hva Speil forventer`() {
        val tilstandstypeneSomSpeilForventer = listOf(
            "AVVENTER_HISTORIKK",
            "AVVENTER_GODKJENNING",
            "TIL_UTBETALING",
            "TIL_INFOTRYGD",
            "AVSLUTTET",
            "UTBETALING_FEILET",
            "START",
            "MOTTATT_SYKMELDING_FERDIG_FORLENGELSE",
            "MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE",
            "MOTTATT_SYKMELDING_FERDIG_GAP",
            "MOTTATT_SYKMELDING_UFERDIG_GAP",
            "AVVENTER_SØKNAD_FERDIG_GAP",
            "AVVENTER_SØKNAD_UFERDIG_GAP",
            "AVVENTER_VILKÅRSPRØVING_GAP",
            "AVVENTER_GAP",
            "AVVENTER_INNTEKTSMELDING_FERDIG_GAP",
            "AVVENTER_INNTEKTSMELDING_UFERDIG_GAP",
            "AVVENTER_UFERDIG_GAP",
            "AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE",
            "AVVENTER_SØKNAD_UFERDIG_FORLENGELSE",
            "AVVENTER_UFERDIG_FORLENGELSE"
        )

        tilstandstypeneSomSpeilForventer.forEach {
            try {
                TilstandType.valueOf(it)
            } catch (e: Exception) {
                fail("'$it' forventes i Speil")
            }
        }

        TilstandType.values().forEach {
            if (!tilstandstypeneSomSpeilForventer.contains(it.name)) {
                fail("'$it' er ikke en forventet tilstand i Speil")
            }
        }
    }
}
