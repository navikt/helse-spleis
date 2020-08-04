package no.nav.helse.spleis.e2e

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

internal class TestArbeidsgiverInspektør(
    private val person: Person,
    orgnummer: String? = null
) : ArbeidsgiverVisitor {
    internal var vedtaksperiodeTeller: Int = 0
        private set
    private var vedtaksperiodeindeks: Int = -1
    private val tilstander = mutableMapOf<Int, TilstandType>()
    private val forkastedeTilstander = mutableMapOf<Int, TilstandType>()
    private val førsteFraværsdager = mutableMapOf<Int, LocalDate>()
    private val maksdatoer = mutableMapOf<Int, LocalDate>()
    private val gjenståendeSykedagerer = mutableMapOf<Int, Int>()
    private val forkastetMaksdatoer = mutableMapOf<Int, LocalDate>()
    private val forkastetGjenståendeSykedagerer = mutableMapOf<Int, Int>()
    private val vedtaksperiodeIder = mutableMapOf<Int, UUID>()
    private val forkastedePerioderIder = mutableMapOf<Int, UUID>()
    private val vilkårsgrunnlag = mutableMapOf<Int, Vilkårsgrunnlag.Grunnlagsdata>()
    internal val personLogg: Aktivitetslogg
    internal lateinit var arbeidsgiver: Arbeidsgiver
    internal lateinit var inntektshistorikk: Inntekthistorikk
    internal lateinit var sykdomshistorikk: Sykdomshistorikk
    internal lateinit var sykdomstidslinje: Sykdomstidslinje
    internal var låstePerioder = emptyList<Periode>()
    internal val dagtelling = mutableMapOf<KClass<out Dag>, Int>()
    internal val inntekter = mutableListOf<Inntekthistorikk.Inntektsendring>()
    internal lateinit var utbetalinger: List<Utbetaling>
    internal val arbeidsgiverOppdrag = mutableListOf<Oppdrag>()
    internal val totalBeløp = mutableListOf<Int>()
    internal val nettoBeløp = mutableListOf<Int>()
    private val utbetalingstidslinjer = mutableMapOf<UUID, Utbetalingstidslinje>()
    private val vedtaksperioder = mutableMapOf<Int, Vedtaksperiode>()
    private var inGyldigePerioder = false
    private var inVedtaksperiode = false
    private val forlengelserFraInfotrygd = mutableMapOf<Int, ForlengelseFraInfotrygd>()
    private val periodeIder = mutableMapOf<Int, UUID>()
    private val hendelseIder = mutableMapOf<UUID, List<UUID>>()

    init {
        HentAktivitetslogg(person, orgnummer).also { results ->
            personLogg = results.aktivitetslogg
            results.arbeidsgiver.accept(this)
        }
    }

    private class HentAktivitetslogg(person: Person, private val valgfriOrgnummer: String?) : PersonVisitor {
        internal lateinit var aktivitetslogg: Aktivitetslogg
        internal lateinit var arbeidsgiver: Arbeidsgiver

        init {
            person.accept(this)
        }

        override fun visitPersonAktivitetslogg(aktivitetslogg: Aktivitetslogg) {
            this.aktivitetslogg = aktivitetslogg
        }

        override fun preVisitArbeidsgiver(arbeidsgiver: Arbeidsgiver, id: UUID, organisasjonsnummer: String) {
            if (organisasjonsnummer == valgfriOrgnummer) this.arbeidsgiver = arbeidsgiver
            if (this::arbeidsgiver.isInitialized) return
            this.arbeidsgiver = arbeidsgiver
        }
    }

    internal fun vedtaksperiodeId(index: Int) = requireNotNull(vedtaksperiodeIder[index])
    internal fun forkastetVedtaksperiodeId(index: Int) = requireNotNull(forkastedePerioderIder[index])

    internal fun periodeErForkastet(id: UUID) = forkastedePerioderIder.containsValue(id)
    internal fun periodeErIkkeForkastet(id: UUID) = vedtaksperiodeIder.containsValue(id)

    override fun preVisitArbeidsgiver(
        arbeidsgiver: Arbeidsgiver,
        id: UUID,
        organisasjonsnummer: String
    ) {
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
        arbeidsgiverNettoBeløp: Int,
        personNettoBeløp: Int,
        periode: Periode,
        opprinneligPeriode: Periode,
        hendelseIder: List<UUID>
    ) {
        inVedtaksperiode = true
        vedtaksperiodeTeller += 1
        vedtaksperiodeindeks += 1
        periodeIder[vedtaksperiodeindeks] = id
        this.hendelseIder[id] = hendelseIder

        if (!inGyldigePerioder) return
        vedtaksperioder[vedtaksperiodeindeks] = vedtaksperiode
    }

    override fun preVisit(tidslinje: Utbetalingstidslinje) {
        if (inVedtaksperiode) utbetalingstidslinjer[periodeIder.getValue(vedtaksperiodeindeks)] = tidslinje
    }

    override fun visitForlengelseFraInfotrygd(forlengelseFraInfotrygd: ForlengelseFraInfotrygd) {
        if (!inGyldigePerioder) return
        forlengelserFraInfotrygd[vedtaksperiodeindeks] = forlengelseFraInfotrygd
    }

    override fun postVisitVedtaksperiode(
        vedtaksperiode: Vedtaksperiode,
        id: UUID,
        arbeidsgiverNettoBeløp: Int,
        personNettoBeløp: Int,
        periode: Periode,
        opprinneligPeriode: Periode
    ) {
        inVedtaksperiode = false
    }

    override fun preVisitUtbetalinger(utbetalinger: List<Utbetaling>) {
        this.utbetalinger = utbetalinger
    }

    override fun preVisitArbeidsgiverOppdrag(oppdrag: Oppdrag) {
        arbeidsgiverOppdrag.add(oppdrag)
    }

    override fun preVisitOppdrag(oppdrag: Oppdrag, totalBeløp: Int, nettoBeløp: Int) {
        if (oppdrag != arbeidsgiverOppdrag.last()) return
        this.totalBeløp.add(totalBeløp)
        this.nettoBeløp.add(nettoBeløp)
    }

    internal fun etterspurteBehov(vedtaksperiodeId: UUID, behovtype: Aktivitetslogg.Aktivitet.Behov.Behovtype) =
        personLogg.etterspurteBehovFinnes(vedtaksperiodeId, behovtype)

    override fun visitFørsteFraværsdag(førsteFraværsdag: LocalDate?) {
        if (!inGyldigePerioder || førsteFraværsdag == null) return
        førsteFraværsdager[vedtaksperiodeindeks] = førsteFraværsdag
    }

    override fun visitMaksdato(maksdato: LocalDate?) {
        if (maksdato == null) return
        if (!inGyldigePerioder) forkastetMaksdatoer[vedtaksperiodeindeks] = maksdato
        else maksdatoer[vedtaksperiodeindeks] = maksdato
    }

    override fun visitGjenståendeSykedager(gjenståendeSykedager: Int?) {
        if (gjenståendeSykedager == null) return
        if (!inGyldigePerioder) forkastetGjenståendeSykedagerer[vedtaksperiodeindeks] = gjenståendeSykedager
        else gjenståendeSykedagerer[vedtaksperiodeindeks] = gjenståendeSykedager
    }

    override fun preVisitInntekthistorikk(inntekthistorikk: Inntekthistorikk) {
        this.inntektshistorikk = inntekthistorikk
    }

    override fun visitInntekt(inntektsendring: Inntekthistorikk.Inntektsendring, id: UUID) {
        inntekter.add(inntektsendring)
    }

    override fun preVisitSykdomshistorikk(sykdomshistorikk: Sykdomshistorikk) {
        if (inVedtaksperiode) return
        this.sykdomshistorikk = sykdomshistorikk
        if (!sykdomshistorikk.isEmpty()) {
            sykdomstidslinje = sykdomshistorikk.sykdomstidslinje()
            this.sykdomshistorikk.sykdomstidslinje().accept(Dagteller())
        }
        lagreLås(sykdomshistorikk)
    }

    private inner class LåsInspektør : SykdomstidslinjeVisitor {
        override fun preVisitSykdomstidslinje(tidslinje: Sykdomstidslinje, låstePerioder: List<Periode>) {
            this@TestArbeidsgiverInspektør.låstePerioder = låstePerioder
        }
    }

    private fun lagreLås(sykdomshistorikk: Sykdomshistorikk) {
        if (!sykdomshistorikk.isEmpty()) sykdomshistorikk.sykdomstidslinje().accept(LåsInspektør())
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
        override fun visitDag(dag: ProblemDag, dato: LocalDate, kilde: Hendelseskilde, melding: String) =
            inkrementer(dag)

        private fun inkrementer(klasse: Dag) {
            dagtelling.compute(klasse::class) { _, value -> 1 + (value ?: 0) }
        }
    }

    internal fun forkastetMaksdato(indeks: Int) = forkastetMaksdatoer[indeks] ?: fail {
        "Missing collection initialization"
    }

    internal fun maksdato(vedtaksperiodeId: UUID) = maksdatoer[vedtaksperiodeIder.entries.associate { (key, value) -> value to key }[vedtaksperiodeId]] ?: fail {
        "Missing collection initialization"
    }

    internal fun gjenståendeSykedager(indeks: Int) = gjenståendeSykedagerer[indeks] ?: fail {
        "Missing collection initialization"
    }

    internal fun forkastetGjenståendeSykedager(indeks: Int) = forkastetGjenståendeSykedagerer[indeks] ?: fail {
        "Missing collection initialization"
    }

    internal fun forlengelseFraInfotrygd(indeks: Int) = forlengelserFraInfotrygd[indeks] ?: fail {
        "Missing collection initialization"
    }

    internal fun vilkårsgrunnlag(indeks: Int) = vilkårsgrunnlag[indeks] ?: fail {
        "Missing collection initialization"
    }

    internal fun utbetalingslinjer(indeks: Int) = arbeidsgiverOppdrag[indeks]

    internal fun sisteTilstand(indeks: Int) = tilstander[indeks] ?: fail {
        "Missing collection initialization"
    }

    internal fun sisteForkastetTilstand(indeks: Int) = forkastedeTilstander[indeks] ?: fail {
        "Missing collection initialization"
    }

    internal fun førsteFraværsdag(indeks: Int) = førsteFraværsdager[indeks] ?: fail {
        "Missing collection initialization"
    }

    internal fun utbetalingstidslinjer(indeks: Int) = utbetalingstidslinjer(vedtaksperiodeId(indeks))

    internal fun utbetalingstidslinjer(vedtaksperiodeId: UUID) = utbetalingstidslinjer[vedtaksperiodeId] ?: fail {
        "Missing collection initialization"
    }

    internal fun vedtaksperioder(indeks: Int) = vedtaksperioder[indeks] ?: fail {
        "Missing collection initialization"
    }

    internal fun hendelseIder(vedtaksperiodeId: UUID) = hendelseIder[vedtaksperiodeId] ?: fail {
        "Missing collection initialization"
    }

    internal fun dagTeller(klasse: KClass<out Utbetalingstidslinje.Utbetalingsdag>) =
        TestTidslinjeInspektør(arbeidsgiver.nåværendeTidslinje()).dagtelling[klasse] ?: 0
}

