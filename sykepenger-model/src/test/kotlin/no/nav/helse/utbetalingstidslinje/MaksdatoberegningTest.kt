package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import java.util.*
import no.nav.helse.april
import no.nav.helse.august
import no.nav.helse.desember
import no.nav.helse.erHelg
import no.nav.helse.februar
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.juli
import no.nav.helse.juni
import no.nav.helse.mai
import no.nav.helse.mars
import no.nav.helse.november
import no.nav.helse.oktober
import no.nav.helse.plus
import no.nav.helse.september
import no.nav.helse.testhelpers.AP
import no.nav.helse.testhelpers.ARB
import no.nav.helse.testhelpers.AVV
import no.nav.helse.testhelpers.FOR
import no.nav.helse.testhelpers.FRI
import no.nav.helse.testhelpers.HELG
import no.nav.helse.testhelpers.NAP
import no.nav.helse.testhelpers.NAV
import no.nav.helse.testhelpers.NAVDAGER
import no.nav.helse.testhelpers.UTELATE
import no.nav.helse.testhelpers.Utbetalingsdager
import no.nav.helse.testhelpers.tidslinjeOf
import no.nav.helse.ukedager
import no.nav.helse.utbetalingstidslinje.MaksimumSykepengedagerregler.Companion.NormalArbeidstaker
import no.nav.helse.utbetalingstidslinje.Begrunnelse.MinimumSykdomsgrad
import no.nav.helse.utbetalingstidslinje.Begrunnelse.SykepengedagerOppbrukt
import no.nav.helse.utbetalingstidslinje.Maksdatoberegning.Companion.TILSTREKKELIG_OPPHOLD_I_SYKEDAGER
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class MaksdatoberegningTest {
    private companion object {
        private val UNG_PERSON_FNR_2018 = 12.februar(1992)
        private val PERSON_70_ÅR_10_JANUAR_2018 = 10.januar(1948)
        private val PERSON_67_ÅR_11_JANUAR_2018 = 11.januar(1951)
    }

    private lateinit var vurderinger: List<Maksdatokontekst>
    private lateinit var maksdatoresultater: List<Maksdatoresultat>
    private lateinit var maksdatoer: List<LocalDate>
    private lateinit var forbrukteDager: List<Int>
    private lateinit var gjenståendeDager: List<Int>

    @Test
    fun `vurderer maksdato med eget regelsett`() {
        val tidslinje = tidslinjeOf(16.AP, 10.NAV)
        val avslåtteDager = tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018, regler = object : MaksimumSykepengedagerregler {
            override fun maksSykepengedager() = 5
            override fun maksSykepengedagerOver67() = 5
        })
        assertEquals((24.januar til 26.januar).utenHelg(), avslåtteDager)
        assertEquals(listOf(5), forbrukteDager)
        assertEquals(listOf(0), gjenståendeDager)
        assertEquals(listOf(23.januar), maksdatoer)
    }

    @Test
    fun `vurderer maksdato for infotrygdtidslinje som strekker seg ut over vedtaksperioden`() {
        val a1 = tidslinjeOf(16.AP, 248.NAVDAGER)
        val infotrygdlinje = tidslinjeOf(200.ARB, 10.NAVDAGER, startDato = 1.januar(2019))
        val avvisteDager = listOf(a1).utbetalingsavgrenser(UNG_PERSON_FNR_2018, infotrygdlinje)

        assertEquals(listOf<List<Nothing>>(emptyList(), emptyList()), avvisteDager)
        assertEquals(listOf(248, 10), forbrukteDager)
        assertEquals(listOf(0, 238), gjenståendeDager)
        assertEquals(listOf(28.desember, 1.juli(2020)), maksdatoer)
    }

    @Test
    fun `avviser ikke dager som strekker seg forbi arbeidsgiver som beregner utbetalinger`() {
        val a1 = tidslinjeOf(16.AP, 248.NAVDAGER)
        val a2 = tidslinjeOf(16.AP, 250.NAVDAGER, 182.ARB, 10.NAV)
        val avvisteDager = listOf(a1, a2).utbetalingsavgrenser(UNG_PERSON_FNR_2018)

        assertEquals(listOf(listOf(31.desember, 1.januar(2019)), emptyList()), avvisteDager)
        assertEquals(listOf(248, 8), forbrukteDager)
        assertEquals(listOf(0, 240), gjenståendeDager)
        assertEquals(listOf(28.desember, 12.juni(2020)), maksdatoer)
    }

    @Test
    fun `avslår dager etter dødsfall - under 67 år`() {
        val tidslinje = tidslinjeOf(16.AP, 10.NAV)
        val avslåtteDager = tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018, dødsdato = 20.januar)

        assertEquals((21.januar til 26.januar).toList(), avslåtteDager)
        assertEquals(listOf(3), forbrukteDager)
        assertEquals(listOf(0), gjenståendeDager)
        assertEquals(listOf(20.januar), maksdatoer)

        avslåtteDager.map {
            assertEquals(Begrunnelse.EtterDødsdato, begrunnelse(it))
        }
    }

    @Test
    fun `under 67 år - dødsdato i fremtiden`() {
        val tidslinje = tidslinjeOf(16.AP, 10.NAV)
        val avslåtteDager = tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018, dødsdato = 31.januar)

        assertEquals(emptyList<Nothing>(), avslåtteDager)
        assertEquals(listOf(8), forbrukteDager)
        assertEquals(listOf(3), gjenståendeDager)
        assertEquals(listOf(31.januar), maksdatoer)
    }

    @Test
    fun `avslår dager etter dødsfall - under 67 år - forbrukt alle dager`() {
        val tidslinje = tidslinjeOf(250.NAVDAGER)
        val avslåtteDager = tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018, dødsdato = 13.desember)

        assertEquals(listOf(13.desember, 14.desember), avslåtteDager)
        assertEquals(listOf(248), forbrukteDager)
        assertEquals(listOf(0), gjenståendeDager)
        assertEquals(listOf(12.desember), maksdatoer)
        assertEquals(SykepengedagerOppbrukt, begrunnelse(13.desember))
        assertEquals(Begrunnelse.EtterDødsdato, begrunnelse(14.desember))
    }

    @Test
    fun `avslår dager etter dødsfall - under 67 år - forbrukt alle dager - over 182 dager opphold`() {
        val tidslinje = tidslinjeOf((248 + 190).NAVDAGER)
        val avslåtteDager = tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018, dødsdato = 13.desember)

        assertEquals((13.desember til 4.september(2019)).toList(), avslåtteDager)
        assertEquals(listOf(248), forbrukteDager)
        assertEquals(listOf(0), gjenståendeDager)
        assertEquals(listOf(12.desember), maksdatoer)
        assertEquals(SykepengedagerOppbrukt, begrunnelse(13.desember))
        avslåtteDager.drop(1).forEach {
            assertEquals(Begrunnelse.EtterDødsdato, begrunnelse(it)) { "Feil begrunnelse for $it" }
        }
    }

    @Test
    fun `avslår dager etter dødsfall - over 67 år`() {
        val tidslinje = tidslinjeOf(16.AP, 10.NAV)
        val avslåtteDager = tidslinje.utbetalingsavgrenser(PERSON_67_ÅR_11_JANUAR_2018, dødsdato = 20.januar)

        assertEquals((21.januar til 26.januar).toList(), avslåtteDager)
        assertEquals(listOf(3), forbrukteDager)
        assertEquals(listOf(0), gjenståendeDager)
        assertEquals(listOf(20.januar), maksdatoer)
        avslåtteDager.map {
            assertEquals(Begrunnelse.EtterDødsdato, begrunnelse(it)) { "Feil begrunnelse for $it" }
        }
    }

    @Test
    fun `over 67 år - dødsfall i fremtiden`() {
        val tidslinje = tidslinjeOf(16.AP, 10.NAV)
        val avslåtteDager = tidslinje.utbetalingsavgrenser(PERSON_67_ÅR_11_JANUAR_2018, dødsdato = 31.januar)

        assertEquals(emptyList<Nothing>(), avslåtteDager)
        assertEquals(listOf(8), forbrukteDager)
        assertEquals(listOf(3), gjenståendeDager)
        assertEquals(listOf(31.januar), maksdatoer)
    }

    @Test
    fun `avslår dager etter dødsfall - over 67 år - forbrukt alle dager`() {
        val tidslinje = tidslinjeOf(62.NAVDAGER, startDato = 12.januar)
        val avslåtteDager = tidslinje.utbetalingsavgrenser(PERSON_67_ÅR_11_JANUAR_2018, dødsdato = 6.april)

        assertEquals((6.april til 9.april).toList(), avslåtteDager)
        assertEquals(listOf(60), forbrukteDager)
        assertEquals(listOf(0), gjenståendeDager)
        assertEquals(listOf(5.april), maksdatoer)
        assertEquals(Begrunnelse.SykepengedagerOppbruktOver67, begrunnelse(6.april))
        assertEquals(Begrunnelse.EtterDødsdato, begrunnelse(9.april))
    }

    @Test
    fun `avslår dager etter dødsfall - over 67 år - forbrukt alle dager - over 182 dager opphold`() {
        val tidslinje = tidslinjeOf((62 + 190).NAVDAGER, startDato = 12.januar)
        val avslåtteDager = tidslinje.utbetalingsavgrenser(PERSON_67_ÅR_11_JANUAR_2018, dødsdato = 6.april)

        assertEquals((6.april til 31.desember).toList(), avslåtteDager)
        assertEquals(listOf(60), forbrukteDager)
        assertEquals(listOf(0), gjenståendeDager)
        assertEquals(listOf(5.april), maksdatoer)
        assertEquals(Begrunnelse.SykepengedagerOppbruktOver67, begrunnelse(6.april))
        avslåtteDager.drop(1).forEach {
            assertEquals(Begrunnelse.EtterDødsdato, begrunnelse(it)) { "Feil begrunnelse for $it" }
        }
    }

    @Test
    fun `forbrukte dager før 67 år`() {
        tidslinjeOf(11.NAVDAGER, startDato = 1.desember(2017)).also { tidslinje ->
            val avslåtteDager = tidslinje.utbetalingsavgrenser(PERSON_67_ÅR_11_JANUAR_2018)
            assertEquals(emptyList<LocalDate>(), avslåtteDager)
            assertEquals(listOf(11), forbrukteDager)
            assertEquals(listOf(79), gjenståendeDager)
            assertEquals(listOf(11.januar + 60.ukedager), maksdatoer)
        }
        tidslinjeOf(10.NAV).also { tidslinje ->
            val avslåtteDager = tidslinje.utbetalingsavgrenser(PERSON_67_ÅR_11_JANUAR_2018)
            assertEquals(emptyList<LocalDate>(), avslåtteDager)
            assertEquals(listOf(8), forbrukteDager)
            assertEquals(listOf(61), gjenståendeDager)
            assertEquals(listOf(11.januar + 60.ukedager), maksdatoer)
        }
    }

    @Test
    fun `sjekk 60 dagers grense for 67 åringer`() {
        val tidslinje = tidslinjeOf(11.NAV, 61.NAVDAGER)
        val avslåtteDager = tidslinje.utbetalingsavgrenser(PERSON_67_ÅR_11_JANUAR_2018)

        assertEquals(listOf(6.april), avslåtteDager)
        assertEquals(listOf(69), forbrukteDager)
        assertEquals(listOf(0), gjenståendeDager)
        assertEquals(listOf(5.april), maksdatoer)
    }

    @Test
    fun `sjekk 60 dagers grense for 67 åringer med 26 ukers opphold`() {
        val tidslinje = tidslinjeOf(10.NAV, 60.NAVDAGER, (TILSTREKKELIG_OPPHOLD_I_SYKEDAGER).ARB, 60.NAVDAGER)
        val avslåtteDager = listOf(tidslinje).utbetalingsavgrenser(PERSON_67_ÅR_11_JANUAR_2018)

        assertEquals(listOf(emptyList(), emptyList<Nothing>()), avslåtteDager)
        assertEquals(listOf(68, 60), forbrukteDager)
        assertEquals(listOf(1, 0), gjenståendeDager)
        assertEquals(listOf(4.oktober, 26.desember), maksdatoer)
    }

    @Test
    fun `sjekk at sykepenger er oppbrukt når personen fyller 67 år`() {
        val tidslinje = tidslinjeOf(248.NAVDAGER, 10.NAVDAGER, startDato = 31.januar(2017))
        val avslåtteDager = tidslinje.utbetalingsavgrenser(PERSON_67_ÅR_11_JANUAR_2018)

        assertEquals(10, avslåtteDager.size)
        assertEquals((12.januar til 25.januar).utenHelg(), avslåtteDager)
        assertEquals(listOf(248), forbrukteDager)
        assertEquals(listOf(0), gjenståendeDager)
        assertEquals(listOf(11.januar), maksdatoer)
    }

    @Test
    fun `sjekk at sykepenger er oppbrukt etter at personen har fylt 67 år`() {
        val tidslinje = tidslinjeOf(11.NAV, 61.NAVDAGER)
        val avslåtteDager = tidslinje.utbetalingsavgrenser(PERSON_67_ÅR_11_JANUAR_2018)

        assertEquals(1, avslåtteDager.size)
        assertEquals(listOf(6.april), avslåtteDager)
        assertEquals(listOf(69), forbrukteDager)
        assertEquals(listOf(0), gjenståendeDager)
        assertEquals(listOf(5.april), maksdatoer)
    }

    @Test
    fun `hvis gjenstående sykepengedager er under 60 ved fylte 67 begrunnes avslag med 8-12`() {
        val tidslinje = tidslinjeOf(248.NAVDAGER, 2.NAVDAGER, startDato = 23.februar(2017))
        val avslåtteDager = tidslinje.utbetalingsavgrenser(PERSON_67_ÅR_11_JANUAR_2018)

        assertEquals(listOf(6.februar, 7.februar), avslåtteDager)
        assertEquals(listOf(248), forbrukteDager)
        assertEquals(listOf(0), gjenståendeDager)
        assertEquals(listOf(5.februar), maksdatoer)
        avslåtteDager.forEach {
            assertEquals(SykepengedagerOppbrukt, begrunnelse(it)) { "Feil begrunnelse for $it" }
        }
    }

    @Test
    fun `bruke opp alt fom 67 år`() {
        val tidslinje = tidslinjeOf(188.NAVDAGER, 61.NAVDAGER, (TILSTREKKELIG_OPPHOLD_I_SYKEDAGER).ARB, 61.NAVDAGER, 36.ARB, 365.ARB, 365.ARB, 1.NAVDAGER, startDato = 30.mars(2017))
        val avslåtteDager = listOf(tidslinje).utbetalingsavgrenser(PERSON_67_ÅR_11_JANUAR_2018)

        assertEquals(listOf(listOf(13.mars), listOf(5.desember), listOf(11.januar(2021))), avslåtteDager)
        assertEquals(listOf(248, 60, 0), forbrukteDager)
        assertEquals(listOf(0, 0, 0), gjenståendeDager)
        assertEquals(listOf(12.mars, 4.desember, 8.januar(2021)), maksdatoer)
        assertEquals(SykepengedagerOppbrukt, begrunnelse(13.mars))
        assertEquals(Begrunnelse.SykepengedagerOppbruktOver67, begrunnelse(5.desember))
        assertEquals(Begrunnelse.Over70, begrunnelse(11.januar(2021)))
    }

    @Test
    fun `hvis gjenstående sykepengedager er over 60 ved fylte 67 begrunnes avslag med 8-51`() {
        val tidslinje = tidslinjeOf(11.NAV, 60.NAVDAGER, 2.NAVDAGER)
        val avslåtteDager = tidslinje.utbetalingsavgrenser(PERSON_67_ÅR_11_JANUAR_2018)

        assertEquals(listOf(6.april, 9.april), avslåtteDager)
        assertEquals(listOf(69), forbrukteDager)
        assertEquals(listOf(0), gjenståendeDager)
        assertEquals(listOf(5.april), maksdatoer)
        avslåtteDager.forEach {
            assertEquals(Begrunnelse.SykepengedagerOppbruktOver67, begrunnelse(it)) { "Feil begrunnelse for $it" }
        }
    }

    @Test
    fun `begrunnelsen følger tidspunkt for når sykepengedagene brukes opp, også om det er gap mellom`() {
        val tidslinje = tidslinjeOf(248.NAVDAGER, 25.ARB, 2.NAVDAGER, startDato = 23.februar(2017))
        val avslåtteDager = tidslinje.utbetalingsavgrenser(PERSON_67_ÅR_11_JANUAR_2018)

        assertEquals(listOf(5.mars, 6.mars), avslåtteDager)
        assertEquals(listOf(248), forbrukteDager)
        assertEquals(listOf(0), gjenståendeDager)
        assertEquals(listOf(5.februar), maksdatoer)
        avslåtteDager.forEach {
            assertEquals(SykepengedagerOppbrukt, begrunnelse(it)) { "Feil begrunnelse for $it" }
        }
    }

    @Test
    fun `begrunnelse ved flere oppbrukte rettigheter - hel tidslinje`() {
        val tidslinje = tidslinjeOf(248.NAVDAGER, 1.NAVDAGER, (TILSTREKKELIG_OPPHOLD_I_SYKEDAGER).ARB, 60.NAVDAGER, 1.NAVDAGER, startDato = 23.februar(2017))
        val avslåtteDager = listOf(tidslinje).utbetalingsavgrenser(PERSON_67_ÅR_11_JANUAR_2018)

        assertEquals(listOf(listOf(6.februar), listOf(31.oktober)), avslåtteDager)
        assertEquals(listOf(248, 60), forbrukteDager)
        assertEquals(listOf(0, 0), gjenståendeDager)
        assertEquals(listOf(5.februar, 30.oktober), maksdatoer)
        assertEquals(SykepengedagerOppbrukt, begrunnelse(6.februar))
        assertEquals(Begrunnelse.SykepengedagerOppbruktOver67, begrunnelse(31.oktober))
    }

    @Test
    fun `sjekk at 26 uker med syk etter karantene ikke starter utbetaling gammel person`() {
        val tidslinje = tidslinjeOf(11.NAV, 60.NAVDAGER, (TILSTREKKELIG_OPPHOLD_I_SYKEDAGER).NAVDAGER, 60.NAVDAGER)
        val avslåtteDager = tidslinje.utbetalingsavgrenser(PERSON_67_ÅR_11_JANUAR_2018)

        assertEquals(TILSTREKKELIG_OPPHOLD_I_SYKEDAGER + 60, avslåtteDager.size)
        assertEquals((6.april til 11.mars(2019)).utenHelg(), avslåtteDager)
        assertEquals(listOf(69), forbrukteDager)
        assertEquals(listOf(0), gjenståendeDager)
        assertEquals(listOf(5.april), maksdatoer)
    }

    @Test
    fun `sjekk at 26 uker med syk etter karantene starter utbetaling for gammel person etter første arbeidsdag`() {
        val tidslinje = tidslinjeOf(11.NAV, 60.NAVDAGER, (TILSTREKKELIG_OPPHOLD_I_SYKEDAGER).NAVDAGER, 1.ARB, 60.NAVDAGER)
        val avslåtteDager = listOf(tidslinje).utbetalingsavgrenser(PERSON_67_ÅR_11_JANUAR_2018)

        assertEquals(TILSTREKKELIG_OPPHOLD_I_SYKEDAGER, avslåtteDager[0].size)
        assertEquals(listOf((6.april til 17.desember).utenHelg(), emptyList<Nothing>()), avslåtteDager)
        assertEquals(listOf(69, 60), forbrukteDager)
        assertEquals(listOf(0, 0), gjenståendeDager)
        assertEquals(listOf(5.april, 12.mars(2019)), maksdatoer)
    }

    @Test
    fun `avslår dager etter dødsfall - akkurat 70 år`() {
        val tidslinje = tidslinjeOf(16.AP, 10.NAV)
        val avslåtteDager = tidslinje.utbetalingsavgrenser(PERSON_70_ÅR_10_JANUAR_2018, dødsdato = 10.januar)

        assertEquals((17.januar til 26.januar).toList(), avslåtteDager)
        assertEquals(listOf(0), forbrukteDager)
        assertEquals(listOf(0), gjenståendeDager)
        assertEquals(listOf(9.januar), maksdatoer)
        avslåtteDager.map {
            assertEquals(Begrunnelse.Over70, begrunnelse(it)) { "Feil begrunnelse for $it" }
        }
    }

    @Test
    fun `avslår dager etter dødsfall - rett før 70 år`() {
        val tidslinje = tidslinjeOf(16.AP, 10.NAV)
        val avslåtteDager = tidslinje.utbetalingsavgrenser(PERSON_70_ÅR_10_JANUAR_2018, dødsdato = 4.januar)

        assertEquals((17.januar til 26.januar).toList(), avslåtteDager)
        assertEquals(listOf(0), forbrukteDager)
        assertEquals(listOf(0), gjenståendeDager)
        assertEquals(listOf(4.januar), maksdatoer)
        avslåtteDager.map {
            assertEquals(Begrunnelse.EtterDødsdato, begrunnelse(it)) { "Feil begrunnelse for $it" }
        }
    }

    @Test
    fun `avslår dager etter dødsfall - over 70 år`() {
        val tidslinje = tidslinjeOf(16.AP, 10.NAV)
        val avslåtteDager = tidslinje.utbetalingsavgrenser(PERSON_70_ÅR_10_JANUAR_2018, dødsdato = 18.januar)

        assertEquals((17.januar til 26.januar).toList(), avslåtteDager)
        assertEquals(listOf(0), forbrukteDager)
        assertEquals(listOf(0), gjenståendeDager)
        assertEquals(listOf(9.januar), maksdatoer)
        avslåtteDager.map {
            assertEquals(Begrunnelse.Over70, begrunnelse(it)) { "Feil begrunnelse for $it" }
        }
    }

    @Test
    fun `utbetaling stopper når du blir 70 år`() {
        val tidslinje = tidslinjeOf(11.NAV)
        val avslåtteDager = tidslinje.utbetalingsavgrenser(PERSON_70_ÅR_10_JANUAR_2018)

        assertEquals(listOf(10.januar, 11.januar), avslåtteDager)
        assertEquals(listOf(7), forbrukteDager)
        assertEquals(listOf(0), gjenståendeDager)
        assertEquals(listOf(9.januar), maksdatoer)
    }

    @Test
    fun `kan ikke bli syk på 70årsdagen`() {
        val tidslinje = tidslinjeOf(9.UTELATE, 2.NAVDAGER)
        val avslåtteDager = tidslinje.utbetalingsavgrenser(PERSON_70_ÅR_10_JANUAR_2018)

        assertEquals(listOf(10.januar, 11.januar), avslåtteDager)
        assertEquals(listOf(0), forbrukteDager)
        assertEquals(listOf(0), gjenståendeDager)
        assertEquals(listOf(9.januar), maksdatoer)
    }

    @Test
    fun `kan ikke bli syk etter 70årsdagen`() {
        val tidslinje = tidslinjeOf(10.UTELATE, 2.NAVDAGER)
        val avslåtteDager = tidslinje.utbetalingsavgrenser(PERSON_70_ÅR_10_JANUAR_2018)

        assertEquals(listOf(11.januar, 12.januar), avslåtteDager)
        assertEquals(listOf(0), forbrukteDager)
        assertEquals(listOf(0), gjenståendeDager)
        assertEquals(listOf(9.januar), maksdatoer)
    }

    @Test
    fun `begrunnelse ved fylte 70`() {
        val tidslinje = tidslinjeOf(11.NAV)
        val avslåtteDager = tidslinje.utbetalingsavgrenser(PERSON_70_ÅR_10_JANUAR_2018)

        assertEquals(listOf(10.januar, 11.januar), avslåtteDager)
        assertEquals(listOf(7), forbrukteDager)
        assertEquals(listOf(0), gjenståendeDager)
        assertEquals(listOf(9.januar), maksdatoer)
        avslåtteDager.map {
            assertEquals(Begrunnelse.Over70, begrunnelse(it)) { "Feil begrunnelse for $it" }
        }
    }

    @Test
    fun `riktig antall dager - nav utbetaler arbeidsgiverperioden`() {
        val tidslinje = tidslinjeOf(16.NAP, 10.NAV)

        val avslåtteDager = tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018)
        assertEquals(emptyList<Nothing>(), avslåtteDager)
        assertEquals(listOf(8), forbrukteDager)
        assertEquals(listOf(240), gjenståendeDager)
        assertEquals(listOf(28.desember), maksdatoer)
    }

    @Test
    fun `Når vi utbetaler maksdato må vi nullstille oppholdstellingen`() {
        val tidslinje = tidslinjeOf(16.AP, 247.NAVDAGER, 180.ARB, 1.NAVDAGER, 2.ARB, 10.NAVDAGER)
        val avvisteDager = tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018)

        assertEquals((1.juli(2019) til 12.juli(2019)).utenHelg(), avvisteDager)
        assertEquals(listOf(248), forbrukteDager)
        assertEquals(listOf(0), gjenståendeDager)
        assertEquals(listOf(26.juni(2019)), maksdatoer)
        avvisteDager.map {
            assertEquals(SykepengedagerOppbrukt, begrunnelse(it)) { "Feil begrunnelse for $it" }
        }
    }

    @Test
    fun `stopper betaling etter 260 dager dersom nav utbetaler arbeidsgiverperioden`() {
        val tidslinje = tidslinjeOf(16.NAP, 249.NAVDAGER)
        val avslåtteDager = tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018)

        assertEquals(listOf(31.desember), avslåtteDager)
        assertEquals(listOf(248), forbrukteDager)
        assertEquals(listOf(0), gjenståendeDager)
        assertEquals(listOf(28.desember), maksdatoer)
    }

    @Test
    fun `ny maksdato dersom 182 dager frisk - nav utbetaler arbeidsgiverperioden`() {
        val tidslinje = tidslinjeOf(248.NAVDAGER, 166.ARB, 16.NAP, 10.NAV)
        val avslåtteDager = listOf(tidslinje).utbetalingsavgrenser(UNG_PERSON_FNR_2018)

        assertEquals(listOf<List<LocalDate>>(emptyList(), emptyList()), avslåtteDager)
        assertEquals(listOf(248, 7), forbrukteDager)
        assertEquals(listOf(0, 241), gjenståendeDager)
        assertEquals(listOf(12.desember, 25.mai(2020)), maksdatoer)
    }

    @Test
    fun `ikke ny maksdato dersom mindre enn 182 dager frisk - nav utbetaler arbeidsgiverperioden`() {
        val tidslinje = tidslinjeOf(248.NAVDAGER, 100.ARB, 16.NAP, 10.NAV)
        val avslåtteDager = tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018)

        assertEquals((8.april(2019) til 17.april(2019)).utenHelg(), avslåtteDager)
        assertEquals(listOf(248), forbrukteDager)
        assertEquals(listOf(0), gjenståendeDager)
        assertEquals(listOf(12.desember), maksdatoer)
    }

    @Test
    fun `bare avviste dager`() {
        val tidslinje = tidslinjeOf(16.AP, 10.AVV)
        val avslåtteDager = tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018)

        assertEquals(emptyList<Nothing>(), avslåtteDager)
        assertEquals(listOf(0), forbrukteDager)
        assertEquals(listOf(248), gjenståendeDager)
        assertEquals(listOf(9.januar(2019)), maksdatoer)
    }

    @Test
    fun `avvist dag som siste oppholdsdag`() {
        val tidslinje = tidslinjeOf(16.AP, 248.NAVDAGER, 181.ARB, 2.AVV(1000, begrunnelse = MinimumSykdomsgrad))
        val avvisteDager = tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018)

        assertEquals(listOf(28.juni(2019), 29.juni(2019)), avvisteDager)
        assertEquals(listOf(248), forbrukteDager)
        assertEquals(listOf(0), gjenståendeDager)
        assertEquals(listOf(28.desember), maksdatoer)
        assertEquals(SykepengedagerOppbrukt, begrunnelse(28.juni(2019)))
        assertEquals(Begrunnelse.NyVilkårsprøvingNødvendig, begrunnelse(29.juni(2019)))
    }

    @Test
    fun `avviste dager etter opphold`() {
        val tidslinje = tidslinjeOf(1.NAVDAGER, 15.ARB, 10.AVV)
        val avslåtteDager = tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018)

        assertEquals(emptyList<Nothing>(), avslåtteDager)
        assertEquals(listOf(1), forbrukteDager)
        assertEquals(listOf(247), gjenståendeDager)
        assertEquals(listOf(8.januar(2019)), maksdatoer)
    }

    @Test
    fun `avviste dager før maksdato, etter syk, teller som opphold`() {
        val tidslinje = tidslinjeOf(240.NAVDAGER, 182.AVV, 10.NAVDAGER)
        val avslåtteDager = listOf(tidslinje).utbetalingsavgrenser(UNG_PERSON_FNR_2018)

        assertEquals(listOf<List<Nothing>>(emptyList(), emptyList()), avslåtteDager)
        assertEquals(listOf(240, 10), forbrukteDager)
        assertEquals(listOf(8, 238), gjenståendeDager)
        assertEquals(listOf(12.juni(2019), 13.mai(2020)), maksdatoer)
    }

    @Test
    fun `avviste dager før maksdato, etter opphold, teller som opphold`() {
        val tidslinje = tidslinjeOf(240.NAVDAGER, 1.ARB, 181.AVV, 10.NAVDAGER)
        val avslåtteDager = listOf(tidslinje).utbetalingsavgrenser(UNG_PERSON_FNR_2018)

        assertEquals(listOf(emptyList(), emptyList<Nothing>()), avslåtteDager)
        assertEquals(listOf(240, 10), forbrukteDager)
        assertEquals(listOf(8, 238), gjenståendeDager)
        assertEquals(listOf(12.juni(2019), 13.mai(2020)), maksdatoer)
    }

    @Test
    fun `avviste dager før maksdato, etter fri, teller som opphold`() {
        val tidslinje = tidslinjeOf(240.NAVDAGER, 1.FRI, 181.AVV, 10.NAVDAGER)
        val avslåtteDager = listOf(tidslinje).utbetalingsavgrenser(UNG_PERSON_FNR_2018)

        assertEquals(listOf(emptyList<Nothing>(), emptyList()), avslåtteDager)
        assertEquals(listOf(240, 10), forbrukteDager)
        assertEquals(listOf(8, 238), gjenståendeDager)
        assertEquals(listOf(12.juni(2019), 13.mai(2020)), maksdatoer)
    }

    @Test
    fun `foreldet dager før maksdato, etter syk, teller som opphold`() {
        val tidslinje = tidslinjeOf(240.NAVDAGER, 182.FOR, 10.NAVDAGER)
        val avslåtteDager = listOf(tidslinje).utbetalingsavgrenser(UNG_PERSON_FNR_2018)

        assertEquals(listOf(emptyList(), emptyList<Nothing>()), avslåtteDager)
        assertEquals(listOf(240, 10), forbrukteDager)
        assertEquals(listOf(8, 238), gjenståendeDager)
        assertEquals(listOf(12.juni(2019), 13.mai(2020)), maksdatoer)
    }

    @Test
    fun `foreldet dager før maksdato, etter opphold, teller som opphold`() {
        val tidslinje = tidslinjeOf(240.NAVDAGER, 1.ARB, 181.FOR, 10.NAVDAGER)
        val avslåtteDager = listOf(tidslinje).utbetalingsavgrenser(UNG_PERSON_FNR_2018)

        assertEquals(listOf(emptyList(), emptyList<Nothing>()), avslåtteDager)
        assertEquals(listOf(240, 10), forbrukteDager)
        assertEquals(listOf(8, 238), gjenståendeDager)
        assertEquals(listOf(12.juni(2019), 13.mai(2020)), maksdatoer)
    }

    @Test
    fun `foreldet dager før maksdato, etter fri, teller som opphold`() {
        val tidslinje = tidslinjeOf(240.NAVDAGER, 1.FRI, 181.FOR, 10.NAVDAGER)
        val avslåtteDager = listOf(tidslinje).utbetalingsavgrenser(UNG_PERSON_FNR_2018)

        assertEquals(listOf(emptyList(), emptyList<Nothing>()), avslåtteDager)
        assertEquals(listOf(240, 10), forbrukteDager)
        assertEquals(listOf(8, 238), gjenståendeDager)
        assertEquals(listOf(12.juni(2019), 13.mai(2020)), maksdatoer)
    }

    @Test
    fun `avviste dager avvises med maksdatobegrunnelse i tillegg`() {
        val tidslinje = tidslinjeOf(16.AP, 248.NAVDAGER, 182.AVV(1000, begrunnelse = MinimumSykdomsgrad), 16.AP, 248.NAVDAGER, startDato = 5.juli(2016))
        val forventetFørsteAvvisteDag = 4.juli(2017)
        val forventetSisteAvvisteDag = forventetFørsteAvvisteDag.plusDays(181)
        val avvisteDager = (forventetFørsteAvvisteDag til forventetSisteAvvisteDag).toList()
        val avslåtteDager = listOf(tidslinje).utbetalingsavgrenser(UNG_PERSON_FNR_2018)

        assertEquals(listOf(avvisteDager, emptyList<Nothing>()), avslåtteDager)
        assertEquals(listOf(248, 248), forbrukteDager)
        assertEquals(listOf(0, 0), gjenståendeDager)
        assertEquals(listOf(3.juli(2017), 31.desember), maksdatoer)
        avslåtteDager.map { dager ->
            dager.forEach {
                assertEquals(SykepengedagerOppbrukt, begrunnelse(it)) { "Feil begrunnelse for $it" }
            }
        }
    }

    @Test
    fun `maksdato med fravær på slutten`() {
        val tidslinje = tidslinjeOf(16.AP, 10.FRI)
        val avslåtteDager = tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018)

        assertEquals(emptyList<Nothing>(), avslåtteDager)
        assertEquals(listOf(0), forbrukteDager)
        assertEquals(listOf(248), gjenståendeDager)
        assertEquals(listOf(9.januar(2019)), maksdatoer)
    }

    @Test
    fun `riktig antall dager`() {
        val tidslinje = tidslinjeOf(16.AP, 10.NAV)
        val avslåtteDager = tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018)

        assertEquals(emptyList<Nothing>(), avslåtteDager)
        assertEquals(listOf(8), forbrukteDager)
        assertEquals(listOf(240), gjenståendeDager)
        assertEquals(listOf(28.desember), maksdatoer)
    }

    @Test
    fun `stopper betaling etter 248 dager`() {
        val tidslinje = tidslinjeOf(16.AP, 249.NAVDAGER)
        val avslåtteDager = tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018)

        assertEquals(listOf(31.desember), avslåtteDager)
        assertEquals(listOf(248), forbrukteDager)
        assertEquals(listOf(0), gjenståendeDager)
        assertEquals(listOf(28.desember), maksdatoer)
    }

    @Test
    fun `stopper betaling etter 248 dager inkl persontidslinje`() {
        val persontidslinje = tidslinjeOf(16.AP, 247.NAVDAGER)
        val tidslinje = tidslinjeOf(2.NAVDAGER, startDato = persontidslinje.periode().endInclusive.plusDays(1))
        val avslåtteDager = tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018, personTidslinje = persontidslinje)

        assertEquals(listOf(31.desember), avslåtteDager)
        assertEquals(listOf(248), forbrukteDager)
        assertEquals(listOf(0), gjenståendeDager)
        assertEquals(listOf(28.desember), maksdatoer)
    }

    @Test
    fun `26 uker arbeid resetter utbetalingsgrense`() {
        val tidslinje = tidslinjeOf(16.AP, 248.NAVDAGER, (TILSTREKKELIG_OPPHOLD_I_SYKEDAGER).ARB, 10.NAV)
        val avslåtteDager = listOf(tidslinje).utbetalingsavgrenser(UNG_PERSON_FNR_2018)

        assertEquals(listOf(emptyList(), emptyList<Nothing>()), avslåtteDager)
        assertEquals(listOf(248, 6), forbrukteDager)
        assertEquals(listOf(0, 242), gjenståendeDager)
        assertEquals(listOf(28.desember, 10.juni(2020)), maksdatoer)
    }

    @Test
    fun `utbetalingsgrense resettes ikke når oppholdsdager nås ved sykedager`() {
        val tidslinje = tidslinjeOf(16.AP, 248.NAVDAGER, (25 * 7).ARB, 10.NAV)
        val avslåtteDager = tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018)
        val periode = 22.juni(2019) til 1.juli(2019)

        assertEquals(periode.utenHelg(), avslåtteDager)
        assertEquals(listOf(248), forbrukteDager)
        assertEquals(listOf(0), gjenståendeDager)
        assertEquals(listOf(28.desember), maksdatoer)
    }

    @Test
    fun `utbetalingsgrense resettes ikke når oppholdsdager nås ved sykedager 1`() {
        val tidslinje = tidslinjeOf(16.AP, 248.NAVDAGER)
        val avslåtteDager = tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018)

        assertEquals(emptyList<Nothing>(), avslåtteDager)
        assertEquals(listOf(248), forbrukteDager)
        assertEquals(listOf(0), gjenståendeDager)
        assertEquals(listOf(28.desember), maksdatoer)
    }

    @Test
    fun `utbetalingsgrense resettes ikke når oppholdsdager nås ved sykedager 2`() {
        val tidslinje = tidslinjeOf(16.AP, 248.NAVDAGER, (25 * 7).ARB)
        val avslåtteDager = tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018)

        assertEquals(emptyList<Nothing>(), avslåtteDager)
        assertEquals(listOf(248), forbrukteDager)
        assertEquals(listOf(0), gjenståendeDager)
        assertEquals(listOf(28.desember), maksdatoer)
    }

    @Test
    fun `utbetalingsgrense resettes ikke når oppholdsdager nås ved sykhelg`() {
        val tidslinje = tidslinjeOf(16.AP, 248.NAVDAGER, (25 * 7).ARB, 5.ARB, 10.NAV) // de to første navdagene er helg
        val periode = 27.juni(2019) til 6.juli(2019)
        val avslåtteDager = tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018)

        assertEquals(periode.utenHelg(), avslåtteDager)
        assertEquals(listOf(248), forbrukteDager)
        assertEquals(listOf(0), gjenståendeDager)
        assertEquals(listOf(28.desember), maksdatoer)
    }

    @Test
    fun `utbetalingsgrense resettes ved første arbeidsdag`() {
        val tidslinje = tidslinjeOf(16.AP, 248.NAVDAGER, (25 * 7).ARB, 10.NAV, 1.ARB, 10.NAV)
        val periode = 22.juni(2019) til 1.juli(2019)
        val avslåtteDager = listOf(tidslinje).utbetalingsavgrenser(UNG_PERSON_FNR_2018)

        assertEquals(listOf(periode.utenHelg(), emptyList()), avslåtteDager)
        assertEquals(listOf(248, 8), forbrukteDager)
        assertEquals(listOf(0, 240), gjenståendeDager)
        assertEquals(listOf(28.desember, 12.juni(2020)), maksdatoer)
    }

    @Test
    fun `utbetalingsgrense resettes ved første arbeidsgiverperiodedag`() {
        val tidslinje = tidslinjeOf(16.AP, 248.NAVDAGER, (25 * 7).ARB, 10.NAV, 1.AP, 10.NAV)
        val periode = 22.juni(2019) til 1.juli(2019)
        val avslåtteDager = listOf(tidslinje).utbetalingsavgrenser(UNG_PERSON_FNR_2018)

        assertEquals(listOf(periode.utenHelg(), emptyList()), avslåtteDager)
        assertEquals(listOf(248, 8), forbrukteDager)
        assertEquals(listOf(0, 240), gjenståendeDager)
        assertEquals(listOf(28.desember, 12.juni(2020)), maksdatoer)
    }

    @Test
    fun `en ubetalt sykedag før opphold`() {
        val tidslinje = tidslinjeOf(16.AP, 249.NAVDAGER, (TILSTREKKELIG_OPPHOLD_I_SYKEDAGER).ARB, 10.NAV)
        val avslåtteDager = listOf(tidslinje).utbetalingsavgrenser(UNG_PERSON_FNR_2018)

        assertEquals(listOf(listOf(31.desember), emptyList()), avslåtteDager)
        assertEquals(listOf(248, 8), forbrukteDager)
        assertEquals(listOf(0, 240), gjenståendeDager)
        assertEquals(listOf(28.desember, 11.juni(2020)), maksdatoer)
    }

    @Test
    fun `noe som helst sykdom i opphold resetter teller`() {
        val tidslinje = tidslinjeOf(16.AP, 248.NAVDAGER, (24 * 7).ARB, 7.NAV, (2 * 7).ARB, 10.NAV)
        val avslåtteDager = listOf(tidslinje).utbetalingsavgrenser(UNG_PERSON_FNR_2018)

        assertEquals(listOf((17.juni(2019) til 21.juni(2019)).toList(), emptyList<Nothing>()), avslåtteDager)
        assertEquals(listOf(248, 6), forbrukteDager)
        assertEquals(listOf(0, 242), gjenståendeDager)
        assertEquals(listOf(28.desember, 17.juni(2020)), maksdatoer)
    }

    @Test
    fun `fridag etter sykdag er en del av opphold`() {
        val tidslinje = tidslinjeOf(248.NAVDAGER, (25 * 7).FRI, 7.ARB, 7.NAV)
        val avslåtteDager = listOf(tidslinje).utbetalingsavgrenser(UNG_PERSON_FNR_2018)

        assertEquals(listOf(emptyList(), emptyList<Nothing>()), avslåtteDager)
        assertEquals(listOf(248, 5), forbrukteDager)
        assertEquals(listOf(0, 243), gjenståendeDager)
        assertEquals(listOf(12.desember, 25.mai(2020)), maksdatoer)
    }

    @Test
    fun `opphold på mindre enn 26 uker skal ikke nullstille telleren`() {
        val tidslinje = tidslinjeOf(16.AP, 248.NAVDAGER, (TILSTREKKELIG_OPPHOLD_I_SYKEDAGER - 1).FRI, 1.NAVDAGER)
        val avslåtteDager = tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018)

        assertEquals(listOf(28.juni(2019)), avslåtteDager)
        assertEquals(listOf(248), forbrukteDager)
        assertEquals(listOf(0), gjenståendeDager)
        assertEquals(listOf(28.desember), maksdatoer)
    }

    @Test
    fun `sjekk at 26 uker med syk etter karantene ikke starter utbetaling`() {
        val tidslinje = tidslinjeOf(16.AP, 248.NAVDAGER, (TILSTREKKELIG_OPPHOLD_I_SYKEDAGER).NAVDAGER, 60.NAVDAGER)
        val avslåtteDager = tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018)

        assertEquals(TILSTREKKELIG_OPPHOLD_I_SYKEDAGER + 60, avslåtteDager.size)
        assertEquals((31.desember til 3.desember(2019)).utenHelg(), avslåtteDager)
        assertEquals(listOf(248), forbrukteDager)
        assertEquals(listOf(0), gjenståendeDager)
        assertEquals(listOf(28.desember), maksdatoer)
        avslåtteDager.take(130).map {
            assertEquals(Begrunnelse.SykepengedagerOppbrukt, begrunnelse(it)) { "Feil begrunnelse for $it" }
        }
        avslåtteDager.drop(130).map {
            assertEquals(Begrunnelse.NyVilkårsprøvingNødvendig, begrunnelse(it)) { "Feil begrunnelse for $it" }
        }
    }

    @Test
    fun `sjekk at 26 uker med syk etter karantene starter utbetaling etter første arbeidsdag`() {
        val tidslinje = tidslinjeOf(16.AP, 248.NAVDAGER, (TILSTREKKELIG_OPPHOLD_I_SYKEDAGER).NAVDAGER, 1.ARB, 60.NAVDAGER)
        val avslåtteDager = listOf(tidslinje).utbetalingsavgrenser(UNG_PERSON_FNR_2018)

        assertEquals(TILSTREKKELIG_OPPHOLD_I_SYKEDAGER, avslåtteDager[0].size)
        assertEquals(listOf((31.desember til 10.september(2019)).utenHelg(), emptyList<Nothing>()), avslåtteDager)
        assertEquals(listOf(248, 60), forbrukteDager)
        assertEquals(listOf(0, 188), gjenståendeDager)
        assertEquals(listOf(28.desember, 24.august(2020)), maksdatoer)
    }

    @Test
    fun `helgedager teller ikke som opphold`() {
        val tidslinje = tidslinjeOf(248.NAVDAGER, (TILSTREKKELIG_OPPHOLD_I_SYKEDAGER).HELG, 60.NAVDAGER)
        val avslåtteDager = tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018)

        assertEquals(60, avslåtteDager.size)
        assertEquals((13.juni(2019) til 4.september(2019)).utenHelg(), avslåtteDager)
        assertEquals(listOf(248), forbrukteDager)
        assertEquals(listOf(0), gjenståendeDager)
        assertEquals(listOf(12.desember), maksdatoer)
    }

    @Test
    fun `helgedager teller som opphold hvis før av arbeidsdag`() {
        val tidslinje = tidslinjeOf(248.NAVDAGER, (TILSTREKKELIG_OPPHOLD_I_SYKEDAGER - 1).HELG, 1.ARB, 60.NAVDAGER)
        val avslåtteDager = listOf(tidslinje).utbetalingsavgrenser(UNG_PERSON_FNR_2018)

        assertEquals(listOf(emptyList(), emptyList<Nothing>()), avslåtteDager)
        assertEquals(listOf(248, 60), forbrukteDager)
        assertEquals(listOf(0, 188), gjenståendeDager)
        assertEquals(listOf(12.desember, 25.mai(2020)), maksdatoer)
    }

    @Test
    fun `helgedager før sykdom er ikke opphold`() {
        val tidslinje = tidslinjeOf(248.NAVDAGER, 1.ARB, (TILSTREKKELIG_OPPHOLD_I_SYKEDAGER - 1).HELG, 60.NAVDAGER)
        val avslåtteDager = tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018)

        assertEquals(60, avslåtteDager.size)
        assertEquals((13.juni(2019) til 4.september(2019)).utenHelg(), avslåtteDager)
        assertEquals(listOf(248), forbrukteDager)
        assertEquals(listOf(0), gjenståendeDager)
        assertEquals(listOf(12.desember), maksdatoer)
    }

    @Test
    fun `248 dager nådd på 3 år med helg`() {
        val tidslinje = tidslinjeOf(
            48.NAVDAGER,
            127.ARB,
            48.NAVDAGER,
            127.ARB,
            48.NAVDAGER,
            127.ARB,
            19.NAVDAGER,
            180.ARB,
            2.HELG, // 2 helgedager pluss forrige 180 dager utgjør 182 dager gap => resett
            48.NAVDAGER,
            141.ARB,
            6.NAVDAGER,
            90.ARB, // 54 remaining days from here, outside 26 week gap
            1.NAVDAGER,
            3.ARB,
            5.NAVDAGER,
            (248 - 60).NAVDAGER,
            1.NAVDAGER
        )
        val avslåtteDager = listOf(tidslinje).utbetalingsavgrenser(UNG_PERSON_FNR_2018)
        assertEquals(listOf(emptyList(), listOf(6.oktober(2021))), avslåtteDager)
        assertEquals(listOf(163, 248), forbrukteDager)
        assertEquals(listOf(85, 0), gjenståendeDager)
        assertEquals(listOf(29.juni(2020), 5.oktober(2021)), maksdatoer)
    }

    @Test
    fun `helgedager teller som opphold hvis før og etter arbeidsdag`() {
        val tidslinje = tidslinjeOf(248.NAVDAGER, 1.ARB, (TILSTREKKELIG_OPPHOLD_I_SYKEDAGER - 2).HELG, 1.ARB, 60.NAVDAGER)
        val avslåtteDager = listOf(tidslinje).utbetalingsavgrenser(UNG_PERSON_FNR_2018)

        assertEquals(listOf(emptyList(), emptyList<Nothing>()), avslåtteDager)
        assertEquals(listOf(248, 60), forbrukteDager)
        assertEquals(listOf(0, 188), gjenståendeDager)
        assertEquals(listOf(12.desember, 25.mai(2020)), maksdatoer)
    }

    @Test
    fun `helgedager sammen med utbetalingsdager teller som ikke opphold`() {
        val tidslinje = tidslinjeOf(248.NAVDAGER, (20 * 7).HELG, 7.NAVDAGER, (5 * 7).HELG, 60.NAVDAGER)
        val avslåtteDager = tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018)

        assertEquals(67, avslåtteDager.size)
        assertEquals((2.mai(2019) til 10.mai(2019)).utenHelg() + (17.juni(2019) til 6.september(2019)).utenHelg(), avslåtteDager)
        assertEquals(listOf(248), forbrukteDager)
        assertEquals(listOf(0), gjenståendeDager)
        assertEquals(listOf(12.desember), maksdatoer)
    }

    @Test
    fun `sjekk at sykdom i arbgiver periode ikke ødelegger oppholdsperioden`() {
        val tidslinje = tidslinjeOf(50.NAVDAGER, (25 * 7).ARB, 7.AP, 248.NAVDAGER)
        val avslåtteDager = listOf(tidslinje).utbetalingsavgrenser(UNG_PERSON_FNR_2018)

        assertEquals(listOf(emptyList(), emptyList<Nothing>()), avslåtteDager)
        assertEquals(listOf(50, 248), forbrukteDager)
        assertEquals(listOf(198, 0), gjenståendeDager)
        assertEquals(listOf(12.juni(2019), 21.august(2019)), maksdatoer)
    }

    @Test
    fun `helgedager innimellom utbetalingsdager betales ikke`() {
        val tidslinje = tidslinjeOf(16.AP, 200.NAVDAGER, 40.HELG, 48.NAVDAGER, 1.NAVDAGER)
        val avslåtteDager = tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018)

        assertEquals(listOf(7.februar(2019)), avslåtteDager)
        assertEquals(listOf(248), forbrukteDager)
        assertEquals(listOf(0), gjenståendeDager)
        assertEquals(listOf(6.februar(2019)), maksdatoer)
    }

    @Test
    fun `ukjente dager generert når du legger til to utbetalingstidslinjer teller som ikke-utbetalingsdager`() {
        val tidslinje1 = tidslinjeOf(50.NAVDAGER)
        val tidslinje2 = tidslinjeOf((TILSTREKKELIG_OPPHOLD_I_SYKEDAGER).UTELATE, 248.NAVDAGER, startDato = tidslinje1.periode().endInclusive.plusDays(1))
        val tidslinje = tidslinje1 + tidslinje2
        val avslåtteDager = listOf(tidslinje).utbetalingsavgrenser(UNG_PERSON_FNR_2018)

        assertEquals(listOf(emptyList(), emptyList<Nothing>()), avslåtteDager)
        assertEquals(listOf(50, 248), forbrukteDager)
        assertEquals(listOf(198, 0), gjenståendeDager)
        assertEquals(listOf(12.juni(2019), 21.august(2019)), maksdatoer)
    }

    @Test
    fun `248 dager nådd på 3 år`() {
        val tidslinje = tilbakevendendeSykdom(3.NAVDAGER, 10.ARB, 1.NAVDAGER)
        val avslåtteDager = tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018)

        assertEquals(listOf(5.januar(2021), 18.januar(2021)), avslåtteDager)
        assertEquals(listOf(248), forbrukteDager)
        assertEquals(listOf(0), gjenståendeDager)
        assertEquals(listOf(4.januar(2021)), maksdatoer)
    }

    @Test
    fun `sykepengedager eldre enn tre år teller ikke lenger som forbrukte dager`() {
        val tidslinje = tilbakevendendeSykdom(1.NAVDAGER, 3.ARB, 5.NAVDAGER, 1.NAVDAGER)
        val avslåtteDager = tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018)

        assertEquals(listOf(12.januar(2021)), avslåtteDager)
        assertEquals(listOf(248), forbrukteDager)
        assertEquals(listOf(0), gjenståendeDager)
        assertEquals(listOf(11.januar(2021)), maksdatoer)
    }

    @Test
    fun `246 dager nådd på 3 år`() {
        val tidslinje = tilbakevendendeSykdom()
        val avslåtteDager = tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018)

        assertEquals(emptyList<Nothing>(), avslåtteDager)
        assertEquals(listOf(246), forbrukteDager)
        assertEquals(listOf(2), gjenståendeDager)
        assertEquals(listOf(4.januar(2021)), maksdatoer)
    }

    @Test
    fun `gyldig ferie påvirker ikke 3 årsvinduet`() {
        val tidslinje = tilbakevendendeSykdom(1.NAVDAGER, 10.FRI, 2.NAVDAGER)
        val avslåtteDager = tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018)

        assertEquals(listOf(13.januar(2021)), avslåtteDager)
        assertEquals(listOf(248), forbrukteDager)
        assertEquals(listOf(0), gjenståendeDager)
        assertEquals(listOf(12.januar(2021)), maksdatoer)
    }

    @Test
    fun `ugyldig ferie påvirker 3 årsvinduet`() {
        val tidslinje = tilbakevendendeSykdom(10.FRI, 3.NAVDAGER)
        val avslåtteDager = tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018)

        assertTrue(avslåtteDager.isEmpty())
        assertEquals(emptyList<Nothing>(), avslåtteDager)
        assertEquals(listOf(241), forbrukteDager)
        assertEquals(listOf(7), gjenståendeDager)
        assertEquals(listOf(22.januar(2021)), maksdatoer)
    }

    @Test
    fun `ferie hos ag1 og arbeidsdag hos ag2 - treårsvindu forskyves ikke`() {
        val ag1 = tidslinjeOf(
            11.AP, 52.ARB, 12.NAV, 109.ARB, 8.NAV, 66.ARB, 1.FRI, 15.AP, 1.ARB, 13.NAV, 87.ARB,
            16.AP, 34.NAV, 36.ARB, 1.FRI, 1.AP, 1.ARB, 1.AP, 7.ARB, 1.AP, 6.ARB, 13.AP, 13.NAV, 26.FRI, 51.NAV, 12.FRI, 30.NAV, startDato = 1.mars(2021)
        )
        // 3. oktober er fridag hos ag1; utfallet skal ikke være at vi teller dagen som Arbeidsdag, da det vil medføre at vi går inn i en
        // opphold-situasjon, som vi ville gått ut av 17. oktober (ved neste Nav-dag).
        // Når man går ut av en Opphold-situasjon så vil vi sette et nytt starttidspunkt for treårsvinduet, og da beregne
        // forbrukte dager annerledes.
        val ag2 = tidslinjeOf(20.ARB, startDato = 3.oktober(2022))
        val infotrygd = tidslinjeOf(
            6.NAV, 62.ARB, 124.NAV, 151.ARB, 21.NAV, 12.ARB, 3.NAV, 116.ARB, 19.NAV,
            163.ARB, 14.NAV, 108.ARB, 8.NAV, 83.ARB, 13.NAV, 103.ARB, 34.NAV, startDato = 25.juni(2019)
        )

        val avslåtteDager = listOf(ag1, ag2).utbetalingsavgrenser(UNG_PERSON_FNR_2018, personTidslinje = infotrygd)

        assertEquals(listOf(emptyList<Nothing>()), avslåtteDager)
        assertEquals(listOf(236), forbrukteDager)
        assertEquals(listOf(12), gjenståendeDager)
        assertEquals(listOf(30.november(2022)), maksdatoer)
    }

    @Test
    fun `nødvendig opphold nådd nesten ved ferie, fullført med én arbeidsdag`() {
        val tidslinje = tidslinjeOf(247.NAVDAGER, 181.FRI, 1.ARB, 1.NAVDAGER)
        val avslåtteDager = listOf(tidslinje).utbetalingsavgrenser(UNG_PERSON_FNR_2018)

        assertEquals(listOf(emptyList(), emptyList<Nothing>()), avslåtteDager)
        assertEquals(listOf(247, 1), forbrukteDager)
        assertEquals(listOf(1, 247), gjenståendeDager)
        assertEquals(listOf(12.juni(2019), 22.mai(2020)), maksdatoer)
    }

    @Test
    fun `gyldig ferie tas ikke med i videre oppholdstelling`() {
        val tidslinje = tidslinjeOf(82.NAVDAGER, 27.FRI, 82.NAVDAGER, 155.ARB, 85.NAVDAGER)
        val avslåtteDager = tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018)

        assertEquals(listOf(13.juni(2019)), avslåtteDager)
        assertEquals(listOf(248), forbrukteDager)
        assertEquals(listOf(0), gjenståendeDager)
        assertEquals(listOf(12.juni(2019)), maksdatoer)
    }

    @Test
    fun `gyldig ferie påvirker 26 ukers telling`() {
        val tidslinje = tidslinjeOf(247.NAVDAGER, 182.FRI, 10.NAVDAGER)
        val avslåtteDager = listOf(tidslinje).utbetalingsavgrenser(UNG_PERSON_FNR_2018)

        assertEquals(listOf(emptyList<LocalDate>(), emptyList()), avslåtteDager)
        assertEquals(listOf(247, 10), forbrukteDager)
        assertEquals(listOf(1, 238), gjenståendeDager)
        assertEquals(listOf(12.juni(2019), 22.mai(2020)), maksdatoer)
    }

    @Test
    fun `ugyldig ferie påvirker 26 ukers telling`() {
        val tidslinje = tidslinjeOf(247.NAVDAGER, 181.FRI, 1.ARB, 10.NAVDAGER)
        val avslåtteDager = listOf(tidslinje).utbetalingsavgrenser(UNG_PERSON_FNR_2018)

        assertEquals(listOf(emptyList(), emptyList<Nothing>()), avslåtteDager)
        assertEquals(listOf(247, 10), forbrukteDager)
        assertEquals(listOf(1, 238), gjenståendeDager)
        assertEquals(listOf(12.juni(2019), 22.mai(2020)), maksdatoer)
    }

    @Test
    fun `ferie mellom utbetalinger gir ikke ny rettighet etter maksdato`() {
        val tidslinje = tidslinjeOf(248.NAVDAGER, 182.FRI, 10.NAVDAGER)
        val avslåtteDager = tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018)

        assertEquals(10, avslåtteDager.size)
        assertEquals((13.juni(2019) til 26.juni(2019)).utenHelg(), avslåtteDager)
        assertEquals(listOf(248), forbrukteDager)
        assertEquals(listOf(0), gjenståendeDager)
        assertEquals(listOf(12.desember), maksdatoer)
    }

    @Test
    fun `må ha vært på arbeid for å få ny utbetaling etter ferie etter maksdato`() {
        val tidslinje = tidslinjeOf(248.NAVDAGER, 181.FRI, 1.ARB, 10.NAVDAGER)
        val avslåtteDager = listOf(tidslinje).utbetalingsavgrenser(UNG_PERSON_FNR_2018)

        assertEquals(listOf(emptyList(), emptyList<Nothing>()), avslåtteDager)
        assertEquals(listOf(248, 10), forbrukteDager)
        assertEquals(listOf(0, 238), gjenståendeDager)
        assertEquals(listOf(12.desember, 25.mai(2020)), maksdatoer)
    }

    @Test
    fun `26 ukers friske tilbakestiller skyvevindu på 3 år`() {
        val tidslinje = enAnnenSykdom(1.NAVDAGER, 3.ARB, 5.NAVDAGER, (248 - 60).NAVDAGER, 1.NAVDAGER)
        val avslåtteDager = listOf(tidslinje).utbetalingsavgrenser(UNG_PERSON_FNR_2018)

        assertEquals(listOf(emptyList(), listOf(1.oktober(2021))), avslåtteDager)
        assertEquals(listOf(162, 248), forbrukteDager)
        assertEquals(listOf(86, 0), gjenståendeDager)
        assertEquals(listOf(29.juni(2020), 30.september(2021)), maksdatoer)
    }

    @Test
    fun `teller sykedager med 26 uker`() {
        val tidslinje = enAnnenSykdom()
        val avslåtteDager = listOf(tidslinje).utbetalingsavgrenser(UNG_PERSON_FNR_2018)

        assertEquals(listOf(emptyList(), emptyList<Nothing>()), avslåtteDager)
        assertEquals(listOf(162, 54), forbrukteDager)
        assertEquals(listOf(86, 194), gjenståendeDager)
        assertEquals(listOf(29.juni(2020), 29.september(2021)), maksdatoer)
    }

    @Test
    fun `teller sykedager med opphold i sykdom`() {
        val tidslinje = tidslinjeOf(12.NAV, startDato = 1.mars)
        val historikk = tidslinjeOf(45.NAV, startDato = 1.januar(2018))
        val avslåtteDager = tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018, personTidslinje = historikk)

        assertEquals(emptyList<Nothing>(), avslåtteDager)
        assertEquals(listOf(41), forbrukteDager)
        assertEquals(listOf(207), gjenståendeDager)
        assertEquals(listOf(26.desember), maksdatoer)
    }

    @Test
    fun `teller sykedager med overlapp`() {
        val tidslinje = tidslinjeOf(12.NAV, startDato = 1.februar)
        val historikk = tidslinjeOf(12.ARB, 45.NAV, startDato = 1.januar(2018))
        val avslåtteDager = tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018, personTidslinje = historikk)

        assertEquals(emptyList<Nothing>(), avslåtteDager)
        assertEquals(listOf(31), forbrukteDager)
        assertEquals(listOf(217), gjenståendeDager)
        assertEquals(listOf(26.desember), maksdatoer)
    }

    @Test
    fun `teller sykedager med konflikt`() {
        val tidslinje = tidslinjeOf(12.NAV)
        val historikk = tidslinjeOf(12.ARB, 45.NAV)
        val avslåtteDager = tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018, personTidslinje = historikk)

        assertEquals(emptyList<Nothing>(), avslåtteDager)
        assertEquals(listOf(41), forbrukteDager)
        assertEquals(listOf(207), gjenståendeDager)
        assertEquals(listOf(12.desember), maksdatoer)
    }

    // No 26 week gap with base of 246 NAV days
    private fun tilbakevendendeSykdom(vararg utbetalingsdager: Utbetalingsdager): Utbetalingstidslinje {
        return tidslinjeOf(
            48.NAVDAGER,
            140.ARB,
            48.NAVDAGER,
            140.ARB,
            48.NAVDAGER,
            140.ARB,
            48.NAVDAGER,
            140.ARB,
            48.NAVDAGER,
            140.ARB,
            6.NAVDAGER,
            52.ARB,
            *utbetalingsdager
        )
    }

    // 26 week gap inside 3 year window of 246 days with 54 NAV days after the gap
    private fun enAnnenSykdom(vararg utbetalingsdager: Utbetalingsdager): Utbetalingstidslinje {
        return tidslinjeOf(
            48.NAVDAGER,
            127.ARB,
            48.NAVDAGER,
            127.ARB,
            48.NAVDAGER,
            127.ARB,
            18.NAVDAGER,
            182.ARB,
            48.NAVDAGER,
            141.ARB,
            6.NAVDAGER,
            90.ARB,
            *utbetalingsdager
        )
    }

    private fun begrunnelse(dato: LocalDate): Begrunnelse? {
        return vurderinger.firstNotNullOfOrNull { it.begrunnelser[dato] }
    }

    private fun Periode.utenHelg() = filterNot { it.erHelg() }
    private fun Utbetalingstidslinje.utbetalingsavgrenser(
        fødselsdato: LocalDate,
        personTidslinje: Utbetalingstidslinje = Utbetalingstidslinje(),
        dødsdato: LocalDate? = null,
        regler: MaksimumSykepengedagerregler = NormalArbeidstaker
    ): List<LocalDate> {
        return listOf(this).utbetalingsavgrenser(fødselsdato, personTidslinje, dødsdato, regler).single()
    }

    private fun List<Utbetalingstidslinje>.utbetalingsavgrenser(
        fødselsdato: LocalDate,
        personTidslinje: Utbetalingstidslinje = Utbetalingstidslinje(),
        dødsdato: LocalDate? = null,
        regler: MaksimumSykepengedagerregler = NormalArbeidstaker
    ): List<List<LocalDate>> {
        val sekstisyvårsdagen = fødselsdato.plusYears(67)
        val syttiårsdagen = fødselsdato.plusYears(70)

        val maksdatoberegning = Maksdatoberegning(
            sekstisyvårsdagen = sekstisyvårsdagen,
            syttiårsdagen = syttiårsdagen,
            dødsdato = dødsdato,
            regler = regler,
            infotrygdtidslinje = personTidslinje
        )

        val tidslinjer = this.mapIndexed { index, it ->
            Arbeidsgiverberegning(
                yrkesaktivitet = Behandlingsporing.Yrkesaktivitet.Arbeidstaker("a${index+1}"),
                vedtaksperioder = listOf(
                    Vedtaksperiodeberegning(
                        vedtaksperiodeId = UUID.randomUUID(),
                        utbetalingstidslinje = it
                    )
                ),
                ghostOgAndreInntektskilder = emptyList()
            )
        }
        vurderinger = maksdatoberegning.beregn(tidslinjer)
        maksdatoresultater = vurderinger
            .map { it.beregnMaksdato(syttiårsdagen, dødsdato) }
        maksdatoer = maksdatoresultater.map {
            it.maksdato
        }
        forbrukteDager = maksdatoresultater.map {
            it.antallForbrukteDager
        }
        gjenståendeDager = maksdatoresultater.map {
            it.gjenståendeDager
        }
        return vurderinger.map { it.avslåtteDager.toList() }
    }
}
