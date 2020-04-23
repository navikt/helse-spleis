package no.nav.helse.person

import no.nav.helse.hendelser.Simulering
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.person.Vedtaksperiode.Vedtaksperiodetilstand
import no.nav.helse.sykdomstidslinje.Grad
import no.nav.helse.sykdomstidslinje.NyDag.*
import no.nav.helse.sykdomstidslinje.NySykdomstidslinje
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse.Hendelseskilde
import no.nav.helse.sykdomstidslinje.dag.*
import no.nav.helse.utbetalingslinjer.Endringskode
import no.nav.helse.utbetalingslinjer.Oppdrag
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingslinjer.Utbetalingslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
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
    fun preVisitPerioder() {}
    fun postVisitPerioder() {}
    fun postVisitArbeidsgiver(
        arbeidsgiver: Arbeidsgiver,
        id: UUID,
        organisasjonsnummer: String
    ) {}
}

internal interface VedtaksperiodeVisitor : SykdomshistorikkVisitor, UtbetalingsdagVisitor {
    fun preVisitVedtaksperiode(vedtaksperiode: Vedtaksperiode, id: UUID, gruppeId: UUID) {}
    fun visitMaksdato(maksdato: LocalDate?) {}
    fun visitForbrukteSykedager(forbrukteSykedager: Int?) {}
    fun visitGodkjentAv(godkjentAv: String?) {}
    fun visitFørsteFraværsdag(førsteFraværsdag: LocalDate?) {}
    fun visitDataForVilkårsvurdering(dataForVilkårsvurdering: Vilkårsgrunnlag.Grunnlagsdata?) {}
    fun visitDataForSimulering(dataForSimuleringResultat: Simulering.SimuleringResultat?) {}
    fun visitTilstand(tilstand: Vedtaksperiodetilstand) {}
    fun postVisitVedtaksperiode(vedtaksperiode: Vedtaksperiode, id: UUID, gruppeId: UUID) {}
}

internal interface UtbetalingsdagVisitor {
    fun preVisitUtbetalingstidslinje(tidslinje: Utbetalingstidslinje) {}
    fun visitArbeidsgiverperiodeDag(dag: Utbetalingstidslinje.Utbetalingsdag.ArbeidsgiverperiodeDag) {}
    fun visitNavDag(dag: Utbetalingstidslinje.Utbetalingsdag.NavDag) {}
    fun visitNavHelgDag(dag: Utbetalingstidslinje.Utbetalingsdag.NavHelgDag) {}
    fun visitArbeidsdag(dag: Utbetalingstidslinje.Utbetalingsdag.Arbeidsdag) {}
    fun visitFridag(dag: Utbetalingstidslinje.Utbetalingsdag.Fridag) {}
    fun visitAvvistDag(dag: Utbetalingstidslinje.Utbetalingsdag.AvvistDag) {}
    fun visitForeldetDag(dag: Utbetalingstidslinje.Utbetalingsdag.ForeldetDag) {}
    fun visitUkjentDag(dag: Utbetalingstidslinje.Utbetalingsdag.UkjentDag) {}
    fun postVisitUtbetalingstidslinje(tidslinje: Utbetalingstidslinje) {}
}

internal interface SykdomshistorikkVisitor : SykdomstidslinjeVisitor {
    fun preVisitSykdomshistorikk(sykdomshistorikk: Sykdomshistorikk) {}
    fun preVisitSykdomshistorikkElement(
        element: Sykdomshistorikk.Element,
        id: UUID,
        tidsstempel: LocalDateTime
    ) {}
    fun preVisitHendelseSykdomstidslinje(tidslinje: Sykdomstidslinje) {}
    fun postVisitHendelseSykdomstidslinje(tidslinje: Sykdomstidslinje) {}
    fun preVisitBeregnetSykdomstidslinje(tidslinje: Sykdomstidslinje) {}
    fun postVisitBeregnetSykdomstidslinje(tidslinje: Sykdomstidslinje) {}
    fun postVisitSykdomshistorikkElement(
        element: Sykdomshistorikk.Element,
        id: UUID,
        tidsstempel: LocalDateTime
    ) {}
    fun postVisitSykdomshistorikk(sykdomshistorikk: Sykdomshistorikk) {}
}

internal interface SykdomstidslinjeVisitor {
    fun preVisitSykdomstidslinje(tidslinje: Sykdomstidslinje) {}
    fun visitArbeidsdag(dag: Arbeidsdag.Inntektsmelding) {}
    fun visitArbeidsdag(dag: Arbeidsdag.Søknad) {}
    fun visitEgenmeldingsdag(dag: Egenmeldingsdag.Inntektsmelding) {}
    fun visitEgenmeldingsdag(dag: Egenmeldingsdag.Søknad) {}
    fun visitFeriedag(dag: Feriedag.Inntektsmelding) {}
    fun visitFeriedag(dag: Feriedag.Søknad) {}
    fun visitFriskHelgedag(dag: FriskHelgedag.Inntektsmelding) {}
    fun visitFriskHelgedag(dag: FriskHelgedag.Søknad) {}
    fun visitImplisittDag(dag: ImplisittDag) {}
    fun visitKunArbeidsgiverSykedag(dag: KunArbeidsgiverSykedag) {}
    fun visitPermisjonsdag(dag: Permisjonsdag.Søknad) {}
    fun visitPermisjonsdag(dag: Permisjonsdag.Aareg) {}
    fun visitStudiedag(dag: Studiedag) {}
    fun visitSykHelgedag(dag: SykHelgedag.Sykmelding) {}
    fun visitSykHelgedag(dag: SykHelgedag.Søknad) {}
    fun visitSykedag(dag: Sykedag.Sykmelding) {}
    fun visitSykedag(dag: Sykedag.Søknad) {}
    fun visitUbestemt(dag: Ubestemtdag) {}
    fun visitUtenlandsdag(dag: Utenlandsdag) {}
    fun postVisitSykdomstidslinje(tidslinje: Sykdomstidslinje) {}
}

internal interface NySykdomstidslinjeVisitor {
    fun preVisitNySykdomstidslinje(tidslinje: NySykdomstidslinje, id: UUID, tidsstempel: LocalDateTime) {}
    fun visitDag(dag: NyUkjentDag, dato: LocalDate, kilde: Hendelseskilde) {}
    fun visitDag(dag: NyArbeidsdag, dato: LocalDate, kilde: Hendelseskilde) {}
    fun visitDag(dag: NyArbeidsgiverdag, dato: LocalDate, grad: Grad, kilde: Hendelseskilde) {}
    fun visitDag(dag: NyFeriedag, dato: LocalDate, kilde: Hendelseskilde) {}
    fun visitDag(dag: NyFriskHelgedag, dato: LocalDate, kilde: Hendelseskilde) {}
    fun visitDag(dag: NyArbeidsgiverHelgedag, dato: LocalDate, grad: Grad, kilde: Hendelseskilde) {}
    fun visitDag(dag: NySykedag, dato: LocalDate, grad: Grad, kilde: Hendelseskilde) {}
    fun visitDag(dag: NyKunArbeidsgiverdag, dato: LocalDate, grad: Grad, kilde: Hendelseskilde) {}
    fun visitDag(dag: NySykHelgedag, dato: LocalDate, grad: Grad, kilde: Hendelseskilde) {}
//    fun visitDag(dag: NyPermisjonsdag, dato: LocalDate, kilde: Hendelseskilde) {}
//    fun visitDag(dag: NyStudiedag, dato: LocalDate, kilde: Hendelseskilde) {}
//    fun visitDag(dag: NyUtenlandsdag, dato: LocalDate, kilde: Hendelseskilde) {}
    fun visitDag(dag: ProblemDag, dato: LocalDate, kilde: Hendelseskilde, melding: String) {}
    fun postVisitNySykdomstidslinje(tidslinje: NySykdomstidslinje, id: UUID, tidsstempel: LocalDateTime) {}
}

internal interface InntekthistorikkVisitor {
    fun preVisitInntekthistorikk(inntekthistorikk: Inntekthistorikk) {}
//    fun preVisitTidslinjer() {}
    fun visitInntekt(inntekt: Inntekthistorikk.Inntekt) {}
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
    fun preVisitOppdrag(oppdrag: Oppdrag) {}
    fun visitUtbetalingslinje(
        linje: Utbetalingslinje,
        fom: LocalDate,
        tom: LocalDate,
        dagsats: Int,
        grad: Double,
        delytelseId: Int,
        refDelytelseId: Int?,
        refFagsystemId: String?,
        endringskode: Endringskode
    ) {}
    fun postVisitOppdrag(oppdrag: Oppdrag) {}
}
