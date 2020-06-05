package no.nav.helse.serde

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Simulering
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.person.*
import no.nav.helse.person.Vedtaksperiode.*
import no.nav.helse.serde.PersonData.ArbeidsgiverData
import no.nav.helse.serde.PersonData.ArbeidsgiverData.SykdomstidslinjeData
import no.nav.helse.serde.PersonData.UtbetalingstidslinjeData
import no.nav.helse.serde.mapping.JsonMedlemskapstatus
import no.nav.helse.serde.mapping.konverterTilAktivitetslogg
import no.nav.helse.serde.migration.*
import no.nav.helse.serde.reflection.*
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.utbetalingslinjer.*
import no.nav.helse.utbetalingstidslinje.Begrunnelse
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag
import java.math.RoundingMode

internal val serdeObjectMapper = jacksonObjectMapper()
    .registerModule(JavaTimeModule())
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

class SerialisertPerson(val json: String) {
    internal companion object {
        private val migrations = listOf(
            V1EndreKunArbeidsgiverSykedagEnum(),
            V2Medlemskapstatus(),
            V3BeregnerGjenståendeSykedagerFraMaksdato(),
            V4LeggTilNySykdomstidslinje(),
            V5BegrensGradTilMellom0Og100(),
            V6LeggTilNySykdomstidslinje(),
            V7DagsatsSomHeltall(),
            V8LeggerTilLønnIUtbetalingslinjer(),
            V9FjernerGamleSykdomstidslinjer(),
            V10EndreNavnPåSykdomstidslinjer(),
            V11LeggeTilForlengelseFraInfotrygd(),
            V12Aktivitetslogg(),
            V13NettoBeløpIOppdrag(),
            V14NettoBeløpIVedtaksperiode(),
            V15ØkonomiSykdomstidslinjer(),
            V16StatusIUtbetaling(),
            V17ForkastedePerioder(),
            V18UtbetalingstidslinjeØkonomi(),
            V19KlippOverlappendeVedtaksperioder()
        )

        fun gjeldendeVersjon() = JsonMigration.gjeldendeVersjon(migrations)
        fun medSkjemaversjon(jsonNode: JsonNode) = JsonMigration.medSkjemaversjon(migrations, jsonNode)
    }

    val skjemaVersjon = gjeldendeVersjon()

    private fun migrate(jsonNode: JsonNode) {
        migrations.migrate(jsonNode)
    }

    fun deserialize(): Person {
        val jsonNode = serdeObjectMapper.readTree(json)

        migrate(jsonNode)

        val personData: PersonData = serdeObjectMapper.treeToValue(jsonNode)
        val arbeidsgivere = mutableListOf<Arbeidsgiver>()
        val aktivitetslogg = personData.aktivitetslogg?.let(::konverterTilAktivitetslogg) ?: Aktivitetslogg()

        val person = createPerson(
            aktørId = personData.aktørId,
            fødselsnummer = personData.fødselsnummer,
            arbeidsgivere = arbeidsgivere,
            aktivitetslogg = aktivitetslogg
        )

        arbeidsgivere.addAll(personData.arbeidsgivere.map { konverterTilArbeidsgiver(person, personData, it) })

        return person
    }

    private fun konverterTilArbeidsgiver(
        person: Person,
        personData: PersonData,
        data: ArbeidsgiverData
    ): Arbeidsgiver {
        val inntekthistorikk = Inntekthistorikk()
        val vedtaksperioder = mutableListOf<Vedtaksperiode>()
        val forkastede = mutableListOf<Vedtaksperiode>()

        data.inntekter.forEach { inntektData ->
            inntekthistorikk.add(
                fom = inntektData.fom,
                hendelseId = inntektData.hendelseId,
                beløp = inntektData.beløp.setScale(1, RoundingMode.HALF_UP)
            )
        }

        val arbeidsgiver = createArbeidsgiver(
            person = person,
            organisasjonsnummer = data.organisasjonsnummer,
            id = data.id,
            inntekthistorikk = inntekthistorikk,
            perioder = vedtaksperioder,
            forkastede = forkastede,
            utbetalinger = data.utbetalinger.map(::konverterTilUtbetaling).toMutableList()
        )

        vedtaksperioder.addAll(data.vedtaksperioder.map {
            parseVedtaksperiode(
                person,
                arbeidsgiver,
                personData,
                data,
                it
            )
        })

        forkastede.addAll(data.forkastede.map {
            parseVedtaksperiode(
                person,
                arbeidsgiver,
                personData,
                data,
                it
            )
        })
        Vedtaksperiode.sorter(vedtaksperioder)

        return arbeidsgiver
    }

    private fun konverterTilUtbetaling(data: PersonData.UtbetalingData) = createUtbetaling(
        konverterTilUtbetalingstidslinje(data.utbetalingstidslinje),
        konverterTilOppdrag(data.arbeidsgiverOppdrag),
        konverterTilOppdrag(data.personOppdrag),
        data.tidsstempel,
        enumValueOf(data.status)
    )

    private fun konverterTilOppdrag(data: PersonData.OppdragData): Oppdrag {
        return createOppdrag(
            data.mottaker,
            Fagområde.from(data.fagområde),
            data.linjer.map(::konverterTilUtbetalingslinje),
            data.fagsystemId,
            Endringskode.valueOf(data.endringskode),
            data.sisteArbeidsgiverdag,
            data.nettoBeløp
        )
    }

    private fun konverterTilUtbetalingstidslinje(data: UtbetalingstidslinjeData): Utbetalingstidslinje {
        return createUtbetalingstidslinje(data.dager.map {
            when (it.type) {
                UtbetalingstidslinjeData.TypeData.ArbeidsgiverperiodeDag -> {
                    Utbetalingsdag.ArbeidsgiverperiodeDag(dato = it.dato, økonomi = createØkonomi(it))
                }
                UtbetalingstidslinjeData.TypeData.NavDag -> {
                    createNavUtbetalingdag(
                        dato = it.dato,
                        økonomi = createØkonomi(it)
                    )
                }
                UtbetalingstidslinjeData.TypeData.NavHelgDag -> {
                    Utbetalingsdag.NavHelgDag(dato = it.dato, økonomi = createØkonomi(it))
                }
                UtbetalingstidslinjeData.TypeData.Arbeidsdag -> {
                    Utbetalingsdag.Arbeidsdag(dato = it.dato, økonomi = createØkonomi(it))
                }
                UtbetalingstidslinjeData.TypeData.Fridag -> {
                    Utbetalingsdag.Fridag(dato = it.dato, økonomi = createØkonomi(it))
                }
                UtbetalingstidslinjeData.TypeData.AvvistDag -> {
                    Utbetalingsdag.AvvistDag(
                        dato = it.dato, økonomi = createØkonomi(it), begrunnelse = when (it.begrunnelse) {
                            UtbetalingstidslinjeData.BegrunnelseData.SykepengedagerOppbrukt -> Begrunnelse.SykepengedagerOppbrukt
                            UtbetalingstidslinjeData.BegrunnelseData.MinimumInntekt -> Begrunnelse.MinimumInntekt
                            UtbetalingstidslinjeData.BegrunnelseData.EgenmeldingUtenforArbeidsgiverperiode -> Begrunnelse.EgenmeldingUtenforArbeidsgiverperiode
                            UtbetalingstidslinjeData.BegrunnelseData.MinimumSykdomsgrad -> Begrunnelse.MinimumSykdomsgrad
                            null -> error("Prøver å deserialisere avvist dag uten begrunnelse")
                        }
                    )
                }
                UtbetalingstidslinjeData.TypeData.UkjentDag -> {
                    Utbetalingsdag.UkjentDag(dato = it.dato, økonomi = createØkonomi(it))
                }
                UtbetalingstidslinjeData.TypeData.ForeldetDag -> {
                    Utbetalingsdag.ForeldetDag(dato = it.dato, økonomi = createØkonomi(it))
                }
            }
        }
            .toMutableList())
    }

    private fun parseVedtaksperiode(
        person: Person,
        arbeidsgiver: Arbeidsgiver,
        personData: PersonData,
        arbeidsgiverData: ArbeidsgiverData,
        data: ArbeidsgiverData.VedtaksperiodeData
    ): Vedtaksperiode {
        return createVedtaksperiode(
            person = person,
            arbeidsgiver = arbeidsgiver,
            id = data.id,
            gruppeId = data.gruppeId,
            aktørId = personData.aktørId,
            fødselsnummer = personData.fødselsnummer,
            organisasjonsnummer = arbeidsgiverData.organisasjonsnummer,
            tilstand = parseTilstand(data.tilstand),
            maksdato = data.maksdato,
            gjenståendeSykedager = data.gjenståendeSykedager,
            forbrukteSykedager = data.forbrukteSykedager,
            godkjentAv = data.godkjentAv,
            godkjenttidspunkt = data.godkjenttidspunkt,
            førsteFraværsdag = data.førsteFraværsdag,
            dataForSimulering = data.dataForSimulering?.let(::parseDataForSimulering),
            dataForVilkårsvurdering = data.dataForVilkårsvurdering?.let(::parseDataForVilkårsvurdering),
            sykdomshistorikk = parseSykdomshistorikk(data.sykdomshistorikk),
            utbetalingstidslinje = konverterTilUtbetalingstidslinje(data.utbetalingstidslinje),
            personFagsystemId = data.personFagsystemId,
            personNettoBeløp = data.personNettoBeløp,
            arbeidsgiverFagsystemId = data.arbeidsgiverFagsystemId,
            arbeidsgiverNettoBeløp = data.arbeidsgiverNettoBeløp,
            forlengelseFraInfotrygd = data.forlengelseFraInfotrygd
        )
    }

    private fun parseSykdomstidslinje(
        tidslinjeData: SykdomstidslinjeData
    ): Sykdomstidslinje = createSykdomstidslinje(tidslinjeData)

    private fun parseTilstand(tilstand: TilstandType) = when (tilstand) {
        TilstandType.AVVENTER_HISTORIKK -> AvventerHistorikk
        TilstandType.AVVENTER_GODKJENNING -> AvventerGodkjenning
        TilstandType.AVVENTER_SIMULERING -> AvventerSimulering
        TilstandType.TIL_UTBETALING -> TilUtbetaling
        TilstandType.AVSLUTTET -> Avsluttet
        TilstandType.AVSLUTTET_UTEN_UTBETALING -> AvsluttetUtenUtbetaling
        TilstandType.AVSLUTTET_UTEN_UTBETALING_MED_INNTEKTSMELDING -> AvsluttetUtenUtbetalingMedInntektsmelding
        TilstandType.UTBETALING_FEILET -> UtbetalingFeilet
        TilstandType.TIL_INFOTRYGD -> TilInfotrygd
        TilstandType.START -> Start
        TilstandType.MOTTATT_SYKMELDING_FERDIG_FORLENGELSE -> MottattSykmeldingFerdigForlengelse
        TilstandType.MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE -> MottattSykmeldingUferdigForlengelse
        TilstandType.MOTTATT_SYKMELDING_FERDIG_GAP -> MottattSykmeldingFerdigGap
        TilstandType.MOTTATT_SYKMELDING_UFERDIG_GAP -> MottattSykmeldingUferdigGap
        TilstandType.AVVENTER_SØKNAD_FERDIG_GAP -> AvventerSøknadFerdigGap
        TilstandType.AVVENTER_VILKÅRSPRØVING_GAP -> AvventerVilkårsprøvingGap
        TilstandType.AVVENTER_VILKÅRSPRØVING_ARBEIDSGIVERSØKNAD -> AvventerVilkårsprøvingArbeidsgiversøknad
        TilstandType.AVVENTER_GAP -> AvventerGap
        TilstandType.AVVENTER_SØKNAD_UFERDIG_GAP -> AvventerSøknadUferdigGap
        TilstandType.AVVENTER_INNTEKTSMELDING_FERDIG_GAP -> AvventerInntektsmeldingFerdigGap
        TilstandType.AVVENTER_INNTEKTSMELDING_UFERDIG_GAP -> AvventerInntektsmeldingUferdigGap
        TilstandType.AVVENTER_UFERDIG_GAP -> AvventerUferdigGap
        TilstandType.AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE -> AvventerInntektsmeldingUferdigForlengelse
        TilstandType.AVVENTER_SØKNAD_UFERDIG_FORLENGELSE -> AvventerSøknadUferdigForlengelse
        TilstandType.AVVENTER_UFERDIG_FORLENGELSE -> AvventerUferdigForlengelse
    }

    private fun konverterTilUtbetalingslinje(
        data: PersonData.UtbetalingslinjeData
    ): Utbetalingslinje = createUtbetalingslinje(
        data.fom,
        data.tom,
        data.dagsats,
        data.lønn,
        data.grad,
        data.refFagsystemId,
        data.delytelseId,
        data.refDelytelseId,
        Endringskode.valueOf(data.endringskode),
        Klassekode.from(data.klassekode),
        data.datoStatusFom
    )

    private fun parseDataForVilkårsvurdering(
        data: ArbeidsgiverData.VedtaksperiodeData.DataForVilkårsvurderingData
    ): Vilkårsgrunnlag.Grunnlagsdata =
        Vilkårsgrunnlag.Grunnlagsdata(
            erEgenAnsatt = data.erEgenAnsatt,
            beregnetÅrsinntektFraInntektskomponenten = data.beregnetÅrsinntektFraInntektskomponenten,
            avviksprosent = data.avviksprosent,
            harOpptjening = data.harOpptjening,
            antallOpptjeningsdagerErMinst = data.antallOpptjeningsdagerErMinst,
            medlemskapstatus = when (data.medlemskapstatus) {
                JsonMedlemskapstatus.JA -> Medlemskapsvurdering.Medlemskapstatus.Ja
                JsonMedlemskapstatus.NEI -> Medlemskapsvurdering.Medlemskapstatus.Nei
                else -> Medlemskapsvurdering.Medlemskapstatus.VetIkke
            }
        )

    private fun parseDataForSimulering(
        data: ArbeidsgiverData.VedtaksperiodeData.DataForSimuleringData
    ) = Simulering.SimuleringResultat(
        totalbeløp = data.totalbeløp,
        perioder = data.perioder.map { periode ->
            Simulering.SimulertPeriode(
                periode = Periode(periode.fom, periode.tom),
                utbetalinger = periode.utbetalinger.map { utbetaling ->
                    Simulering.SimulertUtbetaling(
                        forfallsdato = utbetaling.forfallsdato,
                        utbetalesTil = Simulering.Mottaker(
                            id = utbetaling.utbetalesTil.id,
                            navn = utbetaling.utbetalesTil.navn
                        ),
                        feilkonto = utbetaling.feilkonto,
                        detaljer = utbetaling.detaljer.map { detalj ->
                            Simulering.Detaljer(
                                periode = Periode(detalj.fom, detalj.tom),
                                konto = detalj.konto,
                                beløp = detalj.beløp,
                                klassekode = Simulering.Klassekode(
                                    kode = detalj.klassekode.kode,
                                    beskrivelse = detalj.klassekode.beskrivelse
                                ),
                                uføregrad = detalj.uføregrad,
                                utbetalingstype = detalj.utbetalingstype,
                                tilbakeføring = detalj.tilbakeføring,
                                sats = Simulering.Sats(
                                    sats = detalj.sats.sats,
                                    antall = detalj.sats.antall,
                                    type = detalj.sats.type
                                ),
                                refunderesOrgnummer = detalj.refunderesOrgnummer
                            )
                        }
                    )
                }
            )
        }
    )

    private fun parseSykdomshistorikk(
        data: List<ArbeidsgiverData.VedtaksperiodeData.SykdomshistorikkData>
    ): Sykdomshistorikk {
        return createSykdomshistorikk(data.map { sykdomshistorikkData ->
            createSykdomshistorikkElement(
                timestamp = sykdomshistorikkData.tidsstempel,
                hendelseId = sykdomshistorikkData.hendelseId,
                hendelseSykdomstidslinje = parseSykdomstidslinje(sykdomshistorikkData.hendelseSykdomstidslinje),
                beregnetSykdomstidslinje = parseSykdomstidslinje(sykdomshistorikkData.beregnetSykdomstidslinje)
            )
        })
    }

}
