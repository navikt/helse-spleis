package no.nav.helse.spleis.e2e

import java.time.LocalDate
import java.time.Year
import java.util.UUID
import no.nav.helse.desember
import no.nav.helse.dsl.Behovsoppsamler
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.TestPerson
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.person.Behandlinger.Behandling.Endring.Arbeidssituasjon
import no.nav.helse.person.EventSubscription
import org.junit.jupiter.api.Assertions.assertEquals

internal fun TestPerson.TestArbeidsgiver.assertSykepengegrunnlagsfakta(
    vedtaksperiodeId: UUID = 1.vedtaksperiode,
    sykepengegrunnlagsfakta: Map<String, Any?>,
    behovsoppsamler: Behovsoppsamler
) {
    val actualSykepengegrunnlagsfakta = behovsoppsamler.behovsdetaljer<Behovsoppsamler.Behovsdetaljer.Godkjenning>().last { it.vedtaksperiodeId == vedtaksperiodeId }.event.sykepengegrunnlagsfakta
    assertEquals(somArbedistakerSykepengegrunnlagsfakta(sykepengegrunnlagsfakta), actualSykepengegrunnlagsfakta)
}

internal fun TestPerson.TestArbeidsgiver.assertGodkjenningsbehov(
    tags: Set<String>,
    skjæringstidspunkt: LocalDate = 1.januar,
    periodeFom: LocalDate = 1.januar,
    periodeTom: LocalDate = 31.januar,
    vedtaksperiodeId: UUID = 1.vedtaksperiode,
    relevanteSøknader: Set<UUID>? = null,
    orgnummere: Set<String> = setOf(orgnummer),
    kanAvvises: Boolean = true,
    periodeType: String = "FØRSTEGANGSBEHANDLING",
    førstegangsbehandling: Boolean = true,
    utbetalingstype: String = "UTBETALING",
    inntektskilde: String = "EN_ARBEIDSGIVER",
    behandlingId: UUID = inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.behandlinger.last().id,
    perioderMedSammeSkjæringstidspunkt: List<Map<String, String>> = listOf(
        mapOf("vedtaksperiodeId" to 1.vedtaksperiode.toString(), "behandlingId" to behandlingId.toString(), "fom" to 1.januar.toString(), "tom" to 31.januar.toString()),
    ),
    forbrukteSykedager: Int = 11,
    gjenståendeSykedager: Int = 237,
    foreløpigBeregnetSluttPåSykepenger: LocalDate = 28.desember,
    utbetalingsdager: List<Map<String, Any>> = standardUtbetalingsdager(1431, 0),
    sykepengegrunnlagsfakta: Map<String, Any?> = mapOf(
        "sykepengegrunnlag" to 372_000.0,
        "6G" to 561_804.0,
        "fastsatt" to "EtterHovedregel",
        "arbeidsgivere" to listOf(
            mapOf(
                "arbeidsgiver" to orgnummer,
                "omregnetÅrsinntekt" to INNTEKT.årlig,
                "inntektskilde" to "Arbeidsgiver"
            )
        ),
        "selvstendig" to null
    ),
    arbeidssituasjon: Arbeidssituasjon = Arbeidssituasjon.ARBEIDSTAKER,
    behovsoppsamler: Behovsoppsamler
) {
    val benyttetUtbetalingId = UUID.randomUUID()
    val benyttetVilkårsgrunnlagId = UUID.randomUUID()

    val expected = EventSubscription.GodkjenningEvent(
        utbetalingId = benyttetUtbetalingId,
        vilkårsgrunnlagId = benyttetVilkårsgrunnlagId,
        vedtaksperiodeId = vedtaksperiodeId,
        behandlingId = behandlingId,
        periode = periodeFom til periodeTom,
        skjæringstidspunkt = skjæringstidspunkt,
        periodetype = periodeType,
        førstegangsbehandling = førstegangsbehandling,
        utbetalingtype = utbetalingstype,
        inntektskilde = inntektskilde,
        orgnummereMedRelevanteArbeidsforhold = orgnummere,
        kanAvvises = kanAvvises,
        tags = tags,
        relevanteSøknader = relevanteSøknader ?: emptySet(),
        forbrukteSykedager = forbrukteSykedager,
        gjenståendeSykedager = gjenståendeSykedager,
        foreløpigBeregnetSluttPåSykepenger = foreløpigBeregnetSluttPåSykepenger,
        arbeidssituasjon = arbeidssituasjon.name,
        utbetalingsdager = utbetalingsdager.map { EventSubscription.Utbetalingsdag(
            dato = (it.getValue("dato") as String).let { LocalDate.parse(it) },
            type = (it.getValue("type") as String).let { EventSubscription.Utbetalingsdag.Dagtype.valueOf(it) },
            beløpTilArbeidsgiver = it.getValue("beløpTilArbeidsgiver") as Int,
            beløpTilBruker = it.getValue("beløpTilBruker") as Int,
            sykdomsgrad = it.getValue("sykdomsgrad") as Int,
            dekningsgrad = it.getValue("dekningsgrad") as Int,
            begrunnelser = (it.getValue("begrunnelser") as List<String>).takeUnless { it.isEmpty() }?.map { EventSubscription.Utbetalingsdag.EksternBegrunnelseDTO.valueOf(it) }
        )},
        yrkesaktivitetssporing = behandlingsporing,
        perioderMedSammeSkjæringstidspunkt = perioderMedSammeSkjæringstidspunkt.map { EventSubscription.GodkjenningEvent.PeriodeMedSammeSkjæringstidspunkt(
            vedtaksperiodeId = UUID.fromString(it.getValue("vedtaksperiodeId") as String),
            behandlingId = UUID.fromString(it.getValue("behandlingId") as String),
            periode = Periode(
                fom = it.getValue("fom").let { LocalDate.parse(it) },
                tom = it.getValue("tom").let { LocalDate.parse(it) }
            ),
        )},
        sykepengegrunnlagsfakta = when (behandlingsporing) {
            Behandlingsporing.Yrkesaktivitet.Arbeidsledig,
            is Behandlingsporing.Yrkesaktivitet.Arbeidstaker -> somArbedistakerSykepengegrunnlagsfakta(sykepengegrunnlagsfakta)
            Behandlingsporing.Yrkesaktivitet.Selvstendig -> somSelvstendigSykepengegrunnlagsfakta(sykepengegrunnlagsfakta)
            Behandlingsporing.Yrkesaktivitet.Frilans -> error("Støtter ikke frilans")
        }

    )
    val actual = behovsoppsamler.behovsdetaljer<Behovsoppsamler.Behovsdetaljer.Godkjenning>().last().event.let { ubesudlet ->
        ubesudlet.copy(
            utbetalingId = benyttetUtbetalingId,
            vilkårsgrunnlagId = benyttetVilkårsgrunnlagId,
            relevanteSøknader = when (relevanteSøknader) {
                null -> emptySet()
                else -> ubesudlet.relevanteSøknader
            },
            tags = ubesudlet.tags
        )
    }
    assertEquals(expected, actual)
}

private fun somArbedistakerSykepengegrunnlagsfakta(sykepengegrunnlagsfakta: Map<String, Any?>) = when (sykepengegrunnlagsfakta.getValue("fastsatt") as String) {
    "EtterHovedregel" -> EventSubscription.GodkjenningEvent.Sykepengegrunnlagsfakta.ArbeidstakerEtterHovedregel(
        sykepengegrunnlag = sykepengegrunnlagsfakta.getValue("sykepengegrunnlag") as Double,
        seksG = sykepengegrunnlagsfakta.getValue("6G") as Double,
        arbeidsgivere = (sykepengegrunnlagsfakta.getValue("arbeidsgivere") as List<Map<String, Any>>).map { EventSubscription.GodkjenningEvent.Sykepengegrunnlagsfakta.ArbeidstakerEtterHovedregel.Arbeidsgiver(
            arbeidsgiver = it.getValue("arbeidsgiver") as String,
            omregnetÅrsinntekt = it.getValue("omregnetÅrsinntekt") as Double,
            inntektskilde = it.getValue("inntektskilde") as String
        )}
    )
    "EtterSkjønn" -> EventSubscription.GodkjenningEvent.Sykepengegrunnlagsfakta.ArbeidstakerEtterSkjønn(
        sykepengegrunnlag = sykepengegrunnlagsfakta.getValue("sykepengegrunnlag") as Double,
        seksG = sykepengegrunnlagsfakta.getValue("6G") as Double,
        arbeidsgivere = (sykepengegrunnlagsfakta.getValue("arbeidsgivere") as List<Map<String, Any>>).map { EventSubscription.GodkjenningEvent.Sykepengegrunnlagsfakta.ArbeidstakerEtterSkjønn.Arbeidsgiver(
            arbeidsgiver = it.getValue("arbeidsgiver") as String,
            omregnetÅrsinntekt = it.getValue("omregnetÅrsinntekt") as Double,
            skjønnsfastsatt = it.getValue("skjønnsfastsatt") as Double
        )}
    )
    "IInfotrygd" -> EventSubscription.GodkjenningEvent.Sykepengegrunnlagsfakta.ArbeidstakerFraInfotrygd(
        sykepengegrunnlag = sykepengegrunnlagsfakta.getValue("sykepengegrunnlag") as Double,
        seksG = sykepengegrunnlagsfakta.getValue("6G") as Double,
    )
    else -> error("Ukjent fastsatt-type")
}
private fun somSelvstendigSykepengegrunnlagsfakta(sykepengegrunnlagsfakta: Map<String, Any?>) = EventSubscription.GodkjenningEvent.Sykepengegrunnlagsfakta.SelvstendigEtterHovedregel(
    sykepengegrunnlag = sykepengegrunnlagsfakta.getValue("sykepengegrunnlag") as Double,
    seksG = sykepengegrunnlagsfakta.getValue("6G") as Double,
    pensjonsgivendeInntekter = ((sykepengegrunnlagsfakta.getValue("selvstendig") as Map<String, Any>).getValue("pensjonsgivendeInntekter") as List<Map<String, Any>>).map { EventSubscription.GodkjenningEvent.Sykepengegrunnlagsfakta.SelvstendigEtterHovedregel.PensjonsgivendeInntekt(
        årstall = Year.of(it.getValue("årstall") as Int),
        beløp = it.getValue("beløp") as Double
    )},
    beregningsgrunnlag = (sykepengegrunnlagsfakta.getValue("selvstendig") as Map<String, Any>).getValue("beregningsgrunnlag") as Double
)

internal fun standardUtbetalingsdager(beløpTilArbeidsgiver: Int, beløpTilBruker: Int) =
    listOf(
        utbetalingsdag(1.januar, "ArbeidsgiverperiodeDag", 0, 0, 100, 100),
        utbetalingsdag(2.januar, "ArbeidsgiverperiodeDag", 0, 0, 100, 100),
        utbetalingsdag(3.januar, "ArbeidsgiverperiodeDag", 0, 0, 100, 100),
        utbetalingsdag(4.januar, "ArbeidsgiverperiodeDag", 0, 0, 100, 100),
        utbetalingsdag(5.januar, "ArbeidsgiverperiodeDag", 0, 0, 100, 100),
        utbetalingsdag(6.januar, "ArbeidsgiverperiodeDag", 0, 0, 100, 100),
        utbetalingsdag(7.januar, "ArbeidsgiverperiodeDag", 0, 0, 100, 100),
        utbetalingsdag(8.januar, "ArbeidsgiverperiodeDag", 0, 0, 100, 100),
        utbetalingsdag(9.januar, "ArbeidsgiverperiodeDag", 0, 0, 100, 100),
        utbetalingsdag(10.januar, "ArbeidsgiverperiodeDag", 0, 0, 100, 100),
        utbetalingsdag(11.januar, "ArbeidsgiverperiodeDag", 0, 0, 100, 100),
        utbetalingsdag(12.januar, "ArbeidsgiverperiodeDag", 0, 0, 100, 100),
        utbetalingsdag(13.januar, "ArbeidsgiverperiodeDag", 0, 0, 100, 100),
        utbetalingsdag(14.januar, "ArbeidsgiverperiodeDag", 0, 0, 100, 100),
        utbetalingsdag(15.januar, "ArbeidsgiverperiodeDag", 0, 0, 100, 100),
        utbetalingsdag(16.januar, "ArbeidsgiverperiodeDag", 0, 0, 100, 100),
        utbetalingsdag(17.januar, "NavDag", beløpTilArbeidsgiver, beløpTilBruker, 100, 100),
        utbetalingsdag(18.januar, "NavDag", beløpTilArbeidsgiver, beløpTilBruker, 100, 100),
        utbetalingsdag(19.januar, "NavDag", beløpTilArbeidsgiver, beløpTilBruker, 100, 100),
        utbetalingsdag(20.januar, "NavHelgDag", 0, 0, 100, 100),
        utbetalingsdag(21.januar, "NavHelgDag", 0, 0, 100, 100),
        utbetalingsdag(22.januar, "NavDag", beløpTilArbeidsgiver, beløpTilBruker, 100, 100),
        utbetalingsdag(23.januar, "NavDag", beløpTilArbeidsgiver, beløpTilBruker, 100, 100),
        utbetalingsdag(24.januar, "NavDag", beløpTilArbeidsgiver, beløpTilBruker, 100, 100),
        utbetalingsdag(25.januar, "NavDag", beløpTilArbeidsgiver, beløpTilBruker, 100, 100),
        utbetalingsdag(26.januar, "NavDag", beløpTilArbeidsgiver, beløpTilBruker, 100, 100),
        utbetalingsdag(27.januar, "NavHelgDag", 0, 0, 100, 100),
        utbetalingsdag(28.januar, "NavHelgDag", 0, 0, 100, 100),
        utbetalingsdag(29.januar, "NavDag", beløpTilArbeidsgiver, beløpTilBruker, 100, 100),
        utbetalingsdag(30.januar, "NavDag", beløpTilArbeidsgiver, beløpTilBruker, 100, 100),
        utbetalingsdag(31.januar, "NavDag", beløpTilArbeidsgiver, beløpTilBruker, 100, 100)
    )

internal fun utbetalingsdag(dato: LocalDate, type: String, beløpTilArbeidsgiver: Int, beløpTilBruker: Int, sykdomsgrad: Int, dekningsgrad: Int, begrunnelser: List<String> = emptyList()) = mapOf(
    "dato" to dato.toString(),
    "type" to type,
    "beløpTilArbeidsgiver" to beløpTilArbeidsgiver,
    "beløpTilBruker" to beløpTilBruker,
    "sykdomsgrad" to sykdomsgrad,
    "dekningsgrad" to dekningsgrad,
    "begrunnelser" to begrunnelser
)
