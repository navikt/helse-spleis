package no.nav.helse.person.beløp

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.Tidslinjedag
import no.nav.helse.april
import no.nav.helse.dsl.BeløpstidslinjeDsl.Arbeidsgiver
import no.nav.helse.dsl.BeløpstidslinjeDsl.Saksbehandler
import no.nav.helse.dsl.BeløpstidslinjeDsl.Sykmeldt
import no.nav.helse.dsl.BeløpstidslinjeDsl.Systemet
import no.nav.helse.dsl.BeløpstidslinjeDsl.fra
import no.nav.helse.dsl.BeløpstidslinjeDsl.hele
import no.nav.helse.dsl.BeløpstidslinjeDsl.kun
import no.nav.helse.dsl.BeløpstidslinjeDsl.og
import no.nav.helse.dsl.BeløpstidslinjeDsl.oppgir
import no.nav.helse.dsl.BeløpstidslinjeDsl.til
import no.nav.helse.dto.BeløpstidslinjeDto
import no.nav.helse.februar
import no.nav.helse.hendelser.Avsender
import no.nav.helse.hendelser.Avsender.ARBEIDSGIVER
import no.nav.helse.hendelser.Avsender.SAKSBEHANDLER
import no.nav.helse.hendelser.Avsender.SYSTEM
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioder
import no.nav.helse.hendelser.somPeriode
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mapWithNext
import no.nav.helse.mars
import no.nav.helse.person.refusjon.Refusjonsdag
import no.nav.helse.person.refusjon.Refusjonstidslinje
import no.nav.helse.person.refusjon.Refusjonstidslinje.Companion.somArray
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertNull
import org.junit.jupiter.api.assertThrows
import org.opentest4j.AssertionFailedError

internal class BeløpstidslinjeTest {

    @Test
    fun `trekke fra en beløpstidslinje`() {
        val (fraInntektsmelding, fraInntektsmeldingRefusjonstidslinje) =
            (Arbeidsgiver oppgir 1000.daglig fra 2.januar til 30.januar).fremOgTilbakeViaRefusjonstidslinje()

        val (overstyring, overstyringRefusjonstidslinje) =
            ((Saksbehandler oppgir 1000.daglig fra 1.januar til 20.januar) og (Saksbehandler oppgir 1500.daglig fra 21.januar til 31.januar)).fremOgTilbakeViaRefusjonstidslinje()

        val (forventetDiff, forventetDiffRefusjonstidslinje) =
            ((Saksbehandler oppgir 1000.daglig kun 1.januar) og (Saksbehandler oppgir 1500.daglig fra 21.januar til 31.januar)).fremOgTilbakeViaRefusjonstidslinje()

        assertEquals(forventetDiff, overstyring - fraInntektsmelding)
        assertRefusjonstidslinje(forventetDiffRefusjonstidslinje, overstyringRefusjonstidslinje - fraInntektsmeldingRefusjonstidslinje)
    }

    @Test
    fun `fylle en hullete beløpstidslinje`() {
        val (beløpstidslinje1, refusjonstidslinje1) = (Arbeidsgiver oppgir 1000.daglig hele januar).fremOgTilbakeViaRefusjonstidslinje()
        val (beløpstidslinje2, refusjonstidslinje2) = (Saksbehandler oppgir 2000.daglig hele mars).fremOgTilbakeViaRefusjonstidslinje()
        val (beløpstidslinje3, refusjonstidslinje3) = (Systemet oppgir 3000.daglig fra 2.april til 30.april).fremOgTilbakeViaRefusjonstidslinje()

        val sammenslått = beløpstidslinje1 + beløpstidslinje2 + beløpstidslinje3
        val sammenslåttRefusjonstidslinje = refusjonstidslinje1 + refusjonstidslinje2 + refusjonstidslinje3

        assertEquals(listOf(januar, mars, 2.april til 30.april), sammenslått.perioderMedBeløp)
        assertEquals(listOf(januar, mars, 2.april til 30.april), sammenslåttRefusjonstidslinje.perioderMedBeløp)

        val fylt = sammenslått.fyll()
        val fyltRefusjonstdslinje = sammenslåttRefusjonstidslinje.fyll()
        assertEquals(listOf(1.januar til 30.april), fylt.perioderMedBeløp)
        assertEquals(listOf(1.januar til 30.april), fyltRefusjonstdslinje.perioderMedBeløp)

        val (forventet, forventetRefusjonstidslinje) = ((Arbeidsgiver oppgir 1000.daglig fra 1.januar til 28.februar) og (Saksbehandler oppgir 2000.daglig fra 1.mars til 1.april) + (Systemet oppgir 3000.daglig fra 2.april til 30.april)).fremOgTilbakeViaRefusjonstidslinje()
        assertEquals(forventet, fylt)
        assertRefusjonstidslinje(forventetRefusjonstidslinje, fyltRefusjonstdslinje)
    }

    @Test
    fun equals() {
        val (beløpstidslinje1, refusjonstidslinje1) = (Arbeidsgiver oppgir 1000.daglig hele januar).fremOgTilbakeViaRefusjonstidslinje()
        val (beløpstidslinje2, refusjonstidslinje2) = (Arbeidsgiver oppgir 1000.daglig hele januar).fremOgTilbakeViaRefusjonstidslinje()
        val (beløpstidslinje3, refusjonstidslinje3) = (Saksbehandler oppgir 1000.daglig hele januar).fremOgTilbakeViaRefusjonstidslinje()
        val (beløpstidslinje4, refusjonstidslinje4) = ((Saksbehandler oppgir 1000.daglig hele januar) og (Saksbehandler oppgir 2000.daglig hele mars)).fremOgTilbakeViaRefusjonstidslinje()
        val (beløpstidslinje5, refusjonstidslinje5) = ((Saksbehandler oppgir 1000.daglig hele januar) og (Saksbehandler oppgir 2000.daglig hele mars)).fremOgTilbakeViaRefusjonstidslinje()

        assertEquals(beløpstidslinje1, beløpstidslinje2)
        assertRefusjonstidslinje(refusjonstidslinje1, refusjonstidslinje2)

        assertNotEquals(beløpstidslinje1, beløpstidslinje3)
        assertNotEquals(refusjonstidslinje1, refusjonstidslinje3)

        assertEquals(beløpstidslinje4, beløpstidslinje5)
        assertRefusjonstidslinje(refusjonstidslinje4, refusjonstidslinje5)
    }

    @Test
    fun `subset av beløpstidslinje`() {
        val (beløpstidslinje, refusjonstidslinje) = (Saksbehandler oppgir 1000.daglig kun 20.januar).fremOgTilbakeViaRefusjonstidslinje()
        val subset = beløpstidslinje.subset(2.januar til 30.januar)
        val subsetRefusjonstidslinje = refusjonstidslinje.subset(2.januar til 30.januar)

        assertEquals(beløpstidslinje, subset.subset(2.januar til 31.januar))
        assertRefusjonstidslinje(refusjonstidslinje, subsetRefusjonstidslinje.subset(2.januar til 31.januar))

        assertEquals(beløpstidslinje, subset.subset(1.januar til 30.januar))
        assertRefusjonstidslinje(refusjonstidslinje, subsetRefusjonstidslinje.subset(1.januar til 30.januar))
    }

    @Test
    fun `frem til og med`() {
        val (beløpstidslinje, refusjonstidslinje) = (Saksbehandler oppgir 1000.daglig fra 1.januar til 20.januar).fremOgTilbakeViaRefusjonstidslinje()
        val subset = beløpstidslinje.tilOgMed(10.januar)
        val subsetRefusjonstidslinje = refusjonstidslinje.tilOgMed(10.januar)

        assertEquals(10, subset.size)
        assertEquals(10, subsetRefusjonstidslinje.size)

        assertEquals(1.januar til 10.januar, subset.perioderMedBeløp.single())
        assertEquals(1.januar til 10.januar, subsetRefusjonstidslinje.perioderMedBeløp.single())
    }

    @Test
    fun `fra og med`() {
        val (beløpstidslinje, refusjonstidslinje) = (Saksbehandler oppgir 1000.daglig fra 1.januar til 20.januar).fremOgTilbakeViaRefusjonstidslinje()
        val subset = beløpstidslinje.fraOgMed(10.januar)
        val subsetRefusjonstidslinje = refusjonstidslinje.fraOgMed(10.januar)

        assertEquals(11, subset.size)
        assertEquals(11, subsetRefusjonstidslinje.size)

        assertEquals(10.januar til 20.januar, subset.perioderMedBeløp.single())
        assertEquals(10.januar til 20.januar, subsetRefusjonstidslinje.perioderMedBeløp.single())
    }

    @Test
    fun `beløpstidlinje lager en tidslinje med beløp og kilde`() {
        val (beløpstidslinje, refusjonstidslinje) = ((Arbeidsgiver oppgir 31000.månedlig fra 1.januar til 10.januar) og (Saksbehandler oppgir 15500.månedlig fra 11.januar til 31.januar)).fremOgTilbakeViaRefusjonstidslinje()

        assertEquals(10, beløpstidslinje.count { it.kilde == Arbeidsgiver })
        assertEquals(21, beløpstidslinje.count { it.kilde == Saksbehandler })
        assertEquals(10, refusjonstidslinje.count { it.verdi?.kilde == Arbeidsgiver })
        assertEquals(21, refusjonstidslinje.count { it.verdi?.kilde == Saksbehandler })
    }

    @Test
    fun `Hvis man slår opp på en dag som ikke finnes, da skal man få en ukjent dag`() {
        val (beløpstidslinje, refusjonstidsloinje) = (Arbeidsgiver oppgir 31000.månedlig hele januar).fremOgTilbakeViaRefusjonstidslinje()
        assertDoesNotThrow { beløpstidslinje[1.februar] }
        assertDoesNotThrow { refusjonstidsloinje[1.februar] }

        assertEquals(UkjentDag, beløpstidslinje[1.februar])
        assertNull(refusjonstidsloinje[1.februar])
    }

    @Test
    fun `Hvis du absolutt vil ha en tom tidslinje, så skal du få det`() {
        val (beløpstidslinje, refusjonstidslinje) = Beløpstidslinje().fremOgTilbakeViaRefusjonstidslinje()
        assertEquals(0, beløpstidslinje.count())
        assertEquals(0, refusjonstidslinje.count())
    }

    @Test
    fun `Man skal ikke kunne opprette en ny tidslinje med overlappende dager`() {
        assertThrows<IllegalArgumentException> {
            Beløpstidslinje(Beløpsdag(1.januar, 1.daglig, Arbeidsgiver), Beløpsdag(1.januar, 2.daglig, Sykmeldt))
        }
        assertThrows<IllegalArgumentException> {
            Refusjonstidslinje(1.januar.somPeriode() to Refusjonsdag(1.daglig, Arbeidsgiver), 1.januar.somPeriode() to Refusjonsdag(2.daglig, Sykmeldt))
        }
    }

    @Test
    fun `Du haver to stykk beløpstidslinje, som du ønsker forent`() {
        val (gammelTidslinje, gammelRefusjonstidslinje) = ((Arbeidsgiver oppgir 31000.månedlig hele januar) og (Arbeidsgiver oppgir 0.daglig hele mars)).fremOgTilbakeViaRefusjonstidslinje()

        val (nyTidslinje, nyRefusjonstidslinje) = ((Saksbehandler oppgir 31005.månedlig fra 20.januar til 10.mars)).fremOgTilbakeViaRefusjonstidslinje()

        val (forventetTidslinje, forventetRefusjonstidslinje) =
            ((Arbeidsgiver oppgir 31000.månedlig fra 1.januar til 19.januar) og
            (Saksbehandler oppgir 31005.månedlig fra 20.januar til 10.mars) og
            (Arbeidsgiver oppgir 0.daglig fra 11.mars til 31.mars)).fremOgTilbakeViaRefusjonstidslinje()

        assertEquals(forventetTidslinje, gammelTidslinje og nyTidslinje)
        assertRefusjonstidslinje(forventetRefusjonstidslinje, gammelRefusjonstidslinje + nyRefusjonstidslinje)
    }

    @Test
    fun `Slår sammen to beløptidslinjer med ulike beløp og tidsstempler`() {
        val kilde1 = Kilde(MeldingsreferanseId(UUID.randomUUID()), ARBEIDSGIVER, LocalDateTime.now().minusDays(1))
        val kilde2 = Kilde(MeldingsreferanseId(UUID.randomUUID()), ARBEIDSGIVER, LocalDateTime.now())

        val (gammelTidslinje, gammelRefusjonstidslinje) = Beløpstidslinje.fra(januar, 500.daglig, kilde1).fremOgTilbakeViaRefusjonstidslinje()
        val (nyTidslinje, nyRefusjonstidslinje) = Beløpstidslinje.fra(januar, 1000.daglig, kilde2).fremOgTilbakeViaRefusjonstidslinje()

        assertEquals(nyTidslinje, gammelTidslinje + nyTidslinje)
        assertRefusjonstidslinje(nyRefusjonstidslinje, gammelRefusjonstidslinje + nyRefusjonstidslinje)
        assertEquals(nyTidslinje, nyTidslinje + gammelTidslinje)
        assertRefusjonstidslinje(nyRefusjonstidslinje, nyRefusjonstidslinje + gammelRefusjonstidslinje)
    }

    @Test
    fun `Slår sammen to beløptidslinjer med like beløp og ulike tidsstempler`() {
        val kilde1 = Kilde(MeldingsreferanseId(UUID.randomUUID()), ARBEIDSGIVER, LocalDateTime.now().minusDays(1))
        val kilde2 = Kilde(MeldingsreferanseId(UUID.randomUUID()), ARBEIDSGIVER, LocalDateTime.now())

        val beløp = 1000.daglig
        val (gammelTidslinje, gammelRefusjonstidslinje) = Beløpstidslinje.fra(januar, beløp, kilde1).fremOgTilbakeViaRefusjonstidslinje()
        val (nyTidslinje, nyRefusjonstidslinje) = Beløpstidslinje.fra(januar, beløp, kilde2).fremOgTilbakeViaRefusjonstidslinje()

        assertEquals(nyTidslinje, gammelTidslinje + nyTidslinje)
        assertRefusjonstidslinje(nyRefusjonstidslinje, gammelRefusjonstidslinje + nyRefusjonstidslinje)
        assertEquals(nyTidslinje, nyTidslinje + gammelTidslinje)
        assertRefusjonstidslinje(nyRefusjonstidslinje, nyRefusjonstidslinje + gammelRefusjonstidslinje)
    }

    @Test
    fun `Slår sammen to beløptidslinjer med like beløp og like tidsstempler`() {
        val tidsstempel = LocalDateTime.now()
        val kilde1 = Kilde(MeldingsreferanseId(UUID.randomUUID()), ARBEIDSGIVER, tidsstempel)
        val kilde2 = Kilde(MeldingsreferanseId(UUID.randomUUID()), ARBEIDSGIVER, tidsstempel)

        val beløp = 1000.daglig
        val (gammelTidslinje, gammelRefusjonstidslinje) = Beløpstidslinje.fra(januar, beløp, kilde1).fremOgTilbakeViaRefusjonstidslinje()
        val (nyTidslinje, nyRefusjonstidslinje) = Beløpstidslinje.fra(januar, beløp, kilde2).fremOgTilbakeViaRefusjonstidslinje()

        assertEquals(nyTidslinje, gammelTidslinje + nyTidslinje)
        assertEquals(gammelTidslinje, nyTidslinje + gammelTidslinje)

        assertRefusjonstidslinje(nyRefusjonstidslinje, gammelRefusjonstidslinje + nyRefusjonstidslinje)
        assertRefusjonstidslinje(gammelRefusjonstidslinje, nyRefusjonstidslinje + gammelRefusjonstidslinje)
    }

    @Test
    fun `Strekker en beløpstidslinje i snuten`() {
        assertEquals(Beløpstidslinje(), Beløpstidslinje().strekk(januar))
        assertRefusjonstidslinje(Refusjonstidslinje(), Refusjonstidslinje().strekk(januar))

        val (beløpstidslinje, refusjonstidslinje) = (Arbeidsgiver oppgir 1000.daglig hele februar).fremOgTilbakeViaRefusjonstidslinje()
        assertEquals(beløpstidslinje, beløpstidslinje.strekk(februar))
        assertRefusjonstidslinje(refusjonstidslinje, refusjonstidslinje.strekk(februar))

        assertEquals(beløpstidslinje, beløpstidslinje.strekk(2.februar til 28.februar))
        assertRefusjonstidslinje(refusjonstidslinje, refusjonstidslinje.strekk(2.februar til 28.februar))

        val (forventetBeløpstidslinje, forventetRefusjonstidslinje) = (Arbeidsgiver oppgir 1000.daglig fra 31.januar til 28.februar).fremOgTilbakeViaRefusjonstidslinje()

        assertEquals(forventetBeløpstidslinje, beløpstidslinje.strekk(31.januar til 28.februar))
        assertRefusjonstidslinje(forventetRefusjonstidslinje, forventetRefusjonstidslinje.strekk(31.januar til 28.februar))
    }

    @Test
    fun `Strekker en beløpstidslinje i halen`() {
        val (tidslinje, refusjonstidslinje) = (Arbeidsgiver oppgir 1000.daglig hele februar).fremOgTilbakeViaRefusjonstidslinje()
        assertEquals(tidslinje, tidslinje.strekk(februar))
        assertRefusjonstidslinje(refusjonstidslinje, refusjonstidslinje.strekk(februar))

        assertEquals(tidslinje, tidslinje.strekk(1.februar til 27.februar))
        assertRefusjonstidslinje(refusjonstidslinje, refusjonstidslinje.strekk(1.februar til 27.februar))

        val (forventetBeløpstidslinje, forventetRefusjonstidslinje) = (Arbeidsgiver oppgir 1000.daglig fra 1.februar til 1.mars).fremOgTilbakeViaRefusjonstidslinje()

        assertEquals(forventetBeløpstidslinje, tidslinje.strekk(1.februar til 1.mars))
        assertRefusjonstidslinje(forventetRefusjonstidslinje, refusjonstidslinje.strekk(1.februar til 1.mars))
    }

    @Test
    fun `Strekker en beløpstidslinje i snuten og halen`() {
        val (tidslinje, refusjonstidslinje) = ((Arbeidsgiver oppgir 1000.daglig kun 1.februar) og (Saksbehandler oppgir 2000.daglig kun 28.februar)).fremOgTilbakeViaRefusjonstidslinje()
        val (forventet, forventetRefusjonstidslinje) = ((Arbeidsgiver oppgir 1000.daglig fra 31.januar til 1.februar) og (Saksbehandler oppgir 2000.daglig fra 28.februar til 1.mars)).fremOgTilbakeViaRefusjonstidslinje()

        assertEquals(forventet, tidslinje.strekk(31.januar til 1.mars))
        assertRefusjonstidslinje(forventetRefusjonstidslinje, refusjonstidslinje.strekk(31.januar til 1.mars))

        assertEquals(UkjentDag, forventet[2.februar])
        assertNull(forventetRefusjonstidslinje[2.februar])
        assertEquals(UkjentDag, forventet[27.februar])
        assertNull(forventetRefusjonstidslinje[27.februar])

        val (forventet2, forventetRefusjonstidslinje2) = (Systemet oppgir 100.daglig fra 5.januar til 7.januar).fremOgTilbakeViaRefusjonstidslinje()
        val (faktisk, faktiskRefusjonstidslinje) = (Systemet oppgir 100.daglig kun 6.januar).fremOgTilbakeViaRefusjonstidslinje()

        assertEquals(forventet2, faktisk.strekk(5.januar til 7.januar))
        assertRefusjonstidslinje(forventetRefusjonstidslinje2, faktiskRefusjonstidslinje.strekk(5.januar til 7.januar))
    }

    @Test
    fun `beholder kun dager med beløp (annet enn 0 kroner)`() {
        val (beløpstidslinje, refusjonstidslinje) = Beløpstidslinje((1.januar til 31.januar).map {
            val beløp = if (it.dayOfMonth % 2 == 0) it.dayOfMonth.daglig else INGEN
            Beløpsdag(it, beløp, UUID.randomUUID().arbeidsgiver)
        }).fremOgTilbakeViaRefusjonstidslinje()

        assertEquals(31, beløpstidslinje.size)
        assertEquals(31, refusjonstidslinje.size)
        assertEquals(listOf(1.januar til 31.januar), beløpstidslinje.perioderMedBeløp)
        assertEquals(listOf(1.januar til 31.januar), refusjonstidslinje.perioderMedBeløp)

        val medBeløp = beløpstidslinje.medBeløp()
        assertEquals(15, medBeløp.size)
        assertEquals(listOf(2,4,6,8,10,12,14,16,18,20,22,24,26,28,30).map { it.januar.somPeriode() }, medBeløp.perioderMedBeløp)

        val medBeløpRefusjonstidslinje = refusjonstidslinje.medBeløp()
        assertEquals(15, medBeløpRefusjonstidslinje.size)
        assertEquals(listOf(2,4,6,8,10,12,14,16,18,20,22,24,26,28,30).map { it.januar.somPeriode() }, medBeløpRefusjonstidslinje.perioderMedBeløp)

        val gjenopprettet = Beløpstidslinje.gjenopprett(medBeløp.dto()) // TODO: dto/gjenopprett
        assertEquals(medBeløp, gjenopprettet)
    }

    @Test
    fun dto() {
        // TODO: dto/gjenopprett
        val tidslinje = (Arbeidsgiver oppgir 500.daglig kun 1.februar) og
            (Arbeidsgiver oppgir 250.daglig fra 2.februar til 10.februar) og
            (Arbeidsgiver oppgir 500.daglig fra 11.februar til 12.februar)

        val kilde = BeløpstidslinjeDto.BeløpstidslinjedagKildeDto(Arbeidsgiver.meldingsreferanseId.dto(), Arbeidsgiver.avsender.dto(), Arbeidsgiver.tidsstempel)
        assertEquals(
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

    internal companion object {
        internal val Beløpstidslinje.perioderMedBeløp get() = filterIsInstance<Beløpsdag>().map { it.dato }.grupperSammenhengendePerioder()
        internal val Refusjonstidslinje.perioderMedBeløp get() = gruppér().keys.toList().grupperSammenhengendePerioder()
        internal val UUID.arbeidsgiver get() = Kilde(MeldingsreferanseId(this), ARBEIDSGIVER, LocalDateTime.now())
        internal val UUID.saksbehandler get() = Kilde(MeldingsreferanseId(this), SAKSBEHANDLER, LocalDateTime.now())
        internal fun Avsender.beløpstidslinje(periode: Periode, beløp: Inntekt) = Beløpstidslinje.fra(periode, beløp, Kilde(MeldingsreferanseId(UUID.randomUUID()), this, LocalDateTime.now()))

        internal fun assertBeløpstidslinje(actual: Beløpstidslinje, periode: Periode, beløp: Inntekt, meldingsreferanseId: UUID? = null) {
            val ignoreMeldingsreferanseId = meldingsreferanseId == null
            val kilde = Kilde(MeldingsreferanseId(meldingsreferanseId ?: UUID.randomUUID()), SYSTEM, LocalDate.EPOCH.atStartOfDay())
            val expected = Beløpstidslinje.fra(periode, beløp, kilde)
            assertBeløpstidslinje(expected, actual, ignoreMeldingsreferanseId = ignoreMeldingsreferanseId, ignoreAvsender = true)
        }

        internal fun assertBeløpstidslinje(expected: Beløpstidslinje, actual: Beløpstidslinje, ignoreMeldingsreferanseId: Boolean = false, ignoreAvsender: Boolean = false) {
            val tøyseteMeldingsreferanseId = UUID.randomUUID()
            val meldingsreferanseId: (ekte: UUID) -> UUID = if (ignoreMeldingsreferanseId) { _ -> tøyseteMeldingsreferanseId } else { ekte -> ekte }
            val avsender: (ekte: Avsender) -> Avsender = if (ignoreAvsender) { _ -> SYSTEM } else { ekte -> ekte }
            assertEquals(expected.besudlet(meldingsreferanseId, avsender), actual.besudlet(meldingsreferanseId, avsender))
        }

        private fun Beløpstidslinje.besudlet(
            meldingsreferanseId: (ekte: UUID) -> UUID,
            avsender: (ekte: Avsender) -> Avsender
        ): Beløpstidslinje {
            val beløpsdager = filterIsInstance<Beløpsdag>().map {
                it.copy(
                    kilde = it.kilde.copy(
                        avsender = avsender(it.kilde.avsender),
                        tidsstempel = LocalDate.EPOCH.atStartOfDay(),
                        meldingsreferanseId = MeldingsreferanseId(meldingsreferanseId(it.kilde.meldingsreferanseId.id))
                    )
                )
            }
            return Beløpstidslinje(beløpsdager)
        }

        private fun fraBeløpstidslinje(beløpstidslinje: Beløpstidslinje): Refusjonstidslinje {
            val tidslinjedager = beløpstidslinje.filterIsInstance<Beløpsdag>().map { beløpsdag ->
                Tidslinjedag(beløpsdag.dato, Refusjonsdag(beløpsdag.beløp, beløpsdag.kilde))
            }.somArray
            return Refusjonstidslinje(*tidslinjedager)
        }

        private fun tilBeløpstidslinje(refusjonstidslinje: Refusjonstidslinje): Beløpstidslinje {
            val beløpsdager = refusjonstidslinje.mapNotNull { refusjonsdag -> when (refusjonsdag.verdi) {
                null -> null
                else -> Beløpsdag(refusjonsdag.dato, refusjonsdag.verdi.beløp, refusjonsdag.verdi.kilde)
            }}.toTypedArray()
            return Beløpstidslinje(*beløpsdager)
        }

        private fun Beløpstidslinje.fremOgTilbakeViaRefusjonstidslinje(): Pair<Beløpstidslinje, Refusjonstidslinje> {
            val refusjonstidslinje = fraBeløpstidslinje(this)
            val beløpstidslinje = tilBeløpstidslinje(refusjonstidslinje)
            assertEquals(this, beløpstidslinje)
            return beløpstidslinje to refusjonstidslinje
        }

        private fun Refusjonstidslinje.asciitabell(): String {
            val gruppert = gruppér().takeUnless { it.isEmpty() } ?: return "Tom refusjonstidslinje"
            return with(StringBuilder()) {
                appendLine("|------------------------------------|")
                appendLine("| Periode                    | Beløp |")
                gruppert.entries.mapWithNext { denne, neste ->
                    appendLine("| ${denne.key}  | ${denne.value.beløp.dagligInt.toString().padStart(5, ' ')} |")
                    if (neste == null) return@mapWithNext
                    Periode.mellom(denne.key, neste.key)?.let { gap ->
                        appendLine("| ${gap}  |  n/a  |")
                    }
                }
                appendLine("|------------------------------------|")
                toString()
            }
        }

        private fun assertRefusjonstidslinje(expected: Refusjonstidslinje, actual: Refusjonstidslinje) {
            try {
                assertEquals(expected, actual)
            } catch (error: AssertionFailedError) {
                println("Forventet:\n${expected.asciitabell()}")
                println("Faktisk:\n${actual.asciitabell()}")
                throw error
            }
        }
    }
}
