package no.nav.helse.dsl

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Avsender.ARBEIDSGIVER
import no.nav.helse.hendelser.Avsender.SAKSBEHANDLER
import no.nav.helse.hendelser.Avsender.SYKMELDT
import no.nav.helse.hendelser.Avsender.SYSTEM
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.person.beløp.Beløpsdag
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.beløp.Kilde
import no.nav.helse.økonomi.Inntekt

object BeløpstidslinjeDsl {
    val ArbeidsgiverId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")
    val SaksbehandlerId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
    val SykmeldtId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000002")
    val SystemId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000003")
    val Arbeidsgiver = Kilde(MeldingsreferanseId(ArbeidsgiverId), ARBEIDSGIVER, LocalDateTime.now())
    val Saksbehandler = Kilde(MeldingsreferanseId(SaksbehandlerId), SAKSBEHANDLER, LocalDateTime.now())
    val Sykmeldt = Kilde(MeldingsreferanseId(SykmeldtId), SYKMELDT, LocalDateTime.now())
    val Systemet = Kilde(MeldingsreferanseId(SystemId), SYSTEM, LocalDateTime.now())

    infix fun Inntekt.fra(fra: LocalDate) = Triple(Systemet, this, fra)
    infix fun Inntekt.kun(kun: LocalDate) = fra(kun) til kun

    infix fun Kilde.oppgir(inntekt: Inntekt) = this to inntekt
    infix fun Pair<Kilde, Inntekt>.fra(fra: LocalDate) = Triple(first, second, fra)
    infix fun Pair<Kilde, Inntekt>.kun(kun: LocalDate) = fra(kun) til kun
    infix fun Pair<Kilde, Inntekt>.hele(periode: Periode) = fra(periode.start) til periode.endInclusive
    infix fun Triple<Kilde, Inntekt, LocalDate>.til(til: LocalDate) = Beløpstidslinje((third til til).map { Beløpsdag(it, second, first) })
    infix fun Beløpstidslinje.og(other: Beløpstidslinje) = this + other
}
