package no.nav.helse.person

import no.nav.helse.hendelser.Simulering
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.person.Vedtaksperiode.Vedtaksperiodetilstand
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.dag.*
import no.nav.helse.utbetalingstidslinje.Utbetalingslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal interface PersonVisitor : ArbeidsgiverVisitor {
    fun preVisitPerson(person: Person, aktørId: String, fødselsnummer: String) {}
    fun visitPersonAktivitetslogg(aktivitetslogg: Aktivitetslogg) {}
    fun preVisitArbeidsgivere() {}
    fun postVisitArbeidsgivere() {}
    fun postVisitPerson(person: Person, aktørId: String, fødselsnummer: String) {}
}

internal interface ArbeidsgiverVisitor : InntekthistorikkVisitor, VedtaksperiodeVisitor {
    fun preVisitArbeidsgiver(
        arbeidsgiver: Arbeidsgiver,
        id: UUID,
        organisasjonsnummer: String
    ) {}
    fun preVisitTidslinjer(tidslinjer: MutableList<Utbetalingstidslinje>) {}
    fun postVisitTidslinjer(tidslinjer: MutableList<Utbetalingstidslinje>) {}
    fun preVisitPerioder() {}
    fun postVisitPerioder() {}
    fun postVisitArbeidsgiver(
        arbeidsgiver: Arbeidsgiver,
        id: UUID,
        organisasjonsnummer: String
    ) {}
}

internal interface VedtaksperiodeVisitor : SykdomshistorikkVisitor, UtbetalingsdagVisitor {
    fun preVisitVedtaksperiode(vedtaksperiode: Vedtaksperiode, id: UUID) {}
    fun visitMaksdato(maksdato: LocalDate?) {}
    fun visitForbrukteSykedager(forbrukteSykedager: Int?) {}
    fun visitGodkjentAv(godkjentAv: String?) {}
    fun visitFørsteFraværsdag(førsteFraværsdag: LocalDate?) {}
    fun visitUtbetalingsreferanse(utbetalingsreferanse: String?) {}
    fun visitDataForVilkårsvurdering(dataForVilkårsvurdering: Vilkårsgrunnlag.Grunnlagsdata?) {}
    fun visitDataForSimulering(dataForSimuleringResultat: Simulering.SimuleringResultat?) {}
    fun visitUtbetalingslinje(utbetalingslinje: Utbetalingslinje) {}
    fun visitTilstand(tilstand: Vedtaksperiodetilstand) {}
    fun preVisitUtbetalingslinjer(linjer: List<Utbetalingslinje>) {}
    fun postVisitUtbetalingslinjer(linjer: List<Utbetalingslinje>) {}
    fun postVisitVedtaksperiode(vedtaksperiode: Vedtaksperiode, id: UUID) {}
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

internal interface InntekthistorikkVisitor {
    fun preVisitInntekthistorikk(inntekthistorikk: Inntekthistorikk) {}
//    fun preVisitTidslinjer() {}
    fun visitInntekt(inntekt: Inntekthistorikk.Inntekt) {}
//    fun postVisitTidslinjer() {}
    fun postVisitInntekthistorikk(inntekthistorikk: Inntekthistorikk) {}
}
