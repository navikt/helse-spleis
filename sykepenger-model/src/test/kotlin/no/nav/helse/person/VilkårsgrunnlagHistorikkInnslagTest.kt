package no.nav.helse.person

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.Fødselsnummer
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.person.etterlevelse.MaskinellJurist
import no.nav.helse.sykepengegrunnlag
import no.nav.helse.testhelpers.NAV
import no.nav.helse.testhelpers.assertNotNull
import no.nav.helse.testhelpers.tidslinjeOf
import no.nav.helse.utbetalingstidslinje.Begrunnelse
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosent.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class VilkårsgrunnlagHistorikkInnslagTest {
    private lateinit var innslag: VilkårsgrunnlagHistorikk.Innslag

    private companion object {
        private const val UNG_PERSON_FNR_2018 = "12029240045"
        private val ALDER = Fødselsnummer.tilFødselsnummer(UNG_PERSON_FNR_2018).alder()
    }

    @BeforeEach
    fun beforeEach() {
        innslag = VilkårsgrunnlagHistorikk.Innslag(null, emptyList())
    }

    @Test
    fun `finner ikke begrunnelser dersom vilkårsgrunnlag ikke er Grunnlagsdata`() {
        val tidslinjer = listOf(tidslinjeOf(1.NAV))
        innslag.add(1.januar, testgrunnlag)
        innslag.avvis(tidslinjer)
        assertEquals(0, avvisteDager(tidslinjer).size)
    }

    @Test
    fun `finner kun begrunnelser fra vilkårsgrunnlag som er Grunnlagsdata`() {
        val tidslinjer = listOf(tidslinjeOf(1.NAV))
        innslag.add(1.januar, testgrunnlag)
        innslag.add(1.januar, grunnlagsdata(1.januar, harOpptjening = false))
        innslag.avvis(tidslinjer)
        val avvisteDager = avvisteDager(tidslinjer)
        assertEquals(1, avvisteDager.size)
        assertNotNull(avvisteDager.first().erAvvistMed(Begrunnelse.ManglerOpptjening))
    }

    @Test
    fun `avviser dager uten opptjening`() {
        val tidslinjer = listOf(tidslinjeOf(1.NAV))
        innslag.add(1.januar, grunnlagsdata(1.januar, harOpptjening = false))
        innslag.avvis(tidslinjer)
        val avvisteDager = avvisteDager(tidslinjer)
        assertEquals(1, avvisteDager.size)
        assertNotNull(avvisteDager.first().erAvvistMed(Begrunnelse.ManglerOpptjening))
    }

    @Test
    fun `avviser ikke dager dersom vurdert ok`() {
        val tidslinjer = listOf(tidslinjeOf(1.NAV))
        innslag.add(1.januar, grunnlagsdata(1.januar, harOpptjening = true, harMinimumInntekt = true, erMedlem = true))
        innslag.avvis(tidslinjer)
        assertEquals(0, avvisteDager(tidslinjer).size)
    }

    @Test
    fun `avviser med flere begrunnelser`() {
        val tidslinjer = listOf(tidslinjeOf(1.NAV))
        innslag.add(1.januar, grunnlagsdata(1.januar, harOpptjening = false, harMinimumInntekt = false))
        innslag.avvis(tidslinjer)
        val avvisteDager = avvisteDager(tidslinjer)
        assertEquals(1, avvisteDager.size)
        avvisteDager.first().also {
            assertNotNull(it.erAvvistMed(Begrunnelse.MinimumInntekt))
            assertNotNull(it.erAvvistMed(Begrunnelse.ManglerOpptjening))
        }
    }

    @Test
    fun `avviser på tvers av vilkårsgrunnlagelementer`() {
        val tidslinjer = listOf(tidslinjeOf(2.NAV, skjæringstidspunkter = listOf(1.januar, 2.januar)))
        innslag.add(1.januar, grunnlagsdata(1.januar, harOpptjening = false))
        innslag.add(2.januar, grunnlagsdata(2.januar, harMinimumInntekt = false))
        innslag.avvis(tidslinjer)
        val avvisteDager = avvisteDager(tidslinjer)
        assertEquals(2, avvisteDager.size)
        assertNotNull(avvisteDager.first().erAvvistMed(Begrunnelse.ManglerOpptjening))
        assertNotNull(avvisteDager.last().erAvvistMed(Begrunnelse.MinimumInntekt))
    }

    @Test
    fun `oppfyller ingen inngangsvilkår deler av perioden`() {
        val tidslinjer = listOf(tidslinjeOf(2.NAV, skjæringstidspunkter = listOf(1.januar, 2.januar)))
        innslag.add(1.januar, grunnlagsdata(1.januar))
        innslag.add(2.januar, grunnlagsdata(2.januar, harOpptjening = false, harMinimumInntekt = false, erMedlem = false))
        innslag.avvis(tidslinjer)
        val avvisteDager = avvisteDager(tidslinjer)
        assertEquals(1, avvisteDager.size)
        avvisteDager.first().also {
            assertNotNull(it.erAvvistMed(Begrunnelse.ManglerOpptjening))
            assertNotNull(it.erAvvistMed(Begrunnelse.MinimumInntekt))
            assertNotNull(it.erAvvistMed(Begrunnelse.ManglerMedlemskap))
        }
    }

    private fun grunnlagsdata(skjæringstidspunkt: LocalDate, harOpptjening: Boolean = true, harMinimumInntekt: Boolean = true, erMedlem: Boolean = true): VilkårsgrunnlagHistorikk.Grunnlagsdata {
        val opptjening = if (!harOpptjening) emptyList() else listOf(
            Opptjening.ArbeidsgiverOpptjeningsgrunnlag("orgnr", listOf(
                Arbeidsforholdhistorikk.Arbeidsforhold(skjæringstidspunkt.minusYears(1), null, false)
            ))
        )
        val inntekt = if (!harMinimumInntekt) 2000.månedlig else 25000.månedlig
        return VilkårsgrunnlagHistorikk.Grunnlagsdata(
            skjæringstidspunkt = skjæringstidspunkt,
            sykepengegrunnlag = Sykepengegrunnlag.opprett(ALDER, listOf(
                ArbeidsgiverInntektsopplysning("orgnr", Inntektshistorikk.Saksbehandler(UUID.randomUUID(), skjæringstidspunkt, UUID.randomUUID(), inntekt))
            ), skjæringstidspunkt, MaskinellJurist(), emptyList()),
            sammenligningsgrunnlag = Sammenligningsgrunnlag(emptyList()),
            avviksprosent = 0.0.prosent,
            opptjening = Opptjening.opptjening(opptjening, 1.januar, MaskinellJurist()),
            medlemskapstatus = when (erMedlem) {
                true -> Medlemskapsvurdering.Medlemskapstatus.Ja
                false -> Medlemskapsvurdering.Medlemskapstatus.Nei
            },
            vurdertOk = harOpptjening && harMinimumInntekt && erMedlem,
            meldingsreferanseId = UUID.randomUUID(),
            vilkårsgrunnlagId = UUID.randomUUID()
        )
    }

    private fun avvisteDager(tidslinjer: List<Utbetalingstidslinje>): List<Utbetalingstidslinje.Utbetalingsdag.AvvistDag> {
        return tidslinjer.flatMap { it.inspektør.avvistedager }
    }

    private val testgrunnlag
        get() = object : VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement {
            override fun add(innslag: VilkårsgrunnlagHistorikk.Innslag) {
                innslag.add(1.januar, this)
            }

            override fun accept(vilkårsgrunnlagHistorikkVisitor: VilkårsgrunnlagHistorikkVisitor) {}

            override fun sykepengegrunnlag() = Inntekt.INGEN.sykepengegrunnlag

            override fun grunnlagsBegrensning() = Sykepengegrunnlag.Begrensning.ER_IKKE_6G_BEGRENSET

            override fun grunnlagForSykepengegrunnlag() = Inntekt.INGEN

            override fun sammenligningsgrunnlag() = Inntekt.INGEN

            override fun sammenligningsgrunnlagPerArbeidsgiver(): Map<String, Inntektshistorikk.Inntektsopplysning> = emptyMap()

            override fun gjelderFlereArbeidsgivere() = false

            override fun valider(aktivitetslogg: Aktivitetslogg) {}

            override fun toSpesifikkKontekst(): SpesifikkKontekst {
                return SpesifikkKontekst(
                    "testgrunnlag",
                    emptyMap()
                )
            }
        }
}
