package no.nav.helse.person.infotrygdhistorikk

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Objects
import java.util.UUID
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.InfotrygdhistorikkVisitor
import no.nav.helse.person.Periodetype
import no.nav.helse.person.SykdomstidslinjeVisitor
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IT_14
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IT_15
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
    private val ugyldigePerioder: List<UgyldigPeriode>,
    private val harStatslønn: Boolean,
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
            arbeidskategorikoder: Map<String, LocalDate>,
            ugyldigePerioder: List<UgyldigPeriode>,
            harStatslønn: Boolean
        ) =
            InfotrygdhistorikkElement(
                id = UUID.randomUUID(),
                tidsstempel = LocalDateTime.now(),
                hendelseId = hendelseId,
                perioder = perioder,
                inntekter = inntekter,
                arbeidskategorikoder = arbeidskategorikoder,
                ugyldigePerioder = ugyldigePerioder,
                harStatslønn = harStatslønn,
                oppdatert = oppdatert
            )

        internal fun ferdigElement(
            id: UUID,
            tidsstempel: LocalDateTime,
            hendelseId: UUID?,
            infotrygdperioder: List<Infotrygdperiode>,
            inntekter: List<Inntektsopplysning>,
            arbeidskategorikoder: Map<String, LocalDate>,
            ugyldigePerioder: List<UgyldigPeriode>,
            harStatslønn: Boolean,
            oppdatert: LocalDateTime
        ): InfotrygdhistorikkElement = InfotrygdhistorikkElement(
            id = id,
            tidsstempel = tidsstempel,
            hendelseId = hendelseId,
            perioder = infotrygdperioder,
            inntekter = inntekter,
            arbeidskategorikoder = arbeidskategorikoder,
            ugyldigePerioder = ugyldigePerioder,
            harStatslønn = harStatslønn,
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

    internal fun periodetype(organisasjonsnummer: String, other: Periode, dag: LocalDate): Periodetype? {
        val utbetalinger = perioder.utbetalingsperioder(organisasjonsnummer)
        if (dag > other.start || utbetalinger.none { dag in it }) return null
        if (dag in other || utbetalinger.any { dag in it && it.erRettFør(other) }) return Periodetype.OVERGANG_FRA_IT
        return Periodetype.INFOTRYGDFORLENGELSE
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
        aktivitetslogg.info("Sjekker arbeidskategorikoder")
        if (!erNormalArbeidstaker(skjæringstidspunkt)) aktivitetslogg.funksjonellFeil(RV_IT_15)
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

    internal fun harBetaltRettFør(periode: Periode) = perioder.harBetaltRettFør(periode)

    internal fun oppfrisket(cutoff: LocalDateTime) =
        oppdatert > cutoff

    internal fun accept(visitor: InfotrygdhistorikkVisitor) {
        visitor.preVisitInfotrygdhistorikkElement(id, tidsstempel, oppdatert, hendelseId, harStatslønn)
        visitor.preVisitInfotrygdhistorikkPerioder()
        perioder.forEach { it.accept(visitor) }
        visitor.postVisitInfotrygdhistorikkPerioder()
        visitor.preVisitInfotrygdhistorikkInntektsopplysninger()
        inntekter.forEach { it.accept(visitor) }
        visitor.postVisitInfotrygdhistorikkInntektsopplysninger()
        visitor.visitUgyldigePerioder(ugyldigePerioder)
        visitor.visitInfotrygdhistorikkArbeidskategorikoder(arbeidskategorikoder)
        visitor.postVisitInfotrygdhistorikkElement(id, tidsstempel, oppdatert, hendelseId, harStatslønn)
    }

    private fun erNormalArbeidstaker(skjæringstidspunkt: LocalDate?): Boolean {
        if (arbeidskategorikoder.isEmpty() || skjæringstidspunkt == null) return true
        return arbeidskategorikoder
            .filter { (_, dato) -> dato >= skjæringstidspunkt }
            .all { (arbeidskategorikode, _) -> arbeidskategorikode == "01" }
    }

    override fun hashCode(): Int {
        return Objects.hash(perioder, inntekter, arbeidskategorikoder, ugyldigePerioder, harStatslønn)
    }

    override fun equals(other: Any?): Boolean {
        if (other !is InfotrygdhistorikkElement) return false
        return equals(other)
    }

    private fun equals(other: InfotrygdhistorikkElement): Boolean {
        if (this.perioder != other.perioder) return false
        if (this.inntekter != other.inntekter) return false
        if (this.arbeidskategorikoder != other.arbeidskategorikoder) return false
        if (this.ugyldigePerioder != other.ugyldigePerioder) return false
        return this.harStatslønn == other.harStatslønn
    }

    internal fun erstatter(other: InfotrygdhistorikkElement): Boolean {
        if (!this.equals(other)) return false
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
}

