package no.nav.helse.serde.api

import no.nav.helse.Toggles
import no.nav.helse.hendelser.*
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.InntektshistorikkVol2
import no.nav.helse.person.Person
import no.nav.helse.person.PersonVisitor
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.testhelpers.*
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class InntektsgrunnlagTest : AbstractEndToEndTest() {

    private class FinnInntektshistorikk(person: Person) : PersonVisitor {
        val inntektshistorikk = mutableMapOf<String, InntektshistorikkVol2>()
        private lateinit var orgnummer: String

        init {
            person.accept(this)
        }

        override fun preVisitArbeidsgiver(arbeidsgiver: Arbeidsgiver, id: UUID, organisasjonsnummer: String) {
            orgnummer = organisasjonsnummer
        }

        override fun preVisitInntekthistorikkVol2(inntektshistorikk: InntektshistorikkVol2) {
            this.inntektshistorikk[orgnummer] = inntektshistorikk
        }
    }

    @BeforeEach
    fun setup() {
        Toggles.NyInntekt.enabled = true
    }

    @AfterEach
    fun tearDown() {
        Toggles.NyInntekt.enabled = false
    }

    private infix fun LocalDate.og(sisteDato: LocalDate) = SpeilBuilder.NøkkeldataOmInntekt(sisteDato).also { it.skjæringstidspunkt = this }
    private infix fun SpeilBuilder.NøkkeldataOmInntekt.avvik(avviksprosent: Double) = this.also { it.avviksprosent = avviksprosent }

    @Test
    fun `Finner inntektsgrunnlag for en arbeidsgiver med inntekt satt av saksbehandler`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))

        val inntektshistorikk = FinnInntektshistorikk(person).inntektshistorikk.also {
            (it.getValue(ORGNUMMER)){
                addSaksbehandler(1.januar, UUID.randomUUID(), INNTEKT)
            }
        }

        håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.januar, 16.januar)))
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
        val dataForSkjæringstidspunkt = listOf(1.januar og 31.januar avvik 0.0)

        val inntektsgrunnlag = inntektsgrunnlag(person, inntektshistorikk, dataForSkjæringstidspunkt)

        assertTrue(inntektsgrunnlag.isNotEmpty())
        inntektsgrunnlag.single { it.skjæringstidspunkt == 1.januar }.also { inntektsgrunnlaget ->
            assertEquals(INNTEKT.reflection { årlig, _, _, _ -> årlig }, inntektsgrunnlaget.sykepengegrunnlag)
            assertEquals(INNTEKT.reflection { årlig, _, _, _ -> årlig }, inntektsgrunnlaget.omregnetÅrsinntekt)
            assertEquals(INNTEKT.reflection { årlig, _, _, _ -> årlig }, inntektsgrunnlaget.sammenligningsgrunnlag)
            assertEquals(0.0, inntektsgrunnlaget.avviksprosent)
            assertEquals(1430.7692307692307, inntektsgrunnlaget.maksUtbetalingPerDag)
            inntektsgrunnlaget.inntekter.single { it.arbeidsgiver == ORGNUMMER }.omregnetÅrsinntekt.also { omregnetÅrsinntekt ->
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

        håndterYtelser(1.vedtaksperiode)   // No history

        val dataForSkjæringstidspunkt = listOf(1.januar og 31.januar avvik 7.7)

        val inntektshistorikk = FinnInntektshistorikk(person).inntektshistorikk
        val inntektsgrunnlag = inntektsgrunnlag(person, inntektshistorikk, dataForSkjæringstidspunkt)

        assertTrue(inntektsgrunnlag.isNotEmpty())
        inntektsgrunnlag.single { it.skjæringstidspunkt == 1.januar }.also { inntektsgrunnlaget ->
            assertEquals(INNTEKT.reflection { årlig, _, _, _ -> årlig }, inntektsgrunnlaget.sykepengegrunnlag)
            assertEquals(INNTEKT.reflection { årlig, _, _, _ -> årlig }, inntektsgrunnlaget.omregnetÅrsinntekt)
            assertEquals(INNTEKT.reflection { _, mnd, _, _ -> mnd } * 13, inntektsgrunnlaget.sammenligningsgrunnlag)
            assertEquals(7.7, inntektsgrunnlaget.avviksprosent)
            assertEquals(1430.7692307692307, inntektsgrunnlaget.maksUtbetalingPerDag)
            inntektsgrunnlaget.inntekter.single { it.arbeidsgiver == ORGNUMMER }.omregnetÅrsinntekt.also { omregnetÅrsinntekt ->
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
                repeat(11) { index ->
                    assertEquals(INNTEKT.reflection { _, mnd, _, _ -> mnd }, it[index].sum)
                }
                assertEquals(INNTEKT.reflection { _, mnd, _, _ -> mnd } * 2, it[11].sum)
            }
        }
    }

    @Test
    fun `Finner inntektsgrunnlag for en arbeidsgiver med inntekt fra Infotrygd`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))

        håndterUtbetalingshistorikk(
            1.vedtaksperiode, Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(1.oktober(2017), 31.desember(2017), 1000, 100, ORGNUMMER),
            inntektshistorikk = listOf(Utbetalingshistorikk.Inntektsopplysning(1.oktober(2017), INNTEKT, ORGNUMMER, true))
        )

        håndterYtelser(
            1.vedtaksperiode,
            Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(1.oktober(2017), 31.desember(2017), 1000, 100, ORGNUMMER),
            inntektshistorikk = listOf(Utbetalingshistorikk.Inntektsopplysning(1.oktober(2017), INNTEKT, ORGNUMMER, true))
        )

        val dataForSkjæringstidspunkt = listOf(1.oktober(2017) og 31.januar)

        val inntektshistorikk = FinnInntektshistorikk(person).inntektshistorikk
        val inntektsgrunnlag = inntektsgrunnlag(person, inntektshistorikk, dataForSkjæringstidspunkt)

        assertTrue(inntektsgrunnlag.isNotEmpty())
        inntektsgrunnlag.single { it.skjæringstidspunkt == 1.oktober(2017) }.also { inntektsgrunnlaget ->
            assertEquals(INNTEKT.reflection { årlig, _, _, _ -> årlig }, inntektsgrunnlaget.sykepengegrunnlag)
            assertEquals(INNTEKT.reflection { årlig, _, _, _ -> årlig }, inntektsgrunnlaget.omregnetÅrsinntekt)
            assertEquals(0.0, inntektsgrunnlaget.sammenligningsgrunnlag)
            assertNull(inntektsgrunnlaget.avviksprosent)
            assertEquals(1430.7692307692307, inntektsgrunnlaget.maksUtbetalingPerDag)
            inntektsgrunnlaget.inntekter.single { it.arbeidsgiver == ORGNUMMER }.omregnetÅrsinntekt.also { omregnetÅrsinntekt ->
                assertEquals(InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.OmregnetÅrsinntektDTO.InntektkildeDTO.Infotrygd, omregnetÅrsinntekt.kilde)
                assertEquals(INNTEKT.reflection { årlig, _, _, _ -> årlig }, omregnetÅrsinntekt.beløp)
                assertEquals(INNTEKT.reflection { _, mnd, _, _ -> mnd }, omregnetÅrsinntekt.månedsbeløp)
            }
        }
        assertNull(inntektsgrunnlag.single { it.skjæringstidspunkt == 1.oktober(2017) }.inntekter.single { it.arbeidsgiver == ORGNUMMER }.sammenligningsgrunnlag)
    }

    @Test
    fun `Finner inntektsgrunnlag for en arbeidsgiver med inntekt fra Skatt`() {
        // Hacker til både infotrygd- og skatteinntekter fordi vi ikke har fler arbeidsgivere eller henter sykepengegrunnlag fra inntektskompontenten enda
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))

        håndterUtbetalingshistorikk(
            1.vedtaksperiode, Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(1.oktober(2017), 31.desember(2017), 1000, 100, ORGNUMMER),
            inntektshistorikk = listOf(Utbetalingshistorikk.Inntektsopplysning(1.november(2017), Inntekt.INGEN, ORGNUMMER, true))
        )

        val inntektshistorikk = FinnInntektshistorikk(person).inntektshistorikk.also {
            inntektperioder {
                inntektsgrunnlag = Inntektsvurdering.Inntektsgrunnlag.SYKEPENGEGRUNNLAG
                1.juli(2017) til 1.september(2017) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }.forEach { inntekt -> inntekt.lagreInntekter(it.getValue(ORGNUMMER), 1.oktober(2017), UUID.randomUUID()) }
        }

        håndterYtelser(
            1.vedtaksperiode,
            Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(1.oktober(2017), 31.desember(2017), 1000, 100, ORGNUMMER),
            inntektshistorikk = listOf(Utbetalingshistorikk.Inntektsopplysning(1.november(2017), Inntekt.INGEN, ORGNUMMER, true))
        )

        val dataForSkjæringstidspunkt = listOf(1.oktober(2017) og 31.januar)

        val inntektsgrunnlag = inntektsgrunnlag(person, inntektshistorikk, dataForSkjæringstidspunkt)

        assertTrue(inntektsgrunnlag.isNotEmpty())
        inntektsgrunnlag.single { it.skjæringstidspunkt == 1.oktober(2017) }.also { inntektsgrunnlaget ->
            assertEquals(INNTEKT.reflection { årlig, _, _, _ -> årlig }, inntektsgrunnlaget.sykepengegrunnlag)
            assertEquals(INNTEKT.reflection { årlig, _, _, _ -> årlig }, inntektsgrunnlaget.omregnetÅrsinntekt)
            assertEquals(0.0, inntektsgrunnlaget.sammenligningsgrunnlag)
            assertNull(inntektsgrunnlaget.avviksprosent)
            assertEquals(1430.7692307692307, inntektsgrunnlaget.maksUtbetalingPerDag)
            inntektsgrunnlaget.inntekter.single { it.arbeidsgiver == ORGNUMMER }.omregnetÅrsinntekt.also { omregnetÅrsinntekt ->
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
}
