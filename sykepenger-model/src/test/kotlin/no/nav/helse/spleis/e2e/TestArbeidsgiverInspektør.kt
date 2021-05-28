package no.nav.helse.spleis.e2e

import no.nav.helse.etterspurteBehov
import no.nav.helse.etterspurteBehovFinnes
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.*
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Dag.*
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse.Hendelseskilde
import no.nav.helse.utbetalingslinjer.*
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Økonomi
import org.junit.jupiter.api.fail
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.reflect.KClass

internal class TestArbeidsgiverInspektør(
    private val person: Person,
    orgnummer: String? = null
) : ArbeidsgiverVisitor {
    internal var vedtaksperiodeTeller: Int = 0
        private set
    private var vedtaksperiodeindeks = 0
    private val tilstander = mutableMapOf<Int, TilstandType>()
    private val perioder = mutableMapOf<Int, Periode>()
    private val skjæringstidspunkter = mutableMapOf<Int, LocalDate>()
    private val maksdatoer = mutableListOf<LocalDate>()
    private val forbrukteSykedagerer = mutableListOf<Int?>()
    private val gjenståendeSykedagerer = mutableListOf<Int?>()
    private val vedtaksperiodeindekser = mutableMapOf<UUID, Int>()
    private val fagsystemIder = mutableMapOf<Int, String>()
    private val vedtaksperiodeForkastet = mutableMapOf<Int, Boolean>()
    private val vilkårsgrunnlag = mutableMapOf<Int, VilkårsgrunnlagHistorikk.Grunnlagsdata?>()
    internal val personLogg: Aktivitetslogg
    internal lateinit var arbeidsgiver: Arbeidsgiver
    internal val inntektInspektør get() = InntektshistorikkInspektør(arbeidsgiver)
    internal lateinit var sykdomshistorikk: Sykdomshistorikk
    internal lateinit var sykdomstidslinje: Sykdomstidslinje
    internal var låstePerioder = emptyList<Periode>()
    internal val sykdomshistorikkDagTeller = mutableMapOf<KClass<out Dag>, Int>()
    internal val vedtaksperiodeDagTeller = mutableMapOf<UUID, MutableMap<KClass<out Dag>, Int>>()
    internal val utbetalinger = mutableListOf<Utbetaling>()
    internal val feriepengeutbetalingslinjer = mutableListOf<Feriepengeutbetalingslinje>()
    internal val infotrygdFeriepengebeløpPerson = mutableListOf<Double>()
    internal val infotrygdFeriepengebeløpArbeidsgiver = mutableListOf<Double>()
    internal val spleisFeriepengebeløpArbeidsgiver = mutableListOf<Double>()
    private val vedtaksperiodeutbetalinger = mutableMapOf<Int, Int>()
    private val utbetalingstilstander = mutableListOf<Utbetaling.Tilstand>()
    private val utbetalingIder = mutableListOf<UUID>()
    private val utbetalingutbetalingstidslinjer = mutableListOf<Utbetalingstidslinje>()
    internal val arbeidsgiverOppdrag = mutableListOf<Oppdrag>()
    internal val totalBeløp = mutableListOf<Int>()
    internal val nettoBeløp = mutableListOf<Int>()
    private val utbetalingstidslinjer = mutableMapOf<Int, Utbetalingstidslinje>()
    private val vedtaksperioder = mutableMapOf<Int, Vedtaksperiode>()
    private var forkastetPeriode = false
    private var inVedtaksperiode = false
    private var inUtbetaling = false
    private var inFeriepengeutbetaling = false
    private val forlengelserFraInfotrygd = mutableMapOf<Int, ForlengelseFraInfotrygd>()
    private val hendelseIder = mutableMapOf<Int, List<UUID>>()
    private val inntektskilder = mutableMapOf<Int, Inntektskilde>()

    init {
        HentAktivitetslogg(person, orgnummer).also { results ->
            personLogg = results.aktivitetslogg
            results.arbeidsgiver.accept(this)
        }
    }

    private class HentAktivitetslogg(person: Person, private val valgfriOrgnummer: String?) : PersonVisitor {
        lateinit var aktivitetslogg: Aktivitetslogg
        lateinit var arbeidsgiver: Arbeidsgiver

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

    override fun preVisitArbeidsgiver(
        arbeidsgiver: Arbeidsgiver,
        id: UUID,
        organisasjonsnummer: String
    ) {
        this.arbeidsgiver = arbeidsgiver
    }

    override fun preVisitForkastedePerioder(vedtaksperioder: List<ForkastetVedtaksperiode>) {
        forkastetPeriode = true
    }

    override fun postVisitForkastedePerioder(vedtaksperioder: List<ForkastetVedtaksperiode>) {
        forkastetPeriode = false
    }

    override fun preVisitVedtaksperiode(
        vedtaksperiode: Vedtaksperiode,
        id: UUID,
        tilstand: Vedtaksperiode.Vedtaksperiodetilstand,
        opprettet: LocalDateTime,
        oppdatert: LocalDateTime,
        periode: Periode,
        opprinneligPeriode: Periode,
        skjæringstidspunkt: LocalDate,
        periodetype: Periodetype,
        forlengelseFraInfotrygd: ForlengelseFraInfotrygd,
        hendelseIder: List<UUID>,
        inntektsmeldingInfo: InntektsmeldingInfo?,
        inntektskilde: Inntektskilde
    ) {
        inVedtaksperiode = true
        vedtaksperiodeTeller += 1
        vedtaksperiodeindekser[id] = vedtaksperiodeindeks
        vedtaksperiodeForkastet[vedtaksperiodeindeks] = forkastetPeriode
        vedtaksperioder[vedtaksperiodeindeks] = vedtaksperiode
        this.hendelseIder[vedtaksperiodeindeks] = hendelseIder
        perioder[vedtaksperiodeindeks] = periode
        tilstander[vedtaksperiodeindeks] = tilstand.type
        inntektskilder[vedtaksperiodeindeks] = inntektskilde
        skjæringstidspunkter[vedtaksperiodeindeks] = skjæringstidspunkt
        forlengelserFraInfotrygd[vedtaksperiodeindeks] = forlengelseFraInfotrygd
        vedtaksperiode.accept(VedtaksperiodeDagTeller())
    }

    override fun preVisit(tidslinje: Utbetalingstidslinje) {
        if (inVedtaksperiode && !inUtbetaling) utbetalingstidslinjer[vedtaksperiodeindeks] = tidslinje
        else if (!inVedtaksperiode && inUtbetaling) utbetalingutbetalingstidslinjer.add(tidslinje)
    }

    override fun postVisitVedtaksperiode(
        vedtaksperiode: Vedtaksperiode,
        id: UUID,
        tilstand: Vedtaksperiode.Vedtaksperiodetilstand,
        opprettet: LocalDateTime,
        oppdatert: LocalDateTime,
        periode: Periode,
        opprinneligPeriode: Periode,
        skjæringstidspunkt: LocalDate,
        periodetype: Periodetype,
        forlengelseFraInfotrygd: ForlengelseFraInfotrygd,
        hendelseIder: List<UUID>,
        inntektsmeldingInfo: InntektsmeldingInfo?,
        inntektskilde: Inntektskilde
    ) {
        vedtaksperiodeindeks += 1
        inVedtaksperiode = false
    }

    override fun preVisitUtbetalinger(utbetalinger: List<Utbetaling>) {
        this.utbetalinger.addAll(utbetalinger)
    }

    override fun preVisitArbeidsgiverOppdrag(oppdrag: Oppdrag) {
        if (!inVedtaksperiode) arbeidsgiverOppdrag.add(oppdrag)
        if (inVedtaksperiode) fagsystemIder[vedtaksperiodeindeks] = oppdrag.fagsystemId()
    }

    override fun preVisitOppdrag(
        oppdrag: Oppdrag,
        totalBeløp: Int,
        nettoBeløp: Int,
        tidsstempel: LocalDateTime
    ) {
        if (oppdrag != arbeidsgiverOppdrag.lastOrNull()) return
        this.totalBeløp.add(totalBeløp)
        this.nettoBeløp.add(nettoBeløp)
    }

    override fun visitUtbetalingslinje(
        linje: Utbetalingslinje,
        fom: LocalDate,
        tom: LocalDate,
        satstype: Satstype,
        beløp: Int?,
        aktuellDagsinntekt: Int?,
        grad: Double?,
        delytelseId: Int,
        refDelytelseId: Int?,
        refFagsystemId: String?,
        endringskode: Endringskode,
        datoStatusFom: LocalDate?,
        klassekode: Klassekode
    ) {
        if(inFeriepengeutbetaling) feriepengeutbetalingslinjer.add(Feriepengeutbetalingslinje(fom, tom, satstype, beløp, grad, klassekode, endringskode))
    }

    internal data class Feriepengeutbetalingslinje(
        val fom: LocalDate,
        val tom: LocalDate,
        val satstype: Satstype,
        val beløp: Int?,
        val grad: Double?,
        val klassekode: Klassekode,
        val endringskode: Endringskode
    )

    override fun preVisitUtbetaling(
        utbetaling: Utbetaling,
        id: UUID,
        beregningId: UUID,
        type: Utbetaling.Utbetalingtype,
        tilstand: Utbetaling.Tilstand,
        tidsstempel: LocalDateTime,
        oppdatert: LocalDateTime,
        arbeidsgiverNettoBeløp: Int,
        personNettoBeløp: Int,
        maksdato: LocalDate,
        forbrukteSykedager: Int?,
        gjenståendeSykedager: Int?
    ) {
        inUtbetaling = true
        if (!inVedtaksperiode) {
            maksdatoer.add(maksdato)
            utbetalingIder.add(id)
            utbetalingstilstander.add(tilstand)
            forbrukteSykedagerer.add(forbrukteSykedager)
            gjenståendeSykedagerer.add(gjenståendeSykedager)
        } else {
            vedtaksperiodeutbetalinger[vedtaksperiodeindeks] = utbetalinger.indexOf(utbetaling)
        }
    }

    override fun postVisitUtbetaling(
        utbetaling: Utbetaling,
        id: UUID,
        beregningId: UUID,
        type: Utbetaling.Utbetalingtype,
        tilstand: Utbetaling.Tilstand,
        tidsstempel: LocalDateTime,
        oppdatert: LocalDateTime,
        arbeidsgiverNettoBeløp: Int,
        personNettoBeløp: Int,
        maksdato: LocalDate,
        forbrukteSykedager: Int?,
        gjenståendeSykedager: Int?
    ) {
        inUtbetaling = false
    }

    override fun preVisitFeriepengeutbetaling(
        feriepengeutbetaling: Feriepengeutbetaling,
        infotrygdFeriepengebeløpPerson: Double,
        infotrygdFeriepengebeløpArbeidsgiver: Double,
        spleisFeriepengebeløpArbeidsgiver: Double,
        overføringstidspunkt: LocalDateTime?,
        avstemmingsnøkkel: Long?,
        utbetalingId: UUID,
    ) {
        inFeriepengeutbetaling = true
        this.infotrygdFeriepengebeløpArbeidsgiver.add(infotrygdFeriepengebeløpArbeidsgiver)
        this.infotrygdFeriepengebeløpPerson.add(infotrygdFeriepengebeløpPerson)
        this.spleisFeriepengebeløpArbeidsgiver.add(spleisFeriepengebeløpArbeidsgiver)
    }

    override fun postVisitFeriepengeutbetaling(
        feriepengeutbetaling: Feriepengeutbetaling,
        infotrygdFeriepengebeløpPerson: Double,
        infotrygdFeriepengebeløpArbeidsgiver: Double,
        spleisFeriepengebeløpArbeidsgiver: Double,
        overføringstidspunkt: LocalDateTime?,
        avstemmingsnøkkel: Long?,
    ) {
        inFeriepengeutbetaling = false
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

    override fun visitDataForVilkårsvurdering(dataForVilkårsvurdering: VilkårsgrunnlagHistorikk.Grunnlagsdata?) {
        vilkårsgrunnlag[vedtaksperiodeindeks] = dataForVilkårsvurdering
    }



    private inner class VedtaksperiodeDagTeller: VedtaksperiodeVisitor {
        private lateinit var vedtaksperiodeId: UUID
        private val telling = mutableMapOf<KClass<out Dag>, Int>()
        override fun preVisitVedtaksperiode(
            vedtaksperiode: Vedtaksperiode,
            id: UUID,
            tilstand: Vedtaksperiode.Vedtaksperiodetilstand,
            opprettet: LocalDateTime,
            oppdatert: LocalDateTime,
            periode: Periode,
            opprinneligPeriode: Periode,
            skjæringstidspunkt: LocalDate,
            periodetype: Periodetype,
            forlengelseFraInfotrygd: ForlengelseFraInfotrygd,
            hendelseIder: List<UUID>,
            inntektsmeldingInfo: InntektsmeldingInfo?,
            inntektskilde: Inntektskilde
        ) {
            vedtaksperiodeId = id
        }

        override fun postVisitVedtaksperiode(
            vedtaksperiode: Vedtaksperiode,
            id: UUID,
            tilstand: Vedtaksperiode.Vedtaksperiodetilstand,
            opprettet: LocalDateTime,
            oppdatert: LocalDateTime,
            periode: Periode,
            opprinneligPeriode: Periode,
            skjæringstidspunkt: LocalDate,
            periodetype: Periodetype,
            forlengelseFraInfotrygd: ForlengelseFraInfotrygd,
            hendelseIder: List<UUID>,
            inntektsmeldingInfo: InntektsmeldingInfo?,
            inntektskilde: Inntektskilde
        ) {
            vedtaksperiodeDagTeller[id] = telling
        }

        override fun preVisitSykdomstidslinje(tidslinje: Sykdomstidslinje, låstePerioder: List<Periode>) {
            tidslinje.accept(Dagteller(telling))
        }
    }

    private inner class Dagteller(private val akkumulator: MutableMap<KClass<out Dag>, Int> = sykdomshistorikkDagTeller) : SykdomstidslinjeVisitor {
        override fun visitDag(dag: UkjentDag, dato: LocalDate, kilde: Hendelseskilde) =
            inkrementer(dag, akkumulator)
        override fun visitDag(dag: Arbeidsdag, dato: LocalDate, kilde: Hendelseskilde) = inkrementer(dag, akkumulator)
        override fun visitDag(
            dag: Arbeidsgiverdag,
            dato: LocalDate,
            økonomi: Økonomi,
            kilde: Hendelseskilde
        ) = inkrementer(dag, akkumulator)
        override fun visitDag(dag: Feriedag, dato: LocalDate, kilde: Hendelseskilde) = inkrementer(dag, akkumulator)

        override fun visitDag(dag: FriskHelgedag, dato: LocalDate, kilde: Hendelseskilde) = inkrementer(dag, akkumulator)
        override fun visitDag(
            dag: ArbeidsgiverHelgedag,
            dato: LocalDate,
            økonomi: Økonomi,
            kilde: Hendelseskilde
        ) = inkrementer(dag, akkumulator)
        override fun visitDag(
            dag: Sykedag,
            dato: LocalDate,
            økonomi: Økonomi,
            kilde: Hendelseskilde
        ) = inkrementer(dag, akkumulator)

        override fun visitDag(
            dag: ForeldetSykedag,
            dato: LocalDate,
            økonomi: Økonomi,
            kilde: Hendelseskilde
        ) = inkrementer(dag, akkumulator)

        override fun visitDag(
            dag: SykHelgedag,
            dato: LocalDate,
            økonomi: Økonomi,
            kilde: Hendelseskilde
        ) = inkrementer(dag, akkumulator)

        override fun visitDag(dag: Permisjonsdag, dato: LocalDate, kilde: Hendelseskilde) = inkrementer(dag, akkumulator)

        override fun visitDag(dag: ProblemDag, dato: LocalDate, kilde: Hendelseskilde, melding: String) =
            inkrementer(dag, akkumulator)

        private fun inkrementer(klasse: Dag, akkumulator: MutableMap<KClass<out Dag>, Int>) {
            akkumulator.compute(klasse::class) { _, value -> 1 + (value ?: 0) }
        }
    }

    private fun <V> UUID.finn(hva: Map<Int, V>) = hva.getValue(this.indeks)
    private val UUID.indeks get() = vedtaksperiodeindekser[this] ?: fail { "Vedtaksperiode $this finnes ikke" }
    private val UUID.utbetalingsindeks get() = this.finn(vedtaksperiodeutbetalinger)

    internal fun vedtaksperiodeutbetaling(id: UUID) = utbetalinger[id.utbetalingsindeks]
    internal fun utbetalingtilstand(indeks: Int) = utbetalingstilstander[indeks]
    internal fun utbetaling(indeks: Int) = utbetalinger[indeks]
    internal fun utbetalingId(indeks: Int) = utbetalingIder[indeks]
    internal fun utbetalingUtbetalingstidslinje(indeks: Int) = utbetalingutbetalingstidslinjer[indeks]
    internal fun periode(id: UUID) = id.finn(perioder)

    internal fun periodeErForkastet(id: UUID) = id.finn(vedtaksperiodeForkastet)

    internal fun periodeErIkkeForkastet(id: UUID) = !periodeErForkastet(id)

    internal fun etterspurteBehov(vedtaksperiodeId: UUID, behovtype: Aktivitetslogg.Aktivitet.Behov.Behovtype) =
        personLogg.etterspurteBehovFinnes(vedtaksperiodeId, behovtype)

    internal fun etterspurteBehov(vedtaksperiodeId: UUID, tilstand: TilstandType, behovtype: Aktivitetslogg.Aktivitet.Behov.Behovtype) =
        personLogg.etterspurteBehovFinnes(vedtaksperiodeId, tilstand, behovtype)

    internal fun etterspurteBehov(vedtaksperiodeId: UUID) =
        personLogg.etterspurteBehov(vedtaksperiodeId)

    internal fun sisteBehov(id: UUID) =
        personLogg.behov().last { it.kontekst()["vedtaksperiodeId"] == id.toString() }

    internal fun sisteBehov(type: Aktivitetslogg.Aktivitet.Behov.Behovtype) =
        personLogg.behov().last { it.type == type }

    internal fun maksdato(indeks: Int) = maksdatoer[indeks]
    internal fun maksdato(id: UUID) = maksdatoer[id.utbetalingsindeks]

    internal fun forbrukteSykedager(indeks: Int) = forbrukteSykedagerer[indeks]
    internal fun gjenståendeSykedager(indeks: Int) = gjenståendeSykedagerer[indeks]

    internal fun gjenståendeSykedager(id: UUID) = gjenståendeSykedagerer[id.utbetalingsindeks] ?: fail {
        "Vedtaksperiode $id har ikke oppgitt gjenstående sykedager"
    }

    internal fun forlengelseFraInfotrygd(id: UUID) = id.finn(forlengelserFraInfotrygd)

    internal fun vilkårsgrunnlag(id: UUID) = person.vilkårsgrunnlagHistorikk.vilkårsgrunnlagFor(skjæringstidspunkt(id))

    internal fun utbetalingslinjer(indeks: Int) = arbeidsgiverOppdrag[indeks]

    internal fun sisteTilstand(id: UUID) = id.finn(tilstander)

    internal fun skjæringstidspunkt(id: UUID) = id.finn(skjæringstidspunkter)

    internal fun utbetalingstidslinjer(id: UUID) = id.finn(utbetalingstidslinjer)

    internal fun vedtaksperioder(id: UUID) = id.finn(vedtaksperioder)

    internal fun hendelseIder(id: UUID) = id.finn(hendelseIder)

    internal fun fagsystemId(id: UUID) = id.finn(fagsystemIder)

    internal fun inntektskilde(id: UUID) = id.finn(inntektskilder)
}
