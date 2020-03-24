package no.nav.helse.person

import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.person.Vedtaksperiode.Vedtaksperiodetilstand
import no.nav.helse.sykdomstidslinje.*
import no.nav.helse.sykdomstidslinje.NySykdomshistorikk
import no.nav.helse.sykdomstidslinje.NySykdomstidslinje
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeVisitor
import no.nav.helse.sykdomstidslinje.dag.*
import no.nav.helse.sykdomstidslinje.dag.Arbeidsdag
import no.nav.helse.sykdomstidslinje.dag.Egenmeldingsdag
import no.nav.helse.sykdomstidslinje.dag.Feriedag
import no.nav.helse.sykdomstidslinje.dag.ImplisittDag
import no.nav.helse.sykdomstidslinje.dag.KunArbeidsgiverSykedag
import no.nav.helse.sykdomstidslinje.dag.Permisjonsdag
import no.nav.helse.sykdomstidslinje.dag.Studiedag
import no.nav.helse.sykdomstidslinje.dag.SykHelgedag
import no.nav.helse.sykdomstidslinje.dag.Sykedag
import no.nav.helse.sykdomstidslinje.dag.Ubestemtdag
import no.nav.helse.sykdomstidslinje.dag.Utenlandsdag
import no.nav.helse.utbetalingstidslinje.Utbetalingslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.UtbetalingsdagVisitor
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

internal interface ArbeidsgiverVisitor : InntekthistorikkVisitor, UtbetalingsdagVisitor, VedtaksperiodeVisitor {
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

internal interface VedtaksperiodeVisitor : SykdomstidslinjeVisitor, SykdomshistorikkVisitor, UtbetalingsdagVisitor {
    fun preVisitVedtaksperiode(vedtaksperiode: Vedtaksperiode, id: UUID) {}
    fun visitMaksdato(maksdato: LocalDate?) {}
    fun visitForbrukteSykedager(forbrukteSykedager: Int?) {}
    fun visitGodkjentAv(godkjentAv: String?) {}
    fun visitFørsteFraværsdag(førsteFraværsdag: LocalDate?) {}
    fun visitUtbetalingsreferanse(utbetalingsreferanse: String) {}
    fun visitDataForVilkårsvurdering(dataForVilkårsvurdering: Vilkårsgrunnlag.Grunnlagsdata?) {}
    fun visitUtbetalingslinje(utbetalingslinje: Utbetalingslinje) {}
    fun visitTilstand(tilstand: Vedtaksperiodetilstand) {}
    fun preVisitUtbetalingslinjer(linjer: List<Utbetalingslinje>) {}
    fun postVisitUtbetalingslinjer(linjer: List<Utbetalingslinje>) {}
    fun postVisitVedtaksperiode(vedtaksperiode: Vedtaksperiode, id: UUID) {}
}

internal interface SykdomshistorikkVisitor : SykdomstidslinjeVisitor {
    fun preVisitSykdomshistorikk(sykdomshistorikk: Sykdomshistorikk) {}
    fun preVisitSykdomshistorikkElement(
        element: Sykdomshistorikk.Element,
        id: UUID,
        tidsstempel: LocalDateTime
    ) {}
    fun postVisitSykdomshistorikkElement(
        element: Sykdomshistorikk.Element,
        id: UUID,
        tidsstempel: LocalDateTime
    ) {}
    fun postVisitSykdomshistorikk(sykdomshistorikk: Sykdomshistorikk) {}
    fun preVisitHendelseSykdomstidslinje() {}
    fun postVisitHendelseSykdomstidslinje() {}
    fun preVisitBeregnetSykdomstidslinje() {}
    fun postVisitBeregnetSykdomstidslinje() {}
}

internal interface NySykdomshistorikkVisitor : NySykdomstidslinjeVisitor {
    fun preVisitSykdomshistorikk(sykdomshistorikk: NySykdomshistorikk) {}
    fun preVisitSykdomshistorikkElement(
        element: NySykdomshistorikk.Element,
        id: UUID,
        tidsstempel: LocalDateTime
    ) {}
    fun preVisitHendelseSykdomstidslinje(tidslinje: NySykdomstidslinje) {}
    fun postVisitHendelseSykdomstidslinje(tidslinje: NySykdomstidslinje) {}
    fun preVisitBeregnetSykdomstidslinje(tidslinje: NySykdomstidslinje) {}
    fun postVisitBeregnetSykdomstidslinje(tidslinje: NySykdomstidslinje) {}
    fun postVisitSykdomshistorikkElement(
        element: NySykdomshistorikk.Element,
        id: UUID,
        tidsstempel: LocalDateTime
    ) {}
    fun postVisitSykdomshistorikk(sykdomshistorikk: NySykdomshistorikk) {}
}

internal interface NySykdomstidslinjeVisitor {
    fun preVisitSykdomstidslinje(tidslinje: NySykdomstidslinje) {}
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
    fun postVisitSykdomstidslinje(tidslinje: NySykdomstidslinje) {}
}

internal interface InntekthistorikkVisitor {
    fun preVisitInntekthistorikk(inntekthistorikk: Inntekthistorikk) {}
//    fun preVisitTidslinjer() {}
    fun visitInntekt(inntekt: Inntekthistorikk.Inntekt) {}
//    fun postVisitTidslinjer() {}
    fun postVisitInntekthistorikk(inntekthistorikk: Inntekthistorikk) {}
}
