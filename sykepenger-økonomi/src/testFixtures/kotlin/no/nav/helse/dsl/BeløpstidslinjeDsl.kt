package no.nav.helse.dsl

import no.nav.helse.hendelser.Avsender
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.person.beløp.Beløpsdag
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.beløp.Kilde
import no.nav.helse.økonomi.Inntekt
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioder
import org.junit.jupiter.api.Assertions

object BeløpstidslinjeDsl {
    val ArbeidsgiverId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")
    val SaksbehandlerId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
    val SykmeldtId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000002")
    val SystemId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000003")
    val Arbeidsgiver = Kilde(ArbeidsgiverId, Avsender.ARBEIDSGIVER, LocalDateTime.now())
    val Saksbehandler = Kilde(SaksbehandlerId, Avsender.SAKSBEHANDLER, LocalDateTime.now())
    val Sykmeldt = Kilde(SykmeldtId, Avsender.SYKMELDT, LocalDateTime.now())
    val Systemet = Kilde(SystemId, Avsender.SYSTEM, LocalDateTime.now())

    val UUID.arbeidsgiver get() = Kilde(this, Avsender.ARBEIDSGIVER, LocalDateTime.now())
    val UUID.saksbehandler get() = Kilde(this, Avsender.SAKSBEHANDLER, LocalDateTime.now())
    fun Avsender.beløpstidslinje(periode: Periode, beløp: Inntekt) = Beløpstidslinje.fra(periode, beløp, Kilde(UUID.randomUUID(), this, LocalDateTime.now()))
    val Beløpstidslinje.perioderMedBeløp get() = filterIsInstance<Beløpsdag>().map { it.dato }.grupperSammenhengendePerioder()

    infix fun Inntekt.fra(fra: LocalDate) = Triple(Systemet, this, fra)
    infix fun Inntekt.kun(kun: LocalDate) = fra(kun) til kun

    infix fun Kilde.oppgir(inntekt: Inntekt) = this to inntekt
    infix fun Pair<Kilde, Inntekt>.fra(fra: LocalDate) = Triple(first, second, fra)
    infix fun Pair<Kilde, Inntekt>.kun(kun: LocalDate) = fra(kun) til kun
    infix fun Pair<Kilde, Inntekt>.hele(periode: Periode) = fra(periode.start) til periode.endInclusive
    infix fun Triple<Kilde, Inntekt, LocalDate>.til(til: LocalDate) =
        Beløpstidslinje((third til til).map { Beløpsdag(it, second, first) })
    infix fun Beløpstidslinje.og(other: Beløpstidslinje) = this + other

    fun assertBeløpstidslinje(actual: Beløpstidslinje, periode: Periode, beløp: Inntekt, meldingsreferanseId: UUID? = null) {
        val ignoreMeldingsreferanseId = meldingsreferanseId == null
        val kilde = Kilde(meldingsreferanseId ?: UUID.randomUUID(), Avsender.SYSTEM, LocalDate.EPOCH.atStartOfDay())
        val expected = Beløpstidslinje.fra(periode, beløp, kilde)
        assertBeløpstidslinje(expected, actual, ignoreMeldingsreferanseId = ignoreMeldingsreferanseId, ignoreAvsender = true)
    }

    fun assertBeløpstidslinje(expected: Beløpstidslinje, actual: Beløpstidslinje, ignoreMeldingsreferanseId: Boolean = false, ignoreAvsender: Boolean = false) {
        val tøyseteMeldingsreferanseId = UUID.randomUUID()
        val meldingsreferanseId: (ekte: UUID) -> UUID = if (ignoreMeldingsreferanseId) { _ -> tøyseteMeldingsreferanseId } else { ekte -> ekte }
        val avsender: (ekte: Avsender) -> Avsender = if (ignoreAvsender) { _ -> Avsender.SYSTEM } else { ekte -> ekte }
        Assertions.assertEquals(
            expected.besudlet(meldingsreferanseId, avsender),
            actual.besudlet(meldingsreferanseId, avsender)
        )
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
                    meldingsreferanseId = meldingsreferanseId(it.kilde.meldingsreferanseId)
                )
            )
        }
        return Beløpstidslinje(beløpsdager)
    }
}
