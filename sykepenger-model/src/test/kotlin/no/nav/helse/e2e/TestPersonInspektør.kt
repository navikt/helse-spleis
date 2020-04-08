package no.nav.helse.e2e

import no.nav.helse.etterspurtBehov
import no.nav.helse.etterspurteBehovFinnes
import no.nav.helse.person.*
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.dag.*
import no.nav.helse.utbetalingslinjer.Utbetalingslinjer
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import org.junit.jupiter.api.fail
import java.time.LocalDate
import java.util.*
import kotlin.reflect.KClass

internal class TestPersonInspektør(person: Person) : PersonVisitor {
    private var arbeidsgiverindeks: Int = -1
    private var vedtaksperiodeindeks: Int = -1
    private val tilstander = mutableMapOf<Int, MutableList<TilstandType>>()
    private val sykdomstidslinjer = mutableMapOf<Int, Sykdomstidslinje>()
    private val førsteFraværsdager = mutableMapOf<Int, LocalDate>()
    private val maksdatoer = mutableMapOf<Int, LocalDate>()
    private val vedtaksperiodeIder = mutableMapOf<Int, UUID>()
    internal lateinit var personLogg: Aktivitetslogg
    internal lateinit var arbeidsgiver: Arbeidsgiver
    internal lateinit var inntektshistorikk: Inntekthistorikk
    internal lateinit var sykdomshistorikk: Sykdomshistorikk
    internal val dagtelling = mutableMapOf<KClass<out Dag>, Int>()
    internal val inntekter = mutableMapOf<Int, MutableList<Inntekthistorikk.Inntekt>>()
    internal val arbeidsgiverUtbetalingslinjer = mutableListOf<Utbetalingslinjer>()
    private val utbetalingstidslinjer = mutableMapOf<Int, Utbetalingstidslinje>()
    private val vedtaksperioder = mutableMapOf<Int, Vedtaksperiode>()
    private var inVedtaksperiode = false
    private val gruppeIder = mutableMapOf<Int, UUID>()

    init {
        person.accept(this)
    }

    internal fun vedtaksperiodeId(index: Int) = requireNotNull(vedtaksperiodeIder[index])
    internal fun gruppeId(index: Int) = requireNotNull(gruppeIder[index])

    override fun preVisitArbeidsgiver(
        arbeidsgiver: Arbeidsgiver,
        id: UUID,
        organisasjonsnummer: String
    ) {
        arbeidsgiverindeks += 1
        this.arbeidsgiver = arbeidsgiver
    }

    override fun preVisitVedtaksperiode(
        vedtaksperiode: Vedtaksperiode,
        id: UUID,
        gruppeId: UUID
    ) {
        vedtaksperiodeindeks += 1
        tilstander[vedtaksperiodeindeks] = mutableListOf()
        vedtaksperiodeIder[vedtaksperiodeindeks] = id
        gruppeIder[vedtaksperiodeindeks] = gruppeId
        vedtaksperioder[vedtaksperiodeindeks] = vedtaksperiode
        inVedtaksperiode = true
    }

    override fun preVisitUtbetalingstidslinje(tidslinje: Utbetalingstidslinje) {
        if (inVedtaksperiode) utbetalingstidslinjer[vedtaksperiodeindeks] = tidslinje
    }

    override fun postVisitVedtaksperiode(vedtaksperiode: Vedtaksperiode, id: UUID, gruppeId: UUID) {
        inVedtaksperiode = false
    }

    override fun preVisitArbeidsgiverUtbetalingslinjer(linjer: Utbetalingslinjer) {
        arbeidsgiverUtbetalingslinjer.add(linjer)
    }

    internal fun etterspurteBehov(vedtaksperiodeIndex: Int, behovtype: Aktivitetslogg.Aktivitet.Behov.Behovtype) =
        personLogg.etterspurteBehovFinnes(requireNotNull(vedtaksperiodeIder[vedtaksperiodeIndex]), behovtype)

    internal inline fun <reified T> etterspurteBehov(vedtaksperiodeIndex: Int, behovtype: Aktivitetslogg.Aktivitet.Behov.Behovtype, felt: String) =
        personLogg.etterspurtBehov<T>(requireNotNull(vedtaksperiodeIder[vedtaksperiodeIndex]), behovtype, felt)

    override fun visitPersonAktivitetslogg(aktivitetslogg: Aktivitetslogg) {
        personLogg = aktivitetslogg
    }

    override fun visitFørsteFraværsdag(førsteFraværsdag: LocalDate?) {
        if (førsteFraværsdag != null) {
            førsteFraværsdager[vedtaksperiodeindeks] = førsteFraværsdag
        }
    }

    override fun visitMaksdato(maksdato: LocalDate?) {
        maksdato?.also { maksdatoer[vedtaksperiodeindeks] = it }
    }

    override fun preVisitInntekthistorikk(inntekthistorikk: Inntekthistorikk) {
        this.inntektshistorikk = inntekthistorikk
    }

    override fun visitInntekt(inntekt: Inntekthistorikk.Inntekt) {
        inntekter.getOrPut(arbeidsgiverindeks) { mutableListOf() }.add(inntekt)
    }

    override fun preVisitSykdomshistorikk(sykdomshistorikk: Sykdomshistorikk) {
        sykdomstidslinjer[vedtaksperiodeindeks] = sykdomshistorikk.sykdomstidslinje()
        this.sykdomshistorikk = sykdomshistorikk
        if(!sykdomshistorikk.isEmpty())
            this.sykdomshistorikk.sykdomstidslinje().accept(Dagteller())
    }

    override fun visitTilstand(tilstand: Vedtaksperiode.Vedtaksperiodetilstand) {
        tilstander[vedtaksperiodeindeks]?.add(tilstand.type) ?: fail {
            "Missing collection initialization"
        }
    }

    private inner class Dagteller : SykdomstidslinjeVisitor {
        override fun visitSykedag(dag: Sykedag.Sykmelding) = inkrementer(
            Sykedag::class)
        override fun visitSykedag(dag: Sykedag.Søknad) = inkrementer(
            Sykedag::class)

        override fun visitEgenmeldingsdag(dag: Egenmeldingsdag.Søknad) = inkrementer(
            Egenmeldingsdag::class)
        override fun visitEgenmeldingsdag(dag: Egenmeldingsdag.Inntektsmelding) = inkrementer(
            Egenmeldingsdag::class)

        override fun visitSykHelgedag(dag: SykHelgedag.Sykmelding) = inkrementer(
            SykHelgedag::class)
        override fun visitSykHelgedag(dag: SykHelgedag.Søknad) = inkrementer(
            SykHelgedag::class)

        override fun visitKunArbeidsgiverSykedag(dag: KunArbeidsgiverSykedag) = inkrementer(
            KunArbeidsgiverSykedag::class)

        override fun visitArbeidsdag(dag: Arbeidsdag.Inntektsmelding) = inkrementer(
            Arbeidsdag::class
        )
        override fun visitArbeidsdag(dag: Arbeidsdag.Søknad) = inkrementer(
            Arbeidsdag::class
        )

        override fun visitFeriedag(dag: Feriedag.Inntektsmelding) = inkrementer(Feriedag::class)
        override fun visitFeriedag(dag: Feriedag.Søknad) = inkrementer(Feriedag::class)

        override fun visitFriskHelgedag(dag: FriskHelgedag.Inntektsmelding) = inkrementer(FriskHelgedag::class)
        override fun visitFriskHelgedag(dag: FriskHelgedag.Søknad) = inkrementer(FriskHelgedag::class)

        private fun inkrementer(klasse: KClass<out Dag>) {
            dagtelling.compute(klasse) { _, value ->
                1 + (value ?: 0)
            }
        }
    }

    internal val vedtaksperiodeTeller get() = vedtaksperiodeindeks + 1

    internal fun maksdato(indeks: Int) = maksdatoer[indeks] ?: fail {
        "Missing collection initialization"
    }

    internal fun utbetalingslinjer(indeks: Int) = arbeidsgiverUtbetalingslinjer[indeks] ?: fail {
        "Missing collection initialization"
    }

    internal fun tilstand(indeks: Int) = tilstander[indeks] ?: fail {
        "Missing collection initialization"
    }

    internal fun sisteTilstand(indeks: Int) = tilstander[indeks]?.last() ?: fail {
        "Missing collection initialization"
    }

    internal fun førsteFraværsdag(indeks: Int) = førsteFraværsdager[indeks] ?:fail {
        "Missing collection initialization"
    }

    internal fun sykdomstidslinje(indeks: Int) = sykdomstidslinjer[indeks] ?:fail {
        "Missing collection initialization"
    }

    internal fun utbetalingstidslinjer(indeks: Int) = utbetalingstidslinjer[indeks] ?: fail {
        "Missing collection initialization"
    }

    internal fun vedtaksperioder(indeks: Int) = vedtaksperioder[indeks] ?: fail {
        "Missing collection initialization"
    }

    internal fun dagTeller(klasse: KClass<out Utbetalingstidslinje.Utbetalingsdag>) =
        TestTidslinjeInspektør(arbeidsgiver.nåværendeTidslinje()).dagtelling[klasse] ?: 0
}
