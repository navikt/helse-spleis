package no.nav.helse.spleis.meldinger

import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.person.TilstandType
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.TestMessageFactory
import no.nav.helse.spleis.e2e.AbstractEndToEndMediatorTest.Companion.AKTØRID
import no.nav.helse.spleis.e2e.AbstractEndToEndMediatorTest.Companion.INNTEKT
import no.nav.helse.spleis.e2e.AbstractEndToEndMediatorTest.Companion.ORGNUMMER
import no.nav.helse.spleis.e2e.AbstractEndToEndMediatorTest.Companion.UNG_PERSON_FNR_2018
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth
import java.util.*

internal class VilkårsgrunnlagRiverTest : RiverTest() {
    override fun river(rapidsConnection: RapidsConnection, mediator: IMessageMediator) {
        VilkårsgrunnlagRiver(rapidsConnection, mediator)
    }

    private val testMessageFactory = TestMessageFactory(
        UNG_PERSON_FNR_2018,
        AKTØRID,
        ORGNUMMER,
        INNTEKT
    )

    @Test
    fun `Kan mappe om message med frilanser og inntekt til modell uten feil`() {
        assertNoErrors(
            testMessageFactory.lagVilkårsgrunnlag(
                vedtaksperiodeId = UUID.randomUUID(),
                tilstand = TilstandType.START,
                inntekter = emptyList(),
                inntekterForSykepengegrunnlag = listOf(TestMessageFactory.InntekterForSykepengegrunnlagFraLøsning(
                    måned = YearMonth.of(2017, 12),
                    inntekter = listOf(TestMessageFactory.InntekterForSykepengegrunnlagFraLøsning.Inntekt(32000.0, ORGNUMMER)),
                    arbeidsforhold = listOf(TestMessageFactory.InntekterForSykepengegrunnlagFraLøsning.Arbeidsforhold(ORGNUMMER, "frilanserOppdragstakerHonorarPersonerMm"))
                )),
                medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Ja,
                arbeidsforhold = listOf(TestMessageFactory.Arbeidsforhold(ORGNUMMER, LocalDate.EPOCH, null))
            )
        )
    }

    @Test
    fun `Kan mappe om message uten arbeidsforhold i sykepengegrunnlaget til modell uten feil`() {
        assertNoErrors(
            testMessageFactory.lagVilkårsgrunnlag(
                vedtaksperiodeId = UUID.randomUUID(),
                tilstand = TilstandType.START,
                inntekter = emptyList(),
                inntekterForSykepengegrunnlag = listOf(TestMessageFactory.InntekterForSykepengegrunnlagFraLøsning(
                    måned = YearMonth.of(2017, 12),
                    inntekter = listOf(TestMessageFactory.InntekterForSykepengegrunnlagFraLøsning.Inntekt(32000.0, "987654322")),
                    arbeidsforhold = emptyList()
                )),
                medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Ja,
                arbeidsforhold = listOf(TestMessageFactory.Arbeidsforhold(ORGNUMMER, LocalDate.EPOCH, null))
            )
        )
    }
}
