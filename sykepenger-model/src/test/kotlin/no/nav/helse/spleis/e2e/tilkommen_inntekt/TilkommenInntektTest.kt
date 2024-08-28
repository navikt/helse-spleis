package no.nav.helse.spleis.e2e.tilkommen_arbeidsgiver

import java.time.LocalDate
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.nyttVedtak
import no.nav.helse.februar
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.Søknad.TilkommenInntekt
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.inntekt.InntektFraSøknad
import no.nav.helse.person.inntekt.Inntektsmelding
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class TilkommenInntektTest : AbstractDslTest() {

    @Test
    fun `oppdaterer sykepengegrunnlag med inntekter fra søknaden`() {
        a1 {
            nyttVedtak(januar, beregnetInntekt = 31000.månedlig)
            håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), tilkomneInntekter = listOf(TilkommenInntekt(fom = 1.februar, tom = null, orgnummer = "a2", beløp = 10000.månedlig)))
            assertVarsel(Varselkode.RV_SV_5)
            assertEquals(2, inspektør.vilkårsgrunnlagHistorikkInnslag().size)
            inspektør.vilkårsgrunnlag(1.januar)!!.inspektør.sykepengegrunnlag.inspektør.let { sykepengegrunnlagInspektør ->
                assertEquals(31000.månedlig, sykepengegrunnlagInspektør.sykepengegrunnlag)
                assertEquals(2, sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
                val inntektA1 = sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver[a1]
                val inntektA2 = sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver[a2]
                assertEquals(1.januar til LocalDate.MAX, inntektA1!!.inspektør.gjelder)
                assertEquals(31000.månedlig, inntektA1.inspektør.inntektsopplysning.fastsattÅrsinntekt())
                assertTrue(inntektA1.inspektør.inntektsopplysning is Inntektsmelding)
                assertEquals(1.februar til LocalDate.MAX, inntektA2!!.inspektør.gjelder)
                assertEquals(10000.månedlig, inntektA2.inspektør.inntektsopplysning.fastsattÅrsinntekt())
                assertTrue(inntektA2.inspektør.inntektsopplysning is InntektFraSøknad)
            }
        }
    }
}