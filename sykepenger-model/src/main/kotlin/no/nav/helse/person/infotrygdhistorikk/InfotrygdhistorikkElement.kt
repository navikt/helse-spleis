package no.nav.helse.person.infotrygdhistorikk

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Objects
import java.util.UUID
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.InfotrygdhistorikkVisitor
import no.nav.helse.person.Periodetype
import no.nav.helse.person.Person
import no.nav.helse.person.SykdomstidslinjeVisitor
import no.nav.helse.person.Sykepengegrunnlag
import no.nav.helse.person.VilkårsgrunnlagHistorikk
import no.nav.helse.person.infotrygdhistorikk.Infotrygdperiode.Companion.harBrukerutbetalingFor
import no.nav.helse.person.infotrygdhistorikk.Infotrygdperiode.Companion.validerInntektForPerioder
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning.Companion.fjern
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning.Companion.lagreVilkårsgrunnlag
import no.nav.helse.sykdomstidslinje.Dag.Companion.replace
import no.nav.helse.sykdomstidslinje.Dag.Companion.sammenhengendeSykdom
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.sykdomstidslinje.erHelg
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverperiode
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverperiodeteller
import no.nav.helse.utbetalingstidslinje.Infotrygddekoratør
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.Fridag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.NavDag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.UkjentDag

internal class InfotrygdhistorikkElement private constructor(
    private val id: UUID,
    private val tidsstempel: LocalDateTime,
    private val hendelseId: UUID? = null,
    perioder: List<Infotrygdperiode>,
    inntekter: List<Inntektsopplysning>,
    private val arbeidskategorikoder: Map<String, LocalDate>,
    private val ugyldigePerioder: List<UgyldigPeriode>,
    private val harStatslønn: Boolean,
    private var oppdatert: LocalDateTime,
    private var lagretInntekter: Boolean,
    private var lagretVilkårsgrunnlag: Boolean
) {
    private val nødnummer = Nødnummer.Sykepenger
    private val inntekter = Inntektsopplysning.sorter(inntekter)
    private val perioder = Infotrygdperiode.sorter(perioder)
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

    private val arbeidsgiverperiodecache = mutableMapOf<String, MutableMap<UUID, List<Arbeidsgiverperiode>>>()
    internal fun arbeidsgiverperiodeFor(organisasjonsnummer: String, sykdomshistorikkId: UUID): List<Arbeidsgiverperiode>? {
        val innslag = arbeidsgiverperiodecache[organisasjonsnummer] ?: return null
        return innslag[sykdomshistorikkId]
    }

    internal fun lagreResultat(organisasjonsnummer: String, sykdomshistorikkId: UUID, resultat: List<Arbeidsgiverperiode>) {
        arbeidsgiverperiodecache.getOrPut(organisasjonsnummer) { mutableMapOf() }[sykdomshistorikkId] = resultat
    }

    internal fun build(organisasjonsnummer: String, sykdomstidslinje: Sykdomstidslinje, teller: Arbeidsgiverperiodeteller, builder: SykdomstidslinjeVisitor) {
        val dekoratør = Infotrygddekoratør(teller, builder, perioder.filterIsInstance<Utbetalingsperiode>().filter { it.gjelder(organisasjonsnummer) })
        historikkFor(organisasjonsnummer, sykdomstidslinje).accept(dekoratør)
    }

    private data class Oppslagsnøkkel(
        val orgnummer: String,
        val sykdomstidslinje: Sykdomstidslinje
    )

    private val oppslag = mutableMapOf<Oppslagsnøkkel, Sykdomstidslinje>()

    internal fun historikkFor(orgnummer: String, sykdomstidslinje: Sykdomstidslinje): Sykdomstidslinje {
        if (sykdomstidslinje.periode() == null) return sykdomstidslinje
        val ulåst = Sykdomstidslinje().merge(sykdomstidslinje, replace)
        return cached(orgnummer, ulåst).fremTilOgMed(sykdomstidslinje.sisteDag())
    }

    private fun cached(orgnummer: String, sykdomstidslinje: Sykdomstidslinje) =
        oppslag.computeIfAbsent(Oppslagsnøkkel(orgnummer, sykdomstidslinje)) {
            sykdomstidslinje(orgnummer, sykdomstidslinje)
        }

    internal fun periodetype(organisasjonsnummer: String, other: Periode, dag: LocalDate): Periodetype? {
        val utbetalinger = utbetalinger(organisasjonsnummer)
        if (dag > other.start || utbetalinger.none { dag in it }) return null
        if (dag in other || utbetalinger.any { dag in it && it.erRettFør(other) }) return Periodetype.OVERGANG_FRA_IT
        return Periodetype.INFOTRYGDFORLENGELSE
    }

    internal fun ingenUkjenteArbeidsgivere(organisasjonsnumre: List<String>, dato: LocalDate): Boolean {
        return perioder.none { it.utbetalingEtter(organisasjonsnumre, dato) }
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

    internal fun kanSlettes() =
        !lagretInntekter && !lagretVilkårsgrunnlag

    private fun erTom() =
        perioder.isEmpty() && inntekter.isEmpty() && arbeidskategorikoder.isEmpty()

    internal fun addInntekter(person: Person, aktivitetslogg: IAktivitetslogg) {
        lagretInntekter = Inntektsopplysning.addInntekter(inntekter, person, aktivitetslogg, id, nødnummer)
    }

    internal fun lagreVilkårsgrunnlag(
        vilkårsgrunnlagHistorikk: VilkårsgrunnlagHistorikk,
        sykepengegrunnlagFor: (skjæringstidspunkt: LocalDate) -> Sykepengegrunnlag
    ) {
        lagretVilkårsgrunnlag = true
        inntekter.fjern(nødnummer).lagreVilkårsgrunnlag(vilkårsgrunnlagHistorikk, sykepengegrunnlagFor)
    }

    internal fun valider(aktivitetslogg: IAktivitetslogg, periodetype: Periodetype, periode: Periode, skjæringstidspunkt: LocalDate, organisasjonsnummer: String): Boolean {
        validerUgyldigePerioder(aktivitetslogg)
        if (periodetype == Periodetype.OVERGANG_FRA_IT) {
            aktivitetslogg.info("Perioden er en direkte overgang fra periode med opphav i Infotrygd")
        }
        validerStatslønn(aktivitetslogg, periodetype)
        if (harNyereOpplysninger(organisasjonsnummer, periode))
            aktivitetslogg.warn("Det er utbetalt en periode i Infotrygd etter perioden du skal behandle nå. Undersøk at antall forbrukte dager og grunnlag i Infotrygd er riktig")
        return valider(aktivitetslogg, perioder, periode, skjæringstidspunkt)
    }

    internal fun validerOverlappende(aktivitetslogg: IAktivitetslogg, periode: Periode, skjæringstidspunkt: LocalDate): Boolean {
        aktivitetslogg.info("Sjekker utbetalte perioder for overlapp mot %s", periode)
        return valider(aktivitetslogg, perioder, periode, skjæringstidspunkt)
    }

    private fun validerUgyldigePerioder(aktivitetslogg: IAktivitetslogg) {
        ugyldigePerioder.forEach { ugyldigPeriode -> ugyldigPeriode.valider(aktivitetslogg)}
    }

    private fun validerStatslønn(aktivitetslogg: IAktivitetslogg, periodetype: Periodetype) {
        if (harStatslønn && periodetype in arrayOf(Periodetype.OVERGANG_FRA_IT, Periodetype.INFOTRYGDFORLENGELSE)) {
            aktivitetslogg.error("Det er lagt inn statslønn i Infotrygd, undersøk at utbetalingen blir riktig.")
        }
    }

    private fun valider(aktivitetslogg: IAktivitetslogg, perioder: List<Infotrygdperiode>, periode: Periode, skjæringstidspunkt: LocalDate): Boolean {
        aktivitetslogg.info("Sjekker utbetalte perioder")
        perioder.filterIsInstance<Utbetalingsperiode>().forEach { it.valider(aktivitetslogg, periode, skjæringstidspunkt, nødnummer) }

        aktivitetslogg.info("Sjekker inntektsopplysninger")
        Inntektsopplysning.valider(inntekter, aktivitetslogg, skjæringstidspunkt, nødnummer)

        aktivitetslogg.info("Sjekker at alle utbetalte perioder har inntektsopplysninger")
        perioder.validerInntektForPerioder(aktivitetslogg, inntekter, nødnummer)

        aktivitetslogg.info("Sjekker arbeidskategorikoder")
        if (!erNormalArbeidstaker(skjæringstidspunkt)) aktivitetslogg.error("Personen er ikke registrert som normal arbeidstaker i Infotrygd")

        return !aktivitetslogg.hasErrorsOrWorse()
    }

    internal fun utbetalingstidslinje() =
        perioder
            .map { it.utbetalingstidslinje() }
            .fold(Utbetalingstidslinje(), Utbetalingstidslinje::plus)

    private val utbetalingstidslinjeoppslag = mutableMapOf<String, Utbetalingstidslinje>()

    internal fun utbetalingstidslinje(organisasjonsnummer: String) =
        utbetalingstidslinjeoppslag.computeIfAbsent(organisasjonsnummer) {
            perioder
                .filter { it.gjelder(organisasjonsnummer) }
                .map { it.utbetalingstidslinje() }
                .fold(Utbetalingstidslinje(), Utbetalingstidslinje::plus)
        }

    internal fun fjernHistorikk(utbetalingstidlinje: Utbetalingstidslinje, organisasjonsnummer: String): Utbetalingstidslinje {
        return utbetalingstidlinje.plus(utbetalingstidslinje(organisasjonsnummer).subset(utbetalingstidlinje.periode())) { spleisDag: Utbetalingstidslinje.Utbetalingsdag, infotrygdDag: Utbetalingstidslinje.Utbetalingsdag ->
            when {
                // fjerner utbetalinger i ukedager (bevarer fridager)
                !infotrygdDag.dato.erHelg() && infotrygdDag is NavDag -> UkjentDag(spleisDag.dato, spleisDag.økonomi)
                // fjerner utbetalinger i helger (bevarer fridager)
                infotrygdDag.dato.erHelg() && infotrygdDag !is Fridag -> UkjentDag(spleisDag.dato, spleisDag.økonomi)
                else -> spleisDag
            }
        }
    }

    internal fun harBetaltRettFør(periode: Periode): Boolean {
        return perioder.filterIsInstance<Utbetalingsperiode>().any {
            it.erRettFør(periode)
        }
    }

    internal fun sisteSykepengedag(orgnummer: String): LocalDate? {
        return utbetalinger(orgnummer).maxOfOrNull { it.endInclusive }
    }

    private fun harNyereOpplysninger(orgnummer: String, periode: Periode): Boolean {
        return utbetalinger(orgnummer).any { it.slutterEtter(periode.endInclusive) }
    }

    private fun utbetalinger(organisasjonsnummer: String) =
        perioder.filterIsInstance<Utbetalingsperiode>().filter { it.gjelder(organisasjonsnummer) }

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
    internal fun harBrukerutbetalingerFor(organisasjonsnummer: String, periode: Periode) = perioder.harBrukerutbetalingFor(organisasjonsnummer, periode)
}
