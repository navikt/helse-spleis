package no.nav.helse.person

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID
import no.nav.helse.Personidentifikator
import no.nav.helse.hendelser.Hendelseskontekst
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.person.PersonObserver.Refusjon.toJsonMap
import no.nav.helse.serde.api.dto.BegrunnelseDTO

interface PersonObserver {
    data class VedtaksperiodeIkkeFunnetEvent(
        val fødselsnummer: String,
        val aktørId: String,
        val organisasjonsnummer: String,
        val vedtaksperiodeId: UUID
    )

    data class VedtaksperiodeEndretEvent(
        val fødselsnummer: String,
        val aktørId: String,
        val organisasjonsnummer: String,
        val vedtaksperiodeId: UUID,
        val gjeldendeTilstand: TilstandType,
        val forrigeTilstand: TilstandType,
        val aktivitetslogg: Map<String, List<Map<String, Any>>>,
        val harVedtaksperiodeWarnings: Boolean,
        val hendelser: Set<UUID>,
        val makstid: LocalDateTime,
        val fom: LocalDate,
        val tom: LocalDate
    )

    data class VedtaksperiodeForkastetEvent(
        val fødselsnummer: String,
        val aktørId: String,
        val organisasjonsnummer: String,
        val vedtaksperiodeId: UUID,
        val gjeldendeTilstand: TilstandType,
        val hendelser: Set<UUID>,
        val fom: LocalDate,
        val tom: LocalDate
    )

    data class OpprettOppgaveForSpeilsaksbehandlereEvent(
        val hendelser: Set<UUID>,
    )

    data class OpprettOppgaveEvent(
        val hendelser: Set<UUID>,
    )

    data class UtsettOppgaveEvent(
        val hendelse: UUID
    )

    data class RevurderingAvvistEvent(
        val fødselsnummer: String,
        val errors: List<String>
    ) {
        fun toJsonMap(): Map<String, Any> =
            mapOf(
                "fødselsnummer" to fødselsnummer,
                "errors" to errors
            )
    }

    data class ManglendeInntektsmeldingEvent(
        val fødselsnummer: String,
        val aktørId: String,
        val organisasjonsnummer: String,
        val vedtaksperiodeId: UUID,
        val fom: LocalDate,
        val tom: LocalDate,
        val søknadIder: Set<UUID>
    )

    data class TrengerIkkeInntektsmeldingEvent(
        val fødselsnummer: String,
        val aktørId: String,
        val organisasjonsnummer: String,
        val vedtaksperiodeId: UUID,
        val fom: LocalDate,
        val tom: LocalDate,
        val søknadIder: Set<UUID>
    )

    class TrengerArbeidsgiveropplysningerEvent(
        val fom: LocalDate,
        val tom: LocalDate,
        val vedtaksperiodeId: UUID,
        val forespurteOpplysninger: List<ForespurtOpplysning>
    ) {
        fun toJsonMap(): Map<String, Any> =
            mapOf(
                "fom" to fom,
                "tom" to tom,
                "vedtaksperiodeId" to vedtaksperiodeId,
                "forespurteOpplysninger" to forespurteOpplysninger.toJsonMap()

            )
    }
    sealed class ForespurtOpplysning {
        fun List<ForespurtOpplysning>.toJsonMap() = map { forespurtOpplysning ->
            when (forespurtOpplysning) {
                is Arbeidsgiverperiode -> mapOf(
                    "opplysningstype" to "Arbeidsgiverperiode",
                    "forslag" to forespurtOpplysning.forslag.map { forslag ->
                        mapOf(
                            "fom" to forslag.start,
                            "tom" to forslag.endInclusive
                        )
                    }
                )
                is Inntekt -> mapOf(
                    "opplysningstype" to "Inntekt",
                    "forslag" to mapOf(
                        "beregningsmåneder" to forespurtOpplysning.forslag.beregningsmåneder
                    )
                )
                is FastsattInntekt -> mapOf(
                    "opplysningstype" to "FastsattInntekt",
                    "fastsattInntekt" to forespurtOpplysning.fastsattInntekt.reflection { _, månedlig, _, _ -> månedlig }
                )
                Refusjon -> mapOf("opplysningstype" to "Refusjon")
            }
        }
    }

    data class Inntektsforslag(val beregningsmåneder: List<YearMonth>)
    data class Inntekt(val forslag: Inntektsforslag) : ForespurtOpplysning()
    data class FastsattInntekt(val fastsattInntekt: no.nav.helse.økonomi.Inntekt) : ForespurtOpplysning()
    data class Arbeidsgiverperiode(val forslag: List<Periode>) : ForespurtOpplysning()
    object Refusjon : ForespurtOpplysning()

    data class UtbetalingAnnullertEvent(
        val utbetalingId: UUID,
        val korrelasjonsId: UUID,
        val arbeidsgiverFagsystemId: String?,
        val personFagsystemId: String?,
        val fom: LocalDate,
        val tom: LocalDate,
        val utbetalingslinjer: List<Utbetalingslinje>,
        val annullertAvSaksbehandler: LocalDateTime,
        val saksbehandlerEpost: String,
        val saksbehandlerIdent: String
    ) {
        init {
            require(arbeidsgiverFagsystemId != null || personFagsystemId != null) {
                "Enten arbeidsgiverFagsystemId eller personfagsystemId må være satt"
            }
        }
        data class Utbetalingslinje(
            val fom: LocalDate,
            val tom: LocalDate,
            val beløp: Int,
            val grad: Double
        )
    }

    data class UtbetalingEndretEvent(
        val utbetalingId: UUID,
        val type: String,
        val forrigeStatus: String,
        val gjeldendeStatus: String,
        val arbeidsgiverOppdrag: Map<String, Any?>,
        val personOppdrag: Map<String, Any?>
    )

    data class UtbetalingUtbetaltEvent(
        val utbetalingId: UUID,
        val korrelasjonsId: UUID,
        val type: String,
        val fom: LocalDate,
        val tom: LocalDate,
        val maksdato: LocalDate,
        val forbrukteSykedager: Int,
        val gjenståendeSykedager: Int,
        val stønadsdager: Int,
        val epost: String,
        val tidspunkt: LocalDateTime,
        val automatiskBehandling: Boolean,
        val arbeidsgiverOppdrag: Map<String, Any?>,
        val personOppdrag: Map<String, Any?>,
        val utbetalingsdager: List<Utbetalingsdag>,
        val vedtaksperiodeIder: List<UUID>,
        val ident: String
    )

    data class Utbetalingsdag(
        val dato: LocalDate,
        val type: Dagtype,
        val begrunnelser: List<BegrunnelseDTO>? = null
    ) {
        enum class Dagtype {
            ArbeidsgiverperiodeDag,
            NavDag,
            NavHelgDag,
            Arbeidsdag,
            Fridag,
            AvvistDag,
            UkjentDag,
            ForeldetDag,
            Permisjonsdag,
            Feriedag
        }
    }

    data class FeriepengerUtbetaltEvent(
        val arbeidsgiverOppdrag: Map<String, Any?>,
        val personOppdrag: Map<String, Any?> = mapOf("linjer" to emptyList<String>())
    )

    data class HendelseIkkeHåndtertEvent(
        val hendelseId: UUID,
        val årsaker: List<String>,
    )

    data class VedtakFattetEvent(
        val fødselsnummer: String,
        val aktørId: String,
        val organisasjonsnummer: String,
        val vedtaksperiodeId: UUID,
        val periode: Periode,
        val hendelseIder: Set<UUID>,
        val skjæringstidspunkt: LocalDate,
        val sykepengegrunnlag: Double,
        val beregningsgrunnlag: Double,
        val omregnetÅrsinntektPerArbeidsgiver: Map<String, Double>,
        val inntekt: Double,
        val utbetalingId: UUID?,
        val sykepengegrunnlagsbegrensning: String,
        val vedtakFattetTidspunkt: LocalDateTime
    )

    data class RevurderingIgangsattEvent(
        val årsak: String,
        val skjæringstidspunkt: LocalDate,
        val periodeForEndring: Periode,
        val berørtePerioder: List<VedtaksperiodeData>
    ) {
        val typeEndring get() = if (berørtePerioder.any { it.typeEndring == "REVURDERING" }) "REVURDERING" else "OVERSTYRING"

        data class VedtaksperiodeData(
            val orgnummer: String,
            val vedtaksperiodeId: UUID,
            val periode: Periode,
            val skjæringstidspunkt: LocalDate,
            val typeEndring: String
        )
    }

    fun inntektsmeldingReplay(personidentifikator: Personidentifikator, vedtaksperiodeId: UUID) {}
    fun vedtaksperiodePåminnet(vedtaksperiodeId: UUID, organisasjonsnummer: String, påminnelse: Påminnelse) {}
    fun vedtaksperiodeIkkePåminnet(vedtaksperiodeId: UUID, organisasjonsnummer: String, nåværendeTilstand: TilstandType) {}
    fun vedtaksperiodeEndret(event: VedtaksperiodeEndretEvent) {}
    fun vedtaksperiodeForkastet(event: VedtaksperiodeForkastetEvent) {}
    fun opprettOppgaveForSpeilsaksbehandlere(hendelseskontekst: Hendelseskontekst, event: OpprettOppgaveForSpeilsaksbehandlereEvent) {}
    fun opprettOppgave(hendelseskontekst: Hendelseskontekst, event: OpprettOppgaveEvent) {}
    fun utsettOppgave(hendelseskontekst: Hendelseskontekst, event: UtsettOppgaveEvent) {}
    fun vedtaksperiodeIkkeFunnet(event: VedtaksperiodeIkkeFunnetEvent) {}
    fun manglerInntektsmelding(event: ManglendeInntektsmeldingEvent) {}
    fun trengerIkkeInntektsmelding(event: TrengerIkkeInntektsmeldingEvent) {}
    fun trengerArbeidsgiveropplysninger(hendelseskontekst: Hendelseskontekst, event: TrengerArbeidsgiveropplysningerEvent) {}
    fun utbetalingEndret(hendelseskontekst: Hendelseskontekst, event: UtbetalingEndretEvent) {}
    fun utbetalingUtbetalt(hendelseskontekst: Hendelseskontekst, event: UtbetalingUtbetaltEvent) {}
    fun utbetalingUtenUtbetaling(hendelseskontekst: Hendelseskontekst, event: UtbetalingUtbetaltEvent) {}
    fun feriepengerUtbetalt(hendelseskontekst: Hendelseskontekst, event: FeriepengerUtbetaltEvent) {}
    fun annullering(hendelseskontekst: Hendelseskontekst, event: UtbetalingAnnullertEvent) {}
    fun avstemt(hendelseskontekst: Hendelseskontekst, result: Map<String, Any>) {}
    fun vedtakFattet(event: VedtakFattetEvent) {}
    fun revurderingAvvist(hendelseskontekst: Hendelseskontekst, event: RevurderingAvvistEvent) {}
    fun nyVedtaksperiodeUtbetaling(
        personidentifikator: Personidentifikator,
        aktørId: String,
        organisasjonsnummer: String,
        utbetalingId: UUID,
        vedtaksperiodeId: UUID
    ) {}

    fun revurderingIgangsatt(
        event: RevurderingIgangsattEvent,
        personidentifikator: Personidentifikator,
        aktørId: String
    ) {}
}
