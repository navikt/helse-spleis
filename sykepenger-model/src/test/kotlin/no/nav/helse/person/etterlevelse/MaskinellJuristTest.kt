package no.nav.helse.person.etterlevelse

import no.nav.helse.somFødselsnummer
import no.nav.helse.somOrganisasjonsnummer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class MaskinellJuristTest {


    @Test
    fun `child jurist setter kontekstene til parent`() {
        val jurist = MaskinellJurist()

        val personJurist = jurist.medFnr("10052088033".somFødselsnummer())
        val arbeidgiverJurist = personJurist.medArbeidsgiver("123456789".somOrganisasjonsnummer())
        val vedtaksperiodeJurist = arbeidgiverJurist.medVedtaksperiode(UUID.fromString("6bce6c83-28ab-4a8c-b7f6-8402988bc8fc"))
        val spesifikKontekst = vedtaksperiodeJurist.medKontekst("søknadsId" to "asdasd-asd-asd-asds")

        spesifikKontekst.`§8-2 ledd 1`(true, LocalDate.now(), 1, emptyList(), 1)

        assertKontekster(jurist.vurderinger()[0], "10052088033", "123456789", "6bce6c83-28ab-4a8c-b7f6-8402988bc8fc", "asdasd-asd-asd-asds")

    }

    private fun assertKontekster(juridiskVurdering: JuridiskVurdering, vararg kontekster: String) =
        assertEquals(
                kontekster.toList().sorted(),
                juridiskVurdering.kontekster.values.sorted()
            )

}
