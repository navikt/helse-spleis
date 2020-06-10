package no.nav.helse.person

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Simulering
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.person.Vedtaksperiode.Vedtaksperiodetilstand
import no.nav.helse.sykdomstidslinje.Dag.*
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse.Hendelseskilde
import no.nav.helse.utbetalingslinjer.Endringskode
import no.nav.helse.utbetalingslinjer.Oppdrag
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingslinjer.Utbetalingslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Prosentdel
import no.nav.helse.økonomi.Økonomi
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal interface PersonVisitor : ArbeidsgiverVisitor, AktivitetsloggVisitor {
    fun preVisitPerson(person: Person, aktørId: String, fødselsnummer: String) {}
    fun visitPersonAktivitetslogg(aktivitetslogg: Aktivitetslogg) {}
    fun preVisitArbeidsgivere() {}
    fun postVisitArbeidsgivere() {}
    fun postVisitPerson(person: Person, aktørId: String, fødselsnummer: String) {}
}

internal interface ArbeidsgiverVisitor : InntekthistorikkVisitor, VedtaksperiodeVisitor, UtbetalingVisitor {
    fun preVisitArbeidsgiver(
        arbeidsgiver: Arbeidsgiver,
        id: UUID,
        organisasjonsnummer: String
    ) {}
    fun preVisitUtbetalinger(utbetalinger: List<Utbetaling>) {}
    fun postVisitUtbetalinger(utbetalinger: List<Utbetaling>) {}
    fun preVisitPerioder(vedtaksperioder: List<Vedtaksperiode>) {}
    fun postVisitPerioder(vedtaksperioder: List<Vedtaksperiode>) {}
    fun preVisitForkastedePerioder(vedtaksperioder: List<Vedtaksperiode>) {}
    fun postVisitForkastedePerioder(vedtaksperioder: List<Vedtaksperiode>) {}
    fun postVisitArbeidsgiver(
        arbeidsgiver: Arbeidsgiver,
        id: UUID,
        organisasjonsnummer: String
    ) {}
}

internal interface VedtaksperiodeVisitor : SykdomshistorikkVisitor, UtbetalingsdagVisitor {
    fun preVisitVedtaksperiode(vedtaksperiode: Vedtaksperiode, id: UUID, gruppeId: UUID, arbeidsgiverNettoBeløp: Int, personNettoBeløp: Int, periode: Periode) {}
    fun visitMaksdato(maksdato: LocalDate?) {}
    fun visitGjenståendeSykedager(gjenståendeSykedager: Int?) {}
    fun visitForbrukteSykedager(forbrukteSykedager: Int?) {}
    fun visitArbeidsgiverFagsystemId(fagsystemId: String?) {}
    fun visitPersonFagsystemId(fagsystemId: String?) {}
    fun visitGodkjentAv(godkjentAv: String?) {}
    fun visitFørsteFraværsdag(førsteFraværsdag: LocalDate?) {}
    fun visitDataForVilkårsvurdering(dataForVilkårsvurdering: Vilkårsgrunnlag.Grunnlagsdata?) {}
    fun visitDataForSimulering(dataForSimuleringResultat: Simulering.SimuleringResultat?) {}
    fun visitTilstand(tilstand: Vedtaksperiodetilstand) {}
    fun visitForlengelseFraInfotrygd(forlengelseFraInfotrygd: ForlengelseFraInfotrygd) {}
    fun postVisitVedtaksperiode(vedtaksperiode: Vedtaksperiode, id: UUID, gruppeId: UUID, arbeidsgiverNettoBeløp: Int, personNettoBeløp: Int, periode: Periode) {}
}

internal interface UtbetalingsdagVisitor {
    fun preVisit(tidslinje: Utbetalingstidslinje) {}
    fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.ArbeidsgiverperiodeDag,
        dato: LocalDate,
        økonomi: Økonomi,
        aktuellDagsinntekt: Double
    ) {}
    fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.NavDag,
        dato: LocalDate,
        økonomi: Økonomi,
        grad: Prosentdel,
        aktuellDagsinntekt: Double,
        dekningsgrunnlag: Double,
        arbeidsgiverbeløp: Int,
        personbeløp: Int
    ) {}
    fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.NavHelgDag,
        dato: LocalDate,
        økonomi: Økonomi,
        grad: Prosentdel
    ) {}
    fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.Arbeidsdag,
        dato: LocalDate,
        økonomi: Økonomi,
        aktuellDagsinntekt: Double
    ) {}
    fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.Fridag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {}
    fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.AvvistDag,
        dato: LocalDate,
        økonomi: Økonomi,
        grad: Prosentdel,
        aktuellDagsinntekt: Double,
        dekningsgrunnlag: Double,
        arbeidsgiverbeløp: Int,
        personbeløp: Int
    ) {}
    fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.ForeldetDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {}
    fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.UkjentDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {}
    fun postVisit(tidslinje: Utbetalingstidslinje) {}
}

internal interface SykdomshistorikkVisitor : SykdomstidslinjeVisitor {
    fun preVisitSykdomshistorikk(sykdomshistorikk: Sykdomshistorikk) {}
    fun preVisitSykdomshistorikkElement(
        element: Sykdomshistorikk.Element,
        id: UUID?,
        tidsstempel: LocalDateTime
    ) {}
    fun preVisitHendelseSykdomstidslinje(tidslinje: Sykdomstidslinje) {}
    fun postVisitHendelseSykdomstidslinje(tidslinje: Sykdomstidslinje) {}
    fun preVisitBeregnetSykdomstidslinje(tidslinje: Sykdomstidslinje) {}
    fun postVisitBeregnetSykdomstidslinje(tidslinje: Sykdomstidslinje) {}
    fun postVisitSykdomshistorikkElement(
        element: Sykdomshistorikk.Element,
        id: UUID?,
        tidsstempel: LocalDateTime
    ) {}
    fun postVisitSykdomshistorikk(sykdomshistorikk: Sykdomshistorikk) {}
}

internal interface SykdomstidslinjeVisitor {
    fun preVisitSykdomstidslinje(tidslinje: Sykdomstidslinje, låstePerioder: List<Periode>, id: UUID, tidsstempel: LocalDateTime) {}
    fun visitDag(dag: UkjentDag, dato: LocalDate, kilde: Hendelseskilde) {}
    fun visitDag(dag: Arbeidsdag, dato: LocalDate, kilde: Hendelseskilde) {}
    fun visitDag(
        dag: Arbeidsgiverdag,
        dato: LocalDate,
        økonomi: Økonomi,
        grad: Prosentdel,
        arbeidsgiverBetalingProsent: Prosentdel,
        kilde: Hendelseskilde
    ) {}
    fun visitDag(dag: Feriedag, dato: LocalDate, kilde: Hendelseskilde) {}
    fun visitDag(dag: FriskHelgedag, dato: LocalDate, kilde: Hendelseskilde) {}
    fun visitDag(
        dag: ArbeidsgiverHelgedag,
        dato: LocalDate,
        økonomi: Økonomi,
        grad: Prosentdel,
        arbeidsgiverBetalingProsent: Prosentdel,
        kilde: Hendelseskilde
    ) {}
    fun visitDag(
        dag: Sykedag,
        dato: LocalDate,
        økonomi: Økonomi,
        grad: Prosentdel,
        arbeidsgiverBetalingProsent: Prosentdel,
        kilde: Hendelseskilde
    ) {}
    fun visitDag(
        dag: ForeldetSykedag,
        dato: LocalDate,
        økonomi: Økonomi,
        grad: Prosentdel,
        arbeidsgiverBetalingProsent: Prosentdel,
        kilde: Hendelseskilde
    ) {}
    fun visitDag(
        dag: SykHelgedag,
        dato: LocalDate,
        økonomi: Økonomi,
        grad: Prosentdel,
        arbeidsgiverBetalingProsent: Prosentdel,
        kilde: Hendelseskilde
    ) {}
    fun visitDag(dag: Permisjonsdag, dato: LocalDate, kilde: Hendelseskilde) {}
    fun visitDag(dag: Studiedag, dato: LocalDate, kilde: Hendelseskilde) {}
    fun visitDag(dag: Utenlandsdag, dato: LocalDate, kilde: Hendelseskilde) {}
    fun visitDag(dag: ProblemDag, dato: LocalDate, kilde: Hendelseskilde, melding: String) {}
    fun postVisitSykdomstidslinje(tidslinje: Sykdomstidslinje, id: UUID, tidsstempel: LocalDateTime) {}
}

internal interface InntekthistorikkVisitor {
    fun preVisitInntekthistorikk(inntekthistorikk: Inntekthistorikk) {}
//    fun preVisitTidslinjer() {}
    fun visitInntekt(inntekt: Inntekthistorikk.Inntekt, id: UUID) {}
//    fun postVisitTidslinjer() {}
    fun postVisitInntekthistorikk(inntekthistorikk: Inntekthistorikk) {}
}

internal interface UtbetalingVisitor: UtbetalingsdagVisitor, OppdragVisitor {
    fun preVisitUtbetaling(utbetaling: Utbetaling, tidsstempel: LocalDateTime) {}
    fun preVisitTidslinjer(tidslinjer: MutableList<Utbetalingstidslinje>) {}
    fun postVisitTidslinjer(tidslinjer: MutableList<Utbetalingstidslinje>) {}
    fun preVisitArbeidsgiverOppdrag(oppdrag: Oppdrag) {}
    fun postVisitArbeidsgiverOppdrag(oppdrag: Oppdrag) {}
    fun preVisitPersonOppdrag(oppdrag: Oppdrag) {}
    fun postVisitPersonOppdrag(oppdrag: Oppdrag) {}
    fun postVisitUtbetaling(utbetaling: Utbetaling, tidsstempel: LocalDateTime) {}
}

internal interface OppdragVisitor {
    fun preVisitOppdrag(oppdrag: Oppdrag, totalBeløp: Int, nettoBeløp: Int) {}
    fun visitUtbetalingslinje(
        linje: Utbetalingslinje,
        fom: LocalDate,
        tom: LocalDate,
        beløp: Int,
        aktuellDagsinntekt: Int,
        grad: Double,
        delytelseId: Int,
        refDelytelseId: Int?,
        refFagsystemId: String?,
        endringskode: Endringskode,
        datoStatusFom: LocalDate?
    ) {}
    fun postVisitOppdrag(oppdrag: Oppdrag) {}
}
