package no.nav.helse.person

import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.inspectors.inspektør
import no.nav.helse.spleis.e2e.AbstractEndToEndTest.Companion.INNTEKT
import no.nav.helse.testhelpers.januar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

internal class InntektsmeldingInfoHistorikkTest {

    private lateinit var historikk: InntektsmeldingInfoHistorikk

    @BeforeEach
    fun setup() {
        historikk = InntektsmeldingInfoHistorikk()
    }

    @Test
    fun `legger ikke inn samme inntektsmelding flere ganger`() {
        val inntektsmelding = inntektsmelding(UUID.randomUUID())
        historikk.opprett(1.januar, inntektsmelding)
        historikk.opprett(1.januar, inntektsmelding)
        assertEquals(1, historikk.inspektør.antallElementer)
        assertEquals(1, historikk.inspektør.antallDatoElementer(1.januar))
    }

    @Test
    fun `legger inn ulike inntektsmeldinger`() {
        val id = UUID.randomUUID()
        historikk.opprett(1.januar, inntektsmelding(id))
        historikk.opprett(1.januar, inntektsmelding(id, "arbId1"))
        assertEquals(1, historikk.inspektør.antallElementer)
        assertEquals(2, historikk.inspektør.antallDatoElementer(1.januar))
    }

    private fun inntektsmelding(id: UUID, arbeidsforholdId: String? = null) =
        Inntektsmelding(
            meldingsreferanseId = id,
            refusjon = Inntektsmelding.Refusjon(null, null),
            orgnummer = "orgnr",
            fødselsnummer = "fnr",
            aktørId = "aktør",
            førsteFraværsdag = 1.januar,
            beregnetInntekt = INNTEKT,
            arbeidsgiverperioder = emptyList(),
            arbeidsforholdId = arbeidsforholdId,
            begrunnelseForReduksjonEllerIkkeUtbetalt = null,
            mottatt = LocalDateTime.now()
        )
}
