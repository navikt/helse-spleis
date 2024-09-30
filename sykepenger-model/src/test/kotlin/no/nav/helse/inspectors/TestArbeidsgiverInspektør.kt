package no.nav.helse.inspectors

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.dto.SimuleringResultatDto
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.ArbeidsgiverVisitor
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.ForkastetVedtaksperiode
import no.nav.helse.person.IdInnhenter
import no.nav.helse.person.Person
import no.nav.helse.person.TilstandType
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.inntekt.Refusjonshistorikk.Refusjon.EndringIRefusjon.Companion.refusjonsopplysninger
import no.nav.helse.person.inntekt.Refusjonsopplysning.Refusjonsopplysninger
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.utbetalingslinjer.Endringskode
import no.nav.helse.utbetalingslinjer.Fagområde
import no.nav.helse.utbetalingslinjer.Feriepengeutbetaling
import no.nav.helse.utbetalingslinjer.Klassekode
import no.nav.helse.utbetalingslinjer.Oppdrag
import no.nav.helse.utbetalingslinjer.Oppdragstatus
import no.nav.helse.utbetalingslinjer.Satstype
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingslinjer.Utbetalingslinje
import no.nav.helse.utbetalingslinjer.Utbetalingstatus
import org.junit.jupiter.api.fail

internal class TestArbeidsgiverInspektør(
    private val person: Person,
    val orgnummer: String
) : ArbeidsgiverVisitor {
    internal companion object {
        internal operator fun TestArbeidsgiverInspektør.invoke(blokk: TestArbeidsgiverInspektør.() -> Unit) {
            this.apply(blokk)
        }
    }
    internal var arbeidsgiver: Arbeidsgiver = person.arbeidsgivere.first { it.organisasjonsnummer() == orgnummer }

    private val personInspektør = person.inspektør
    internal var vedtaksperiodeTeller: Int = 0
        private set
    private var vedtaksperiodeindeks = 0
    private val tilstander = mutableMapOf<Int, TilstandType>()
    private val vedtaksperiodeindekser = mutableMapOf<UUID, Int>()
    private val vedtaksperiodeForkastet = mutableMapOf<Int, Boolean>()
    internal val inntektInspektør get() = InntektshistorikkInspektør(arbeidsgiver.inspektør.inntektshistorikk)
    val sykdomshistorikk = arbeidsgiver.view().sykdomshistorikk.inspektør
    internal val sykdomstidslinje: Sykdomstidslinje get() = sykdomshistorikk.tidslinje(0)
    private val utbetalinger = arbeidsgiver.view().utbetalinger.map { it.inspektør }
    internal val antallUtbetalinger get() = utbetalinger.size
    internal val feriepengeoppdrag = mutableListOf<Feriepengeoppdrag>()
    internal val infotrygdFeriepengebeløpPerson = mutableListOf<Double>()
    internal val infotrygdFeriepengebeløpArbeidsgiver = mutableListOf<Double>()
    internal val spleisFeriepengebeløpArbeidsgiver = mutableListOf<Double>()
    internal val spleisFeriepengebeløpPerson = mutableListOf<Double>()
    private val vedtaksperioder = mutableMapOf<UUID, Vedtaksperiode>()
    private var forkastetPeriode = false
    private var inFeriepengeutbetaling = false
    private val sykmeldingsperioder = mutableListOf<Periode>()

    internal fun vilkårsgrunnlagHistorikkInnslag() = person.vilkårsgrunnlagHistorikk.inspektør.vilkårsgrunnlagHistorikkInnslag()

    init {
        this.arbeidsgiver.accept(this)
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
        hendelseIder: Set<Dokumentsporing>,
        egenmeldingsperioder: List<Periode>
    ) {
        vedtaksperiodeTeller += 1
        vedtaksperiodeindekser[id] = vedtaksperiodeindeks
        vedtaksperiodeForkastet[vedtaksperiodeindeks] = forkastetPeriode
        vedtaksperioder[id] = vedtaksperiode
        tilstander[vedtaksperiodeindeks] = tilstand.type
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
        hendelseIder: Set<Dokumentsporing>
    ) {
        vedtaksperiodeindeks += 1
    }

    override fun preVisitOppdrag(
        oppdrag: Oppdrag,
        fagområde: Fagområde,
        fagsystemId: String,
        mottaker: String,
        nettoBeløp: Int,
        tidsstempel: LocalDateTime,
        endringskode: Endringskode,
        avstemmingsnøkkel: Long?,
        status: Oppdragstatus?,
        overføringstidspunkt: LocalDateTime?,
        erSimulert: Boolean,
        simuleringsResultat: SimuleringResultatDto?
    ) {
        if (inFeriepengeutbetaling) feriepengeoppdrag.add(Feriepengeoppdrag(oppdrag.fagsystemId()))
    }

    override fun visitUtbetalingslinje(
        linje: Utbetalingslinje,
        fom: LocalDate,
        tom: LocalDate,
        satstype: Satstype,
        beløp: Int?,
        grad: Int?,
        delytelseId: Int,
        refDelytelseId: Int?,
        refFagsystemId: String?,
        endringskode: Endringskode,
        datoStatusFom: LocalDate?,
        statuskode: String?,
        klassekode: Klassekode
    ) {
        if(inFeriepengeutbetaling) {
            feriepengeoppdrag
                .lastOrNull()
                ?.feriepengeutbetalingslinjer
                ?.add(Feriepengeutbetalingslinje(fom, tom, satstype, beløp, grad, klassekode, endringskode, statuskode))
        }
    }

    internal data class Feriepengeoppdrag(
        val fagsystemId: String,
        val feriepengeutbetalingslinjer: MutableList<Feriepengeutbetalingslinje> = mutableListOf()
    ) {
        internal companion object {
            val List<Feriepengeoppdrag>.utbetalingslinjer get(): List<Feriepengeutbetalingslinje> {
                val sisteOppdragPerFagsystemId = groupBy { it.fagsystemId }.map { (_, oppdrag) -> oppdrag.last() }
                return sisteOppdragPerFagsystemId.flatMap { it.feriepengeutbetalingslinjer }
            }
        }
    }

    internal data class Feriepengeutbetalingslinje(
        val fom: LocalDate,
        val tom: LocalDate,
        val satstype: Satstype,
        val beløp: Int?,
        val grad: Int?,
        val klassekode: Klassekode,
        val endringskode: Endringskode,
        val statuskode: String? = null
    )

    override fun preVisitFeriepengeutbetaling(
        feriepengeutbetaling: Feriepengeutbetaling,
        infotrygdFeriepengebeløpPerson: Double,
        infotrygdFeriepengebeløpArbeidsgiver: Double,
        spleisFeriepengebeløpArbeidsgiver: Double,
        spleisFeriepengebeløpPerson: Double,
        overføringstidspunkt: LocalDateTime?,
        avstemmingsnøkkel: Long?,
        utbetalingId: UUID,
        sendTilOppdrag: Boolean,
        sendPersonoppdragTilOS: Boolean,
    ) {
        inFeriepengeutbetaling = true
        this.infotrygdFeriepengebeløpArbeidsgiver.add(infotrygdFeriepengebeløpArbeidsgiver)
        this.infotrygdFeriepengebeløpPerson.add(infotrygdFeriepengebeløpPerson)
        this.spleisFeriepengebeløpArbeidsgiver.add(spleisFeriepengebeløpArbeidsgiver)
        this.spleisFeriepengebeløpPerson.add(spleisFeriepengebeløpPerson)
    }

    override fun postVisitFeriepengeutbetaling(
        feriepengeutbetaling: Feriepengeutbetaling,
        infotrygdFeriepengebeløpPerson: Double,
        infotrygdFeriepengebeløpArbeidsgiver: Double,
        spleisFeriepengebeløpArbeidsgiver: Double,
        spleisFeriepengebeløpPerson: Double,
        overføringstidspunkt: LocalDateTime?,
        avstemmingsnøkkel: Long?,
        utbetalingId: UUID,
        sendTilOppdrag: Boolean,
        sendPersonoppdragTilOS: Boolean,
    ) {
        inFeriepengeutbetaling = false
    }

    override fun visitSykmeldingsperiode(periode: Periode) {
        this.sykmeldingsperioder.add(periode)
    }

    private fun <V> IdInnhenter.finn(hva: Map<Int, V>) = hva.getValue(this.indeks)
    private val IdInnhenter.indeks get() = id(orgnummer).indeks

    private fun <V> UUID.finn(hva: Map<Int, V>) = hva.getValue(this.indeks)
    private val UUID.indeks get() = vedtaksperiodeindekser[this] ?: fail { "Vedtaksperiode $this finnes ikke" }

    internal fun sisteAvsluttedeUtbetalingForVedtaksperiode(vedtaksperiodeIdInnhenter: IdInnhenter) = avsluttedeUtbetalingerForVedtaksperiode(vedtaksperiodeIdInnhenter).last()
    internal fun gjeldendeUtbetalingForVedtaksperiode(vedtaksperiodeIdInnhenter: IdInnhenter) = vedtaksperioder(vedtaksperiodeIdInnhenter).inspektør.utbetalinger.last()
    internal fun ikkeUtbetalteUtbetalingerForVedtaksperiode(vedtaksperiodeIdInnhenter: IdInnhenter) = ikkeUtbetalteUtbetalingerForVedtaksperiode(vedtaksperiodeIdInnhenter.id(orgnummer))
    internal fun ikkeUtbetalteUtbetalingerForVedtaksperiode(vedtaksperiodeId: UUID) = vedtaksperioder(vedtaksperiodeId).inspektør.utbetalinger.filter { it.inspektør.erUbetalt }
    internal fun avsluttedeUtbetalingerForVedtaksperiode(vedtaksperiodeIdInnhenter: IdInnhenter) = avsluttedeUtbetalingerForVedtaksperiode(vedtaksperiodeIdInnhenter.id(orgnummer))
    internal fun avsluttedeUtbetalingerForVedtaksperiode(vedtaksperiodeId: UUID) = vedtaksperioder(vedtaksperiodeId).inspektør.utbetalinger.filter { it.erAvsluttet() }
    internal fun utbetalinger(vedtaksperiodeIdInnhenter: IdInnhenter) = utbetalinger(vedtaksperiodeIdInnhenter.id(orgnummer))
    internal fun utbetalinger(vedtaksperiodeId: UUID) = vedtaksperioder(vedtaksperiodeId).inspektør.utbetalinger

    internal fun utbetalingerInFlight() = utbetalinger.filter { it.tilstand == Utbetalingstatus.OVERFØRT }
    internal fun sisteUtbetaling() = utbetalinger.last()
    internal fun utbetalingtilstand(indeks: Int) = utbetalinger[indeks].tilstand
    internal fun utbetaling(indeks: Int) = utbetalinger[indeks]
    internal fun utbetalingId(indeks: Int) = utbetalinger[indeks].utbetalingId
    internal fun utbetalingUtbetalingstidslinje(indeks: Int) = utbetalinger[indeks].utbetalingstidslinje
    internal fun sisteUtbetalingUtbetalingstidslinje() = utbetalinger.last().utbetalingstidslinje
    internal fun utbetalingslinjer(indeks: Int) = utbetalinger[indeks].arbeidsgiverOppdrag

    internal fun periode(vedtaksperiodeIdInnhenter: IdInnhenter) = periode(vedtaksperiodeIdInnhenter.id(orgnummer))
    internal fun periode(vedtaksperiodeId: UUID) = vedtaksperioder(vedtaksperiodeId).inspektør.periode
    internal fun vedtaksperiodeSykdomstidslinje(vedtaksperiodeIdInnhenter: IdInnhenter) = vedtaksperioder(vedtaksperiodeIdInnhenter).inspektør.sykdomstidslinje

    internal fun periodeErForkastet(vedtaksperiodeIdInnhenter: IdInnhenter) = vedtaksperiodeIdInnhenter.finn(vedtaksperiodeForkastet)
    internal fun periodeErForkastet(vedtaksperiodeId: UUID) = vedtaksperiodeId.finn(vedtaksperiodeForkastet)

    internal fun periodeErIkkeForkastet(vedtaksperiodeIdInnhenter: IdInnhenter) = !periodeErForkastet(vedtaksperiodeIdInnhenter)
    internal fun periodeErIkkeForkastet(vedtaksperiodeId: UUID) = !periodeErForkastet(vedtaksperiodeId)

    internal fun sisteMaksdato(vedtaksperiodeIdInnhenter: IdInnhenter) = sisteMaksdato(vedtaksperiodeIdInnhenter.id(orgnummer))
    internal fun sisteMaksdato(vedtaksperiodeId: UUID) = vedtaksperioder(vedtaksperiodeId).inspektør.maksdatoer.last()

    internal fun sisteUtbetalingId(vedtaksperiodeIdInnhenter: IdInnhenter) = vedtaksperioder(vedtaksperiodeIdInnhenter).inspektør.utbetalinger.last().id

    internal fun vilkårsgrunnlag(vedtaksperiodeIdInnhenter: IdInnhenter) = person.vilkårsgrunnlagFor(skjæringstidspunkt(vedtaksperiodeIdInnhenter))
    internal fun vilkårsgrunnlag(vedtaksperiodeId: UUID) = person.vilkårsgrunnlagFor(skjæringstidspunkt(vedtaksperiodeId))
    internal fun vilkårsgrunnlag(skjæringstidspunkt: LocalDate) = person.vilkårsgrunnlagFor(skjæringstidspunkt)

    internal fun sisteTilstand(vedtaksperiodeIdInnhenter: IdInnhenter) = vedtaksperiodeIdInnhenter.finn(tilstander)

    internal fun skjæringstidspunkt(vedtaksperiodeIdInnhenter: IdInnhenter) = skjæringstidspunkt(vedtaksperiodeIdInnhenter.id(orgnummer))
    internal fun skjæringstidspunkt(vedtaksperiodeId: UUID) = vedtaksperioder(vedtaksperiodeId).inspektør.skjæringstidspunkt

    internal fun utbetalingstidslinjer(vedtaksperiodeIdInnhenter: IdInnhenter) = utbetalingstidslinjer(vedtaksperiodeIdInnhenter.id(orgnummer))
    internal fun utbetalingstidslinjer(vedtaksperiodeId: UUID) = vedtaksperioder(vedtaksperiodeId).inspektør.utbetalingstidslinje

    internal fun vedtaksperioder(vedtaksperiodeIdInnhenter: IdInnhenter) = vedtaksperioder.getValue(vedtaksperiodeIdInnhenter.id(orgnummer))
    internal fun vedtaksperioder(vedtaksperiodeId: UUID) = vedtaksperioder.getValue(vedtaksperiodeId)

    internal fun hendelser(vedtaksperiodeIdInnhenter: IdInnhenter) = vedtaksperioder(vedtaksperiodeIdInnhenter.id(orgnummer)).inspektør.hendelser
    internal fun hendelseIder(vedtaksperiodeIdInnhenter: IdInnhenter) = hendelseIder(vedtaksperiodeIdInnhenter.id(orgnummer))
    internal fun hendelseIder(vedtaksperiodeId: UUID) = vedtaksperioder(vedtaksperiodeId).inspektør.hendelseIder

    internal fun sisteArbeidsgiveroppdragFagsystemId(vedtaksperiodeIdInnhenter: IdInnhenter) = vedtaksperioder(vedtaksperiodeIdInnhenter).inspektør.utbetalinger.last().arbeidsgiverOppdrag().fagsystemId()

    internal fun inntektskilde(vedtaksperiodeIdInnhenter: IdInnhenter) = vilkårsgrunnlag(vedtaksperiodeIdInnhenter)?.inntektskilde()
    internal fun inntektskilde(vedtaksperiodeId: UUID) = vilkårsgrunnlag(vedtaksperiodeId)?.inntektskilde()

    internal fun vedtaksperiodeId(vedtaksperiodeIdInnhenter: IdInnhenter) = vedtaksperiodeIdInnhenter.id(orgnummer)

    internal fun sykmeldingsperioder() = sykmeldingsperioder.toList()

    internal fun arbeidsgiverperioden(vedtaksperiodeIdInnhenter: IdInnhenter) = vedtaksperioder(vedtaksperiodeIdInnhenter).inspektør.arbeidsgiverperiode
    internal fun arbeidsgiverperioder(vedtaksperiodeIdInnhenter: IdInnhenter) = arbeidsgiverperioden(vedtaksperiodeIdInnhenter)
    internal fun arbeidsgiverperiode(vedtaksperiodeIdInnhenter: IdInnhenter) = arbeidsgiverperioder(vedtaksperiodeIdInnhenter)
    private fun <R> Collection<R>.singleOrNullOrThrow() = if (size < 2) this.firstOrNull() else throw IllegalStateException("Listen inneholder $size elementer: $this")

    internal fun refusjonsopplysningerFraVilkårsgrunnlag(skjæringstidspunkt: LocalDate = person.vilkårsgrunnlagHistorikk.inspektør.aktiveSpleisSkjæringstidspunkt.max()) =
        personInspektør.vilkårsgrunnlagHistorikk.grunnlagsdata(skjæringstidspunkt).inspektør.inntektsgrunnlag.inspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver[orgnummer]?.inspektør?.refusjonsopplysninger ?: Refusjonsopplysninger()
    internal fun refusjonsopplysningerFraRefusjonshistorikk(skjæringstidspunkt: LocalDate = person.vilkårsgrunnlagHistorikk.inspektør.aktiveSpleisSkjæringstidspunkt.max()) =
        arbeidsgiver.inspektør.refusjonshistorikk.refusjonsopplysninger(skjæringstidspunkt)

}
