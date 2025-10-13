package no.nav.helse.person.beløp

import no.nav.helse.april
import no.nav.helse.dto.BeløpstidslinjeDto
import no.nav.helse.februar
import no.nav.helse.hendelser.Avsender
import no.nav.helse.hendelser.somPeriode
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.dsl.BeløpstidslinjeDsl.Arbeidsgiver
import no.nav.helse.dsl.BeløpstidslinjeDsl.Saksbehandler
import no.nav.helse.dsl.BeløpstidslinjeDsl.Sykmeldt
import no.nav.helse.dsl.BeløpstidslinjeDsl.Systemet
import no.nav.helse.dsl.BeløpstidslinjeDsl.arbeidsgiver
import no.nav.helse.dsl.BeløpstidslinjeDsl.fra
import no.nav.helse.dsl.BeløpstidslinjeDsl.hele
import no.nav.helse.dsl.BeløpstidslinjeDsl.kun
import no.nav.helse.dsl.BeløpstidslinjeDsl.og
import no.nav.helse.dsl.BeløpstidslinjeDsl.oppgir
import no.nav.helse.dsl.BeløpstidslinjeDsl.perioderMedBeløp
import no.nav.helse.dsl.BeløpstidslinjeDsl.til

internal class BeløpstidslinjeTest {

    @Test
    fun `fyll og forkort kant i kant`() {
        val beløpstidslinje =
            (Arbeidsgiver oppgir 1000.daglig hele januar) og (Arbeidsgiver oppgir 2000.daglig hele februar)

        val forventetForkortet =
            (Arbeidsgiver oppgir 1000.daglig kun 1.januar) og (Arbeidsgiver oppgir 2000.daglig kun 1.februar)

        Assertions.assertEquals(forventetForkortet, beløpstidslinje.forkort())
        Assertions.assertEquals(beløpstidslinje, forventetForkortet.fyll(28.februar))
    }

    @Test
    fun `fyll og forkort med gap`() {
        val beløpstidslinje =
            (Arbeidsgiver oppgir 1000.daglig hele januar) og (Saksbehandler oppgir 2000.daglig hele mars)

        val forventetForkortet =
            (Arbeidsgiver oppgir 1000.daglig kun 1.januar) og (Saksbehandler oppgir 2000.daglig kun 1.mars)

        Assertions.assertEquals(forventetForkortet, beløpstidslinje.forkort())

        // Nå fylles også gapet med opplysningene i forkant
        val forventetFylt =
            (Arbeidsgiver oppgir 1000.daglig fra 1.januar til 28.februar) og (Saksbehandler oppgir 2000.daglig hele mars)

        Assertions.assertEquals(forventetFylt, forventetForkortet.fyll(31.mars))
    }

    @Test
    fun `trekke fra en beløpstidslinje`() {
        val fraInntektsmelding =
            (Arbeidsgiver oppgir 1000.daglig fra 2.januar til 30.januar)

        val overstyring =
            (Saksbehandler oppgir 1000.daglig fra 1.januar til 20.januar) og
                (Saksbehandler oppgir 1500.daglig fra 21.januar til 31.januar)

        val forventetDiff =
            (Saksbehandler oppgir 1000.daglig kun 1.januar) og
                (Saksbehandler oppgir 1500.daglig fra 21.januar til 31.januar)

        Assertions.assertEquals(forventetDiff, overstyring - fraInntektsmelding)
    }

    @Test
    fun `fylle en hullete beløpstidslinje`() {
        val beløpstidslinje1 = (Arbeidsgiver oppgir 1000.daglig hele januar)
        val beløpstidslinje2 = (Saksbehandler oppgir 2000.daglig hele mars)
        val beløpstidslinje3 = (Systemet oppgir 3000.daglig fra 2.april til 30.april)

        val sammenslått = beløpstidslinje1 + beløpstidslinje2 + beløpstidslinje3
        Assertions.assertEquals(listOf(januar, mars, 2.april til 30.april), sammenslått.perioderMedBeløp)
        val fylt = sammenslått.fyll()
        Assertions.assertEquals(listOf(1.januar til 30.april), fylt.perioderMedBeløp)

        val forventet = (Arbeidsgiver oppgir 1000.daglig fra 1.januar til 28.februar) og (Saksbehandler oppgir 2000.daglig fra 1.mars til 1.april) + (Systemet oppgir 3000.daglig fra 2.april til 30.april)
        Assertions.assertEquals(forventet, fylt)
    }

    @Test
    fun equals() {
        val beløpstidslinje1 = (Arbeidsgiver oppgir 1000.daglig hele januar)
        val beløpstidslinje2 = (Arbeidsgiver oppgir 1000.daglig hele januar)
        val beløpstidslinje3 = (Saksbehandler oppgir 1000.daglig hele januar)
        val beløpstidslinje4 = (Saksbehandler oppgir 1000.daglig hele januar) og (Saksbehandler oppgir 2000.daglig hele mars)
        val beløpstidslinje5 = (Saksbehandler oppgir 1000.daglig hele januar) og (Saksbehandler oppgir 2000.daglig hele mars)
        Assertions.assertEquals(beløpstidslinje1, beløpstidslinje2)
        Assertions.assertNotEquals(beløpstidslinje1, beløpstidslinje3)
        Assertions.assertEquals(beløpstidslinje4, beløpstidslinje5)
    }

    @Test
    fun `subset av beløpstidslinje`() {
        val beløpstidslinje = (Saksbehandler oppgir 1000.daglig kun 20.januar)
        val subset = beløpstidslinje.subset(2.januar til 30.januar)
        Assertions.assertEquals(beløpstidslinje, subset.subset(2.januar til 31.januar))
        Assertions.assertEquals(beløpstidslinje, subset.subset(1.januar til 30.januar))
    }

    @Test
    fun `frem til og med`() {
        val beløpstidslinje = (Saksbehandler oppgir 1000.daglig fra 1.januar til 20.januar)
        val subset = beløpstidslinje.tilOgMed(10.januar)
        Assertions.assertEquals(10, subset.size)
        Assertions.assertEquals(1.januar til 10.januar, subset.perioderMedBeløp.single())
    }

    @Test
    fun `fra og med`() {
        val beløpstidslinje = (Saksbehandler oppgir 1000.daglig fra 1.januar til 20.januar)
        val subset = beløpstidslinje.fraOgMed(10.januar)
        Assertions.assertEquals(11, subset.size)
        Assertions.assertEquals(10.januar til 20.januar, subset.perioderMedBeløp.single())
    }

    @Test
    fun `beløpstidlinje lager en tidslinje med beløp og kilde`() {
        val beløpstidslinje = (Arbeidsgiver oppgir 31000.månedlig fra 1.januar til 10.januar) og (Saksbehandler oppgir 15500.månedlig fra 11.januar til 31.januar)

        Assertions.assertEquals(10, beløpstidslinje.count { it.kilde == Arbeidsgiver })
        Assertions.assertEquals(21, beløpstidslinje.count { it.kilde == Saksbehandler })
    }

    @Test
    fun `Hvis man slår opp på en dag som ikke finnes, da skal man få en ukjent dag`() {
        val beløpstidslinje = Arbeidsgiver oppgir 31000.månedlig hele januar
        assertDoesNotThrow { beløpstidslinje[1.februar] }
        Assertions.assertEquals(UkjentDag, beløpstidslinje[1.februar])
    }

    @Test
    fun `Hvis du absolutt vil ha en tom tidslinje, så skal du få det`() {
        val beløpstidslinje = Beløpstidslinje()
        Assertions.assertEquals(0, beløpstidslinje.count())
    }

    @Test
    fun `Man skal ikke kunne opprette en ny tidslinje med overlappende dager`() {
        assertThrows<IllegalArgumentException> {
            Beløpstidslinje(Beløpsdag(1.januar, 1.daglig, Arbeidsgiver), Beløpsdag(1.januar, 2.daglig, Sykmeldt))
        }
    }

    @Test
    fun `Du haver to stykk beløpstidslinje, som du ønsker forent`() {
        val gammelTidslinje = (Arbeidsgiver oppgir 31000.månedlig hele januar) og (Arbeidsgiver oppgir 0.daglig hele mars)

        val nyTidslinje = (Saksbehandler oppgir 31005.månedlig fra 20.januar til 10.mars)

        val forventetTidslinje =
            (Arbeidsgiver oppgir 31000.månedlig fra 1.januar til 19.januar) og
                (Saksbehandler oppgir 31005.månedlig fra 20.januar til 10.mars) og
                (Arbeidsgiver oppgir 0.daglig fra 11.mars til 31.mars)

        Assertions.assertEquals(forventetTidslinje, gammelTidslinje og nyTidslinje)
    }

    @Test
    fun `Slår sammen to beløptidslinjer med ulike beløp og tidsstempler`() {
        val kilde1 = Kilde(UUID.randomUUID(), Avsender.ARBEIDSGIVER, LocalDateTime.now().minusDays(1))
        val kilde2 = Kilde(UUID.randomUUID(), Avsender.ARBEIDSGIVER, LocalDateTime.now())

        val gammelTidslinje = Beløpstidslinje.fra(januar, 500.daglig, kilde1)
        val nyTidslinje = Beløpstidslinje.fra(januar, 1000.daglig, kilde2)

        Assertions.assertEquals(nyTidslinje, gammelTidslinje + nyTidslinje)
        Assertions.assertEquals(nyTidslinje, nyTidslinje + gammelTidslinje)
    }

    @Test
    fun `Slår sammen to beløptidslinjer med like beløp og ulike tidsstempler`() {
        val kilde1 = Kilde(UUID.randomUUID(), Avsender.ARBEIDSGIVER, LocalDateTime.now().minusDays(1))
        val kilde2 = Kilde(UUID.randomUUID(), Avsender.ARBEIDSGIVER, LocalDateTime.now())

        val beløp = 1000.daglig
        val gammelTidslinje = Beløpstidslinje.fra(januar, beløp, kilde1)
        val nyTidslinje = Beløpstidslinje.fra(januar, beløp, kilde2)

        Assertions.assertEquals(nyTidslinje, gammelTidslinje + nyTidslinje)
        Assertions.assertEquals(nyTidslinje, nyTidslinje + gammelTidslinje)
    }

    @Test
    fun `Slår sammen to beløptidslinjer med like beløp og like tidsstempler`() {
        val tidsstempel = LocalDateTime.now()
        val kilde1 = Kilde(UUID.randomUUID(), Avsender.ARBEIDSGIVER, tidsstempel)
        val kilde2 = Kilde(UUID.randomUUID(), Avsender.ARBEIDSGIVER, tidsstempel)

        val beløp = 1000.daglig
        val gammelTidslinje = Beløpstidslinje.fra(januar, beløp, kilde1)
        val nyTidslinje = Beløpstidslinje.fra(januar, beløp, kilde2)

        Assertions.assertEquals(nyTidslinje, gammelTidslinje + nyTidslinje)
        Assertions.assertEquals(gammelTidslinje, nyTidslinje + gammelTidslinje)
    }

    @Test
    fun `Trekke dager fra på en beløpstidslinje`() {
        val tidslinje = (1.daglig fra 1.januar til 2.januar) og (2.daglig fra 4.januar til 5.januar)
        Assertions.assertEquals(1.daglig, tidslinje[1.januar].beløp)
        Assertions.assertEquals(UkjentDag, tidslinje[3.januar])
        Assertions.assertEquals(2.daglig, tidslinje[4.januar].beløp)

        val forventet = (1.daglig kun 2.januar) og (2.daglig kun 5.januar)

        val fratrukket = tidslinje - 4.januar - setOf(1.januar)

        Assertions.assertEquals(forventet, fratrukket)

        Assertions.assertEquals(UkjentDag, fratrukket[1.januar])
        Assertions.assertEquals(UkjentDag, fratrukket[4.januar])
    }

    @Test
    fun `Strekker en beløpstidslinje i snuten`() {
        Assertions.assertEquals(Beløpstidslinje(), Beløpstidslinje().strekk(januar))
        val tidslinje = Arbeidsgiver oppgir 1000.daglig hele februar
        Assertions.assertEquals(tidslinje, tidslinje.strekk(februar))
        Assertions.assertEquals(tidslinje, tidslinje.strekk(2.februar til 28.februar))
        Assertions.assertEquals(
            Arbeidsgiver oppgir 1000.daglig fra 31.januar til 28.februar,
            tidslinje.strekk(31.januar til 28.februar)
        )
    }

    @Test
    fun `Strekker en beløpstidslinje i halen`() {
        val tidslinje = Arbeidsgiver oppgir 1000.daglig hele februar
        Assertions.assertEquals(tidslinje, tidslinje.strekk(februar))
        Assertions.assertEquals(tidslinje, tidslinje.strekk(1.februar til 27.februar))
        Assertions.assertEquals(
            Arbeidsgiver oppgir 1000.daglig fra 1.februar til 1.mars,
            tidslinje.strekk(1.februar til 1.mars)
        )
    }

    @Test
    fun `Strekker en beløpstidslinje i snuten og halen`() {
        val tidslinje = (Arbeidsgiver oppgir 1000.daglig kun 1.februar) og (Saksbehandler oppgir 2000.daglig kun 28.februar)
        val forventet = (Arbeidsgiver oppgir 1000.daglig fra 31.januar til 1.februar) og (Saksbehandler oppgir 2000.daglig fra 28.februar til 1.mars)
        Assertions.assertEquals(forventet, tidslinje.strekk(31.januar til 1.mars))
        Assertions.assertEquals(UkjentDag, forventet[2.februar])
        Assertions.assertEquals(UkjentDag, forventet[27.februar])

        Assertions.assertEquals(
            Systemet oppgir 100.daglig fra 5.januar til 7.januar,
            (Systemet oppgir 100.daglig kun 6.januar).strekk(5.januar til 7.januar)
        )
    }

    @Test
    fun `Finne første endring i beløp`() {
        Assertions.assertNull(Beløpstidslinje().førsteDagMedUliktBeløp(Beløpstidslinje()))
        Assertions.assertNull((Saksbehandler oppgir 100.daglig hele januar).førsteDagMedUliktBeløp(Arbeidsgiver oppgir 100.daglig hele januar))
        Assertions.assertEquals(
            1.januar,
            (Saksbehandler oppgir 100.daglig hele januar).førsteDagMedUliktBeløp(Arbeidsgiver oppgir 101.daglig hele januar)
        )
        Assertions.assertEquals(
            15.januar,
            (Saksbehandler oppgir 100.daglig hele januar).førsteDagMedUliktBeløp((Arbeidsgiver oppgir 100.daglig fra 1.januar til 14.januar) og (Arbeidsgiver oppgir 101.daglig kun 15.januar))
        )

        Assertions.assertEquals(
            1.januar,
            (Saksbehandler oppgir 100.daglig hele januar).førsteDagMedUliktBeløp(Arbeidsgiver oppgir 100.daglig fra 2.januar til 31.januar)
        )
        Assertions.assertEquals(
            31.januar,
            (Saksbehandler oppgir 100.daglig hele januar).førsteDagMedUliktBeløp(Arbeidsgiver oppgir 100.daglig fra 1.januar til 30.januar)
        )
        val hulleteSaksbehandler = (Saksbehandler oppgir 100.daglig kun 1.januar) og (Saksbehandler oppgir 100.daglig kun 31.januar)
        val hulleteArbeidsgiver = (Saksbehandler oppgir 100.daglig kun 1.januar) og (Saksbehandler oppgir 100.daglig kun 30.januar)
        Assertions.assertEquals(30.januar, hulleteSaksbehandler.førsteDagMedUliktBeløp(hulleteArbeidsgiver))
        Assertions.assertEquals(30.januar, hulleteArbeidsgiver.førsteDagMedUliktBeløp(hulleteSaksbehandler))
    }

    @Test
    fun `beholder kun dager med beløp (annet enn 0 kroner)`() {
        val beløpstidslinje = Beløpstidslinje((1.januar til 31.januar).map {
            val beløp = if (it.dayOfMonth % 2 == 0) it.dayOfMonth.daglig else Inntekt.INGEN
            Beløpsdag(it, beløp, UUID.randomUUID().arbeidsgiver)
        })
        Assertions.assertEquals(31, beløpstidslinje.size)
        Assertions.assertEquals(listOf(1.januar til 31.januar), beløpstidslinje.perioderMedBeløp)

        val medBeløp = beløpstidslinje.medBeløp()
        Assertions.assertEquals(15, medBeløp.size)
        Assertions.assertEquals(
            listOf(
                2,
                4,
                6,
                8,
                10,
                12,
                14,
                16,
                18,
                20,
                22,
                24,
                26,
                28,
                30
            ).map { it.januar.somPeriode() }, medBeløp.perioderMedBeløp
        )

        val gjenopprettet = Beløpstidslinje.gjenopprett(medBeløp.dto())
        Assertions.assertEquals(medBeløp, gjenopprettet)
    }

    @Test
    fun dto() {
        val tidslinje = (Arbeidsgiver oppgir 500.daglig kun 1.februar) og
            (Arbeidsgiver oppgir 250.daglig fra 2.februar til 10.februar) og
            (Arbeidsgiver oppgir 500.daglig fra 11.februar til 12.februar)

        val kilde = BeløpstidslinjeDto.BeløpstidslinjedagKildeDto(Arbeidsgiver.meldingsreferanseId, Arbeidsgiver.avsender.dto(), Arbeidsgiver.tidsstempel)
        Assertions.assertEquals(
            BeløpstidslinjeDto(
                perioder = listOf(
                    BeløpstidslinjeDto.BeløpstidslinjeperiodeDto(
                        fom = 1.februar,
                        tom = 1.februar,
                        dagligBeløp = 500.0,
                        kilde = kilde
                    ),
                    BeløpstidslinjeDto.BeløpstidslinjeperiodeDto(
                        fom = 2.februar,
                        tom = 10.februar,
                        dagligBeløp = 250.0,
                        kilde = kilde
                    ),
                    BeløpstidslinjeDto.BeløpstidslinjeperiodeDto(
                        fom = 11.februar,
                        tom = 12.februar,
                        dagligBeløp = 500.0,
                        kilde = kilde
                    )
                )
            ), tidslinje.dto()
        )


    }
}
