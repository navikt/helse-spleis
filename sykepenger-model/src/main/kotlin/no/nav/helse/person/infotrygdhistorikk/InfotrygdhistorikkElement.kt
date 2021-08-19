package no.nav.helse.person.infotrygdhistorikk

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.*
import no.nav.helse.person.infotrygdhistorikk.Infotrygdperiode.Companion.validerInntektForPerioder
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning.Companion.lagreVilkårsgrunnlag
import no.nav.helse.sykdomstidslinje.Dag.Companion.sammenhengendeSykdom
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.sykdomstidslinje.erHelg
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class InfotrygdhistorikkElement private constructor(
    private val id: UUID,
    private val tidsstempel: LocalDateTime,
    private val hendelseId: UUID? = null,
    perioder: List<Infotrygdperiode>,
    private val inntekter: List<Inntektsopplysning>,
    private val arbeidskategorikoder: Map<String, LocalDate>,
    private val ugyldigePerioder: List<Pair<LocalDate?, LocalDate?>>,
    private val harStatslønn: Boolean,
    private var oppdatert: LocalDateTime,
    private var lagretInntekter: Boolean,
    private var lagretVilkårsgrunnlag: Boolean
) {
    private val perioder = perioder.sortedBy { it.start }
    private val kilde = SykdomstidslinjeHendelse.Hendelseskilde("Infotrygdhistorikk", id, tidsstempel)

    init {
        if (!erTom()) requireNotNull(hendelseId) { "HendelseID må være satt når elementet inneholder data" }
    }

    internal companion object {
        fun opprett(
            oppdatert: LocalDateTime,
            hendelseId: UUID,
            perioder: List<Infotrygdperiode>,
            inntekter: List<Inntektsopplysning>,
            arbeidskategorikoder: Map<String, LocalDate>,
            ugyldigePerioder: List<Pair<LocalDate?, LocalDate?>>,
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
                oppdatert = oppdatert,
                lagretInntekter = false,
                lagretVilkårsgrunnlag = false
            )

        fun opprettTom() =
            InfotrygdhistorikkElement(
                id = UUID.randomUUID(),
                tidsstempel = LocalDateTime.now(),
                hendelseId = null,
                perioder = emptyList(),
                inntekter = emptyList(),
                arbeidskategorikoder = emptyMap(),
                ugyldigePerioder = emptyList(),
                harStatslønn = false,
                oppdatert = LocalDateTime.MIN,
                lagretInntekter = false,
                lagretVilkårsgrunnlag = false
            )
    }

    internal fun historikkFor(orgnummer: String, sykdomstidslinje: Sykdomstidslinje): Sykdomstidslinje {
        return perioder.fold(sykdomstidslinje) { result, periode ->
            periode.historikkFor(orgnummer, result, kilde)
        }
    }

    internal fun harBetalt(organisasjonsnummer: String, dato: LocalDate): Boolean {
        return utbetalingstidslinje(organisasjonsnummer).harBetalt(dato)
    }

    internal fun ingenUkjenteArbeidsgivere(organisasjonsnumre: List<String>, dato: LocalDate): Boolean {
        return perioder.none { it.utbetalingEtter(organisasjonsnumre, dato) }
    }

    internal fun sykdomstidslinje(): Sykdomstidslinje {
        return perioder.fold(Sykdomstidslinje()) { result, periode ->
            result.merge(periode.sykdomstidslinje(kilde), sammenhengendeSykdom)
        }
    }

    internal fun kanSlettes() =
        !lagretInntekter && !lagretVilkårsgrunnlag

    private fun erTom() =
        perioder.isEmpty() && inntekter.isEmpty() && arbeidskategorikoder.isEmpty()

    internal fun addInntekter(person: Person, aktivitetslogg: IAktivitetslogg) {
        if (inntekter.isNotEmpty()) lagretInntekter = true
        Inntektsopplysning.addInntekter(inntekter, person, aktivitetslogg, id)
    }

    internal fun lagreVilkårsgrunnlag(
        vilkårsgrunnlagHistorikk: VilkårsgrunnlagHistorikk,
        sykepengegrunnlagFor: (skjæringstidspunkt: LocalDate) -> Sykepengegrunnlag
    ) {
        lagretVilkårsgrunnlag = true
        inntekter.lagreVilkårsgrunnlag(vilkårsgrunnlagHistorikk, sykepengegrunnlagFor)
    }

    internal fun valider(aktivitetslogg: IAktivitetslogg, periodetype: Periodetype, periode: Periode, skjæringstidspunkt: LocalDate?): Boolean {
        validerUgyldigePerioder(aktivitetslogg)
        validerStatslønn(aktivitetslogg, periodetype)
        return valider(aktivitetslogg, perioder, periode, skjæringstidspunkt)
    }

    internal fun validerOverlappende(aktivitetslogg: IAktivitetslogg, periode: Periode, skjæringstidspunkt: LocalDate?): Boolean {
        aktivitetslogg.info("Sjekker utbetalte perioder for overlapp mot %s", periode)
        return valider(aktivitetslogg, perioder, periode, skjæringstidspunkt)
    }

    private fun validerUgyldigePerioder(aktivitetslogg: IAktivitetslogg) {
        ugyldigePerioder.forEach { (fom,  tom) ->
            val tekst = when {
                fom == null || tom == null -> "mangler fom- eller tomdato"
                fom > tom -> "fom er nyere enn tom"
                else -> null
            }
            aktivitetslogg.error("Det er en ugyldig utbetalingsperiode i Infotrygd%s", tekst?.let { " ($it)" } ?: "")
        }
    }

    private fun validerStatslønn(aktivitetslogg: IAktivitetslogg, periodetype: Periodetype) {
        if (periodetype != Periodetype.OVERGANG_FRA_IT) return
        aktivitetslogg.info("Perioden er en direkte overgang fra periode med opphav i Infotrygd")
        if (!harStatslønn) return
        aktivitetslogg.error("Det er lagt inn statslønn i Infotrygd, undersøk at utbetalingen blir riktig.")
    }

    private fun valider(aktivitetslogg: IAktivitetslogg, perioder: List<Infotrygdperiode>, periode: Periode, skjæringstidspunkt: LocalDate?): Boolean {
        aktivitetslogg.info("Sjekker utbetalte perioder")
        perioder.filterIsInstance<Utbetalingsperiode>().forEach { it.valider(aktivitetslogg, periode) }

        aktivitetslogg.info("Sjekker inntektsopplysninger")
        Inntektsopplysning.valider(inntekter, aktivitetslogg, periode, skjæringstidspunkt)

        aktivitetslogg.info("Sjekker at alle utbetalte perioder har inntektsopplysninger")
        perioder.validerInntektForPerioder(aktivitetslogg, inntekter)

        aktivitetslogg.info("Sjekker arbeidskategorikoder")
        if (!erNormalArbeidstaker(skjæringstidspunkt)) aktivitetslogg.error("Personen er ikke registrert som normal arbeidstaker i Infotrygd")

        return !aktivitetslogg.hasErrorsOrWorse()
    }

    internal fun utbetalingstidslinje() =
        perioder
            .map { it.utbetalingstidslinje() }
            .fold(Utbetalingstidslinje(), Utbetalingstidslinje::plus)

    private fun utbetalingstidslinje(organisasjonsnummer: String) =
        perioder
            .filter { it.gjelder(organisasjonsnummer) }
            .map { it.utbetalingstidslinje() }
            .fold(Utbetalingstidslinje(), Utbetalingstidslinje::plus)

    internal fun fjernHistorikk(utbetalingstidlinje: Utbetalingstidslinje, organisasjonsnummer: String, tidligsteDato: LocalDate): Utbetalingstidslinje {
        return utbetalingstidlinje.plus(utbetalingstidslinje(organisasjonsnummer)) { spleisDag: Utbetalingstidslinje.Utbetalingsdag, infotrygdDag: Utbetalingstidslinje.Utbetalingsdag ->
            when {
                // fjerner ledende dager
                spleisDag.dato < tidligsteDato -> UkjentDag(spleisDag.dato, spleisDag.økonomi)
                // fjerner utbetalinger i ukedager (bevarer fridager)
                !infotrygdDag.dato.erHelg() && infotrygdDag is NavDag -> UkjentDag(spleisDag.dato, spleisDag.økonomi)
                // fjerner utbetalinger i helger (bevarer fridager)
                infotrygdDag.dato.erHelg() && infotrygdDag !is Fridag -> UkjentDag(spleisDag.dato, spleisDag.økonomi)
                else -> spleisDag
            }
        }
    }

    internal fun sisteSykepengedag(orgnummer: String): LocalDate? {
        return perioder.filterIsInstance<Utbetalingsperiode>()
            .filter { it.gjelder(orgnummer) }
            .maxOfOrNull { it.endInclusive }
    }

    internal fun oppfrisket(cutoff: LocalDateTime) =
        oppdatert > cutoff

    internal fun accept(visitor: InfotrygdhistorikkVisitor) {
        visitor.preVisitInfotrygdhistorikkElement(id, tidsstempel, oppdatert, hendelseId, lagretInntekter, lagretVilkårsgrunnlag, harStatslønn)
        visitor.preVisitInfotrygdhistorikkPerioder()
        perioder.forEach { it.accept(visitor) }
        visitor.postVisitInfotrygdhistorikkPerioder()
        visitor.preVisitInfotrygdhistorikkInntektsopplysninger()
        inntekter.forEach { it.accept(visitor) }
        visitor.postVisitInfotrygdhistorikkInntektsopplysninger()
        visitor.visitUgyldigePerioder(ugyldigePerioder)
        visitor.visitInfotrygdhistorikkArbeidskategorikoder(arbeidskategorikoder)
        visitor.postVisitInfotrygdhistorikkElement(id, tidsstempel, oppdatert, hendelseId, lagretInntekter, lagretVilkårsgrunnlag, harStatslønn)
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

    internal fun harEndretHistorikk(utbetaling: Utbetaling): Boolean {
        return utbetaling.erEldreEnn(tidsstempel)
    }
}
