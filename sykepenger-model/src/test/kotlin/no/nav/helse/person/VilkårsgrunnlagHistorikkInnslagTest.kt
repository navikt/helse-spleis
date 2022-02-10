package no.nav.helse.person

import no.nav.helse.Fødselsnummer
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.inspectors.inspektør
import no.nav.helse.testhelpers.NAV
import no.nav.helse.testhelpers.assertNotNull
import no.nav.helse.januar
import no.nav.helse.person.etterlevelse.MaskinellJurist
import no.nav.helse.testhelpers.tidslinjeOf
import no.nav.helse.utbetalingstidslinje.Begrunnelse
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Prosent.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class VilkårsgrunnlagHistorikkInnslagTest {
    private lateinit var innslag: VilkårsgrunnlagHistorikk.Innslag

    private companion object {
        private const val UNG_PERSON_FNR_2018 = "12029240045"
        private val ALDER = Fødselsnummer.tilFødselsnummer(UNG_PERSON_FNR_2018).alder()
    }

    @BeforeEach
    fun beforeEach() {
        innslag = VilkårsgrunnlagHistorikk.Innslag(UUID.randomUUID(), LocalDateTime.now())
    }

    @Test
    fun `finner ikke begrunnelser dersom vilkårsgrunnlag ikke er Grunnlagsdata`() {
        val tidslinjer = listOf(tidslinjeOf(1.NAV))
        innslag.add(1.januar, testgrunnlag)
        innslag.avvis(tidslinjer, ALDER)
        assertEquals(0, avvisteDager(tidslinjer).size)
    }

    @Test
    fun `finner kun begrunnelser fra vilkårsgrunnlag som er Grunnlagsdata`() {
        val tidslinjer = listOf(tidslinjeOf(1.NAV))
        innslag.add(1.januar, testgrunnlag)
        innslag.add(1.januar, grunnlagsdata(1.januar, vurdertOk = false, harOpptjening = false))
        innslag.avvis(tidslinjer, ALDER)
        val avvisteDager = avvisteDager(tidslinjer)
        assertEquals(1, avvisteDager.size)
        assertNotNull(avvisteDager.first().erAvvistMed(Begrunnelse.ManglerOpptjening))
    }

    @Test
    fun `avviser dager uten opptjening`() {
        val tidslinjer = listOf(tidslinjeOf(1.NAV))
        innslag.add(1.januar, grunnlagsdata(1.januar, vurdertOk = false, harOpptjening = false))
        innslag.avvis(tidslinjer, ALDER)
        val avvisteDager = avvisteDager(tidslinjer)
        assertEquals(1, avvisteDager.size)
        assertNotNull(avvisteDager.first().erAvvistMed(Begrunnelse.ManglerOpptjening))
    }

    @Test
    fun `avviser ikke dager dersom vurdert ok`() {
        val tidslinjer = listOf(tidslinjeOf(1.NAV))
        innslag.add(1.januar, grunnlagsdata(1.januar, vurdertOk = true))
        innslag.avvis(tidslinjer, ALDER)
        assertEquals(0, avvisteDager(tidslinjer).size)
    }

    @Test
    fun `avviser med flere begrunnelser`() {
        val tidslinjer = listOf(tidslinjeOf(1.NAV))
        innslag.add(1.januar, grunnlagsdata(1.januar, vurdertOk = false, harMinimumInntekt = false, harOpptjening = false))
        innslag.avvis(tidslinjer, ALDER)
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
        innslag.add(1.januar, grunnlagsdata(1.januar, vurdertOk = false, harOpptjening = false))
        innslag.add(2.januar, grunnlagsdata(2.januar, vurdertOk = false, harMinimumInntekt = false))
        innslag.avvis(tidslinjer, ALDER)
        val avvisteDager = avvisteDager(tidslinjer)
        assertEquals(2, avvisteDager.size)
        assertNotNull(avvisteDager.first().erAvvistMed(Begrunnelse.ManglerOpptjening))
        assertNotNull(avvisteDager.last().erAvvistMed(Begrunnelse.MinimumInntekt))
    }

    @Test
    fun `oppfyller ingen inngangsvilkår deler av perioden`() {
        val tidslinjer = listOf(tidslinjeOf(2.NAV, skjæringstidspunkter = listOf(1.januar, 2.januar)))
        innslag.add(1.januar, grunnlagsdata(1.januar, vurdertOk = true))
        innslag.add(2.januar, grunnlagsdata(2.januar, vurdertOk = false, harOpptjening = false, harMinimumInntekt = false, erMedlem = false))
        innslag.avvis(tidslinjer, ALDER)
        val avvisteDager = avvisteDager(tidslinjer)
        assertEquals(1, avvisteDager.size)
        avvisteDager.first().also {
            assertNotNull(it.erAvvistMed(Begrunnelse.ManglerOpptjening))
            assertNotNull(it.erAvvistMed(Begrunnelse.MinimumInntekt))
            assertNotNull(it.erAvvistMed(Begrunnelse.ManglerMedlemskap))
        }
    }

    private fun grunnlagsdata(skjæringstidspunkt: LocalDate, vurdertOk: Boolean = true, harOpptjening: Boolean = true, harMinimumInntekt: Boolean = true, erMedlem: Boolean = true) =
        VilkårsgrunnlagHistorikk.Grunnlagsdata(
            skjæringstidspunkt = skjæringstidspunkt,
            sykepengegrunnlag = Sykepengegrunnlag.opprett(emptyList(), skjæringstidspunkt, MaskinellJurist()),
            sammenligningsgrunnlag = Sammenligningsgrunnlag(emptyList()),
            avviksprosent = 0.0.prosent,
            antallOpptjeningsdagerErMinst = 28,
            harOpptjening = harOpptjening,
            medlemskapstatus = when (erMedlem) {
                true -> Medlemskapsvurdering.Medlemskapstatus.Ja
                false -> Medlemskapsvurdering.Medlemskapstatus.Nei
            },
            harMinimumInntekt = harMinimumInntekt,
            vurdertOk = vurdertOk,
            meldingsreferanseId = UUID.randomUUID(),
            vilkårsgrunnlagId = UUID.randomUUID()
        )

    private fun avvisteDager(tidslinjer: List<Utbetalingstidslinje>): List<Utbetalingstidslinje.Utbetalingsdag.AvvistDag> {
        return tidslinjer.flatMap { it.inspektør.avvistedager }
    }

    private val testgrunnlag
        get() = object : VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement {
            override fun accept(skjæringstidspunkt: LocalDate, vilkårsgrunnlagHistorikkVisitor: VilkårsgrunnlagHistorikkVisitor) {}

            override fun sykepengegrunnlag() = Sykepengegrunnlag(Inntekt.INGEN, emptyList(), Inntekt.INGEN, Sykepengegrunnlag.Begrensning.ER_IKKE_6G_BEGRENSET)

            override fun grunnlagsBegrensning() = Sykepengegrunnlag.Begrensning.ER_IKKE_6G_BEGRENSET

            override fun grunnlagForSykepengegrunnlag() = Inntekt.INGEN

            override fun sammenligningsgrunnlag() = Inntekt.INGEN

            override fun sammenligningsgrunnlagPerArbeidsgiver(): Map<String, Inntektshistorikk.Inntektsopplysning> = emptyMap()

            override fun inntektsopplysningPerArbeidsgiver(): Map<String, Inntektshistorikk.Inntektsopplysning> = emptyMap()

            override fun gjelderFlereArbeidsgivere() = false

            override fun skjæringstidspunkt() = 1.januar

            override fun valider(aktivitetslogg: Aktivitetslogg) {}

            override fun toSpesifikkKontekst(): SpesifikkKontekst {
                return SpesifikkKontekst(
                    "testgrunnlag",
                    emptyMap()
                )
            }
        }
}
