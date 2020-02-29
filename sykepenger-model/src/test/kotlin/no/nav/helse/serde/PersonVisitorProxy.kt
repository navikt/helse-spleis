package no.nav.helse.serde

import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.person.*
import no.nav.helse.sykdomstidslinje.CompositeSykdomstidslinje
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
import no.nav.helse.sykdomstidslinje.dag.*
import no.nav.helse.utbetalingstidslinje.Utbetalingslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import java.time.LocalDate
import java.util.*

internal open class PersonVisitorProxy(protected val target: PersonVisitor) : PersonVisitor {

    override fun preVisitPerson(person: Person, aktørId: String, fødselsnummer: String) {
        target.preVisitPerson(person, aktørId, fødselsnummer)
    }

    override fun preVisitArbeidsgivere() {
        target.preVisitArbeidsgivere()
    }

    override fun postVisitArbeidsgivere() {
        target.postVisitArbeidsgivere()
    }

    override fun postVisitPerson(person: Person, aktørId: String, fødselsnummer: String) {
        target.postVisitPerson(person, aktørId, fødselsnummer)
    }

    override fun preVisitArbeidsgiver(arbeidsgiver: Arbeidsgiver, id: UUID, organisasjonsnummer: String) {
        target.preVisitArbeidsgiver(arbeidsgiver, id, organisasjonsnummer)
    }

    override fun preVisitInntekthistorikk(inntekthistorikk: Inntekthistorikk) {
        target.preVisitInntekthistorikk(inntekthistorikk)
    }

    override fun postVisitInntekthistorikk(inntekthistorikk: Inntekthistorikk) {
        target.postVisitInntekthistorikk(inntekthistorikk)
    }

    override fun preVisitTidslinjer() {
        target.preVisitTidslinjer()
    }

    override fun postVisitTidslinjer() {
        target.postVisitTidslinjer()
    }

    override fun preVisitPerioder() {
        target.preVisitPerioder()
    }

    override fun postVisitPerioder() {
        target.postVisitPerioder()
    }

    override fun postVisitArbeidsgiver(arbeidsgiver: Arbeidsgiver, id: UUID, organisasjonsnummer: String) {
        target.postVisitArbeidsgiver(arbeidsgiver, id, organisasjonsnummer)
    }

    override fun preVisitInntekter() {
        target.preVisitInntekter()
    }

    override fun postVisitInntekter() {
        target.postVisitInntekter()
    }

    override fun visitInntekt(inntekt: Inntekthistorikk.Inntekt) {
        target.visitInntekt(inntekt)
    }

    override fun preVisitUtbetalingstidslinje(tidslinje: Utbetalingstidslinje) {
        target.preVisitUtbetalingstidslinje(tidslinje)
    }

    override fun visitArbeidsgiverperiodeDag(dag: Utbetalingstidslinje.Utbetalingsdag.ArbeidsgiverperiodeDag) {
        target.visitArbeidsgiverperiodeDag(dag)
    }

    override fun visitNavDag(dag: Utbetalingstidslinje.Utbetalingsdag.NavDag) {
        target.visitNavDag(dag)
    }

    override fun visitNavHelgDag(dag: Utbetalingstidslinje.Utbetalingsdag.NavHelgDag) {
        target.visitNavHelgDag(dag)
    }

    override fun visitArbeidsdag(dag: Utbetalingstidslinje.Utbetalingsdag.Arbeidsdag) {
        target.visitArbeidsdag(dag)
    }

    override fun visitArbeidsdag(arbeidsdag: Arbeidsdag.Inntektsmelding) {
        target.visitArbeidsdag(arbeidsdag)
    }

    override fun visitArbeidsdag(arbeidsdag: Arbeidsdag.Søknad) {
        target.visitArbeidsdag(arbeidsdag)
    }

    override fun visitFridag(dag: Utbetalingstidslinje.Utbetalingsdag.Fridag) {
        target.visitFridag(dag)
    }

    override fun visitAvvistDag(dag: Utbetalingstidslinje.Utbetalingsdag.AvvistDag) {
        target.visitAvvistDag(dag)
    }

    override fun visitUkjentDag(dag: Utbetalingstidslinje.Utbetalingsdag.UkjentDag) {
        target.visitUkjentDag(dag)
    }

    override fun postVisitUtbetalingstidslinje(tidslinje: Utbetalingstidslinje) {
        target.postVisitUtbetalingstidslinje(tidslinje)
    }

    override fun equals(other: Any?): Boolean {
        return target.equals(other)
    }

    override fun hashCode(): Int {
        return target.hashCode()
    }

    override fun toString(): String {
        return target.toString()
    }

    override fun preVisitVedtaksperiode(vedtaksperiode: Vedtaksperiode, id: UUID) {
        target.preVisitVedtaksperiode(vedtaksperiode, id)
    }

    override fun visitMaksdato(maksdato: LocalDate?) {
        target.visitMaksdato(maksdato)
    }

    override fun visitforbrukteSykedager(forbrukteSykedager: Int?) {
        target.visitforbrukteSykedager(forbrukteSykedager)
    }

    override fun visitGodkjentAv(godkjentAv: String?) {
        target.visitGodkjentAv(godkjentAv)
    }

    override fun visitFørsteFraværsdag(førsteFraværsdag: LocalDate?) {
        target.visitFørsteFraværsdag(førsteFraværsdag)
    }

    override fun visitInntektFraInntektsmelding(inntektFraInntektsmelding: Double?) {
        target.visitInntektFraInntektsmelding(inntektFraInntektsmelding)
    }

    override fun visitDataForVilkårsvurdering(dataForVilkårsvurdering: Vilkårsgrunnlag.Grunnlagsdata?) {
        target.visitDataForVilkårsvurdering(dataForVilkårsvurdering)
    }

    override fun visitUtbetalingslinje(utbetalingslinje: Utbetalingslinje) {
        target.visitUtbetalingslinje(utbetalingslinje)
    }

    override fun visitTilstand(tilstand: Vedtaksperiode.Vedtaksperiodetilstand) {
        target.visitTilstand(tilstand)
    }

    override fun preVisitUtbetalingslinjer() {
        target.preVisitUtbetalingslinjer()
    }

    override fun postVisitUtbetalingslinjer() {
        target.postVisitUtbetalingslinjer()
    }

    override fun postVisitVedtaksperiode(vedtaksperiode: Vedtaksperiode, id: UUID) {
        target.postVisitVedtaksperiode(vedtaksperiode, id)
    }

    override fun preVisitVedtaksperiodeSykdomstidslinje() {
        target.preVisitVedtaksperiodeSykdomstidslinje()
    }

    override fun postVisitVedtaksperiodeSykdomstidslinje() {
        target.postVisitVedtaksperiodeSykdomstidslinje()
    }

    override fun visitEgenmeldingsdag(egenmeldingsdag: Egenmeldingsdag.Inntektsmelding) {
        target.visitEgenmeldingsdag(egenmeldingsdag)
    }

    override fun visitEgenmeldingsdag(egenmeldingsdag: Egenmeldingsdag.Søknad) {
        target.visitEgenmeldingsdag(egenmeldingsdag)
    }

    override fun visitFeriedag(feriedag: Feriedag.Inntektsmelding) {
        target.visitFeriedag(feriedag)
    }

    override fun visitFeriedag(feriedag: Feriedag.Søknad) {
        target.visitFeriedag(feriedag)
    }

    override fun visitImplisittDag(implisittDag: ImplisittDag) {
        target.visitImplisittDag(implisittDag)
    }

    override fun visitPermisjonsdag(permisjonsdag: Permisjonsdag.Søknad) {
        target.visitPermisjonsdag(permisjonsdag)
    }

    override fun visitPermisjonsdag(permisjonsdag: Permisjonsdag.Aareg) {
        target.visitPermisjonsdag(permisjonsdag)
    }

    override fun visitStudiedag(studiedag: Studiedag) {
        target.visitStudiedag(studiedag)
    }

    override fun visitSykHelgedag(sykHelgedag: SykHelgedag.Sykmelding) {
        target.visitSykHelgedag(sykHelgedag)
    }

    override fun visitSykHelgedag(sykHelgedag: SykHelgedag.Søknad) {
        target.visitSykHelgedag(sykHelgedag)
    }

    override fun visitSykedag(sykedag: Sykedag.Sykmelding) {
        target.visitSykedag(sykedag)
    }

    override fun visitSykedag(sykedag: Sykedag.Søknad) {
        target.visitSykedag(sykedag)
    }

    override fun visitUbestemt(ubestemtdag: Ubestemtdag) {
        target.visitUbestemt(ubestemtdag)
    }

    override fun visitUtenlandsdag(utenlandsdag: Utenlandsdag) {
        target.visitUtenlandsdag(utenlandsdag)
    }

    override fun preVisitComposite(compositeSykdomstidslinje: CompositeSykdomstidslinje) {
        target.preVisitComposite(compositeSykdomstidslinje)
    }

    override fun postVisitComposite(compositeSykdomstidslinje: CompositeSykdomstidslinje) {
        target.postVisitComposite(compositeSykdomstidslinje)
    }

    override fun preVisitSykdomshistorikk(sykdomshistorikk: Sykdomshistorikk) {
        target.preVisitSykdomshistorikk(sykdomshistorikk)
    }

    override fun preVisitSykdomshistorikkElement(element: Sykdomshistorikk.Element) {
        target.preVisitSykdomshistorikkElement(element)
    }

    override fun postVisitSykdomshistorikkElement(element: Sykdomshistorikk.Element) {
        target.postVisitSykdomshistorikkElement(element)
    }

    override fun postVisitSykdomshistorikk(sykdomshistorikk: Sykdomshistorikk) {
        target.postVisitSykdomshistorikk(sykdomshistorikk)
    }

    override fun preVisitHendelseSykdomstidslinje() {
        target.preVisitHendelseSykdomstidslinje()
    }

    override fun postVisitHendelseSykdomstidslinje() {
        target.postVisitHendelseSykdomstidslinje()
    }

    override fun preVisitBeregnetSykdomstidslinje() {
        target.preVisitBeregnetSykdomstidslinje()
    }

    override fun postVisitBeregnetSykdomstidslinje() {
        target.postVisitBeregnetSykdomstidslinje()
    }
}
