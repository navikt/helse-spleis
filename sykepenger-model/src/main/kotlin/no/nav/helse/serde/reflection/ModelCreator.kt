package no.nav.helse.serde.reflection

import no.nav.helse.hendelser.Simulering
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.person.*
import no.nav.helse.serde.PersonData.ArbeidsgiverData.NySykdomstidslinjeData
import no.nav.helse.sykdomstidslinje.NySykdomstidslinje
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.utbetalingslinjer.*
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible

internal fun createPerson(
    aktørId: String,
    fødselsnummer: String,
    arbeidsgivere: MutableList<Arbeidsgiver>,
    aktivitetslogg: Aktivitetslogg
) = Person::class.primaryConstructor!!
    .apply { isAccessible = true }
    .call(aktørId, fødselsnummer, arbeidsgivere, aktivitetslogg)

internal fun createArbeidsgiver(
    person: Person,
    organisasjonsnummer: String,
    id: UUID,
    inntekthistorikk: Inntekthistorikk,
    perioder: MutableList<Vedtaksperiode>,
    utbetalinger: MutableList<Utbetaling>
) = Arbeidsgiver::class.primaryConstructor!!
    .apply { isAccessible = true }
    .call(person, organisasjonsnummer, id, inntekthistorikk, perioder, utbetalinger)

internal fun createVedtaksperiode(
    person: Person,
    arbeidsgiver: Arbeidsgiver,
    id: UUID,
    gruppeId: UUID,
    aktørId: String,
    fødselsnummer: String,
    organisasjonsnummer: String,
    tilstand: Vedtaksperiode.Vedtaksperiodetilstand,
    maksdato: LocalDate?,
    gjenståendeSykedager: Int?,
    forbrukteSykedager: Int?,
    godkjentAv: String?,
    godkjenttidspunkt: LocalDateTime?,
    førsteFraværsdag: LocalDate?,
    dataForVilkårsvurdering: Vilkårsgrunnlag.Grunnlagsdata?,
    dataForSimulering: Simulering.SimuleringResultat?,
    sykdomshistorikk: Sykdomshistorikk,
    utbetalingstidslinje: Utbetalingstidslinje?,
    personFagsystemId: String?,
    arbeidsgiverFagsystemId: String?
) = Vedtaksperiode::class.primaryConstructor!!
    .apply { isAccessible = true }
    .call(
        person,
        arbeidsgiver,
        id,
        gruppeId,
        aktørId,
        fødselsnummer,
        organisasjonsnummer,
        tilstand,
        maksdato,
        gjenståendeSykedager,
        forbrukteSykedager,
        godkjentAv,
        godkjenttidspunkt,
        førsteFraværsdag,
        dataForVilkårsvurdering,
        dataForSimulering,
        sykdomshistorikk,
        utbetalingstidslinje,
        personFagsystemId,
        arbeidsgiverFagsystemId
    )

internal fun createUtbetaling(
    utbetalingstidslinje: Utbetalingstidslinje,
    arbeidsgiverOppdrag: Oppdrag,
    personOppdrag: Oppdrag,
    tidsstempel: LocalDateTime
) = Utbetaling::class.primaryConstructor!!
    .apply { isAccessible = true }
    .call(utbetalingstidslinje, arbeidsgiverOppdrag, personOppdrag, tidsstempel)

internal fun createOppdrag(
    mottaker: String,
    fagområde: Fagområde,
    linjer: List<Utbetalingslinje>,
    fagsystemId: String,
    endringskode: Endringskode,
    sisteArbeidsgiverdag: LocalDate? = null,
    sjekksum: Int
) = Oppdrag::class.primaryConstructor!!
    .apply { isAccessible = true }
    .call(mottaker, fagområde, linjer, fagsystemId, endringskode, sisteArbeidsgiverdag, sjekksum)

internal fun createUtbetalingslinje(
    fom: LocalDate,
    tom: LocalDate,
    dagsats: Int,
    lønn: Int,
    grad: Double,
    refFagsystemId: String? = null,
    delytelseId: Int,
    refDelytelseId: Int? = null,
    endringskode: Endringskode,
    klassekode: Klassekode,
    datoStatusFom: LocalDate?
) = Utbetalingslinje::class.primaryConstructor!!
    .apply { isAccessible = true }
    .call(fom, tom, dagsats, lønn, grad, refFagsystemId, delytelseId, refDelytelseId, endringskode, klassekode, datoStatusFom)

internal fun createSykdomshistorikk(
    elementer: List<Sykdomshistorikk.Element>
) = Sykdomshistorikk::class.primaryConstructor!!
    .apply { isAccessible = true }
    .call(elementer)

internal fun createSykdomstidslinje(
    tidslinje: NySykdomstidslinjeData
) = NySykdomstidslinje::class.primaryConstructor!!
    .apply { isAccessible = true }
    .call(
        tidslinje.dagerMap,
        null,
        tidslinje.låstePerioder,
        tidslinje.id,
        tidslinje.tidsstempel
    )

internal fun createSykdomshistorikkElement(
    timestamp: LocalDateTime,
    hendelseSykdomstidslinje: Sykdomstidslinje,
    beregnetSykdomstidslinje: Sykdomstidslinje,
    nyHendelseSykdomstidslinje: NySykdomstidslinje,
    nyBeregnetSykdomstidslinje: NySykdomstidslinje,
    hendelseId: UUID
) = Sykdomshistorikk.Element::class.primaryConstructor!!
    .apply { isAccessible = true }
    .call(hendelseId, timestamp, hendelseSykdomstidslinje, beregnetSykdomstidslinje, nyHendelseSykdomstidslinje, nyBeregnetSykdomstidslinje)

internal fun createUtbetalingstidslinje(
    utbetalingsdager: MutableList<Utbetalingstidslinje.Utbetalingsdag>
) = Utbetalingstidslinje::class.primaryConstructor!!
    .apply { isAccessible = true }
    .call(utbetalingsdager)

internal fun createNavUtbetalingdag(
    inntekt: Int,
    dato: LocalDate,
    utbetaling: Int,
    grad: Double
) = Utbetalingstidslinje.Utbetalingsdag.NavDag::class.primaryConstructor!!
    .apply { isAccessible = true }
    .call(inntekt, dato, utbetaling, grad)
