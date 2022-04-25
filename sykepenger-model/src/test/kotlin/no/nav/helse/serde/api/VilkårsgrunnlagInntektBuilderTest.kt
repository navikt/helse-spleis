package no.nav.helse.serde.api

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.desember
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.oktober
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.Inntektshistorikk
import no.nav.helse.person.Person
import no.nav.helse.person.PersonVisitor
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.serde.api.builders.VilkårsgrunnlagInntektBuilder
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.håndterInntektsmeldingMedValidering
import no.nav.helse.spleis.e2e.håndterSykmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterSøknadMedValidering
import no.nav.helse.spleis.e2e.håndterUtbetalingshistorikk
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlag
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.testhelpers.inntektperioderForSammenligningsgrunnlag
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class VilkårsgrunnlagInntektBuilderTest : AbstractEndToEndTest() {

    private class FinnInntektshistorikk(person: Person, private val builder: VilkårsgrunnlagInntektBuilder) : PersonVisitor {
        val inntektshistorikk = mutableMapOf<String, Inntektshistorikk>()
        private lateinit var orgnummer: String

        init {
            person.accept(this)
        }

        override fun preVisitArbeidsgiver(arbeidsgiver: Arbeidsgiver, id: UUID, organisasjonsnummer: String) {
            orgnummer = organisasjonsnummer
        }

        override fun preVisitInntekthistorikk(inntektshistorikk: Inntektshistorikk) {
            this.inntektshistorikk[orgnummer] = inntektshistorikk
        }
    }

    private infix fun LocalDate.og(sisteDato: LocalDate) = VilkårsgrunnlagInntektBuilder.NøkkeldataOmInntekt(sisteDato, skjæringstidspunkt = this)
    private infix fun VilkårsgrunnlagInntektBuilder.NøkkeldataOmInntekt.avvik(avviksprosent: Double) = this.also { it.avviksprosent = avviksprosent }

    @Test
    fun `Finner inntektsgrunnlag for en arbeidsgiver med inntekt satt av saksbehandler`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))

        val builder = VilkårsgrunnlagInntektBuilder(person)
        FinnInntektshistorikk(person, builder).also {
            (it.inntektshistorikk.getValue(ORGNUMMER)).append {
                addSaksbehandler(1.januar, UUID.randomUUID(), INNTEKT)
            }
        }

        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.januar, 16.januar)))

        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
                inntekter = inntektperioderForSammenligningsgrunnlag{ 1.januar(2017) til 1.desember(2017) inntekter {
                        ORGNUMMER inntekt INNTEKT
                    }
                })
        )

        håndterYtelser(1.vedtaksperiode)

        builder.nøkkeldataOmInntekt(1.januar og 31.januar avvik 0.0)
        val inntektsgrunnlag = builder.build()

        assertTrue(inntektsgrunnlag.isNotEmpty())
        inntektsgrunnlag.single { it.skjæringstidspunkt == 1.januar }.also { inntektsgrunnlaget ->
            assertEquals(INNTEKT.reflection { årlig, _, _, _ -> årlig }, inntektsgrunnlaget.sykepengegrunnlag)
            assertEquals(INNTEKT.reflection { årlig, _, _, _ -> årlig }, inntektsgrunnlaget.omregnetÅrsinntekt)
            assertEquals(INNTEKT.reflection { årlig, _, _, _ -> årlig }, inntektsgrunnlaget.sammenligningsgrunnlag)
            assertEquals(0.0, inntektsgrunnlaget.avviksprosent)
            assertEquals(1430.7692307692307, inntektsgrunnlaget.maksUtbetalingPerDag)
            requireNotNull(inntektsgrunnlaget.inntekter.single { it.arbeidsgiver == ORGNUMMER }.omregnetÅrsinntekt).also { omregnetÅrsinntekt ->
                assertEquals(InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.OmregnetÅrsinntektDTO.InntektkildeDTO.Saksbehandler, omregnetÅrsinntekt.kilde)
                assertEquals(INNTEKT.reflection { årlig, _, _, _ -> årlig }, omregnetÅrsinntekt.beløp)
                assertEquals(INNTEKT.reflection { _, mnd, _, _ -> mnd }, omregnetÅrsinntekt.månedsbeløp)
            }
        }
    }

    @Test
    fun `Finner inntektsgrunnlag for en arbeidsgiver med en inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))

        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.januar, 16.januar)))

        håndterYtelser(1.vedtaksperiode)

        håndterVilkårsgrunnlag(
            1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
                inntekter = inntektperioderForSammenligningsgrunnlag{ 1.januar(2017) til 1.desember(2017) inntekter {
                        ORGNUMMER inntekt INNTEKT
                    }
                    1.desember(2017) til 1.desember(2017) inntekter {
                        ORGNUMMER inntekt INNTEKT
                    }
                })
        )

        håndterYtelser(1.vedtaksperiode)

        val builder = VilkårsgrunnlagInntektBuilder(person)
        builder.nøkkeldataOmInntekt(1.januar og 31.januar avvik 7.7)
        FinnInntektshistorikk(person, builder)
        val inntektsgrunnlag = builder.build()

        assertTrue(inntektsgrunnlag.isNotEmpty())
        inntektsgrunnlag.single { it.skjæringstidspunkt == 1.januar }.also { inntektsgrunnlaget ->
            assertEquals(INNTEKT.reflection { årlig, _, _, _ -> årlig }, inntektsgrunnlaget.sykepengegrunnlag)
            assertEquals(INNTEKT.reflection { årlig, _, _, _ -> årlig }, inntektsgrunnlaget.omregnetÅrsinntekt)
            assertEquals(INNTEKT.reflection { _, mnd, _, _ -> mnd } * 13, inntektsgrunnlaget.sammenligningsgrunnlag)
            assertEquals(7.7, inntektsgrunnlaget.avviksprosent)
            assertEquals(1430.7692307692307, inntektsgrunnlaget.maksUtbetalingPerDag)
            requireNotNull(inntektsgrunnlaget.inntekter.single { it.arbeidsgiver == ORGNUMMER }.omregnetÅrsinntekt).also { omregnetÅrsinntekt ->
                assertEquals(InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.OmregnetÅrsinntektDTO.InntektkildeDTO.Inntektsmelding, omregnetÅrsinntekt.kilde)
                assertEquals(INNTEKT.reflection { årlig, _, _, _ -> årlig }, omregnetÅrsinntekt.beløp)
                assertEquals(INNTEKT.reflection { _, mnd, _, _ -> mnd }, omregnetÅrsinntekt.månedsbeløp)
            }
        }
        inntektsgrunnlag.single { it.skjæringstidspunkt == 1.januar }.inntekter.single { it.arbeidsgiver == ORGNUMMER }.sammenligningsgrunnlag.also { sammenligningsgrunnlag ->
            requireNotNull(sammenligningsgrunnlag)
            assertEquals(INNTEKT.reflection { _, mnd, _, _ -> mnd } * 13, sammenligningsgrunnlag.beløp)
            sammenligningsgrunnlag.inntekterFraAOrdningen.also {
                assertEquals(12, it.size)
                it.take(11).forEach { inntekt -> assertEquals(INNTEKT.reflection { _, mnd, _, _ -> mnd }, inntekt.sum) }
                assertEquals(INNTEKT.reflection { _, mnd, _, _ -> mnd } * 2, it[11].sum)
            }
        }
    }

    @Test
    fun `Finner inntektsgrunnlag for en arbeidsgiver med inntekt fra Infotrygd`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.januar, 31.januar, 100.prosent))

        val historikk = ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.oktober(2017),  31.desember(2017), 100.prosent, 1000.daglig)
        val inntekter = listOf(Inntektsopplysning(ORGNUMMER, 1.oktober(2017), INNTEKT, true))
        håndterUtbetalingshistorikk(1.vedtaksperiode, historikk, inntektshistorikk = inntekter)
        håndterYtelser(1.vedtaksperiode)

        val builder = VilkårsgrunnlagInntektBuilder(person)
        builder.nøkkeldataOmInntekt(1.oktober(2017) og 31.januar)

        FinnInntektshistorikk(person, builder)
        val inntektsgrunnlag = builder.build()

        assertTrue(inntektsgrunnlag.isNotEmpty())
        inntektsgrunnlag.single { it.skjæringstidspunkt == 1.oktober(2017) }.also { inntektsgrunnlaget ->
            assertEquals(INNTEKT.reflection { årlig, _, _, _ -> årlig }, inntektsgrunnlaget.sykepengegrunnlag)
            assertEquals(INNTEKT.reflection { årlig, _, _, _ -> årlig }, inntektsgrunnlaget.omregnetÅrsinntekt)
            assertNull(inntektsgrunnlaget.sammenligningsgrunnlag)
            assertNull(inntektsgrunnlaget.avviksprosent)
            assertEquals(1430.7692307692307, inntektsgrunnlaget.maksUtbetalingPerDag)
            requireNotNull(inntektsgrunnlaget.inntekter.single { it.arbeidsgiver == ORGNUMMER }.omregnetÅrsinntekt).also { omregnetÅrsinntekt ->
                assertEquals(InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.OmregnetÅrsinntektDTO.InntektkildeDTO.Infotrygd, omregnetÅrsinntekt.kilde)
                assertEquals(INNTEKT.reflection { årlig, _, _, _ -> årlig }, omregnetÅrsinntekt.beløp)
                assertEquals(INNTEKT.reflection { _, mnd, _, _ -> mnd }, omregnetÅrsinntekt.månedsbeløp)
            }
        }
        assertNull(inntektsgrunnlag.single { it.skjæringstidspunkt == 1.oktober(2017) }.inntekter.single { it.arbeidsgiver == ORGNUMMER }.sammenligningsgrunnlag)
    }

    @Test
    fun `Finner inntektsgrunnlag for en arbeidsgiver med inntekt fra Infotrygd på senere dato enn skjærinstidspunkt`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 16.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 16.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode, besvart = LocalDate.EPOCH.atStartOfDay())

        håndterSykmelding(Sykmeldingsperiode(25.januar, 31.januar, 100.prosent))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(25.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 25.januar)
        håndterYtelser(
            2.vedtaksperiode,
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 17.januar,  24.januar, 100.prosent, 1000.daglig),
            inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 17.januar, INNTEKT, true))
        )

        val builder = VilkårsgrunnlagInntektBuilder(person)
        builder.nøkkeldataOmInntekt(1.januar og 31.januar)

        FinnInntektshistorikk(person, builder)
        val inntektsgrunnlag = builder.build()

        assertTrue(inntektsgrunnlag.isNotEmpty())
        inntektsgrunnlag.single { it.skjæringstidspunkt == 1.januar }.also { inntektsgrunnlaget ->
            assertEquals(INNTEKT.reflection { årlig, _, _, _ -> årlig }, inntektsgrunnlaget.sykepengegrunnlag)
            assertEquals(INNTEKT.reflection { årlig, _, _, _ -> årlig }, inntektsgrunnlaget.omregnetÅrsinntekt)
            assertNull(inntektsgrunnlaget.sammenligningsgrunnlag)
            assertNull(inntektsgrunnlaget.avviksprosent)
            assertEquals(1430.7692307692307, inntektsgrunnlaget.maksUtbetalingPerDag)
            requireNotNull(inntektsgrunnlaget.inntekter.single { it.arbeidsgiver == ORGNUMMER }.omregnetÅrsinntekt).also { omregnetÅrsinntekt ->
                assertEquals(InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.OmregnetÅrsinntektDTO.InntektkildeDTO.Infotrygd, omregnetÅrsinntekt.kilde)
                assertEquals(INNTEKT.reflection { årlig, _, _, _ -> årlig }, omregnetÅrsinntekt.beløp)
                assertEquals(INNTEKT.reflection { _, mnd, _, _ -> mnd }, omregnetÅrsinntekt.månedsbeløp)
            }
        }
        assertNull(inntektsgrunnlag.single { it.skjæringstidspunkt == 1.januar }.inntekter.single { it.arbeidsgiver == ORGNUMMER }.sammenligningsgrunnlag)
    }
}
