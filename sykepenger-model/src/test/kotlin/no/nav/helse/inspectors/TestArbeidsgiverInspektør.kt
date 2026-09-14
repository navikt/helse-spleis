package no.nav.helse.inspectors

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.feriepenger.Feriepengerendringskode
import no.nav.helse.feriepenger.Feriepengerklassekode
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.ForkastetVedtaksperiode
import no.nav.helse.person.Person
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.Yrkesaktivitet
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.utbetalingslinjer.Utbetalingstatus
import org.junit.jupiter.api.fail

internal class TestArbeidsgiverInspektør(
    private val person: Person,
    val orgnummer: String
) {
    internal companion object {
        internal operator fun TestArbeidsgiverInspektør.invoke(blokk: TestArbeidsgiverInspektør.() -> Unit) {
            this.apply(blokk)
        }
    }

    internal var yrkesaktivitet: Yrkesaktivitet = person.yrkesaktiviteter.first { it.organisasjonsnummer() == orgnummer }

    private val personInspektør = person.inspektør
    internal val vedtaksperiodeTeller: Int = yrkesaktivitet.vedtaksperioder.size + yrkesaktivitet.forkastede.size
    private val vedtaksperioder: Map<UUID, Vedtaksperiode> = (yrkesaktivitet.vedtaksperioder + yrkesaktivitet.forkastede.map(ForkastetVedtaksperiode::vedtaksperiode))
        .associateBy { it.id }
    private val tilstander = (yrkesaktivitet.vedtaksperioder + yrkesaktivitet.forkastede.map(ForkastetVedtaksperiode::vedtaksperiode))
        .mapIndexed { index, periode -> index to periode.tilstand.type }
        .toMap()

    private val vedtaksperiodeindekser = (yrkesaktivitet.vedtaksperioder + yrkesaktivitet.forkastede.map(ForkastetVedtaksperiode::vedtaksperiode)).mapIndexed { index, periode ->
        periode.id to index
    }.toMap()

    private val vedtaksperiodeForkastet = yrkesaktivitet.forkastede.map { it.vedtaksperiode.id }.toSet()
    internal val inntektInspektør get() = InntektshistorikkInspektør(yrkesaktivitet.inntektshistorikk)
    val sykdomshistorikk = yrkesaktivitet.sykdomshistorikk.inspektør
    internal val sykdomstidslinje: Sykdomstidslinje get() = sykdomshistorikk.tidslinje(0)
    internal val utbetalinger = yrkesaktivitet.utbetalinger.map { it.inspektør }
    internal val antallUtbetalinger get() = utbetalinger.size

    val ubrukteRefusjonsopplysninger = yrkesaktivitet.ubrukteRefusjonsopplysninger

    internal val feriepengeoppdrag = yrkesaktivitet.feriepengeutbetalinger
        .flatMap { listOf(it.oppdrag, it.personoppdrag) }
        .map {
            Feriepengeoppdrag(
                fagsystemId = it.fagsystemId,
                feriepengeutbetalingslinjer = listOfNotNull(it.linje?.let { linje ->
                    Feriepengeutbetalingslinje(linje.fom, linje.tom, linje.beløp, linje.klassekode, linje.endringskode, linje.statuskode)
                })
            )
        }
    internal val infotrygdFeriepengebeløpPerson = yrkesaktivitet.feriepengeutbetalinger.map { it.infotrygdFeriepengebeløpPerson }
    internal val infotrygdFeriepengebeløpArbeidsgiver = yrkesaktivitet.feriepengeutbetalinger.map { it.infotrygdFeriepengebeløpArbeidsgiver }
    internal val spleisFeriepengebeløpArbeidsgiver = yrkesaktivitet.feriepengeutbetalinger.map { it.spleisFeriepengebeløpArbeidsgiver }
    internal val spleisFeriepengebeløpPerson = yrkesaktivitet.feriepengeutbetalinger.map { it.spleisFeriepengebeløpPerson }

    private val sykmeldingsperioder = yrkesaktivitet.sykmeldingsperioder.perioder()

    internal fun vilkårsgrunnlaghistorikk() = person.vilkårsgrunnlagHistorikk.inspektør
    internal fun vilkårsgrunnlagHistorikkInnslag() = vilkårsgrunnlaghistorikk().vilkårsgrunnlagHistorikkInnslag()

    internal data class Feriepengeoppdrag(
        val fagsystemId: String,
        val feriepengeutbetalingslinjer: List<Feriepengeutbetalingslinje>
    ) {
        internal companion object {
            val List<Feriepengeoppdrag>.utbetalingslinjer
                get(): List<Feriepengeutbetalingslinje> {
                    val sisteOppdragPerFagsystemId = groupBy { it.fagsystemId }.map { (_, oppdrag) -> oppdrag.last() }
                    return sisteOppdragPerFagsystemId.flatMap { it.feriepengeutbetalingslinjer }
                }
        }
    }

    internal data class Feriepengeutbetalingslinje(
        val fom: LocalDate,
        val tom: LocalDate,
        val beløp: Int?,
        val klassekode: Feriepengerklassekode,
        val endringskode: Feriepengerendringskode,
        val statuskode: String? = null
    )

    private fun <V> UUID.finn(hva: Map<Int, V>) = hva.getValue(this.indeks)
    private val UUID.indeks get() = vedtaksperiodeindekser[this] ?: fail { "Vedtaksperiode $this finnes ikke" }

    internal fun sisteAvsluttedeUtbetalingForVedtaksperiode(vedtaksperiodeId: UUID) = avsluttedeUtbetalingerForVedtaksperiode(vedtaksperiodeId).last()
    internal fun ikkeUtbetalteUtbetalingerForVedtaksperiode(vedtaksperiodeId: UUID) = vedtaksperioder(vedtaksperiodeId).inspektør.utbetalinger.filter { it.inspektør.erUbetalt }
    internal fun avsluttedeUtbetalingerForVedtaksperiode(vedtaksperiodeId: UUID) = vedtaksperioder(vedtaksperiodeId).inspektør.utbetalinger.filter { it.erAvsluttet() }
    internal fun utbetalinger(vedtaksperiodeId: UUID) = vedtaksperioder(vedtaksperiodeId).inspektør.utbetalinger

    internal fun utbetalingerInFlight() = utbetalinger.filter { it.tilstand == Utbetalingstatus.OVERFØRT }
    internal fun sisteUtbetaling() = utbetalinger.last()
    internal fun utbetalingtilstand(indeks: Int) = utbetalinger[indeks].tilstand
    internal fun utbetaling(indeks: Int) = utbetalinger[indeks]
    internal fun utbetalingId(indeks: Int) = utbetalinger[indeks].utbetalingId
    internal fun utbetalingslinjer(indeks: Int) = utbetalinger[indeks].arbeidsgiverOppdrag

    internal fun periode(vedtaksperiodeId: UUID) = vedtaksperioder(vedtaksperiodeId).inspektør.periode
    internal fun vedtaksperiodeSykdomstidslinje(vedtaksperiodeId: UUID) = vedtaksperioder(vedtaksperiodeId).inspektør.sykdomstidslinje

    internal fun periodeErForkastet(vedtaksperiodeId: UUID) = vedtaksperiodeId in vedtaksperiodeForkastet

    internal fun periodeErIkkeForkastet(vedtaksperiodeId: UUID) = !periodeErForkastet(vedtaksperiodeId)

    internal fun sisteMaksdato(vedtaksperiodeId: UUID) = vedtaksperioder(vedtaksperiodeId).inspektør.maksdatoer.last()

    internal fun sisteUtbetalingId(vedtaksperiodeId: UUID) = vedtaksperioder(vedtaksperiodeId).inspektør.utbetalinger.last().id

    internal fun vilkårsgrunnlag(vedtaksperiodeId: UUID) = person.vilkårsgrunnlagFor(skjæringstidspunkt(vedtaksperiodeId))
    internal fun vilkårsgrunnlag(skjæringstidspunkt: LocalDate) = person.vilkårsgrunnlagFor(skjæringstidspunkt)

    internal fun sisteTilstand(vedtaksperiodeId: UUID) = vedtaksperiodeId.finn(tilstander)

    internal fun skjæringstidspunkt(vedtaksperiodeId: UUID) = vedtaksperioder(vedtaksperiodeId).inspektør.skjæringstidspunkt

    internal fun skjæringstidspunkter(vedtaksperiodeId: UUID) = vedtaksperioder(vedtaksperiodeId).inspektør.skjæringstidspunkter

    internal fun førsteFraværsdag(vedtaksperiodeId: UUID) = vedtaksperioder(vedtaksperiodeId).inspektør.førsteFraværsdag

    internal fun utbetalingstidslinjer(vedtaksperiodeId: UUID) = vedtaksperioder(vedtaksperiodeId).inspektør.utbetalingstidslinje

    internal fun vedtaksperioder(vedtaksperiodeId: UUID) = vedtaksperioder.getValue(vedtaksperiodeId)
    internal fun vedtaksperioder(periode: Periode) = vedtaksperioder.values.first { it.periode == periode }

    internal fun hendelser(vedtaksperiodeId: UUID) = vedtaksperioder(vedtaksperiodeId).inspektør.hendelser
    internal fun hendelseIder(vedtaksperiodeId: UUID) = vedtaksperioder(vedtaksperiodeId).inspektør.hendelseIder.map { it.id }.toSet()

    internal fun sisteArbeidsgiveroppdragFagsystemId(vedtaksperiodeId: UUID) = vedtaksperioder(vedtaksperiodeId).inspektør.utbetalinger.last().arbeidsgiverOppdrag.fagsystemId


    internal fun sykmeldingsperioder() = sykmeldingsperioder.toList()

    internal fun venteperiode(vedtaksperiodeId: UUID) = vedtaksperioder(vedtaksperiodeId).inspektør.dagerUtenNavAnsvar

    internal fun egenmeldingsdager(vedtaksperiodeId: UUID) = vedtaksperioder(vedtaksperiodeId).inspektør.egenmeldingsperioder

    internal fun refusjon(vedtaksperiodeId: UUID) = vedtaksperioder.getValue(vedtaksperiodeId).refusjonstidslinje

    internal fun dagerNavOvertarAnsvar(vedtaksperiodeId: UUID) = vedtaksperioder.getValue(vedtaksperiodeId).inspektør.dagerNavOvertarAnsvar

    internal fun faktaavklartInntekt(vedtaksperiodeId: UUID) = vedtaksperioder.getValue(vedtaksperiodeId).inspektør.faktaavklartInntekt
    internal fun korrigertInntekt(vedtaksperiodeId: UUID) = vedtaksperioder.getValue(vedtaksperiodeId).inspektør.korrigertInntekt
}
