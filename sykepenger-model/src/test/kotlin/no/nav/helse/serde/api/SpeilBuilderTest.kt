package no.nav.helse.serde.api

import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.helse.desember
import no.nav.helse.februar
import no.nav.helse.hendelser.Dagtype.Feriedag
import no.nav.helse.hendelser.InntektForSykepengegrunnlag
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.november
import no.nav.helse.oktober
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.serde.api.dto.EndringskodeDTO
import no.nav.helse.serde.api.v2.BeregnetPeriode
import no.nav.helse.serde.api.v2.Inntektkilde
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.finnSkjæringstidspunkt
import no.nav.helse.spleis.e2e.grunnlag
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.håndterOverstyrTidslinje
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
import no.nav.helse.testhelpers.inntektperioderForSammenligningsgrunnlag
import no.nav.helse.testhelpers.inntektperioderForSykepengegrunnlag
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class SpeilBuilderTest : AbstractEndToEndTest() {

    @Test
    fun `happy case`() {
        nyttVedtak(1.januar, 31.januar)
        val personDTO = speilApi()
        assertEquals(UNG_PERSON_FNR_2018.toString(), personDTO.fødselsnummer)
        assertEquals(1, personDTO.arbeidsgivere.size)
        assertNotNull(personDTO.versjon)
    }

    @Test
    fun `arbeidsgivere uten vedtaksperioder som skal vises i speil, filtreres bort`() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
        person.invaliderAllePerioder(hendelselogg, null)
        assertTrue(serializePersonForSpeil(person).arbeidsgivere.isEmpty())
    }

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
    fun `Akkumulerer inntekter fra a-orningen pr måned`() {
        val fom = 1.januar
        val tom = 31.januar

        håndterSykmelding(Sykmeldingsperiode(fom, tom, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(fom, tom, 100.prosent), sendtTilNAVEllerArbeidsgiver = fom.plusDays(1), orgnummer = a1)
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(fom til fom.plusDays(15)),
            refusjon = Inntektsmelding.Refusjon(beløp = 1000.månedlig, opphørsdato = null, endringerIRefusjon = emptyList()),
            beregnetInntekt = 1000.månedlig,
            orgnummer = a1
        )

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            inntektsvurdering = Inntektsvurdering(inntektperioderForSammenligningsgrunnlag {
                1.januar(2017) til 1.desember(2017) inntekter {
                    a1 inntekt 1000.månedlig
                    a2 inntekt 600.månedlig
                    a2 inntekt 400.månedlig
                }
            }),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter = inntektperioderForSykepengegrunnlag {
                1.oktober(2017) til 1.desember(2017) inntekter {
                    a1 inntekt 1000.månedlig
                    a2 inntekt 600.månedlig
                    a2 inntekt 400.månedlig
                }
            }, arbeidsforhold = emptyList()),
            arbeidsforhold = listOf(Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH), Vilkårsgrunnlag.Arbeidsforhold(a2, LocalDate.EPOCH)),
            orgnummer = a1
        )

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, automatiskBehandling = false)

        val personForSpeil = serializePersonForSpeil(person)
        val inntekterFraAOrdningenFraVilkårsgrunnlag = personForSpeil
            .vilkårsgrunnlagHistorikk
            .values
            .first()
            .values
            .first()
            .inntekter
            .first { it.organisasjonsnummer == a2 }
            .omregnetÅrsinntekt!!
            .inntekterFraAOrdningen!!
        assertEquals(3, inntekterFraAOrdningenFraVilkårsgrunnlag.size)
        assertTrue(inntekterFraAOrdningenFraVilkårsgrunnlag.all { it.sum == 1000.0 })
    }

    @Test
    fun `Flere arbeidsgivere med ghosts`() {
        val a5 = "567891234"
        håndterSykmelding(Sykmeldingsperiode(1.januar, 15.mars, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 15.mars, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1)

        val gamleITPerioder = arrayOf(
            ArbeidsgiverUtbetalingsperiode(a5, 1.januar(2009), 31.januar(2009), 100.prosent, 20000.månedlig)
        )
        val gamleITInntekter = listOf(Inntektsopplysning(a5, 1.januar(2009), 20000.månedlig, true))
        håndterYtelser(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            orgnummer = a1,
            utbetalinger = gamleITPerioder,
            inntektshistorikk = gamleITInntekter,
            besvart = LocalDateTime.MIN
        )
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 31000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 32000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(a4, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 1000.månedlig.repeat(2))
                )
            ),
            orgnummer = a1,
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                inntekter = listOf(
                    grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 31000.månedlig.repeat(3)),
                    grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), listOf(31000.månedlig, 32000.månedlig, 33000.månedlig)),
                    grunnlag(a4, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 1000.månedlig.repeat(2)),
                ), arbeidsforhold = emptyList()
            ),
            arbeidsforhold = listOf(
                Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH, null),
                Vilkårsgrunnlag.Arbeidsforhold(a2, LocalDate.EPOCH, null),
                Vilkårsgrunnlag.Arbeidsforhold(a3, LocalDate.EPOCH, null),
                Vilkårsgrunnlag.Arbeidsforhold(a4, LocalDate.EPOCH, 1.desember(2017))
            )
        )
        håndterYtelser(vedtaksperiodeIdInnhenter = 1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        val personDto = serializePersonForSpeil(person)

        assertEquals(listOf(a1, a2, a4).map(String::toString), personDto.arbeidsgivere.map { it.organisasjonsnummer })
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

    @Test
    fun `endringskode på oppdragslinjer`() {
        nyttVedtak(1.januar, 31.januar, 100.prosent, refusjon = Inntektsmelding.Refusjon(INNTEKT, 23.januar))
        assertEndringskoder(arbeidsgiverEndringskode = EndringskodeDTO.NY, personEndringskode = EndringskodeDTO.NY)
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(26.januar, Feriedag)))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()
        assertEndringskoder(arbeidsgiverEndringskode = EndringskodeDTO.UEND, personEndringskode = EndringskodeDTO.ENDR)
    }

    private fun assertEndringskoder(arbeidsgiverEndringskode: EndringskodeDTO, personEndringskode: EndringskodeDTO) {
        val personDto = serializePersonForSpeil(person)
        val beregnetPeriodetype = personDto.arbeidsgivere.first().generasjoner.first().perioder.first() as BeregnetPeriode
        assertEquals(arbeidsgiverEndringskode, beregnetPeriodetype.utbetaling.oppdrag.getValue(beregnetPeriodetype.utbetaling.arbeidsgiverFagsystemId).utbetalingslinjer.first().endringskode)
        assertEquals(personEndringskode, beregnetPeriodetype.utbetaling.oppdrag.getValue(beregnetPeriodetype.utbetaling.personFagsystemId).utbetalingslinjer.first().endringskode)
    }
}
