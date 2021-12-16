package no.nav.helse.spleis.meldinger

import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.person.TilstandType
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.TestMessageFactory
import no.nav.helse.spleis.e2e.AbstractEndToEndMediatorTest
import no.nav.helse.spleis.e2e.AbstractEndToEndMediatorTest.Companion.AKTØRID
import no.nav.helse.spleis.e2e.AbstractEndToEndMediatorTest.Companion.INNTEKT
import no.nav.helse.spleis.e2e.AbstractEndToEndMediatorTest.Companion.ORGNUMMER
import no.nav.helse.spleis.e2e.AbstractEndToEndMediatorTest.Companion.UNG_PERSON_FNR_2018
import org.intellij.lang.annotations.Language
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
    fun `Kan mappe om message til modell uten feil`() {
        assertNoErrors(
            testMessageFactory.lagVilkårsgrunnlag(
                vedtaksperiodeId = UUID.randomUUID(),
                tilstand = TilstandType.START,
                inntekter = emptyList(),
                inntekterForSykepengegrunnlag = listOf(TestMessageFactory.InntekterForSykepengegrunnlagFraLøsning(
                    YearMonth.of(2017, 12),
                    listOf(TestMessageFactory.InntekterForSykepengegrunnlagFraLøsning.Inntekt(32000.0, ORGNUMMER)),
                    listOf(TestMessageFactory.InntekterForSykepengegrunnlagFraLøsning.Arbeidsforhold(ORGNUMMER, "frilanserOppdragstakerHonorarPersonerMm"))
                )),
                medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Ja,
                arbeidsforhold = listOf(TestMessageFactory.Arbeidsforhold(ORGNUMMER, LocalDate.EPOCH, null))
            )
        )
    }
}
