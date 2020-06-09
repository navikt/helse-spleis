package no.nav.helse.spleis.e2e

import no.nav.helse.etterspurtBehov
import no.nav.helse.etterspurteBehovFinnes
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.person.*
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Dag.*
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse.Hendelseskilde
import no.nav.helse.utbetalingslinjer.Oppdrag
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Prosentdel
import no.nav.helse.økonomi.Økonomi
import org.junit.jupiter.api.fail
import java.time.LocalDate
import java.util.*
import kotlin.reflect.KClass

internal class TestPersonInspektør(person: Person) : PersonVisitor {
    internal var vedtaksperiodeTeller: Int = 0
        private set
    private var arbeidsgiverindeks: Int = -1
    private var vedtaksperiodeindeks: Int = -1
    private val tilstander = mutableMapOf<Int, TilstandType>()
    private val forkastedeTilstander = mutableMapOf<Int, TilstandType>()
    private val sykdomstidslinjer = mutableMapOf<Int, Sykdomstidslinje>()
    private val førsteFraværsdager = mutableMapOf<Int, LocalDate>()
    private val maksdatoer = mutableMapOf<Int, LocalDate>()
    private val forkastetMaksdatoer = mutableMapOf<Int, LocalDate>()
    private val vedtaksperiodeIder = mutableMapOf<Int, UUID>()
    private val forkastedePerioderIder = mutableMapOf<Int, UUID>()
    private val vilkårsgrunnlag = mutableMapOf<Int, Vilkårsgrunnlag.Grunnlagsdata>()
    internal lateinit var personLogg: Aktivitetslogg
    internal lateinit var arbeidsgiver: Arbeidsgiver
    internal lateinit var inntektshistorikk: Inntekthistorikk
    internal lateinit var sykdomshistorikk: Sykdomshistorikk
    internal val dagtelling = mutableMapOf<KClass<out Dag>, Int>()
    internal val inntekter = mutableMapOf<Int, MutableList<Inntekthistorikk.Inntekt>>()
    private val arbeidsgiverutbetalinger = mutableMapOf<Int, List<Utbetaling>>()
    internal val arbeidsgiverOppdrag = mutableListOf<Oppdrag>()
    internal val totalBeløp = mutableListOf<Int>()
    internal val nettoBeløp = mutableListOf<Int>()
    private val utbetalingstidslinjer = mutableMapOf<Int, Utbetalingstidslinje>()
    private val vedtaksperioder = mutableMapOf<Int, Vedtaksperiode>()
    private var inGyldigePerioder = false
    private var inVedtaksperiode = false
    private val gruppeIder = mutableMapOf<Int, UUID>()
    private val forlengelserFraInfotrygd = mutableMapOf<Int, ForlengelseFraInfotrygd>()
    private val periodeIder = mutableMapOf<Int, UUID>()

    init {
        person.accept(this)
    }

    internal fun vedtaksperiodeId(index: Int) = requireNotNull(vedtaksperiodeIder[index])
    internal fun forkastetVedtaksperiodeId(index: Int) = requireNotNull(forkastedePerioderIder[index])
    internal fun gruppeId(index: Int) = requireNotNull(gruppeIder[index])

    override fun preVisitArbeidsgiver(
        arbeidsgiver: Arbeidsgiver,
        id: UUID,
        organisasjonsnummer: String
    ) {
        arbeidsgiverindeks += 1
        this.arbeidsgiver = arbeidsgiver
    }

    override fun preVisitPerioder(vedtaksperioder: List<Vedtaksperiode>) {
        inGyldigePerioder = true
        vedtaksperiodeindeks = -1
        periodeIder.clear()
    }

    override fun postVisitPerioder(vedtaksperioder: List<Vedtaksperiode>) {
        vedtaksperiodeIder.putAll(periodeIder)
        inGyldigePerioder = false
    }

    override fun preVisitForkastedePerioder(vedtaksperioder: List<Vedtaksperiode>) {
        vedtaksperiodeindeks = -1
        periodeIder.clear()
    }

    override fun postVisitForkastedePerioder(vedtaksperioder: List<Vedtaksperiode>) {
        forkastedePerioderIder.putAll(periodeIder)
    }

    override fun preVisitVedtaksperiode(
        vedtaksperiode: Vedtaksperiode,
        id: UUID,
        gruppeId: UUID,
        arbeidsgiverNettoBeløp: Int,
        personNettoBeløp: Int,
        periode: Periode
    ) {
        inVedtaksperiode = true
        vedtaksperiodeTeller += 1
        vedtaksperiodeindeks += 1
        periodeIder[vedtaksperiodeindeks] = id

        if (!inGyldigePerioder) return

        gruppeIder[vedtaksperiodeindeks] = gruppeId
        vedtaksperioder[vedtaksperiodeindeks] = vedtaksperiode
    }

    override fun preVisit(tidslinje: Utbetalingstidslinje) {
        if (inVedtaksperiode) utbetalingstidslinjer[vedtaksperiodeindeks] = tidslinje
    }

    override fun visitForlengelseFraInfotrygd(forlengelseFraInfotrygd: ForlengelseFraInfotrygd) {
        if (!inGyldigePerioder) return
        forlengelserFraInfotrygd[vedtaksperiodeindeks] = forlengelseFraInfotrygd
    }

    override fun postVisitVedtaksperiode(
        vedtaksperiode: Vedtaksperiode,
        id: UUID,
        gruppeId: UUID,
        arbeidsgiverNettoBeløp: Int,
        personNettoBeløp: Int,
        periode: Periode
    ) {
        inVedtaksperiode = false
    }

    override fun preVisitUtbetalinger(utbetalinger: List<Utbetaling>) {
        arbeidsgiverutbetalinger[arbeidsgiverindeks] = utbetalinger
    }

    override fun preVisitArbeidsgiverOppdrag(oppdrag: Oppdrag) {
        arbeidsgiverOppdrag.add(oppdrag)
    }

    override fun preVisitOppdrag(oppdrag: Oppdrag, totalBeløp: Int, nettoBeløp: Int) {
        if (oppdrag != arbeidsgiverOppdrag.last()) return
        this.totalBeløp.add(totalBeløp)
        this.nettoBeløp.add(nettoBeløp)
    }

    internal fun etterspurteBehov(vedtaksperiodeIndex: Int, behovtype: Aktivitetslogg.Aktivitet.Behov.Behovtype) =
        personLogg.etterspurteBehovFinnes(requireNotNull(vedtaksperiodeIder[vedtaksperiodeIndex]), behovtype)

    internal inline fun <reified T> etterspurteBehov(vedtaksperiodeIndex: Int, behovtype: Aktivitetslogg.Aktivitet.Behov.Behovtype, felt: String) =
        personLogg.etterspurtBehov<T>(requireNotNull(vedtaksperiodeIder[vedtaksperiodeIndex]), behovtype, felt)

    override fun visitPersonAktivitetslogg(aktivitetslogg: Aktivitetslogg) {
        personLogg = aktivitetslogg
    }

    override fun visitFørsteFraværsdag(førsteFraværsdag: LocalDate?) {
        if (!inGyldigePerioder || førsteFraværsdag == null) return
        førsteFraværsdager[vedtaksperiodeindeks] = førsteFraværsdag
    }

    override fun visitMaksdato(maksdato: LocalDate?) {
        if (maksdato == null) return
        if (!inGyldigePerioder) forkastetMaksdatoer[vedtaksperiodeindeks] = maksdato
        else maksdatoer[vedtaksperiodeindeks] = maksdato
    }

    override fun preVisitInntekthistorikk(inntekthistorikk: Inntekthistorikk) {
        this.inntektshistorikk = inntekthistorikk
    }

    override fun visitInntekt(inntekt: Inntekthistorikk.Inntekt, id: UUID) {
        inntekter.getOrPut(arbeidsgiverindeks) { mutableListOf() }.add(inntekt)
    }

    override fun preVisitSykdomshistorikk(sykdomshistorikk: Sykdomshistorikk) {
        sykdomstidslinjer[vedtaksperiodeindeks] = sykdomshistorikk.sykdomstidslinje()
        this.sykdomshistorikk = sykdomshistorikk
        if(!sykdomshistorikk.isEmpty())
            this.sykdomshistorikk.sykdomstidslinje().accept(Dagteller())
    }

    override fun visitTilstand(tilstand: Vedtaksperiode.Vedtaksperiodetilstand) {
        if (!inGyldigePerioder) forkastedeTilstander[vedtaksperiodeindeks] = tilstand.type
        else tilstander[vedtaksperiodeindeks] = tilstand.type
    }

    override fun visitDataForVilkårsvurdering(dataForVilkårsvurdering: Vilkårsgrunnlag.Grunnlagsdata?) {
        if (dataForVilkårsvurdering == null) return
        vilkårsgrunnlag[vedtaksperiodeindeks] = dataForVilkårsvurdering
    }

    private inner class Dagteller : SykdomstidslinjeVisitor {
        override fun visitDag(dag: UkjentDag, dato: LocalDate, kilde: Hendelseskilde) = inkrementer(dag)
        override fun visitDag(dag: Arbeidsdag, dato: LocalDate, kilde: Hendelseskilde) = inkrementer(dag)
        override fun visitDag(
            dag: Arbeidsgiverdag,
            dato: LocalDate,
            økonomi: Økonomi,
            grad: Prosentdel,
            arbeidsgiverBetalingProsent: Prosentdel,
            kilde: Hendelseskilde
        ) = inkrementer(dag)
        override fun visitDag(dag: Feriedag, dato: LocalDate, kilde: Hendelseskilde) = inkrementer(dag)
        override fun visitDag(dag: FriskHelgedag, dato: LocalDate, kilde: Hendelseskilde) = inkrementer(dag)
        override fun visitDag(
            dag: ArbeidsgiverHelgedag,
            dato: LocalDate,
            økonomi: Økonomi,
            grad: Prosentdel,
            arbeidsgiverBetalingProsent: Prosentdel,
            kilde: Hendelseskilde
        ) = inkrementer(dag)
        override fun visitDag(
            dag: Sykedag,
            dato: LocalDate,
            økonomi: Økonomi,
            grad: Prosentdel,
            arbeidsgiverBetalingProsent: Prosentdel,
            kilde: Hendelseskilde
        ) = inkrementer(dag)
        override fun visitDag(
            dag: ForeldetSykedag,
            dato: LocalDate,
            økonomi: Økonomi,
            grad: Prosentdel,
            arbeidsgiverBetalingProsent: Prosentdel,
            kilde: Hendelseskilde
        ) = inkrementer(dag)
        override fun visitDag(
            dag: SykHelgedag,
            dato: LocalDate,
            økonomi: Økonomi,
            grad: Prosentdel,
            arbeidsgiverBetalingProsent: Prosentdel,
            kilde: Hendelseskilde
        ) = inkrementer(dag)
        override fun visitDag(dag: Permisjonsdag, dato: LocalDate, kilde: Hendelseskilde) = inkrementer(dag)
        override fun visitDag(dag: Studiedag, dato: LocalDate, kilde: Hendelseskilde) = inkrementer(dag)
        override fun visitDag(dag: Utenlandsdag, dato: LocalDate, kilde: Hendelseskilde) = inkrementer(dag)
        override fun visitDag(dag: ProblemDag, dato: LocalDate, kilde: Hendelseskilde, melding: String) = inkrementer(dag)

        private fun inkrementer(klasse: Dag) {
            dagtelling.compute(klasse::class) { _, value -> 1 + (value ?: 0) }
        }
    }

    internal fun forkastetMaksdato(indeks: Int) = forkastetMaksdatoer[indeks] ?: fail {
        "Missing collection initialization"
    }

    internal fun maksdato(indeks: Int) = maksdatoer[indeks] ?: fail {
        "Missing collection initialization"
    }

    internal fun forlengelseFraInfotrygd(indeks: Int) = forlengelserFraInfotrygd[indeks] ?: fail {
        "Missing collection initialization"
    }

    internal fun vilkårsgrunnlag(indeks: Int) = vilkårsgrunnlag[indeks] ?: fail {
        "Missing collection initialization"
    }

    internal fun arbeidsgiverutbetalinger(indeks: Int) = arbeidsgiverutbetalinger[indeks] ?: fail {
        "Missing collection initialization"
    }

    internal fun utbetalingslinjer(indeks: Int) = arbeidsgiverOppdrag[indeks]

    internal fun sisteTilstand(indeks: Int) = tilstander[indeks] ?: fail {
        "Missing collection initialization"
    }
    internal fun sisteForkastetTilstand(indeks: Int) = forkastedeTilstander[indeks] ?: fail {
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
