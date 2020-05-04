package no.nav.helse.testhelpers

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.sykdomstidslinje.NySykdomstidslinje
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import java.time.LocalDate
import java.util.*

private var dagensDato = LocalDate.of(2018, 1, 1)

internal fun resetSeed(frøDato: LocalDate = LocalDate.of(2018, 1, 1)) {
    dagensDato = frøDato
}

internal val Int.S
    get() = Sykdomstidslinje.sykedager(
        dagensDato, dagensDato.plusDays(this.toLong() - 1),
        100.0,
        Søknad.SøknadDagFactory
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.F
    get() = Sykdomstidslinje.ferie(
        dagensDato, dagensDato.plusDays(this.toLong() - 1),
        Søknad.SøknadDagFactory
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.A
    get() = Sykdomstidslinje.ikkeSykedager(
        dagensDato, dagensDato.plusDays(this.toLong() - 1),
        Søknad.SøknadDagFactory
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.I
    get() = Sykdomstidslinje.implisitteDager(
        dagensDato, dagensDato.plusDays(this.toLong() - 1),
        Søknad.SøknadDagFactory
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.E
    get() = Sykdomstidslinje.egenmeldingsdager(
        dagensDato, dagensDato.plusDays(this.toLong() - 1),
        Søknad.SøknadDagFactory
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.P
    get() = Sykdomstidslinje.permisjonsdager(
        dagensDato, dagensDato.plusDays(this.toLong() - 1),
        Søknad.SøknadDagFactory
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.EDU
    get() = Sykdomstidslinje.studiedager(
        dagensDato, dagensDato.plusDays(this.toLong() - 1),
        Søknad.SøknadDagFactory
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.UT
    get() = Sykdomstidslinje.utenlandsdager(
        dagensDato, dagensDato.plusDays(this.toLong() - 1),
        Søknad.SøknadDagFactory
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.U
    get() = Sykdomstidslinje.ubestemtdager(
        dagensDato, dagensDato.plusDays(this.toLong() - 1),
        Søknad.SøknadDagFactory
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.FO
    get() = Sykdomstidslinje.foreldetSykedager(
        dagensDato, dagensDato.plusDays(this.toLong() - 1),
        100.0,
        Søknad.SøknadDagFactory
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.nS
    get() = NySykdomstidslinje.sykedager(
        dagensDato,
        dagensDato.plusDays(this.toLong() - 1),
        100.0,
        TestHendelse.kilde
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.nU
    get() = NySykdomstidslinje.arbeidsgiverdager(
        dagensDato,
        dagensDato.plusDays(this.toLong() - 1),
        100.0,
        TestHendelse.kilde
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.nK
    get() = NySykdomstidslinje.foreldetSykedag(
        dagensDato,
        dagensDato.plusDays(this.toLong() - 1),
        100.0,
        TestHendelse.kilde
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.nA
    get() = NySykdomstidslinje.arbeidsdager(
        dagensDato,
        dagensDato.plusDays(this.toLong() - 1),
        TestHendelse.kilde
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.n_
    get() = NySykdomstidslinje(
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.nF
    get() = NySykdomstidslinje.feriedager(
        dagensDato,
        dagensDato.plusDays(this.toLong() - 1),
        TestHendelse.kilde
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

private object TestHendelse : SykdomstidslinjeHendelse(UUID.randomUUID()) {
    private const val UNG_PERSON_FNR_2018 = "12020052345"
    private const val AKTØRID = "42"
    private const val ORGNUMMER = "987654321"

    override fun sykdomstidslinje(tom: LocalDate) = Sykdomstidslinje()
    override fun sykdomstidslinje() = Sykdomstidslinje()
    override fun nySykdomstidslinje() = NySykdomstidslinje()
    override fun nySykdomstidslinje(tom: LocalDate) = NySykdomstidslinje()
    override fun valider(periode: Periode) = Aktivitetslogg()
    override fun fortsettÅBehandle(arbeidsgiver: Arbeidsgiver) = Unit
    override fun aktørId() = AKTØRID
    override fun fødselsnummer() = UNG_PERSON_FNR_2018
    override fun organisasjonsnummer() = ORGNUMMER
}
