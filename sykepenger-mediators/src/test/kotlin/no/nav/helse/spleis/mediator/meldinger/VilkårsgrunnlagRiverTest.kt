package no.nav.helse.spleis.mediator.meldinger

import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.person.TilstandType
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.mediator.TestMessageFactory
import no.nav.helse.spleis.mediator.e2e.AbstractEndToEndMediatorTest.Companion.AKTØRID
import no.nav.helse.spleis.mediator.e2e.AbstractEndToEndMediatorTest.Companion.INNTEKT
import no.nav.helse.spleis.mediator.e2e.AbstractEndToEndMediatorTest.Companion.ORGNUMMER
import no.nav.helse.spleis.mediator.e2e.AbstractEndToEndMediatorTest.Companion.UNG_PERSON_FNR_2018
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth
import java.util.*
import no.nav.helse.januar
import no.nav.helse.spleis.mediator.TestMessageFactory.Arbeidsforhold.Arbeidsforholdtype
import no.nav.helse.spleis.mediator.e2e.AbstractEndToEndMediatorTest.Companion.UNG_PERSON_FØDSELSDATO
import no.nav.helse.spleis.meldinger.VilkårsgrunnlagRiver

internal class VilkårsgrunnlagRiverTest : RiverTest() {
    override fun river(rapidsConnection: RapidsConnection, mediator: IMessageMediator) {
        VilkårsgrunnlagRiver(rapidsConnection, mediator)
    }

    private val testMessageFactory = TestMessageFactory(
        UNG_PERSON_FNR_2018,
        AKTØRID,
        ORGNUMMER,
        INNTEKT,
        UNG_PERSON_FØDSELSDATO
    )

    @Test
    fun `Kan mappe om message med frilanser og inntekt til modell uten feil`() {
        assertNoErrors(
            testMessageFactory.lagVilkårsgrunnlag(
                vedtaksperiodeId = UUID.randomUUID(),
                skjæringstidspunkt = 1.januar,
                tilstand = TilstandType.START,
                inntekterForSykepengegrunnlag = listOf(
                    TestMessageFactory.InntekterForSykepengegrunnlagFraLøsning(
                    måned = YearMonth.of(2017, 12),
                    inntekter = listOf(TestMessageFactory.InntekterForSykepengegrunnlagFraLøsning.Inntekt(32000.0, ORGNUMMER)),
                    arbeidsforhold = listOf(TestMessageFactory.InntekterForSykepengegrunnlagFraLøsning.Arbeidsforhold(ORGNUMMER, "frilanserOppdragstakerHonorarPersonerMm"))
                )),
                arbeidsforhold = listOf(TestMessageFactory.Arbeidsforhold(ORGNUMMER, LocalDate.EPOCH, null, Arbeidsforholdtype.ORDINÆRT)),
                medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Ja
            )
        )
    }

    @Test
    fun `Kan mappe om message uten arbeidsforhold i sykepengegrunnlaget til modell uten feil`() {
        assertNoErrors(
            testMessageFactory.lagVilkårsgrunnlag(
                vedtaksperiodeId = UUID.randomUUID(),
                skjæringstidspunkt = 1.januar,
                tilstand = TilstandType.START,
                inntekterForSykepengegrunnlag = listOf(
                    TestMessageFactory.InntekterForSykepengegrunnlagFraLøsning(
                    måned = YearMonth.of(2017, 12),
                    inntekter = listOf(TestMessageFactory.InntekterForSykepengegrunnlagFraLøsning.Inntekt(32000.0, "987654322")),
                    arbeidsforhold = emptyList()
                )),
                arbeidsforhold = listOf(TestMessageFactory.Arbeidsforhold(ORGNUMMER, LocalDate.EPOCH, null, Arbeidsforholdtype.ORDINÆRT)),
                medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Ja
            )
        )
    }
}
