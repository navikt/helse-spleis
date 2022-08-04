package no.nav.helse.person

import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.inspectors.inspektør
import no.nav.helse.spleis.e2e.AbstractEndToEndTest.Companion.INNTEKT
import no.nav.helse.januar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*
import no.nav.helse.dsl.Hendelsefabrikk
import no.nav.helse.februar
import no.nav.helse.somFødselsnummer

internal class InntektsmeldingInfoHistorikkTest {

    private lateinit var historikk: InntektsmeldingInfoHistorikk
    private val hendelsefabrikk = Hendelsefabrikk(
        organisasjonsnummer = "orgnr",
        fødselsnummer = "12029212345".somFødselsnummer(),
        aktørId = "aktør",
        fødselsdato = 12.februar(1992)
    )

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
        hendelsefabrikk.lagInntektsmelding(
            id = id,
            refusjon = Inntektsmelding.Refusjon(null, null),
            førsteFraværsdag = 1.januar,
            beregnetInntekt = INNTEKT,
            arbeidsgiverperioder = emptyList(),
            arbeidsforholdId = arbeidsforholdId,
            begrunnelseForReduksjonEllerIkkeUtbetalt = null
        )
}
