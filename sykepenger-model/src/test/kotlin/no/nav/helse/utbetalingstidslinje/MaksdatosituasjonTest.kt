package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import no.nav.helse.Alder
import no.nav.helse.Alder.Companion.alder
import no.nav.helse.desember
import no.nav.helse.etterlevelse.Ledd
import no.nav.helse.etterlevelse.MaskinellJurist
import no.nav.helse.etterlevelse.Paragraf
import no.nav.helse.etterlevelse.Punktum.Companion.punktum
import no.nav.helse.etterlevelse.SubsumsjonObserver
import no.nav.helse.etterlevelse.Tidslinjedag
import no.nav.helse.hendelser.somPeriode
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.SubsumsjonInspektør
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.november
import no.nav.helse.plus
import no.nav.helse.ukedager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class MaksdatosituasjonTest {
    private companion object {
        val FYLLER_67_ÅR_1_JANUAR_2018 = 1.januar(1951).alder
        val FYLLER_18_ÅR_2_NOVEMBER_2018 = 2.november(2000).alder
        val FYLLER_70_ÅR_10_JANUAR_2018 = 10.januar(1948).alder
    }

    @Test
    fun `siste dag med sykepenger 70 år`() {
        assertSisteDagMedSykepenger(9.januar, 0, Hjemmelbegrunnelse.OVER_70, 10.januar, FYLLER_70_ÅR_10_JANUAR_2018, 0, 0).also { situasjon ->
            val jurist = MaskinellJurist()
            situasjon.vurderMaksdatobestemmelse(
                jurist,
                1.januar til 10.januar,
                emptyList(),
                emptyList(),
                setOf(10.januar)
            )
            val inspektør = SubsumsjonInspektør(jurist)
            inspektør.assertOppfylt(paragraf = Paragraf.PARAGRAF_8_3, ledd = Ledd.LEDD_1, versjon = 16.desember(2011), punktum = 2.punktum)
            inspektør.assertIkkeOppfylt(paragraf = Paragraf.PARAGRAF_8_3, ledd = Ledd.LEDD_1, versjon = 16.desember(2011), punktum = 2.punktum)
        }
        assertSisteDagMedSykepenger(9.januar, 6, Hjemmelbegrunnelse.OVER_70, 1.januar, FYLLER_70_ÅR_10_JANUAR_2018, 0, 0).also { situasjon ->
            val jurist = MaskinellJurist()
            situasjon.vurderMaksdatobestemmelse(jurist, 1.januar til 1.januar, emptyList(), emptyList(), emptySet())

            val inspektør = SubsumsjonInspektør(jurist)
            inspektør.assertOppfylt(paragraf = Paragraf.PARAGRAF_8_3, ledd = Ledd.LEDD_1, versjon = 16.desember(2011), punktum = 2.punktum)
        }
    }

    @Test
    fun `siste dag med sykepenger 67 år`() {
        assertSisteDagMedSykepenger(1.januar + 60.ukedager, 60, Hjemmelbegrunnelse.OVER_67, 1.januar, FYLLER_67_ÅR_1_JANUAR_2018, 0, 0)
        assertSisteDagMedSykepenger(1.januar + 20.ukedager, 20, Hjemmelbegrunnelse.UNDER_67, 1.januar, FYLLER_67_ÅR_1_JANUAR_2018, 228, 1)
        assertSisteDagMedSykepenger(1.januar + 60.ukedager, 81, Hjemmelbegrunnelse.OVER_67, 1.desember(2017), FYLLER_67_ÅR_1_JANUAR_2018, 48, 0)
        assertSisteDagMedSykepenger(1.mars + 40.ukedager, 40, Hjemmelbegrunnelse.OVER_67, 1.mars, FYLLER_67_ÅR_1_JANUAR_2018, 20, 20)
    }

    @Test
    fun `siste dag med sykepenger under 67 år`() {
        assertSisteDagMedSykepenger(1.januar + 248.ukedager, 248, Hjemmelbegrunnelse.UNDER_67, 1.januar, FYLLER_18_ÅR_2_NOVEMBER_2018, 0, 0)
        assertSisteDagMedSykepenger(1.januar, 0, Hjemmelbegrunnelse.UNDER_67, 1.januar, FYLLER_18_ÅR_2_NOVEMBER_2018, 248, 0)
    }

    @Test
    fun `siste dag med sykepenger under 67 år faller på samme dag som over 67`() {
        assertSisteDagMedSykepenger(1.januar + 60.ukedager, 60, Hjemmelbegrunnelse.UNDER_67, 1.januar, FYLLER_18_ÅR_2_NOVEMBER_2018, 188, 0)
    }

    @Test
    fun `siste dag med sykepenger over 67 år faller på samme dag som over 70`() {
        assertSisteDagMedSykepenger(9.januar, 6, Hjemmelbegrunnelse.OVER_67, 1.januar, FYLLER_70_ÅR_10_JANUAR_2018, 54, 54)
    }

    private enum class Hjemmelbegrunnelse { UNDER_67, OVER_67, OVER_70 }

    private fun assertSisteDagMedSykepenger(forventet: LocalDate, forventetDagerIgjen: Int, begrunnelse: Hjemmelbegrunnelse, sisteBetalteDag: LocalDate, alder: Alder, forbrukteDager: Int, forbrukteDagerOver67: Int): Maksdatosituasjon {
        val situasjon = Maksdatosituasjon(
            regler = ArbeidsgiverRegler.Companion.NormalArbeidstaker,
            dato = sisteBetalteDag,
            alder = alder,
            startdatoSykepengerettighet = LocalDate.EPOCH,
            startdatoTreårsvindu = LocalDate.EPOCH,
            betalteDager = if (forbrukteDager > 0) (sisteBetalteDag.minusDays(forbrukteDager.toLong() - 1) til sisteBetalteDag).toSet() else emptySet()
        )
        val hjemmebestemmelse = FangeHjemmebestemmelse()
        assertEquals(forventet, situasjon.maksdato)
        assertEquals(forventetDagerIgjen, situasjon.gjenståendeDager)
        situasjon.vurderMaksdatobestemmelse(
            hjemmebestemmelse,
            LocalDate.EPOCH.somPeriode(),
            emptyList(),
            emptyList(),
            emptySet()
        )
        assertEquals(begrunnelse, hjemmebestemmelse.hjemmel)
        return situasjon
    }

    private class FangeHjemmebestemmelse : SubsumsjonObserver {
        lateinit var hjemmel: Hjemmelbegrunnelse
        override fun `§ 8-12 ledd 1 punktum 1`(
            periode: ClosedRange<LocalDate>,
            tidslinjegrunnlag: List<List<Tidslinjedag>>,
            beregnetTidslinje: List<Tidslinjedag>,
            gjenståendeSykedager: Int,
            forbrukteSykedager: Int,
            maksdato: LocalDate,
            startdatoSykepengerettighet: LocalDate?
        ) {
            hjemmel = Hjemmelbegrunnelse.UNDER_67
        }
        override fun `§ 8-51 ledd 3`(
            periode: ClosedRange<LocalDate>,
            tidslinjegrunnlag: List<List<Tidslinjedag>>,
            beregnetTidslinje: List<Tidslinjedag>,
            gjenståendeSykedager: Int,
            forbrukteSykedager: Int,
            maksdato: LocalDate,
            startdatoSykepengerettighet: LocalDate?
        ) {
            hjemmel = Hjemmelbegrunnelse.OVER_67
        }
        override fun `§ 8-3 ledd 1 punktum 2`(
            oppfylt: Boolean,
            syttiårsdagen: LocalDate,
            utfallFom: LocalDate,
            utfallTom: LocalDate,
            tidslinjeFom: LocalDate,
            tidslinjeTom: LocalDate,
            avvistePerioder: List<ClosedRange<LocalDate>>
        ) {
            if (this::hjemmel.isInitialized) return
            hjemmel = Hjemmelbegrunnelse.OVER_70
        }
    }
}