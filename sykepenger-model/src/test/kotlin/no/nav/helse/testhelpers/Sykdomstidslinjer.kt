package no.nav.helse.testhelpers

import no.nav.helse.hendelser.Periode
import no.nav.helse.januar
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Dag.*
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import no.nav.helse.økonomi.Økonomi
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import java.util.stream.Collectors
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.sykdomstidslinje.Melding

private var threadLocalDagensDato = ThreadLocal.withInitial { 1.januar }
private var dagensDato: LocalDate
    get() = threadLocalDagensDato.get()
    set(value) = threadLocalDagensDato.set(value)

internal fun resetSeed(frøDato: LocalDate = 1.januar) {
    dagensDato = frøDato
}

internal fun resetSeed(frøDato: LocalDate = 1.januar, tidslinjegenerator: () -> Sykdomstidslinje): Sykdomstidslinje {
    resetSeed(frøDato)
    return tidslinjegenerator()
}

internal val Int.S
    get() = Sykdomstidslinje.sykedager(
        dagensDato,
        dagensDato.plusDays(this.toLong() - 1),
        100.prosent,
        SykdomstidslinjeHendelse.Hendelseskilde.INGEN
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal fun Int.S(melding: Melding) = Sykdomstidslinje.sykedager(
    dagensDato,
    dagensDato.plusDays(this.toLong() - 1),
    100.prosent,
    SykdomstidslinjeHendelse.Hendelseskilde(melding, UUID.randomUUID(), LocalDateTime.now())
).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.U
    get() = Sykdomstidslinje.arbeidsgiverdager(
        dagensDato,
        dagensDato.plusDays(this.toLong() - 1),
        100.prosent,
        SykdomstidslinjeHendelse.Hendelseskilde.INGEN
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.K
    get() = Sykdomstidslinje.sykedager(
        dagensDato,
        dagensDato.plusDays(this.toLong() - 1),
        dagensDato.plusMonths(4),
        100.prosent,
        SykdomstidslinjeHendelse.Hendelseskilde.INGEN
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.A
    get() = Sykdomstidslinje.arbeidsdager(
        dagensDato,
        dagensDato.plusDays(this.toLong() - 1),
        SykdomstidslinjeHendelse.Hendelseskilde.INGEN
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.opphold
    get() = Sykdomstidslinje(
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.F
    get() = Sykdomstidslinje.feriedager(
        dagensDato,
        dagensDato.plusDays(this.toLong() - 1),
        SykdomstidslinjeHendelse.Hendelseskilde.INGEN
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.H
    get() = Sykdomstidslinje(
        dagensDato.datesUntil(dagensDato.plusDays(this.toLong() - 1).plusDays(1))
            .collect(Collectors.toMap<LocalDate, LocalDate, Dag>({ it }, { SykHelgedag(it, Økonomi.sykdomsgrad(100.prosent), SykdomstidslinjeHendelse.Hendelseskilde.INGEN) }))
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.P
    get() = Sykdomstidslinje(
        dagensDato.datesUntil(dagensDato.plusDays(this.toLong() - 1).plusDays(1))
            .collect(Collectors.toMap<LocalDate, LocalDate, Dag>({ it }, { Permisjonsdag(it, SykdomstidslinjeHendelse.Hendelseskilde.INGEN) }))
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.R
    get() = Sykdomstidslinje(
        dagensDato.datesUntil(dagensDato.plusDays(this.toLong() - 1).plusDays(1))
            .collect(Collectors.toMap<LocalDate, LocalDate, Dag>({ it }, { FriskHelgedag(it, SykdomstidslinjeHendelse.Hendelseskilde.INGEN) }))
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.UK
    get() = Sykdomstidslinje(
        dagensDato.datesUntil(dagensDato.plusDays(this.toLong() - 1).plusDays(1))
            .collect(Collectors.toMap<LocalDate, LocalDate, Dag>({ it }, { UkjentDag(it, SykdomstidslinjeHendelse.Hendelseskilde.INGEN) }))
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.AV
    get() = Sykdomstidslinje(
        dagensDato.datesUntil(dagensDato.plusDays(this.toLong() - 1).plusDays(1))
            .collect(Collectors.toMap<LocalDate, LocalDate, Dag>({ it }, { AvslåttDag(it, SykdomstidslinjeHendelse.Hendelseskilde.INGEN) }))
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.PROBLEM
    get() = Sykdomstidslinje(
        dagensDato.datesUntil(dagensDato.plusDays(this.toLong() - 1).plusDays(1))
            .collect(Collectors.toMap<LocalDate, LocalDate, Dag>({ it }, { ProblemDag(it, SykdomstidslinjeHendelse.Hendelseskilde.INGEN, "Problemdag") }))
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

private const val UNG_PERSON_FNR_2018 = "12029240045"
private const val AKTØRID = "42"
private const val ORGNUMMER = "987654321"

internal class TestHendelse(private val tidslinje: Sykdomstidslinje = Sykdomstidslinje()) : SykdomstidslinjeHendelse(UUID.randomUUID(), UNG_PERSON_FNR_2018, AKTØRID, ORGNUMMER, LocalDateTime.now()) {
    override fun sykdomstidslinje() = tidslinje
    override fun valider(periode: Periode, subsumsjonObserver: SubsumsjonObserver) = Aktivitetslogg()
    override fun fortsettÅBehandle(arbeidsgiver: Arbeidsgiver) = Unit
    override fun leggTil(hendelseIder: MutableSet<Dokumentsporing>) {}
}
