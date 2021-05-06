package no.nav.helse.serde.api

import no.nav.helse.Toggles
import no.nav.helse.hendelser.*
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.Inntektshistorikk
import no.nav.helse.person.Person
import no.nav.helse.person.PersonVisitor
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.person.infotrygdhistorikk.Utbetalingsperiode
import no.nav.helse.serde.api.builders.InntektshistorikkBuilder
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.testhelpers.*
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class InntektsgrunnlagTest : AbstractEndToEndTest() {

    private class FinnInntektshistorikk(person: Person, private val builder: InntektshistorikkBuilder) : PersonVisitor {
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
            builder.inntektshistorikk(orgnummer, inntektshistorikk)
        }
    }

    private infix fun LocalDate.og(sisteDato: LocalDate) = InntektshistorikkBuilder.NøkkeldataOmInntekt(sisteDato, skjæringstidspunkt = this)
    private infix fun InntektshistorikkBuilder.NøkkeldataOmInntekt.avvik(avviksprosent: Double) = this.also { it.avviksprosent = avviksprosent }

    @Test
    fun `Finner inntektsgrunnlag for en arbeidsgiver med inntekt satt av saksbehandler`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))

        val builder = InntektshistorikkBuilder(person)
        FinnInntektshistorikk(person, builder).also {
            (it.inntektshistorikk.getValue(ORGNUMMER)) {
                addSaksbehandler(1.januar, UUID.randomUUID(), INNTEKT)
            }
        }

        håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.januar, 16.januar)))

        håndterYtelser(1.vedtaksperiode)   // No history
        håndterVilkårsgrunnlag(
            1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
                inntekter = inntektperioder {
                    inntektsgrunnlag = Inntektsvurdering.Inntektsgrunnlag.SAMMENLIGNINGSGRUNNLAG
                    1.januar(2017) til 1.desember(2017) inntekter {
                        ORGNUMMER inntekt INNTEKT
                    }
                })
        )

        håndterYtelser(1.vedtaksperiode)   // No history

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

        håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.januar, 16.januar)))

        håndterYtelser(1.vedtaksperiode)

        håndterVilkårsgrunnlag(
            1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
                inntekter = inntektperioder {
                    inntektsgrunnlag = Inntektsvurdering.Inntektsgrunnlag.SAMMENLIGNINGSGRUNNLAG
                    1.januar(2017) til 1.desember(2017) inntekter {
                        ORGNUMMER inntekt INNTEKT
                    }
                    1.desember(2017) til 1.desember(2017) inntekter {
                        ORGNUMMER inntekt INNTEKT
                    }
                })
        )

        håndterYtelser(1.vedtaksperiode)

        val builder = InntektshistorikkBuilder(person)
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
        håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))

        val historikk = Utbetalingsperiode(ORGNUMMER, 1.oktober(2017),  31.desember(2017), 100.prosent, 1000.daglig)
        val inntekter = listOf(Inntektsopplysning(ORGNUMMER, 1.oktober(2017), INNTEKT, true))
        håndterUtbetalingshistorikk(1.vedtaksperiode, historikk, inntektshistorikk = inntekter)
        håndterYtelser(1.vedtaksperiode)

        val builder = InntektshistorikkBuilder(person)
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
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Søknadsperiode(1.januar, 16.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(25.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 25.januar)
        håndterSøknadMedValidering(2.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(25.januar, 31.januar, 100.prosent))

        håndterYtelser(
            2.vedtaksperiode,
            Utbetalingsperiode(ORGNUMMER, 17.januar,  24.januar, 100.prosent, 1000.daglig),
            inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 17.januar, INNTEKT, true))
        )

        val builder = InntektshistorikkBuilder(person)
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

    @Test
    fun `Finner inntektsgrunnlag for en arbeidsgiver med inntekt fra Skatt`() {
        // Hacker til både infotrygd- og skatteinntekter fordi vi ikke har fler arbeidsgivere eller henter sykepengegrunnlag fra inntektskompontenten enda
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))

        val historikk = Utbetalingsperiode(ORGNUMMER, 1.oktober(2017),  31.desember(2017), 100.prosent, 1000.daglig)
        val inntekter = listOf(Inntektsopplysning(ORGNUMMER, 1.november(2017), Inntekt.INGEN, true))
        håndterUtbetalingshistorikk(1.vedtaksperiode, historikk, inntektshistorikk = inntekter)

        val builder = InntektshistorikkBuilder(person)
        FinnInntektshistorikk(person, builder).also {
            inntektperioder {
                inntektsgrunnlag = Inntektsvurdering.Inntektsgrunnlag.SYKEPENGEGRUNNLAG
                1.juli(2017) til 1.september(2017) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }.forEach { inntekt ->
                inntekt.lagreInntekter(it.inntektshistorikk.getValue(ORGNUMMER), 1.oktober(2017), UUID.randomUUID())
            }
        }

        håndterYtelser(1.vedtaksperiode)

        builder.nøkkeldataOmInntekt(1.oktober(2017) og 31.januar)

        val inntektsgrunnlag = builder.build()

        assertTrue(inntektsgrunnlag.isNotEmpty())
        inntektsgrunnlag.single { it.skjæringstidspunkt == 1.oktober(2017) }.also { inntektsgrunnlaget ->
            assertEquals(INNTEKT.reflection { årlig, _, _, _ -> årlig }, inntektsgrunnlaget.sykepengegrunnlag)
            assertEquals(INNTEKT.reflection { årlig, _, _, _ -> årlig }, inntektsgrunnlaget.omregnetÅrsinntekt)
            assertNull(inntektsgrunnlaget.sammenligningsgrunnlag)
            assertNull(inntektsgrunnlaget.avviksprosent)
            assertEquals(1430.7692307692307, inntektsgrunnlaget.maksUtbetalingPerDag)
            requireNotNull(inntektsgrunnlaget.inntekter.single { it.arbeidsgiver == ORGNUMMER }.omregnetÅrsinntekt).also { omregnetÅrsinntekt ->
                assertEquals(InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.OmregnetÅrsinntektDTO.InntektkildeDTO.AOrdningen, omregnetÅrsinntekt.kilde)
                assertEquals(INNTEKT.reflection { årlig, _, _, _ -> årlig }, omregnetÅrsinntekt.beløp)
                assertEquals(INNTEKT.reflection { _, mnd, _, _ -> mnd }, omregnetÅrsinntekt.månedsbeløp)
                omregnetÅrsinntekt.inntekterFraAOrdningen.also {
                    requireNotNull(it)
                    assertEquals(3, it.size)
                    repeat(3) { index ->
                        assertEquals(INNTEKT.reflection { _, mnd, _, _ -> mnd }, it[index].sum)
                    }
                }
            }
        }
    }

    @Test
    fun `Finner inntektsgrunnlag for en arbeidsgiver med en kopiert inntekt`() {
        Toggles.PraksisendringEnabled.enable()
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))

        håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.januar, 16.januar)))

        håndterYtelser(1.vedtaksperiode)

        håndterVilkårsgrunnlag(
            1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
                inntekter = inntektperioder {
                    inntektsgrunnlag = Inntektsvurdering.Inntektsgrunnlag.SAMMENLIGNINGSGRUNNLAG
                    1.januar(2017) til 1.desember(2017) inntekter {
                        ORGNUMMER inntekt INNTEKT
                    }
                })
        )

        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(10.februar, 28.februar, 100.prosent))
        håndterSøknadMedValidering(2.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(10.februar, 28.februar, 100.prosent))

        val nyttSammenlingningsGrunnlag = INNTEKT + 1000.månedlig

        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(
            2.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
                inntekter = inntektperioder {
                    inntektsgrunnlag = Inntektsvurdering.Inntektsgrunnlag.SAMMENLIGNINGSGRUNNLAG

                    1.februar(2017) til 1.januar(2018) inntekter {
                        ORGNUMMER inntekt nyttSammenlingningsGrunnlag
                    }
                })
        )

        val builder = InntektshistorikkBuilder(person)
        builder.nøkkeldataOmInntekt(10.februar og 28.februar avvik 7.7)

        FinnInntektshistorikk(person, builder)
        val inntektsgrunnlag = builder.build()

        assertTrue(inntektsgrunnlag.isNotEmpty())
        inntektsgrunnlag.single { it.skjæringstidspunkt == 10.februar }.also { inntektsgrunnlaget ->
            assertEquals(INNTEKT.reflection { årlig, _, _, _ -> årlig }, inntektsgrunnlaget.sykepengegrunnlag)
            assertEquals(INNTEKT.reflection { årlig, _, _, _ -> årlig }, inntektsgrunnlaget.omregnetÅrsinntekt)
            assertEquals(nyttSammenlingningsGrunnlag.reflection { _, mnd, _, _ -> mnd } * 12, inntektsgrunnlaget.sammenligningsgrunnlag)
            assertEquals(7.7, inntektsgrunnlaget.avviksprosent)
            assertEquals(1430.7692307692307, inntektsgrunnlaget.maksUtbetalingPerDag)
            requireNotNull(inntektsgrunnlaget.inntekter.single { it.arbeidsgiver == ORGNUMMER }.omregnetÅrsinntekt).also { omregnetÅrsinntekt ->
                assertEquals(InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.OmregnetÅrsinntektDTO.InntektkildeDTO.Inntektsmelding, omregnetÅrsinntekt.kilde)
                assertEquals(INNTEKT.reflection { årlig, _, _, _ -> årlig }, omregnetÅrsinntekt.beløp)
                assertEquals(INNTEKT.reflection { _, mnd, _, _ -> mnd }, omregnetÅrsinntekt.månedsbeløp)
            }
        }
        inntektsgrunnlag.single { it.skjæringstidspunkt == 10.februar }.inntekter.single { it.arbeidsgiver == ORGNUMMER }.sammenligningsgrunnlag.also { sammenligningsgrunnlag ->
            requireNotNull(sammenligningsgrunnlag)
            assertEquals(nyttSammenlingningsGrunnlag.reflection { _, mnd, _, _ -> mnd } * 12, sammenligningsgrunnlag.beløp)
            sammenligningsgrunnlag.inntekterFraAOrdningen.also {
                assertEquals(12, it.size)
                it.forEach { inntekt -> assertEquals(nyttSammenlingningsGrunnlag.reflection { _, mnd, _, _ -> mnd }, inntekt.sum) }
            }
        }
        Toggles.PraksisendringEnabled.pop()
    }

    @Test
    fun `omregnet årsinntekt kan være null`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        val builder = InntektshistorikkBuilder(person)
        FinnInntektshistorikk(person, builder).also {
            inntektperioder {
                inntektsgrunnlag = Inntektsvurdering.Inntektsgrunnlag.SYKEPENGEGRUNNLAG
                1.april til 1.juni inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }.forEach { inntekt ->
                inntekt.lagreInntekter(it.inntektshistorikk.getValue(ORGNUMMER), 1.juni, UUID.randomUUID())
            }
        }

        builder.nøkkeldataOmInntekt(1.oktober og 31.desember)

        val inntektsgrunnlag = builder.build()

        assertTrue(inntektsgrunnlag.isNotEmpty())
        inntektsgrunnlag.single { it.skjæringstidspunkt == 1.oktober }.also { inntektsgrunnlaget ->
            assertNull(inntektsgrunnlaget.sykepengegrunnlag)
            assertNull(inntektsgrunnlaget.omregnetÅrsinntekt)
            assertNull(inntektsgrunnlaget.sammenligningsgrunnlag)
            assertNull(inntektsgrunnlaget.avviksprosent)
            assertNull(inntektsgrunnlaget.maksUtbetalingPerDag)
            assertNull(inntektsgrunnlaget.inntekter.single { it.arbeidsgiver == ORGNUMMER }.omregnetÅrsinntekt)
        }
    }
}
