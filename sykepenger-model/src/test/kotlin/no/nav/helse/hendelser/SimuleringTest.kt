package no.nav.helse.hendelser

import java.util.UUID
import no.nav.helse.person.AbstractPersonTest
import no.nav.helse.person.Varselkode
import no.nav.helse.person.Varselkode.RV_SI_3
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class SimuleringTest {
    @Test
    fun `Varsel for negativt totalbeløp`() {
        sjekkTotalbeløp(-1, listOf(RV_SI_3))
        sjekkTotalbeløp(-0, emptyList())
        sjekkTotalbeløp(0, emptyList())
        sjekkTotalbeløp(1, emptyList())
    }

    private fun sjekkTotalbeløp(totalbeløp: Int, ønskedeVarsler: List<Varselkode>) {
        val simulering = simulering(totalbeløp)
        assertEquals(ønskedeVarsler, simulering.varselkoder())
    }

    private fun simulering(totalbeløp: Int) = Simulering(
        meldingsreferanseId = UUID.randomUUID(),
        vedtaksperiodeId = "1",
        aktørId = "aktørId",
        fødselsnummer = AbstractPersonTest.UNG_PERSON_FNR_2018.toString(),
        orgnummer = AbstractPersonTest.ORGNUMMER,
        fagsystemId = "2",
        fagområde = "SPREF",
        simuleringOK = true,
        melding = "",
        utbetalingId = UUID.randomUUID(),
        simuleringResultat = Simulering.SimuleringResultat(
            totalbeløp = totalbeløp,
            perioder = emptyList()
        )
    )


}