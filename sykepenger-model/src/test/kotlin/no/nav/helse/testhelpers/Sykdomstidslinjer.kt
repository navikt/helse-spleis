package no.nav.helse.testhelpers

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import java.util.stream.Collectors
import no.nav.helse.etterlevelse.SubsumsjonObserver
import no.nav.helse.hendelser.Periode
import no.nav.helse.januar
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Dag.FriskHelgedag
import no.nav.helse.sykdomstidslinje.Dag.Permisjonsdag
import no.nav.helse.sykdomstidslinje.Dag.ProblemDag
import no.nav.helse.sykdomstidslinje.Dag.SykHelgedag
import no.nav.helse.sykdomstidslinje.Dag.UkjentDag
import no.nav.helse.sykdomstidslinje.Melding
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import no.nav.helse.økonomi.Økonomi

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

internal val Int.N
    get() = Sykdomstidslinje.sykedagerNav(
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

internal fun Int.U(melding: Melding) = Sykdomstidslinje.arbeidsgiverdager(
    dagensDato,
    dagensDato.plusDays(this.toLong() - 1),
    100.prosent,
    SykdomstidslinjeHendelse.Hendelseskilde(melding, UUID.randomUUID(), LocalDateTime.now())
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

internal fun Int.A(melding: Melding) = Sykdomstidslinje.arbeidsdager(
    dagensDato,
    dagensDato.plusDays(this.toLong() - 1),
    SykdomstidslinjeHendelse.Hendelseskilde(melding, UUID.randomUUID(), LocalDateTime.now())
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

internal val Int.J
    get() = Sykdomstidslinje.feriedagerUtenSykmelding(
        dagensDato,
        dagensDato.plusDays(this.toLong() - 1),
        SykdomstidslinjeHendelse.Hendelseskilde.INGEN
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.YF
    get() = Sykdomstidslinje.andreYtelsedager(
        dagensDato,
        dagensDato.plusDays(this.toLong() - 1),
        SykdomstidslinjeHendelse.Hendelseskilde.INGEN,
        Dag.AndreYtelser.AnnenYtelse.Foreldrepenger
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.YD
    get() = Sykdomstidslinje.andreYtelsedager(
        dagensDato,
        dagensDato.plusDays(this.toLong() - 1),
        SykdomstidslinjeHendelse.Hendelseskilde.INGEN,
        Dag.AndreYtelser.AnnenYtelse.Dagpenger
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.YA
    get() = Sykdomstidslinje.andreYtelsedager(
        dagensDato,
        dagensDato.plusDays(this.toLong() - 1),
        SykdomstidslinjeHendelse.Hendelseskilde.INGEN,
        Dag.AndreYtelser.AnnenYtelse.AAP
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.YO
    get() = Sykdomstidslinje.andreYtelsedager(
        dagensDato,
        dagensDato.plusDays(this.toLong() - 1),
        SykdomstidslinjeHendelse.Hendelseskilde.INGEN,
        Dag.AndreYtelser.AnnenYtelse.Omsorgspenger
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.YP
    get() = Sykdomstidslinje.andreYtelsedager(
        dagensDato,
        dagensDato.plusDays(this.toLong() - 1),
        SykdomstidslinjeHendelse.Hendelseskilde.INGEN,
        Dag.AndreYtelser.AnnenYtelse.Pleiepenger
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.YS
    get() = Sykdomstidslinje.andreYtelsedager(
        dagensDato,
        dagensDato.plusDays(this.toLong() - 1),
        SykdomstidslinjeHendelse.Hendelseskilde.INGEN,
        Dag.AndreYtelser.AnnenYtelse.Svangerskapspenger
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.YOL
    get() = Sykdomstidslinje.andreYtelsedager(
        dagensDato,
        dagensDato.plusDays(this.toLong() - 1),
        SykdomstidslinjeHendelse.Hendelseskilde.INGEN,
        Dag.AndreYtelser.AnnenYtelse.Opplæringspenger
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

internal val Int.PROBLEM
    get() = Sykdomstidslinje(
        dagensDato.datesUntil(dagensDato.plusDays(this.toLong() - 1).plusDays(1))
            .collect(Collectors.toMap<LocalDate, LocalDate, Dag>({ it }, { ProblemDag(it, SykdomstidslinjeHendelse.Hendelseskilde.INGEN, "Problemdag") }))
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

private const val UNG_PERSON_FNR_2018 = "12029240045"
private const val AKTØRID = "42"
private const val ORGNUMMER = "987654321"

internal class TestHendelse(private val tidslinje: Sykdomstidslinje = Sykdomstidslinje(), meldingsreferanseId: UUID = UUID.randomUUID()) : SykdomstidslinjeHendelse(meldingsreferanseId, UNG_PERSON_FNR_2018, AKTØRID, ORGNUMMER, LocalDateTime.now()) {
    override fun sykdomstidslinje() = tidslinje
    override fun valider(periode: Periode, subsumsjonObserver: SubsumsjonObserver) = Aktivitetslogg()
    override fun leggTil(hendelseIder: MutableSet<Dokumentsporing>) = true
}
