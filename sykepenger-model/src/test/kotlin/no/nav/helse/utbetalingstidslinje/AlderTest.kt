package no.nav.helse.utbetalingstidslinje

import no.nav.helse.*
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.SubsumsjonInspektør
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Ledd.LEDD_1
import no.nav.helse.person.Paragraf.PARAGRAF_8_3
import no.nav.helse.person.Punktum.Companion.punktum
import no.nav.helse.person.etterlevelse.MaskinellJurist
import no.nav.helse.person.etterlevelse.Subsumsjon.Utfall.VILKAR_IKKE_OPPFYLT
import no.nav.helse.person.etterlevelse.Subsumsjon.Utfall.VILKAR_OPPFYLT
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class AlderTest {

    private companion object {
        val FYLLER_67_ÅR_1_JANUAR_2018 = "01015149945".somFødselsnummer().alder()
        val FYLLER_18_ÅR_2_NOVEMBER_2018 = "02110075045".somFødselsnummer().alder()
        val FYLLER_70_ÅR_10_JANUAR_2018 = "10014812345".somFødselsnummer().alder()
        val FYLLER_70_ÅR_13_JANUAR_2018 = "13014812345".somFødselsnummer().alder()
        val FYLLER_70_ÅR_14_JANUAR_2018 = "14014812345".somFødselsnummer().alder()
        val FYLLER_70_ÅR_15_JANUAR_2018 = "15014812345".somFødselsnummer().alder()
    }

    @Test
    fun `alder på gitt dato`() {
        val alder = "12029240045".somFødselsnummer().alder()
        assertEquals(25, alder.alderPåDato(11.februar))
        assertEquals(26, alder.alderPåDato(12.februar))
    }

    @Test
    fun `mindre enn 70`() {
        assertTrue(FYLLER_70_ÅR_10_JANUAR_2018.innenfor70årsgrense(9.januar))
        assertFalse(FYLLER_70_ÅR_10_JANUAR_2018.innenfor70årsgrense(10.januar))
        assertFalse(FYLLER_70_ÅR_10_JANUAR_2018.innenfor70årsgrense(11.januar))
        assertTrue(FYLLER_70_ÅR_13_JANUAR_2018.innenfor70årsgrense(12.januar))
        assertTrue(FYLLER_70_ÅR_14_JANUAR_2018.innenfor70årsgrense(12.januar))
        assertTrue(FYLLER_70_ÅR_15_JANUAR_2018.innenfor70årsgrense(12.januar))
    }

    @Test
    fun `utbetaling skal stoppes selv om man reelt sett er 69 år - dersom 70årsdagen er i en helg`() {
        val dagen = 12.januar
        assertTrue(FYLLER_70_ÅR_13_JANUAR_2018.innenfor70årsgrense(dagen))
        assertTrue(FYLLER_70_ÅR_13_JANUAR_2018.harNådd70årsgrense(dagen))
    }

    @Test
    fun `har fylt 70 år`() {
        assertFalse(FYLLER_70_ÅR_10_JANUAR_2018.harNådd70årsgrense(8.januar))
        assertTrue(FYLLER_70_ÅR_10_JANUAR_2018.harNådd70årsgrense(9.januar))
        assertTrue(FYLLER_70_ÅR_10_JANUAR_2018.harNådd70årsgrense(10.januar))
    }

    @Test
    fun `har fylt 70 år hensyntar helg`() {
        assertFalse(FYLLER_70_ÅR_13_JANUAR_2018.harNådd70årsgrense(11.januar))
        assertFalse(FYLLER_70_ÅR_14_JANUAR_2018.harNådd70årsgrense(11.januar))
        assertFalse(FYLLER_70_ÅR_15_JANUAR_2018.harNådd70årsgrense(11.januar))
        assertTrue(FYLLER_70_ÅR_13_JANUAR_2018.harNådd70årsgrense(12.januar))
        assertTrue(FYLLER_70_ÅR_14_JANUAR_2018.harNådd70årsgrense(12.januar))
        assertTrue(FYLLER_70_ÅR_15_JANUAR_2018.harNådd70årsgrense(12.januar))
    }

    @Test
    fun `67årsgrense`() {
        assertTrue(FYLLER_67_ÅR_1_JANUAR_2018.innenfor67årsgrense(31.desember(2017)))
        assertTrue(FYLLER_67_ÅR_1_JANUAR_2018.innenfor67årsgrense(1.januar))
        assertFalse(FYLLER_67_ÅR_1_JANUAR_2018.innenfor67årsgrense(2.januar))
    }

    @Test
    fun `begrunnelse for alder`() {
        assertEquals(Begrunnelse.SykepengedagerOppbrukt, FYLLER_67_ÅR_1_JANUAR_2018.begrunnelseForAlder(1.januar))
        assertEquals(Begrunnelse.SykepengedagerOppbruktOver67, FYLLER_67_ÅR_1_JANUAR_2018.begrunnelseForAlder(2.januar))
        assertEquals(Begrunnelse.SykepengedagerOppbruktOver67, FYLLER_67_ÅR_1_JANUAR_2018.begrunnelseForAlder(2.januar))
        assertEquals(Begrunnelse.SykepengedagerOppbruktOver67, FYLLER_70_ÅR_10_JANUAR_2018.begrunnelseForAlder(9.januar))
        assertEquals(Begrunnelse.Over70, FYLLER_70_ÅR_10_JANUAR_2018.begrunnelseForAlder(10.januar))
    }

    @Test
    fun `får ikke lov å søke sykepenger dersom personen er mindre enn 18 år på søknadstidspunktet`() {
        assertTrue(FYLLER_18_ÅR_2_NOVEMBER_2018.forUngForÅSøke(1.november))
    }

    @Test
    fun `får lov å søke dersom personen er minst 18 år`() {
        assertFalse(FYLLER_18_ÅR_2_NOVEMBER_2018.forUngForÅSøke(2.november))
    }
    @Test
    fun `Minimum inntekt er en halv g hvis du akkurat har fylt 67`() {
        assertEquals(67, FYLLER_67_ÅR_1_JANUAR_2018.alderPåDato(1.januar))
        assertEquals(Grunnbeløp.halvG.beløp(1.januar), FYLLER_67_ÅR_1_JANUAR_2018.minimumInntekt(1.januar))
        assertEquals((93634 / 2).årlig, FYLLER_67_ÅR_1_JANUAR_2018.minimumInntekt(1.januar))
    }

    @Test
    fun `Minimum inntekt er 2g hvis du er en dag over 67`() {
        assertEquals(67, FYLLER_67_ÅR_1_JANUAR_2018.alderPåDato(2.januar))
        assertEquals(Grunnbeløp.`2G`.beløp(2.januar), FYLLER_67_ÅR_1_JANUAR_2018.minimumInntekt(2.januar))
        assertEquals((93634 * 2).årlig, FYLLER_67_ÅR_1_JANUAR_2018.minimumInntekt(2.januar))
    }

    @Test
    fun `Minimum inntekt er 2g hvis du er 69`() {
        val FYLLER_69_1_JANUAR_2018 = "01014949945".somFødselsnummer().alder()
        assertEquals(69, FYLLER_69_1_JANUAR_2018.alderPåDato(1.januar))
        assertEquals(Grunnbeløp.`2G`.beløp(1.januar), FYLLER_69_1_JANUAR_2018.minimumInntekt(1.januar))
        assertEquals((93634 * 2).årlig, FYLLER_69_1_JANUAR_2018.minimumInntekt(1.januar))
    }

    @Test
    fun `forhøyet inntektskrav`() {
        assertFalse(FYLLER_67_ÅR_1_JANUAR_2018.forhøyetInntektskrav(1.januar))
        assertTrue(FYLLER_67_ÅR_1_JANUAR_2018.forhøyetInntektskrav(2.januar))
    }

    @Test
    fun `riktig begrunnelse for minimum inntekt ved alder over 67`() {
        assertEquals(Begrunnelse.MinimumInntektOver67, FYLLER_67_ÅR_1_JANUAR_2018.begrunnelseForMinimumInntekt(2.januar))
    }

    @Test
    fun `riktig begrunnelse for minimum inntekt ved alder 67 eller under`() {
        assertEquals(Begrunnelse.MinimumInntekt, FYLLER_67_ÅR_1_JANUAR_2018.begrunnelseForMinimumInntekt(1.januar))
    }

    @Test
    fun `etterlevelse for periode før fylte 70`() {
        val jurist = MaskinellJurist()
        Alder(1.januar(2000)).etterlevelse70år(
            aktivitetslogg = Aktivitetslogg(),
            periode = 17.mai(2069) til 31.desember(2069),
            avvisteDager = emptySet(),
            jurist = jurist
        )
        SubsumsjonInspektør(jurist).assertParagraf(PARAGRAF_8_3, LEDD_1, 16.desember(2011), 2.punktum)
    }

    @Test
    fun `siste dag med sykepenger 70 år`() {
        assertSisteDagMedSykepenger(9.januar, Hjemmelbegrunnelse.OVER_70, 10.januar, FYLLER_70_ÅR_10_JANUAR_2018, 248, 60)
        assertSisteDagMedSykepenger(9.januar, Hjemmelbegrunnelse.OVER_70, 1.januar, FYLLER_70_ÅR_10_JANUAR_2018, 248, 60)
    }

    @Test
    fun `siste dag med sykepenger 67 år`() {
        assertSisteDagMedSykepenger(1.januar + 60.ukedager, Hjemmelbegrunnelse.OVER_67, 1.januar, FYLLER_67_ÅR_1_JANUAR_2018, 248, 60)
        assertSisteDagMedSykepenger(1.januar + 20.ukedager, Hjemmelbegrunnelse.UNDER_67, 1.januar, FYLLER_67_ÅR_1_JANUAR_2018, 20, 60)
        assertSisteDagMedSykepenger(1.januar + 60.ukedager, Hjemmelbegrunnelse.OVER_67, 1.desember(2017), FYLLER_67_ÅR_1_JANUAR_2018, 200, 60)
        assertSisteDagMedSykepenger(1.mars + 40.ukedager, Hjemmelbegrunnelse.OVER_67, 1.mars, FYLLER_67_ÅR_1_JANUAR_2018, 248, 40)
    }

    @Test
    fun `siste dag med sykepenger under 67 år`() {
        assertSisteDagMedSykepenger(1.januar + 248.ukedager, Hjemmelbegrunnelse.UNDER_67, 1.januar, FYLLER_18_ÅR_2_NOVEMBER_2018, 248, 60)
        assertSisteDagMedSykepenger(1.januar, Hjemmelbegrunnelse.UNDER_67, 1.januar, FYLLER_18_ÅR_2_NOVEMBER_2018, 0, 60)
    }

    @Test
    fun `siste dag med sykepenger under 67 år faller på samme dag som over 67`() {
        assertSisteDagMedSykepenger(1.januar + 60.ukedager, Hjemmelbegrunnelse.UNDER_67, 1.januar, FYLLER_18_ÅR_2_NOVEMBER_2018, 60, 60)
    }

    @Test
    fun `siste dag med sykepenger over 67 år faller på samme dag som over 70`() {
        assertSisteDagMedSykepenger(9.januar, Hjemmelbegrunnelse.OVER_67, 1.januar, FYLLER_70_ÅR_10_JANUAR_2018, 248, 6)
    }

    private enum class Hjemmelbegrunnelse { UNDER_67, OVER_67, OVER_70 }

    private fun assertSisteDagMedSykepenger(forventet: LocalDate, begrunnelse: Hjemmelbegrunnelse, sisteBetalteDag: LocalDate, alder: Alder, gjenståendeSykepengedager: Int, gjenståendeSykepengedagerOver67: Int) {
        lateinit var hjemmel: Hjemmelbegrunnelse
        assertEquals(forventet, alder.maksimumSykepenger(sisteBetalteDag, 0, gjenståendeSykepengedager, gjenståendeSykepengedagerOver67).sisteDag(object : Alder.MaksimumSykepenger.Begrunnelse {
            override fun `§ 8-12 ledd 1 punktum 1`(dato: LocalDate, gjenståendeSykepengedager: Int) {
                hjemmel = Hjemmelbegrunnelse.UNDER_67
            }

            override fun `§ 8-51 ledd 3`(dato: LocalDate, gjenståendeSykepengedager: Int) {
                hjemmel = Hjemmelbegrunnelse.OVER_67
            }

            override fun `§ 8-3 ledd 1 punktum 2`(dato: LocalDate, gjenståendeSykepengedager: Int) {
                hjemmel = Hjemmelbegrunnelse.OVER_70
            }
        }))
        assertEquals(begrunnelse, hjemmel)
    }

    @Test
    fun `etterlevelse for periode man fyller 70`() {
        val jurist = MaskinellJurist()
        Alder(1.januar(2000)).etterlevelse70år(
            aktivitetslogg = Aktivitetslogg(),
            periode = 17.mai(2069) til 1.januar(2070),
            avvisteDager = setOf(1.januar(2070)),
            jurist = jurist
        )
        SubsumsjonInspektør(jurist).assertParagraf(PARAGRAF_8_3, LEDD_1, 16.desember(2011), 2.punktum, utfall = VILKAR_OPPFYLT)
        SubsumsjonInspektør(jurist).assertParagraf(PARAGRAF_8_3, LEDD_1, 16.desember(2011), 2.punktum, utfall = VILKAR_IKKE_OPPFYLT)
    }

    @Test
    fun `etterlevelse for periode etter fylte 70`() {
        val jurist = MaskinellJurist()
        Alder(1.januar(2000)).etterlevelse70år(
            aktivitetslogg = Aktivitetslogg(),
            periode = 1.januar(2070) til 5.januar(2070),
            avvisteDager = setOf(1.januar(2070), 2.januar(2070), 3.januar(2070), 4.januar(2070), 5.januar(2070)),
            jurist = jurist
        )
        SubsumsjonInspektør(jurist).assertParagraf(PARAGRAF_8_3, LEDD_1, 16.desember(2011), 2.punktum, utfall = VILKAR_IKKE_OPPFYLT)
    }
}
