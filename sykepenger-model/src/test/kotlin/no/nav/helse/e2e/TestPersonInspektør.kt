package no.nav.helse.e2e

import no.nav.helse.etterspurteBehovFinnes
import no.nav.helse.person.*
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
import no.nav.helse.sykdomstidslinje.dag.Dag
import no.nav.helse.sykdomstidslinje.dag.SykHelgedag
import no.nav.helse.sykdomstidslinje.dag.Sykedag
import no.nav.helse.utbetalingstidslinje.Utbetalingslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import org.junit.jupiter.api.Assertions
import java.time.LocalDate
import java.util.*
import kotlin.reflect.KClass

internal class TestPersonInspektør(person: Person) : PersonVisitor {
    private var arbeidsgiverindeks: Int = -1
    private var vedtaksperiodeindeks: Int = -1
    private val tilstander = mutableMapOf<Int, MutableList<TilstandType>>()
    private val sykdomshistorier = mutableMapOf<Int, Sykdomshistorikk>()
    private val vedtaksperiodeIder = mutableMapOf<Int, UUID>()
    internal lateinit var personLogg: Aktivitetslogg
    internal lateinit var arbeidsgiver: Arbeidsgiver
    internal lateinit var inntektshistorikk: Inntekthistorikk
    internal lateinit var sykdomshistorikk: Sykdomshistorikk
    internal val førsteFraværsdager: MutableList<LocalDate> = mutableListOf()
    internal val dagtelling = mutableMapOf<KClass<out Dag>, Int>()
    internal val inntekter = mutableMapOf<Int, MutableList<Inntekthistorikk.Inntekt>>()
    internal val utbetalingslinjer = mutableMapOf<Int, List<Utbetalingslinje>>()

    init {
        person.accept(this)
    }

    internal fun vedtaksperiodeId(index: Int) = requireNotNull(vedtaksperiodeIder[index])

    override fun preVisitArbeidsgiver(
        arbeidsgiver: Arbeidsgiver,
        id: UUID,
        organisasjonsnummer: String
    ) {
        arbeidsgiverindeks += 1
        this.arbeidsgiver = arbeidsgiver
    }

    override fun preVisitVedtaksperiode(vedtaksperiode: Vedtaksperiode, id: UUID) {
        vedtaksperiodeindeks += 1
        tilstander[vedtaksperiodeindeks] = mutableListOf()
        vedtaksperiodeIder[vedtaksperiodeindeks] = id
    }

    override fun preVisitUtbetalingslinjer(linjer: List<Utbetalingslinje>) {
        utbetalingslinjer[vedtaksperiodeindeks] = linjer
    }

    internal fun etterspurteBehov(vedtaksperiodeIndex: Int, behovtype: Aktivitetslogg.Aktivitet.Behov.Behovtype) =
        personLogg.etterspurteBehovFinnes(requireNotNull(vedtaksperiodeIder[vedtaksperiodeIndex]), behovtype)

    override fun visitPersonAktivitetslogg(aktivitetslogg: Aktivitetslogg) {
        personLogg = aktivitetslogg
    }

    override fun visitFørsteFraværsdag(førsteFraværsdag: LocalDate?) {
        if (førsteFraværsdag != null) {
            førsteFraværsdager.add(førsteFraværsdag)
        }
    }

    override fun preVisitInntekthistorikk(inntekthistorikk: Inntekthistorikk) {
        this.inntektshistorikk = inntekthistorikk
    }

    override fun visitInntekt(inntekt: Inntekthistorikk.Inntekt) {
        inntekter.getOrPut(arbeidsgiverindeks) { mutableListOf() }.add(inntekt)
    }

    override fun preVisitSykdomshistorikk(sykdomshistorikk: Sykdomshistorikk) {
        sykdomshistorier[vedtaksperiodeindeks] = sykdomshistorikk
        this.sykdomshistorikk = sykdomshistorikk
        if(!sykdomshistorikk.isEmpty())
            this.sykdomshistorikk.sykdomstidslinje().accept(Dagteller())
    }

    private inner class Dagteller : SykdomstidslinjeVisitor {
        override fun visitSykedag(sykedag: Sykedag.Sykmelding) = inkrementer(
            Sykedag::class)
        override fun visitSykedag(sykedag: Sykedag.Søknad) = inkrementer(
            Sykedag::class)

        override fun visitSykHelgedag(sykHelgedag: SykHelgedag.Sykmelding) = inkrementer(
            SykHelgedag::class)
        override fun visitSykHelgedag(sykHelgedag: SykHelgedag.Søknad) = inkrementer(
            SykHelgedag::class)

        private fun inkrementer(klasse: KClass<out Dag>) {
            dagtelling.compute(klasse) { _, value ->
                1 + (value ?: 0)
            }
        }
    }

    override fun visitTilstand(tilstand: Vedtaksperiode.Vedtaksperiodetilstand) {
        tilstander[vedtaksperiodeindeks]?.add(tilstand.type) ?: Assertions.fail(
            "Missing collection initialization"
        )
    }

    internal val vedtaksperiodeTeller get() = vedtaksperiodeindeks + 1

    internal fun sykdomshistorikk(indeks: Int) = sykdomshistorier[indeks] ?: Assertions.fail(
        "Missing collection initialization"
    )
    internal fun tilstand(indeks: Int) = tilstander[indeks] ?: Assertions.fail(
        "Missing collection initialization"
    )

    internal fun dagTeller(klasse: KClass<out Utbetalingstidslinje.Utbetalingsdag>) =
        TestTidslinjeInspektør(arbeidsgiver.peekTidslinje()).dagtelling[klasse] ?: 0
}
