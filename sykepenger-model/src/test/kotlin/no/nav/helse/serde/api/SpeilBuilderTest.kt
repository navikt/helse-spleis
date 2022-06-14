package no.nav.helse.serde.api

import java.time.LocalDate
import no.nav.helse.hendelser.InntektForSykepengegrunnlag
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.november
import no.nav.helse.serde.api.dto.BeregnetPeriode
import no.nav.helse.serde.api.dto.Inntektkilde
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.finnSkjæringstidspunkt
import no.nav.helse.spleis.e2e.grunnlag
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.håndterInntektsmeldingReplay
import no.nav.helse.spleis.e2e.håndterSimulering
import no.nav.helse.spleis.e2e.håndterSykmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterUtbetalingsgodkjenning
import no.nav.helse.spleis.e2e.håndterUtbetalt
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlag
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.spleis.e2e.nyttVedtak
import no.nav.helse.spleis.e2e.repeat
import no.nav.helse.spleis.e2e.sammenligningsgrunnlag
import no.nav.helse.spleis.e2e.speilApi
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class SpeilBuilderTest : AbstractEndToEndTest() {
    @Test
    fun `Dødsdato ligger på person`() {
        val fom = 1.januar
        val tom = 31.januar
        håndterSykmelding(Sykmeldingsperiode(fom, tom, 100.prosent))
        håndterSøknad(Sykdom(fom, tom, 100.prosent), sendtTilNAVEllerArbeidsgiver = fom.plusDays(1))
        håndterInntektsmelding(listOf(fom til fom.plusDays(15)))
        håndterYtelser(1.vedtaksperiode, dødsdato = 1.januar)

        assertEquals(1.januar, serializePersonForSpeil(person).dødsdato)
    }

    @Test
    fun `Skal ikke ha varsel om flere inntektsmeldinger ved replay av samme IM`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        val inntektsmeldingId = håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterInntektsmeldingReplay(inntektsmeldingId, 1.vedtaksperiode.id(ORGNUMMER))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        val personDto = speilApi()
        val periode: BeregnetPeriode = personDto.arbeidsgivere.first().generasjoner.first().perioder.first() as BeregnetPeriode
        val warnings = periode.aktivitetslogg.filter { it.alvorlighetsgrad ==  "W" }
        assertTrue(warnings.isEmpty())
    }

    @Test
    fun `Viser inntektsgrunnlag for arbeidsforhold som startet innen 3 måneder før skjæringstidspunktet, selvom vi ikke har inntekt`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 15.mars, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 15.mars, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1)

        håndterYtelser(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            orgnummer = a1,
        )
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 31000.månedlig.repeat(12)),
                )
            ),
            orgnummer = a1,
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                inntekter = listOf(
                    grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 31000.månedlig.repeat(3)),
                ), arbeidsforhold = emptyList()
            ),
            arbeidsforhold = listOf(
                Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH, null),
                Vilkårsgrunnlag.Arbeidsforhold(a2, 25.november(2017), null),
            )
        )
        håndterYtelser(vedtaksperiodeIdInnhenter = 1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        val personDto = serializePersonForSpeil(person)

        assertEquals(listOf(a1, a2).map(String::toString), personDto.arbeidsgivere.map { it.organisasjonsnummer })
//        assertEquals(listOf(a1, a2).map(String::toString), personDto.inntektsgrunnlag.single().inntekter.map { it.arbeidsgiver })

        val arbeidsgiverInntektA2 = personDto.vilkårsgrunnlagHistorikk.values.last()[1.januar]?.inntekter?.first { it.organisasjonsnummer == a2 }

        assertEquals(0.0, arbeidsgiverInntektA2?.omregnetÅrsinntekt?.beløp)
        assertEquals(Inntektkilde.IkkeRapportert, arbeidsgiverInntektA2?.omregnetÅrsinntekt?.kilde)
    }

    @Test
    fun `tar med refusjonshistorikk pr arbeidsgiver`() {
        nyttVedtak(
            fom = 1.januar,
            tom = 31.januar,
            grad = 100.prosent,
            refusjon = Inntektsmelding.Refusjon(
                beløp = INNTEKT,
                opphørsdato = null,
                endringerIRefusjon = listOf(
                    Inntektsmelding.Refusjon.EndringIRefusjon(beløp = INNTEKT.plus(1000.månedlig), 19.januar),
                    Inntektsmelding.Refusjon.EndringIRefusjon(beløp = INNTEKT.plus(2000.månedlig), 23.januar),
                )
            )
        )

        val personDto = speilApi()
        val periode: BeregnetPeriode = personDto.arbeidsgivere.first().generasjoner.first().perioder.first() as BeregnetPeriode
        val refusjon = requireNotNull(periode.refusjon)

        assertEquals(2, refusjon.endringer.size)
        assertEquals(32000.0, refusjon.endringer.first().beløp)
        assertEquals(19.januar, refusjon.endringer.first().dato)
        assertEquals(33000.0, refusjon.endringer.last().beløp)
        assertEquals(23.januar, refusjon.endringer.last().dato)
    }
}
