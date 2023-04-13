package no.nav.helse.person

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID
import no.nav.helse.Personidentifikator
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.person.PersonObserver.ForespurtOpplysning.Companion.toJsonMap
import no.nav.helse.person.infotrygdhistorikk.Friperiode
import no.nav.helse.person.infotrygdhistorikk.Utbetalingsperiode
import no.nav.helse.person.inntekt.Refusjonsopplysning
import no.nav.helse.serde.api.dto.BegrunnelseDTO
import no.nav.helse.økonomi.Prosentdel

interface PersonObserver : SykefraværstilfelleeventyrObserver {
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

    data class VedtaksperiodeVenterEvent(
        val fødselsnummer: String,
        val aktørId: String,
        val organisasjonsnummer: String,
        val vedtaksperiodeId: UUID,
        val hendelser: Set<UUID>,
        val ventetSiden: LocalDateTime,
        val venterTil: LocalDateTime,
        val venterPå: VenterPå
    ) {
        data class VenterPå(
            val vedtaksperiodeId: UUID,
            val organisasjonsnummer: String,
            val venteårsak: Venteårsak
        )

        data class Venteårsak(
            val hva : String,
            val hvorfor: String?
        )
    }

    data class VedtaksperiodeForkastetEvent(
        val fødselsnummer: String,
        val aktørId: String,
        val organisasjonsnummer: String,
        val vedtaksperiodeId: UUID,
        val gjeldendeTilstand: TilstandType,
        val hendelser: Set<UUID>,
        val fom: LocalDate,
        val tom: LocalDate,
        val forlengerPeriode: Boolean,
        val harPeriodeInnenfor16Dager: Boolean
    )
    data class InntektsmeldingFørSøknadEvent(
        val inntektsmeldingId: UUID,
        val overlappendeSykmeldingsperioder: List<Periode>,
        val organisasjonsnummer: String
    )

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
        val organisasjonsnummer: String,
        val vedtaksperiodeId: UUID,
        val sykmeldingsperioder: List<Periode>,
        val forespurteOpplysninger: List<ForespurtOpplysning>
    ) {
        fun toJsonMap(): Map<String, Any> =
            mapOf(
                "organisasjonsnummer" to organisasjonsnummer,
                "vedtaksperiodeId" to vedtaksperiodeId,
                "sykmeldingsperioder" to sykmeldingsperioder.map {
                    mapOf(
                        "fom" to it.start,
                        "tom" to it.endInclusive
                    )
                },
                "forespurteOpplysninger" to forespurteOpplysninger.toJsonMap()

            )
    }
    sealed class ForespurtOpplysning {

        companion object {
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
                        "fastsattInntekt" to forespurtOpplysning.fastsattInntekt.månedlig
                    )

                    is Refusjon -> mapOf(
                        "opplysningstype" to "Refusjon",
                        "forslag" to forespurtOpplysning.forslag.map { forslag ->
                            mapOf(
                                "fom" to forslag.fom(),
                                "tom" to forslag.tom(),
                                "beløp" to forslag.beløp().månedlig
                            )
                        }
                    )
                }
            }
        }
    }

    data class Inntektsforslag(val beregningsmåneder: List<YearMonth>)
    data class Inntekt(val forslag: Inntektsforslag) : ForespurtOpplysning()
    data class FastsattInntekt(val fastsattInntekt: no.nav.helse.økonomi.Inntekt) : ForespurtOpplysning()
    data class Arbeidsgiverperiode(val forslag: List<Periode>) : ForespurtOpplysning()
    data class Refusjon(val forslag: List<Refusjonsopplysning>) : ForespurtOpplysning()

    data class UtbetalingAnnullertEvent(
        val organisasjonsnummer: String,
        val utbetalingId: UUID,
        val korrelasjonsId: UUID,
        val arbeidsgiverFagsystemId: String,
        val personFagsystemId: String,
        val fom: LocalDate,
        val tom: LocalDate,
        val annullertAvSaksbehandler: LocalDateTime,
        val saksbehandlerEpost: String,
        val saksbehandlerIdent: String
    ) {
        init {
            require(arbeidsgiverFagsystemId != null || personFagsystemId != null) {
                "Enten arbeidsgiverFagsystemId eller personfagsystemId må være satt"
            }
        }
    }

    data class UtbetalingEndretEvent(
        val organisasjonsnummer: String,
        val utbetalingId: UUID,
        val type: String,
        val forrigeStatus: String,
        val gjeldendeStatus: String,
        val arbeidsgiverOppdrag: Map<String, Any?>,
        val personOppdrag: Map<String, Any?>,
        val korrelasjonsId: UUID
    )

    data class UtbetalingUtbetaltEvent(
        val organisasjonsnummer: String,
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
        val organisasjonsnummer: String,
        val arbeidsgiverOppdrag: Map<String, Any?>,
        val personOppdrag: Map<String, Any?> = mapOf("linjer" to emptyList<String>())
    )

    data class OverlappendeInfotrygdperiodeEtterInfotrygdendring(
        val organisasjonsnummer: String,
        val vedtaksperiodeId: UUID,
        val vedtaksperiodeFom: LocalDate,
        val vedtaksperiodeTom: LocalDate,
        val vedtaksperiodetilstand: String,
        val infotrygdhistorikkHendelseId: String?,
        val infotrygdperioder: List<Infotrygdperiode>
    ) {
        data class Infotrygdperiode(
            val fom: LocalDate,
            val tom: LocalDate,
            val type: String,
            val orgnummer: String?
        )
        internal class InfotrygdperiodeBuilder(infotrygdperiode: no.nav.helse.person.infotrygdhistorikk.Infotrygdperiode): InfotrygdperiodeVisitor {
            var infotrygdperiode: Infotrygdperiode? = null

            init {
                infotrygdperiode.accept(this)
            }

            override fun visitInfotrygdhistorikkFerieperiode(periode: Friperiode, fom: LocalDate, tom: LocalDate) {
                infotrygdperiode = Infotrygdperiode(fom, tom, "FRIPERIODE", null)
            }

            override fun visitInfotrygdhistorikkPersonUtbetalingsperiode(
                periode: Utbetalingsperiode,
                orgnr: String,
                fom: LocalDate,
                tom: LocalDate,
                grad: Prosentdel,
                inntekt: no.nav.helse.økonomi.Inntekt
            ) {
                infotrygdperiode = Infotrygdperiode(fom, tom, "PERSONUTBETALING", orgnr)
            }

            override fun visitInfotrygdhistorikkArbeidsgiverUtbetalingsperiode(
                periode: Utbetalingsperiode,
                orgnr: String,
                fom: LocalDate,
                tom: LocalDate,
                grad: Prosentdel,
                inntekt: no.nav.helse.økonomi.Inntekt
            ) {
                infotrygdperiode = Infotrygdperiode(fom, tom, "ARBEIDSGIVERUTBETALING", orgnr)
            }
        }
    }

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

    data class OverstyringIgangsatt(
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

    data class VedtaksperiodeOpprettet(
        val vedtaksperiodeId: UUID,
        val organisasjonsnummer: String,
        val periode: Periode,
        val skjæringstidspunkt: LocalDate,
        val opprettet: LocalDateTime
    )

    fun inntektsmeldingReplay(personidentifikator: Personidentifikator, aktørId: String, organisasjonsnummer: String, vedtaksperiodeId: UUID, skjæringstidspunkt: LocalDate, førsteDagIArbeidsgiverperioden: LocalDate?) {}
    fun trengerIkkeInntektsmeldingReplay(vedtaksperiodeId: UUID) {}
    fun vedtaksperiodeOpprettet(event: VedtaksperiodeOpprettet) {}
    fun vedtaksperiodePåminnet(vedtaksperiodeId: UUID, organisasjonsnummer: String, påminnelse: Påminnelse) {}
    fun vedtaksperiodeIkkePåminnet(vedtaksperiodeId: UUID, organisasjonsnummer: String, nåværendeTilstand: TilstandType) {}
    fun vedtaksperiodeEndret(event: VedtaksperiodeEndretEvent) {}
    fun vedtaksperiodeVenter(event: VedtaksperiodeVenterEvent) {}
    fun vedtaksperiodeForkastet(event: VedtaksperiodeForkastetEvent) {}
    fun vedtaksperiodeIkkeFunnet(event: VedtaksperiodeIkkeFunnetEvent) {}
    fun manglerInntektsmelding(event: ManglendeInntektsmeldingEvent) {}
    fun trengerIkkeInntektsmelding(event: TrengerIkkeInntektsmeldingEvent) {}
    fun trengerArbeidsgiveropplysninger(event: TrengerArbeidsgiveropplysningerEvent) {}
    fun utbetalingEndret(event: UtbetalingEndretEvent) {}
    fun utbetalingUtbetalt(event: UtbetalingUtbetaltEvent) {}
    fun utbetalingUtenUtbetaling(event: UtbetalingUtbetaltEvent) {}
    fun feriepengerUtbetalt(event: FeriepengerUtbetaltEvent) {}
    fun annullering(event: UtbetalingAnnullertEvent) {}
    fun avstemt(result: Map<String, Any>) {}
    fun vedtakFattet(event: VedtakFattetEvent) {}

    fun nyVedtaksperiodeUtbetaling(
        personidentifikator: Personidentifikator,
        aktørId: String,
        organisasjonsnummer: String,
        utbetalingId: UUID,
        vedtaksperiodeId: UUID
    ) {}
    fun overstyringIgangsatt(event: OverstyringIgangsatt) {}
    fun overlappendeInfotrygdperiodeEtterInfotrygdendring(event: OverlappendeInfotrygdperiodeEtterInfotrygdendring) {}
    fun inntektsmeldingFørSøknad(event: InntektsmeldingFørSøknadEvent) {}
    fun inntektsmeldingIkkeHåndtert(inntektsmeldingId: UUID, organisasjonsnummer: String) {}
    fun inntektsmeldingHåndtert(inntektsmeldingId: UUID, vedtaksperiodeId: UUID, organisasjonsnummer: String) {}
    fun søknadHåndtert(søknadId: UUID, vedtaksperiodeId: UUID, organisasjonsnummer: String) {}
}
