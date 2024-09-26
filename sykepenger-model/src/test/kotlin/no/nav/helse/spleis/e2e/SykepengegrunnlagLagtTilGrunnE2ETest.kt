package no.nav.helse.spleis.e2e

import java.time.YearMonth
import no.nav.helse.Toggle
import no.nav.helse.desember
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.hendelser.ArbeidsgiverInntekt
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.november
import no.nav.helse.oktober
import no.nav.helse.person.PersonObserver
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class SykepengegrunnlagLagtTilGrunnE2ETest : AbstractDslTest() {

    @Test
    fun `event om at vi bruker skatteopplysninger`() = Toggle.InntektsmeldingSomIkkeKommer.enable {
        val inntektFraSkatt = 10000.månedlig
        a1 {
            håndterSøknad(januar)
            håndterSykepengegrunnlagForArbeidsgiver(1.vedtaksperiode, 1.januar, listOf(
                ArbeidsgiverInntekt.MånedligInntekt(YearMonth.of(2017, 12), inntektFraSkatt, ArbeidsgiverInntekt.MånedligInntekt.Inntekttype.LØNNSINNTEKT, "", ""),
                ArbeidsgiverInntekt.MånedligInntekt(YearMonth.of(2017, 11), inntektFraSkatt, ArbeidsgiverInntekt.MånedligInntekt.Inntekttype.LØNNSINNTEKT, "", ""),
                ArbeidsgiverInntekt.MånedligInntekt(YearMonth.of(2017, 10), inntektFraSkatt, ArbeidsgiverInntekt.MånedligInntekt.Inntekttype.LØNNSINNTEKT, "", "")
            ))
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)

            val event = observatør.skatteinntekterLagtTilGrunnEventer.single()
            val forventet = PersonObserver.SkatteinntekterLagtTilGrunnEvent(
                organisasjonsnummer = a1,
                vedtaksperiodeId = 1.vedtaksperiode,
                behandlingId = inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.behandlinger.single().id,
                skatteinntekter = listOf(
                    PersonObserver.SkatteinntekterLagtTilGrunnEvent.Skatteinntekt(desember(2017), 10000.0),
                    PersonObserver.SkatteinntekterLagtTilGrunnEvent.Skatteinntekt(november(2017), 10000.0),
                    PersonObserver.SkatteinntekterLagtTilGrunnEvent.Skatteinntekt(oktober(2017), 10000.0)
                ),
                omregnetÅrsinntekt = 120000.0
            )
            assertEquals(forventet, event)
        }
    }

    @Test
    fun `event om at vi bruker skatteopplysninger med sprø minus`() = Toggle.InntektsmeldingSomIkkeKommer.enable {
        val inntektFraSkatt = 10000.månedlig
        val sprøInntektFraSkatt = 30000.månedlig * -1
        a1 {
            håndterSøknad(januar)
            håndterSykepengegrunnlagForArbeidsgiver(1.vedtaksperiode, 1.januar, listOf(
                ArbeidsgiverInntekt.MånedligInntekt(YearMonth.of(2017, 12), inntektFraSkatt, ArbeidsgiverInntekt.MånedligInntekt.Inntekttype.LØNNSINNTEKT, "", ""),
                ArbeidsgiverInntekt.MånedligInntekt(YearMonth.of(2017, 11), inntektFraSkatt, ArbeidsgiverInntekt.MånedligInntekt.Inntekttype.LØNNSINNTEKT, "", ""),
                ArbeidsgiverInntekt.MånedligInntekt(YearMonth.of(2017, 10), sprøInntektFraSkatt, ArbeidsgiverInntekt.MånedligInntekt.Inntekttype.LØNNSINNTEKT, "", "")
            ))
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)

            val event = observatør.skatteinntekterLagtTilGrunnEventer.single()
            val forventet = PersonObserver.SkatteinntekterLagtTilGrunnEvent(
                organisasjonsnummer = a1,
                vedtaksperiodeId = 1.vedtaksperiode,
                behandlingId = inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.behandlinger.single().id,
                skatteinntekter = listOf(
                    PersonObserver.SkatteinntekterLagtTilGrunnEvent.Skatteinntekt(desember(2017), 10000.0),
                    PersonObserver.SkatteinntekterLagtTilGrunnEvent.Skatteinntekt(november(2017), 10000.0),
                    PersonObserver.SkatteinntekterLagtTilGrunnEvent.Skatteinntekt(oktober(2017), -30000.0),
                ),
                omregnetÅrsinntekt = 0.0
            )
            assertEquals(forventet, event)
        }
    }

}