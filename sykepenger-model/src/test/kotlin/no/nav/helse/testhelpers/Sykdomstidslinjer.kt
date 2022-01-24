package no.nav.helse.testhelpers

import no.nav.helse.hendelser.Periode
import no.nav.helse.januar
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Arbeidsgiver
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

private var threadLocalDagensDato = ThreadLocal.withInitial { 1.januar }
private var dagensDato: LocalDate
    get() = threadLocalDagensDato.get()
    set(value) = threadLocalDagensDato.set(value)

internal fun resetSeed(frøDato: LocalDate = 1.januar) {
    dagensDato = frøDato
}

internal val Int.S
    get() = Sykdomstidslinje.sykedager(
        dagensDato,
        dagensDato.plusDays(this.toLong() - 1),
        100.prosent,
        TestHendelse.kilde
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.U
    get() = Sykdomstidslinje.arbeidsgiverdager(
        dagensDato,
        dagensDato.plusDays(this.toLong() - 1),
        100.prosent,
        TestHendelse.kilde
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.K
    get() = Sykdomstidslinje.foreldetSykedag(
        dagensDato,
        dagensDato.plusDays(this.toLong() - 1),
        100.prosent,
        TestHendelse.kilde
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.A
    get() = Sykdomstidslinje.arbeidsdager(
        dagensDato,
        dagensDato.plusDays(this.toLong() - 1),
        TestHendelse.kilde
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.opphold
    get() = Sykdomstidslinje(
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.F
    get() = Sykdomstidslinje.feriedager(
        dagensDato,
        dagensDato.plusDays(this.toLong() - 1),
        TestHendelse.kilde
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.H
    get() = Sykdomstidslinje(
        dagensDato.datesUntil(dagensDato.plusDays(this.toLong() - 1).plusDays(1))
            .collect(Collectors.toMap<LocalDate, LocalDate, Dag>({ it }, { SykHelgedag(it, Økonomi.sykdomsgrad(100.prosent), TestHendelse.kilde) }))
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.P
    get() = Sykdomstidslinje(
        dagensDato.datesUntil(dagensDato.plusDays(this.toLong() - 1).plusDays(1))
            .collect(Collectors.toMap<LocalDate, LocalDate, Dag>({ it }, { Permisjonsdag(it, TestHendelse.kilde) }))
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.R
    get() = Sykdomstidslinje(
        dagensDato.datesUntil(dagensDato.plusDays(this.toLong() - 1).plusDays(1))
            .collect(Collectors.toMap<LocalDate, LocalDate, Dag>({ it }, { FriskHelgedag(it, TestHendelse.kilde) }))
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.UK
    get() = Sykdomstidslinje(
        dagensDato.datesUntil(dagensDato.plusDays(this.toLong() - 1).plusDays(1))
            .collect(Collectors.toMap<LocalDate, LocalDate, Dag>({ it }, { UkjentDag(it, TestHendelse.kilde) }))
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

private object TestHendelse : SykdomstidslinjeHendelse(UUID.randomUUID(), LocalDateTime.now()) {
    private const val UNG_PERSON_FNR_2018 = "12029240045"
    private const val AKTØRID = "42"
    private const val ORGNUMMER = "987654321"

    override fun sykdomstidslinje() = Sykdomstidslinje()
    override fun valider(periode: Periode) = Aktivitetslogg()
    override fun fortsettÅBehandle(arbeidsgiver: Arbeidsgiver) = Unit
    override fun aktørId() = AKTØRID
    override fun fødselsnummer() = UNG_PERSON_FNR_2018
    override fun organisasjonsnummer() = ORGNUMMER
}
