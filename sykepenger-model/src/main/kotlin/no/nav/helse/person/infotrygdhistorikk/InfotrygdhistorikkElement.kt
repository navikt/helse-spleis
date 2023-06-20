package no.nav.helse.person.infotrygdhistorikk

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.InfotrygdhistorikkVisitor
import no.nav.helse.person.Person
import no.nav.helse.person.SykdomstidslinjeVisitor
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IT_14
import no.nav.helse.person.infotrygdhistorikk.Infotrygdperiode.Companion.harBetaltRettFør
import no.nav.helse.person.infotrygdhistorikk.Infotrygdperiode.Companion.utbetalingsperioder
import no.nav.helse.sykdomstidslinje.Dag.Companion.replace
import no.nav.helse.sykdomstidslinje.Dag.Companion.sammenhengendeSykdom
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverperiodeteller
import no.nav.helse.utbetalingstidslinje.Infotrygddekoratør
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje

class InfotrygdhistorikkElement private constructor(
    private val id: UUID,
    private val tidsstempel: LocalDateTime,
    private val hendelseId: UUID? = null,
    perioder: List<Infotrygdperiode>,
    inntekter: List<Inntektsopplysning>,
    private val arbeidskategorikoder: Map<String, LocalDate>,
    private var oppdatert: LocalDateTime
) {
    private val inntekter = Inntektsopplysning.sorter(inntekter)
    private val perioder = Infotrygdperiode.sorter(perioder)
    private val kilde = SykdomstidslinjeHendelse.Hendelseskilde("Infotrygdhistorikk", id, tidsstempel)

    init {
        if (!erTom()) requireNotNull(hendelseId) { "HendelseID må være satt når elementet inneholder data" }
    }

    companion object {
        fun opprett(
            oppdatert: LocalDateTime,
            hendelseId: UUID,
            perioder: List<Infotrygdperiode>,
            inntekter: List<Inntektsopplysning>,
            arbeidskategorikoder: Map<String, LocalDate>
        ) =
            InfotrygdhistorikkElement(
                id = UUID.randomUUID(),
                tidsstempel = LocalDateTime.now(),
                hendelseId = hendelseId,
                perioder = perioder,
                inntekter = inntekter,
                arbeidskategorikoder = arbeidskategorikoder,
                oppdatert = oppdatert
            )

        internal fun ferdigElement(
            id: UUID,
            tidsstempel: LocalDateTime,
            hendelseId: UUID?,
            infotrygdperioder: List<Infotrygdperiode>,
            inntekter: List<Inntektsopplysning>,
            arbeidskategorikoder: Map<String, LocalDate>,
            oppdatert: LocalDateTime
        ): InfotrygdhistorikkElement = InfotrygdhistorikkElement(
            id = id,
            tidsstempel = tidsstempel,
            hendelseId = hendelseId,
            perioder = infotrygdperioder,
            inntekter = inntekter,
            arbeidskategorikoder = arbeidskategorikoder,
            oppdatert = oppdatert
        )
    }

    internal fun build(organisasjonsnummer: String, sykdomstidslinje: Sykdomstidslinje, teller: Arbeidsgiverperiodeteller, builder: SykdomstidslinjeVisitor) {
        val dekoratør = Infotrygddekoratør(teller, builder, perioder.utbetalingsperioder(organisasjonsnummer))
        historikkFor(organisasjonsnummer, sykdomstidslinje).accept(dekoratør)
    }

    internal fun betaltePerioder(): List<Periode> = perioder.utbetalingsperioder()

    internal fun historikkFor(orgnummer: String, sykdomstidslinje: Sykdomstidslinje): Sykdomstidslinje {
        if (sykdomstidslinje.periode() == null) return sykdomstidslinje
        val ulåst = Sykdomstidslinje().merge(sykdomstidslinje, replace)
        return sykdomstidslinje(orgnummer, ulåst)
    }

    internal fun sykdomstidslinje(orgnummer: String, sykdomstidslinje: Sykdomstidslinje = Sykdomstidslinje()): Sykdomstidslinje {
        return perioder.fold(sykdomstidslinje) { result, periode ->
            periode.historikkFor(orgnummer, result, kilde)
        }
    }

    internal fun sykdomstidslinje(): Sykdomstidslinje {
        return perioder.fold(Sykdomstidslinje()) { result, periode ->
            result.merge(periode.sykdomstidslinje(kilde), sammenhengendeSykdom)
        }
    }

    private fun erTom() =
        perioder.isEmpty() && inntekter.isEmpty() && arbeidskategorikoder.isEmpty()

    internal fun valider(
        aktivitetslogg: IAktivitetslogg,
        periode: Periode,
        skjæringstidspunkt: LocalDate,
        organisasjonsnummer: String
    ): Boolean {
        validerBetaltRettFør(periode, aktivitetslogg)
        aktivitetslogg.info("Sjekker utbetalte perioder")
        perioder.filterIsInstance<Utbetalingsperiode>()
            .forEach { it.valider(aktivitetslogg, organisasjonsnummer, periode) }
        return !aktivitetslogg.harFunksjonelleFeilEllerVerre()
    }

    private fun validerBetaltRettFør(periode: Periode, aktivitetslogg: IAktivitetslogg){
        if (!harBetaltRettFør(periode)) return
        aktivitetslogg.funksjonellFeil(RV_IT_14)
    }

    internal fun utbetalingstidslinje() =
        perioder
            .map { it.utbetalingstidslinje() }
            .fold(Utbetalingstidslinje(), Utbetalingstidslinje::plus)

    private fun harBetaltRettFør(periode: Periode) = perioder.harBetaltRettFør(periode)

    internal fun accept(visitor: InfotrygdhistorikkVisitor) {
        visitor.preVisitInfotrygdhistorikkElement(id, tidsstempel, oppdatert, hendelseId)
        visitor.preVisitInfotrygdhistorikkPerioder()
        perioder.forEach { it.accept(visitor) }
        visitor.postVisitInfotrygdhistorikkPerioder()
        visitor.preVisitInfotrygdhistorikkInntektsopplysninger()
        inntekter.forEach { it.accept(visitor) }
        visitor.postVisitInfotrygdhistorikkInntektsopplysninger()
        visitor.visitInfotrygdhistorikkArbeidskategorikoder(arbeidskategorikoder)
        visitor.postVisitInfotrygdhistorikkElement(id, tidsstempel, oppdatert, hendelseId)
    }

    internal fun funksjoneltLik(other: InfotrygdhistorikkElement): Boolean {
        if (!harLikePerioder(other)) return false
        if (!harLikeInntekter(other)) return false
        return this.arbeidskategorikoder == other.arbeidskategorikoder
    }

    private fun harLikePerioder(other: InfotrygdhistorikkElement) = likhet(this.perioder, other.perioder, Infotrygdperiode::funksjoneltLik)
    private fun harLikeInntekter(other: InfotrygdhistorikkElement) = likhet(this.inntekter, other.inntekter, Inntektsopplysning::funksjoneltLik)
    private fun <R> likhet(one: List<R>, two: List<R>, comparator: (R, R) -> Boolean): Boolean {
        if (one.size != two.size) return false
        return one.zip(two, comparator).all { it }
    }

    internal fun erstatter(other: InfotrygdhistorikkElement): Boolean {
        if (!this.funksjoneltLik(other)) return false
        oppdater(other)
        return true
    }

    private fun oppdater(other: InfotrygdhistorikkElement) {
        other.oppdatert = this.oppdatert
    }

    internal fun erEldreEnn(utbetaling: Utbetaling): Boolean {
        return utbetaling.erNyereEnn(this.tidsstempel)
    }

    internal fun erEndretUtbetaling(sisteElementSomFantesFørUtbetaling: InfotrygdhistorikkElement): Boolean {
        if (this === sisteElementSomFantesFørUtbetaling) return false
        return this.perioder != sisteElementSomFantesFørUtbetaling.perioder
    }

    internal fun utbetalingshistorikkEtterInfotrygdendring(
        vedtaksperiodeId: UUID,
        vedtaksperiode: Periode,
        tilstand: String,
        organisasjonsnummer: String,
        person: Person
    ) {
        val overlappendePerioder : List<Infotrygdperiode> = perioder.filter { it.overlapperMed(vedtaksperiode) }
        if (overlappendePerioder.isEmpty()) return
        person.emitOverlappendeInfotrygdperiodeEtterInfotrygdendring(
            vedtaksperiodeId, vedtaksperiode, tilstand, organisasjonsnummer, overlappendePerioder, hendelseId
        )

    }

    internal fun harUtbetaltI(periode: Periode) = betaltePerioder().any { it.overlapperMed(periode) }

}

