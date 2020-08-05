package no.nav.helse.testhelpers

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Dag.*
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import no.nav.helse.økonomi.Økonomi
import java.time.LocalDate
import java.util.*
import java.util.stream.Collectors

private var dagensDato = LocalDate.of(2018, 1, 1)

internal fun resetSeed(frøDato: LocalDate = LocalDate.of(2018, 1, 1)) {
    dagensDato = frøDato
}

internal val Int.nS
    get() = Sykdomstidslinje.sykedager(
        dagensDato,
        dagensDato.plusDays(this.toLong() - 1),
        100.0,
        TestHendelse.kilde
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.nU
    get() = Sykdomstidslinje.arbeidsgiverdager(
        dagensDato,
        dagensDato.plusDays(this.toLong() - 1),
        100.0,
        TestHendelse.kilde
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.nK
    get() = Sykdomstidslinje.foreldetSykedag(
        dagensDato,
        dagensDato.plusDays(this.toLong() - 1),
        100.0,
        TestHendelse.kilde
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.nA
    get() = Sykdomstidslinje.arbeidsdager(
        dagensDato,
        dagensDato.plusDays(this.toLong() - 1),
        TestHendelse.kilde
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.n_
    get() = Sykdomstidslinje(
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.nF
    get() = Sykdomstidslinje.feriedager(
        dagensDato,
        dagensDato.plusDays(this.toLong() - 1),
        TestHendelse.kilde
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.nEDU
    get() = Sykdomstidslinje(
        dagensDato.datesUntil(dagensDato.plusDays(this.toLong() - 1).plusDays(1))
            .collect(Collectors.toMap({ it }, { Studiedag(it, TestHendelse.kilde) }))
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.nH
    get() = Sykdomstidslinje(
        dagensDato.datesUntil(dagensDato.plusDays(this.toLong() - 1).plusDays(1))
            .collect(Collectors.toMap<LocalDate, LocalDate, Dag>({ it }, { SykHelgedag(it, Økonomi.sykdomsgrad(100.prosent), TestHendelse.kilde) }))
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.nP
    get() = Sykdomstidslinje(
        dagensDato.datesUntil(dagensDato.plusDays(this.toLong() - 1).plusDays(1))
            .collect(Collectors.toMap<LocalDate, LocalDate, Dag>({ it }, { Permisjonsdag(it, TestHendelse.kilde) }))
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.nUT
    get() = Sykdomstidslinje(
        dagensDato.datesUntil(dagensDato.plusDays(this.toLong() - 1).plusDays(1))
            .collect(Collectors.toMap<LocalDate, LocalDate, Dag>({ it }, { Utenlandsdag(it, TestHendelse.kilde) }))
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

private object TestHendelse : SykdomstidslinjeHendelse(UUID.randomUUID()) {
    private const val UNG_PERSON_FNR_2018 = "12020052345"
    private const val AKTØRID = "42"
    private const val ORGNUMMER = "987654321"

    override fun sykdomstidslinje() = Sykdomstidslinje()
    override fun sykdomstidslinje(tom: LocalDate) = Sykdomstidslinje()
    override fun valider(periode: Periode) = Aktivitetslogg()
    override fun fortsettÅBehandle(arbeidsgiver: Arbeidsgiver) = Unit
    override fun aktørId() = AKTØRID
    override fun fødselsnummer() = UNG_PERSON_FNR_2018
    override fun organisasjonsnummer() = ORGNUMMER
}
