package no.nav.helse.person

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Year
import java.time.YearMonth
import java.util.UUID
import no.nav.helse.Personidentifikator
import no.nav.helse.feriepenger.Feriepengeoppdrag
import no.nav.helse.hendelser.Avsender
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.hendelser.Forsikring
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.EventSubscription.Utbetalingsdag.Dagtype
import no.nav.helse.person.EventSubscription.Utbetalingsdag.EksternBegrunnelseDTO
import no.nav.helse.person.tilstandsmaskin.TilstandType
import no.nav.helse.utbetalingslinjer.Oppdrag
import no.nav.helse.utbetalingslinjer.OppdragDetaljer
import no.nav.helse.utbetalingstidslinje.Begrunnelse

interface EventSubscription {

    sealed interface Event

    data class PlanlagtAnnulleringEvent(
        val yrkesaktivitetssporing: Behandlingsporing.Yrkesaktivitet,
        val vedtaksperioder: List<UUID>,
        val fom: LocalDate,
        val tom: LocalDate,
        val saksbehandlerIdent: String,
        val årsaker: List<String>,
        val begrunnelse: String
    ) : Event

    data class SykefraværstilfelleIkkeFunnet(
        val skjæringstidspunkt: LocalDate
    ) : Event

    data class VedtaksperiodePåminnetEvent(
        val vedtaksperiodeId: UUID,
        val yrkesaktivitetssporing: Behandlingsporing.Yrkesaktivitet,
        val tilstand: TilstandType,
        val antallGangerPåminnet: Int,
        val tilstandsendringstidspunkt: LocalDateTime,
        val påminnelsestidspunkt: LocalDateTime,
        val nestePåminnelsestidspunkt: LocalDateTime
    ) : Event

    data class VedtaksperiodeIkkePåminnetEvent(
        val vedtaksperiodeId: UUID,
        val yrkesaktivitetssporing: Behandlingsporing.Yrkesaktivitet,
        val nåværendeTilstand: TilstandType
    ) : Event

    data class VedtaksperiodeEndretEvent(
        val yrkesaktivitetssporing: Behandlingsporing.Yrkesaktivitet,
        val vedtaksperiodeId: UUID,
        val behandlingId: UUID,
        val gjeldendeTilstand: TilstandType,
        val forrigeTilstand: TilstandType,
        val hendelser: Set<UUID>,
        val makstid: LocalDateTime,
        val fom: LocalDate,
        val tom: LocalDate,
        val skjæringstidspunkt: LocalDate
    ) : Event

    data class AnalytiskDatapakkeEvent(
        val yrkesaktivitetssporing: Behandlingsporing.Yrkesaktivitet,
        val vedtaksperiodeId: UUID,
        val behandlingId: UUID,
        val skjæringstidspunkt: LocalDate,
        val beløpTilBruker: Pengeinformasjon,
        val beløpTilArbeidsgiver: Pengeinformasjon,
        val fom: LocalDate,
        val tom: LocalDate,
        val antallForbrukteSykedagerEtterPeriode: Daginformasjon,
        val antallGjenståendeSykedagerEtterPeriode: Daginformasjon,
        val harAndreInntekterIBeregning: Boolean
    ) : Event {
        data class Pengeinformasjon (
            val totalBeløp: Double,
            val nettoBeløp: Double
        )
        data class Daginformasjon(
            val antallDager: Int,
            val nettoDager: Int
        )
    }

    data class VedtaksperioderVenterEvent(val vedtaksperioder: List<VedtaksperiodeVenterEvent>) : Event

    data class VedtaksperiodeVenterEvent(
        val yrkesaktivitetssporing: Behandlingsporing.Yrkesaktivitet,
        val vedtaksperiodeId: UUID,
        val behandlingId: UUID,
        val skjæringstidspunkt: LocalDate,
        val hendelser: Set<UUID>,
        val ventetSiden: LocalDateTime,
        val venterTil: LocalDateTime,
        val venterPå: VenterPå
    ) {
        data class VenterPå(
            val vedtaksperiodeId: UUID,
            val skjæringstidspunkt: LocalDate,
            val yrkesaktivitetssporing: Behandlingsporing.Yrkesaktivitet,
            val venteårsak: Venteårsak
        )

        data class Venteårsak(
            val hva: String,
            val hvorfor: String?
        )
    }

    data class VedtaksperiodeForkastetEvent(
        val yrkesaktivitetssporing: Behandlingsporing.Yrkesaktivitet,
        val vedtaksperiodeId: UUID,
        val gjeldendeTilstand: TilstandType,
        val hendelser: Set<UUID>,
        val fom: LocalDate,
        val tom: LocalDate,
        val sykmeldingsperioder: List<Periode>,
        val speilrelatert: Boolean
    ) : Event {
        val trengerArbeidsgiveropplysninger = sykmeldingsperioder.isNotEmpty()
    }

    data class InntektsmeldingFørSøknadEvent(
        val inntektsmeldingId: UUID,
        val arbeidstaker: Behandlingsporing.Yrkesaktivitet.Arbeidstaker,
    ) : Event

    data class InntektsmeldingIkkeHåndtertEvent(
        val meldingsreferanseId: UUID,
        val arbeidstaker: Behandlingsporing.Yrkesaktivitet.Arbeidstaker,
        val speilrelatert: Boolean
    ) : Event

    data class InntektsmeldingHåndtertEvent(
        val meldingsreferanseId: UUID,
        val vedtaksperiodeId: UUID,
        val arbeidstaker: Behandlingsporing.Yrkesaktivitet.Arbeidstaker,
        val vedtaksperioderMedSammeFørsteFraværsdag: List<UUID>
    ) : Event

    data class SøknadHåndtertEvent(
        val meldingsreferanseId: UUID,
        val vedtaksperiodeId: UUID,
        val yrkesaktivitetssporing: Behandlingsporing.Yrkesaktivitet
    ) : Event

    data class SkatteinntekterLagtTilGrunnEvent(
        val arbeidstaker: Behandlingsporing.Yrkesaktivitet.Arbeidstaker,
        val vedtaksperiodeId: UUID,
        val behandlingId: UUID,
        val skjæringstidspunkt: LocalDate,
        val skatteinntekter: List<Skatteinntekt>,
        val omregnetÅrsinntekt: Double
    ) : Event {
        data class Skatteinntekt(
            val måned: YearMonth,
            val beløp: Double
        )
    }

    data class TrengerInntektsmeldingReplayEvent(val opplysninger: TrengerArbeidsgiveropplysninger) : Event

    data class TrengerArbeidsgiveropplysningerEvent(val opplysninger: TrengerArbeidsgiveropplysninger) : Event

    data class TrengerArbeidsgiveropplysninger(
        val personidentifikator: Personidentifikator,
        val arbeidstaker: Behandlingsporing.Yrkesaktivitet.Arbeidstaker,
        val vedtaksperiodeId: UUID,
        val skjæringstidspunkt: LocalDate,
        val sykmeldingsperioder: List<Periode>,
        val egenmeldingsperioder: List<Periode>,
        val førsteFraværsdager: List<FørsteFraværsdag>,
        val forespurteOpplysninger: Set<ForespurtOpplysning>
    )

    class TrengerIkkeArbeidsgiveropplysningerEvent(
        val arbeidstaker: Behandlingsporing.Yrkesaktivitet.Arbeidstaker,
        val vedtaksperiodeId: UUID
    ) : Event

    data class FørsteFraværsdag(
        val arbeidstaker: Behandlingsporing.Yrkesaktivitet.Arbeidstaker,
        val førsteFraværsdag: LocalDate
    )

    sealed class ForespurtOpplysning

    data object Inntekt : ForespurtOpplysning()
    data object Arbeidsgiverperiode : ForespurtOpplysning()
    data object Refusjon : ForespurtOpplysning()

    data class UtbetalingAnnullertEvent(
        val yrkesaktivitetssporing: Behandlingsporing.Yrkesaktivitet,
        val utbetalingId: UUID,
        val korrelasjonsId: UUID,
        val arbeidsgiverFagsystemId: String,
        val personFagsystemId: String,
        val fom: LocalDate,
        val tom: LocalDate,
        val annullertAvSaksbehandler: LocalDateTime,
        val saksbehandlerEpost: String,
        val saksbehandlerIdent: String
    ) : Event

    data class UtbetalingEndretEvent(
        val yrkesaktivitetssporing: Behandlingsporing.Yrkesaktivitet,
        val utbetalingId: UUID,
        val type: String,
        val forrigeStatus: String,
        val gjeldendeStatus: String,
        val arbeidsgiverOppdrag: OppdragEventDetaljer,
        val personOppdrag: OppdragEventDetaljer,
        val korrelasjonsId: UUID
    ) : Event {
        data class OppdragEventDetaljer(
            val fagsystemId: String,
            val mottaker: String,
            val nettoBeløp: Int,
            val linjer: List<OppdragEventLinjeDetaljer>
        ) {
            data class OppdragEventLinjeDetaljer(
                val fom: LocalDate,
                val tom: LocalDate,
                val totalbeløp: Int
            )

            companion object {
                fun mapOppdrag(oppdrag: Oppdrag) = OppdragEventDetaljer(
                    fagsystemId = oppdrag.fagsystemId,
                    mottaker = oppdrag.mottaker,
                    nettoBeløp = oppdrag.nettoBeløp(),
                    linjer = oppdrag.map { linje ->
                        OppdragEventLinjeDetaljer(
                            fom = linje.fom,
                            tom = linje.tom,
                            totalbeløp = linje.totalbeløp()
                        )
                    })
            }
        }
    }

    data class UtbetalingUtbetaltEvent(
        val yrkesaktivitetssporing: Behandlingsporing.Yrkesaktivitet,
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
        val arbeidsgiverOppdrag: OppdragEventDetaljer,
        val personOppdrag: OppdragEventDetaljer,
        val utbetalingsdager: List<Utbetalingsdag>,
        val ident: String
    ) : Event

    data class UtbetalingUtenUtbetalingEvent(
        val yrkesaktivitetssporing: Behandlingsporing.Yrkesaktivitet,
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
        val arbeidsgiverOppdrag: OppdragEventDetaljer,
        val personOppdrag: OppdragEventDetaljer,
        val utbetalingsdager: List<Utbetalingsdag>,
        val ident: String
    ) : Event

    data class OppdragEventDetaljer(
        val fagsystemId: String,
        val fagområde: String,
        val mottaker: String,
        val nettoBeløp: Int,
        val stønadsdager: Int,
        val fom: LocalDate,
        val tom: LocalDate,
        val linjer: List<OppdragEventLinjeDetaljer>
    ) {
        data class OppdragEventLinjeDetaljer(
            val fom: LocalDate,
            val tom: LocalDate,
            val sats: Int,
            val grad: Double,
            val stønadsdager: Int,
            val totalbeløp: Int,
            val statuskode: String?
        )

        companion object {
            fun mapOppdrag(oppdrag: Oppdrag) = mapOppdragdetaljer(oppdrag.detaljer())
            private fun mapOppdragdetaljer(detaljer: OppdragDetaljer) =
                OppdragEventDetaljer(
                    fagsystemId = detaljer.fagsystemId,
                    fagområde = detaljer.fagområde,
                    mottaker = detaljer.mottaker,
                    nettoBeløp = detaljer.nettoBeløp,
                    stønadsdager = detaljer.stønadsdager,
                    fom = detaljer.fom,
                    tom = detaljer.tom,
                    linjer = detaljer.linjer.map {
                        OppdragEventLinjeDetaljer(
                            fom = it.fom,
                            tom = it.tom,
                            sats = it.sats,
                            grad = it.grad,
                            stønadsdager = it.stønadsdager,
                            totalbeløp = it.totalbeløp,
                            statuskode = it.statuskode
                        )
                    }
                )
        }
    }

    data class Utbetalingsdag(
        val dato: LocalDate,
        val type: Dagtype,
        val beløpTilArbeidsgiver: Int,
        val beløpTilBruker: Int,
        val sykdomsgrad: Int,
        val dekningsgrad: Int,
        val begrunnelser: List<EksternBegrunnelseDTO>?
    ) {
        constructor(dato: LocalDate, type: Dagtype, dekningsgrad: Int) : this(dato, type, 0, 0, 0, dekningsgrad, null)

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
            Feriedag,
            ArbeidIkkeGjenopptattDag,
            AndreYtelser,
            Ventetidsdag
        }

        enum class EksternBegrunnelseDTO {
            SykepengedagerOppbrukt,
            SykepengedagerOppbruktOver67,
            MinimumInntekt,
            MinimumInntektOver67,
            EgenmeldingUtenforArbeidsgiverperiode,
            AndreYtelserAap,
            AndreYtelserDagpenger,
            AndreYtelserForeldrepenger,
            AndreYtelserOmsorgspenger,
            AndreYtelserOpplaringspenger,
            AndreYtelserPleiepenger,
            AndreYtelserSvangerskapspenger,
            MinimumSykdomsgrad,
            EtterDødsdato,
            ManglerMedlemskap,
            ManglerOpptjening,
            Over70;

            internal companion object {
                fun fraBegrunnelse(begrunnelse: Begrunnelse) = when (begrunnelse) {
                    is Begrunnelse.SykepengedagerOppbrukt -> SykepengedagerOppbrukt
                    is Begrunnelse.SykepengedagerOppbruktOver67 -> SykepengedagerOppbruktOver67
                    is Begrunnelse.MinimumSykdomsgrad -> MinimumSykdomsgrad
                    is Begrunnelse.EgenmeldingUtenforArbeidsgiverperiode -> EgenmeldingUtenforArbeidsgiverperiode
                    is Begrunnelse.MeldingTilNavDagUtenforVentetid -> EgenmeldingUtenforArbeidsgiverperiode // TODO: Map til MeldingTilNavDagUtenforVentetid
                    is Begrunnelse.MinimumInntekt -> MinimumInntekt
                    is Begrunnelse.MinimumInntektOver67 -> MinimumInntektOver67
                    is Begrunnelse.EtterDødsdato -> EtterDødsdato
                    is Begrunnelse.ManglerMedlemskap -> ManglerMedlemskap
                    is Begrunnelse.ManglerOpptjening -> ManglerOpptjening
                    is Begrunnelse.Over70 -> Over70
                    is Begrunnelse.AndreYtelserAap -> AndreYtelserAap
                    is Begrunnelse.AndreYtelserDagpenger -> AndreYtelserDagpenger
                    is Begrunnelse.AndreYtelserForeldrepenger -> AndreYtelserForeldrepenger
                    is Begrunnelse.AndreYtelserOmsorgspenger -> AndreYtelserOmsorgspenger
                    is Begrunnelse.AndreYtelserOpplaringspenger -> AndreYtelserOpplaringspenger
                    is Begrunnelse.AndreYtelserPleiepenger -> AndreYtelserPleiepenger
                    is Begrunnelse.AndreYtelserSvangerskapspenger -> AndreYtelserSvangerskapspenger
                    is Begrunnelse.NyVilkårsprøvingNødvendig -> SykepengedagerOppbrukt // TODO: Map til NyVilkårsprøvingNødvendig
                }
            }
        }
    }

    data class FeriepengerUtbetaltEvent(
        val arbeidstaker: Behandlingsporing.Yrkesaktivitet.Arbeidstaker,
        val fom: LocalDate,
        val tom: LocalDate,
        val arbeidsgiverOppdrag: FeriepengeoppdragEventDetaljer,
        val personOppdrag: FeriepengeoppdragEventDetaljer
    ) : Event {
        data class FeriepengeoppdragEventDetaljer(
            val fagsystemId: String,
            val mottaker: String,
            val totalbeløp: Int
        ) {
            companion object {
                fun mapOppdrag(oppdrag: Feriepengeoppdrag) =
                    FeriepengeoppdragEventDetaljer(
                        fagsystemId = oppdrag.fagsystemId,
                        mottaker = oppdrag.mottaker,
                        totalbeløp = oppdrag.totalbeløp
                    )
            }
        }
    }

    data class OverlappendeInfotrygdperioder(
        val overlappendeInfotrygdperioder: List<OverlappendeInfotrygdperiodeEtterInfotrygdendring>,
        val infotrygdhistorikkHendelseId: UUID
    ) : Event

    data class OverlappendeInfotrygdperiodeEtterInfotrygdendring(
        val yrkesaktivitetssporing: Behandlingsporing.Yrkesaktivitet,
        val vedtaksperiodeId: UUID,
        val kanForkastes: Boolean,
        val vedtaksperiodeFom: LocalDate,
        val vedtaksperiodeTom: LocalDate,
        val vedtaksperiodetilstand: String,
        val infotrygdperioder: List<Infotrygdperiode>
    ) {
        data class Infotrygdperiode(
            val fom: LocalDate,
            val tom: LocalDate,
            val type: String,
            val orgnummer: String?
        )
    }

    data class AvsluttetUtenVedtakEvent(
        val yrkesaktivitetssporing: Behandlingsporing.Yrkesaktivitet,
        val vedtaksperiodeId: UUID,
        val behandlingId: UUID,
        val periode: Periode,
        val hendelseIder: Set<UUID>,
        val skjæringstidspunkt: LocalDate,
        val avsluttetTidspunkt: LocalDateTime
    ) : Event

    data class VedtaksperiodeNyUtbetalingEvent(
        val yrkesaktivitetssporing: Behandlingsporing.Yrkesaktivitet,
        val utbetalingId: UUID,
        val vedtaksperiodeId: UUID
    ) : Event

    data class BehandlingLukketEvent(
        val yrkesaktivitetssporing: Behandlingsporing.Yrkesaktivitet,
        val vedtaksperiodeId: UUID,
        val behandlingId: UUID
    ) : Event

    data class BehandlingForkastetEvent(
        val yrkesaktivitetssporing: Behandlingsporing.Yrkesaktivitet,
        val vedtaksperiodeId: UUID,
        val behandlingId: UUID,
        val automatiskBehandling: Boolean
    ) : Event

    data class BehandlingOpprettetEvent(
        val yrkesaktivitetssporing: Behandlingsporing.Yrkesaktivitet,
        val vedtaksperiodeId: UUID,
        val behandlingId: UUID,
        val søknadIder: Set<UUID>,
        val fom: LocalDate,
        val tom: LocalDate,
        val type: Type,
        val kilde: Kilde
    ) : Event {
        enum class Type {
            Søknad,
            Omgjøring,
            Revurdering
        }

        data class Kilde(
            val meldingsreferanseId: UUID,
            val innsendt: LocalDateTime,
            val registert: LocalDateTime,
            val avsender: Avsender
        )
    }

    data class UtkastTilVedtakEvent(
        val skjæringstidspunkt: LocalDate,
        val vedtaksperiodeId: UUID,
        val behandlingId: UUID,
        val tags: Set<String>,
        val `6G`: Double?,
        val yrkesaktivitetssporing: Behandlingsporing.Yrkesaktivitet
    ) : Event {
        sealed interface Sykepengegrunnlagsfakta {
            val fastsatt: String
            val omregnetÅrsinntekt: Double
        }

        data class FastsattIInfotrygd(override val omregnetÅrsinntekt: Double, val arbeidsgiver: String) : Sykepengegrunnlagsfakta {
            override val fastsatt = "IInfotrygd"
        }

        data class FastsattEtterHovedregel(override val omregnetÅrsinntekt: Double, val sykepengegrunnlag: Double, val `6G`: Double, val arbeidsgivere: List<Arbeidsgiver>) : Sykepengegrunnlagsfakta {
            override val fastsatt = "EtterHovedregel"

            data class Arbeidsgiver(val arbeidsgiver: String, val omregnetÅrsinntekt: Double, val inntektskilde: Inntektskilde)
        }

        data class FastsattEtterSkjønn(override val omregnetÅrsinntekt: Double, val sykepengegrunnlag: Double, val `6G`: Double, val arbeidsgivere: List<Arbeidsgiver>) : Sykepengegrunnlagsfakta {
            override val fastsatt = "EtterSkjønn"
            val skjønnsfastsatt = arbeidsgivere.sumOf { it.skjønnsfastsatt }

            data class Arbeidsgiver(val arbeidsgiver: String, val omregnetÅrsinntekt: Double, val skjønnsfastsatt: Double, val inntektskilde: Inntektskilde)
        }

        enum class Inntektskilde {
            Arbeidsgiver,
            AOrdningen,
            Saksbehandler,
            Sigrun
        }
    }

    data class GodkjenningEvent(
        val yrkesaktivitetssporing: Behandlingsporing.Yrkesaktivitet,
        val vedtaksperiodeId: UUID,
        val behandlingId: UUID,
        val utbetalingId: UUID,
        val periode: Periode,
        val vilkårsgrunnlagId: UUID,
        val skjæringstidspunkt: LocalDate,
        val førstegangsbehandling: Boolean,
        val utbetalingtype: String,
        val inntektskilde: String,
        val periodetype: String,
        val tags: Set<String>,
        val orgnummereMedRelevanteArbeidsforhold: Set<String>,
        val kanAvvises: Boolean,
        val relevanteSøknader: Set<UUID>,
        val perioderMedSammeSkjæringstidspunkt: List<PeriodeMedSammeSkjæringstidspunkt>,
        val forbrukteSykedager: Int,
        val gjenståendeSykedager: Int,
        val foreløpigBeregnetSluttPåSykepenger: LocalDate,
        val utbetalingsdager: List<Utbetalingsdag>,
        val arbeidssituasjon: String,
        val sykepengegrunnlagsfakta: Sykepengegrunnlagsfakta
    ) : Event {

        data class PeriodeMedSammeSkjæringstidspunkt(
            val vedtaksperiodeId: UUID,
            val behandlingId: UUID,
            val periode: Periode
        )

        sealed interface Sykepengegrunnlagsfakta {
            val sykepengegrunnlag: Double
            val seksG: Double

            data class ArbeidstakerEtterHovedregel(override val sykepengegrunnlag: Double, override val seksG: Double, val arbeidsgivere: List<Arbeidsgiver>) : Sykepengegrunnlagsfakta {
                data class Arbeidsgiver(val arbeidsgiver: String, val omregnetÅrsinntekt: Double, val inntektskilde: String)
            }

            data class ArbeidstakerEtterSkjønn(override val sykepengegrunnlag: Double, override val seksG: Double, val arbeidsgivere: List<Arbeidsgiver>) : Sykepengegrunnlagsfakta {
                data class Arbeidsgiver(val arbeidsgiver: String, val omregnetÅrsinntekt: Double, val skjønnsfastsatt: Double)
            }

            data class ArbeidstakerFraInfotrygd(override val sykepengegrunnlag: Double, override val seksG: Double) : Sykepengegrunnlagsfakta

            data class SelvstendigEtterHovedregel(override val sykepengegrunnlag: Double, override val seksG: Double, val pensjonsgivendeInntekter: List<PensjonsgivendeInntekt>, val beregningsgrunnlag: Double) : Sykepengegrunnlagsfakta {
                data class PensjonsgivendeInntekt(val årstall: Year, val beløp: Double)
            }
        }

        // TODO: Æsj, dette burde bare vært ute i mediator, men i all tid det sendes to steder så er det for kjedelig å mappe dette to steder og
        val behovInput = mapOf(
            "periodeFom" to "${periode.start}",
            "periodeTom" to "${periode.endInclusive}",
            "skjæringstidspunkt" to "$skjæringstidspunkt",
            "vilkårsgrunnlagId" to "$vilkårsgrunnlagId",
            "periodetype" to periodetype,
            "førstegangsbehandling" to førstegangsbehandling,
            "utbetalingtype" to utbetalingtype,
            "inntektskilde" to inntektskilde,
            "orgnummereMedRelevanteArbeidsforhold" to orgnummereMedRelevanteArbeidsforhold,
            "tags" to tags,
            "kanAvvises" to kanAvvises,
            "behandlingId" to "$behandlingId",
            "relevanteSøknader" to relevanteSøknader,
            "perioderMedSammeSkjæringstidspunkt" to perioderMedSammeSkjæringstidspunkt.map {
                mapOf(
                    "vedtaksperiodeId" to "${it.vedtaksperiodeId}",
                    "behandlingId" to "${it.behandlingId}",
                    "fom" to "${it.periode.start}",
                    "tom" to "${it.periode.endInclusive}"
                )
            },
            "forbrukteSykedager" to forbrukteSykedager,
            "gjenståendeSykedager" to gjenståendeSykedager,
            "foreløpigBeregnetSluttPåSykepenger" to "$foreløpigBeregnetSluttPåSykepenger",
            "utbetalingsdager" to utbetalingsdager.map { it.tilBehovMap() },
            "sykepengegrunnlagsfakta" to when (sykepengegrunnlagsfakta) {
                is Sykepengegrunnlagsfakta.ArbeidstakerEtterHovedregel -> mapOf(
                    "sykepengegrunnlag" to sykepengegrunnlagsfakta.sykepengegrunnlag,
                    "6G" to sykepengegrunnlagsfakta.seksG,
                    "fastsatt" to "EtterHovedregel",
                    "arbeidsgivere" to sykepengegrunnlagsfakta.arbeidsgivere.map {
                        mapOf(
                            "arbeidsgiver" to it.arbeidsgiver,
                            "omregnetÅrsinntekt" to it.omregnetÅrsinntekt,
                            "inntektskilde" to it.inntektskilde
                        )
                    },
                    "selvstendig" to null
                )
                is Sykepengegrunnlagsfakta.ArbeidstakerEtterSkjønn -> mapOf(
                    "sykepengegrunnlag" to sykepengegrunnlagsfakta.sykepengegrunnlag,
                    "6G" to sykepengegrunnlagsfakta.seksG,
                    "fastsatt" to "EtterSkjønn",
                    "arbeidsgivere" to sykepengegrunnlagsfakta.arbeidsgivere.map {
                        mapOf(
                            "arbeidsgiver" to it.arbeidsgiver,
                            "omregnetÅrsinntekt" to it.omregnetÅrsinntekt,
                            "skjønnsfastsatt" to it.skjønnsfastsatt,
                            "inntektskilde" to "Saksbehandler",
                        )
                    },
                    "selvstendig" to null
                )
                is Sykepengegrunnlagsfakta.ArbeidstakerFraInfotrygd -> mapOf(
                    "sykepengegrunnlag" to sykepengegrunnlagsfakta.sykepengegrunnlag,
                    "6G" to sykepengegrunnlagsfakta.seksG,
                    "fastsatt" to "IInfotrygd",
                    "selvstendig" to null
                )
                is Sykepengegrunnlagsfakta.SelvstendigEtterHovedregel -> mapOf(
                    "sykepengegrunnlag" to sykepengegrunnlagsfakta.sykepengegrunnlag,
                    "6G" to sykepengegrunnlagsfakta.seksG,
                    "fastsatt" to "EtterHovedregel",
                    "selvstendig" to mapOf(
                        "pensjonsgivendeInntekter" to sykepengegrunnlagsfakta.pensjonsgivendeInntekter.map {
                            mapOf(
                                "årstall" to it.årstall.value,
                                "beløp" to it.beløp
                            )
                        },
                        "beregningsgrunnlag" to sykepengegrunnlagsfakta.beregningsgrunnlag,
                    ),
                    "arbeidsgivere" to emptyList<Map<String, Any>>(), // Selvstendig har ingen arbeidsgivere i sykepengegrunnlaget
                )
            },
            "arbeidssituasjon" to arbeidssituasjon
        )

        private companion object {
            private fun Utbetalingsdag.tilBehovMap() =
                mapOf(
                    "dato" to "${this.dato}",
                    "type" to when (this.type) {
                        Dagtype.ArbeidsgiverperiodeDag -> "ArbeidsgiverperiodeDag"
                        Dagtype.NavDag -> "NavDag"
                        Dagtype.NavHelgDag -> "NavHelgDag"
                        Dagtype.Arbeidsdag -> "Arbeidsdag"
                        Dagtype.Fridag -> "Fridag"
                        Dagtype.AvvistDag -> "AvvistDag"
                        Dagtype.UkjentDag -> "UkjentDag"
                        Dagtype.ForeldetDag -> "ForeldetDag"
                        Dagtype.Permisjonsdag -> "Permisjonsdag"
                        Dagtype.Feriedag -> "Feriedag"
                        Dagtype.ArbeidIkkeGjenopptattDag -> "ArbeidIkkeGjenopptattDag"
                        Dagtype.AndreYtelser -> "AndreYtelser"
                        Dagtype.Ventetidsdag -> "Ventetidsdag"
                    },
                    "beløpTilArbeidsgiver" to this.beløpTilArbeidsgiver,
                    "beløpTilBruker" to this.beløpTilBruker,
                    "sykdomsgrad" to this.sykdomsgrad,
                    "dekningsgrad" to this.dekningsgrad,
                    "begrunnelser" to (this.begrunnelser?.map {
                        when (it) {
                            EksternBegrunnelseDTO.SykepengedagerOppbrukt -> "SykepengedagerOppbrukt"
                            EksternBegrunnelseDTO.SykepengedagerOppbruktOver67 -> "SykepengedagerOppbruktOver67"
                            EksternBegrunnelseDTO.MinimumInntekt -> "MinimumInntekt"
                            EksternBegrunnelseDTO.MinimumInntektOver67 -> "MinimumInntektOver67"
                            EksternBegrunnelseDTO.EgenmeldingUtenforArbeidsgiverperiode -> "EgenmeldingUtenforArbeidsgiverperiode"
                            EksternBegrunnelseDTO.AndreYtelserAap -> "AndreYtelserAap"
                            EksternBegrunnelseDTO.AndreYtelserDagpenger -> "AndreYtelserDagpenger"
                            EksternBegrunnelseDTO.AndreYtelserForeldrepenger -> "AndreYtelserForeldrepenger"
                            EksternBegrunnelseDTO.AndreYtelserOmsorgspenger -> "AndreYtelserOmsorgspenger"
                            EksternBegrunnelseDTO.AndreYtelserOpplaringspenger -> "AndreYtelserOpplaringspenger"
                            EksternBegrunnelseDTO.AndreYtelserPleiepenger -> "AndreYtelserPleiepenger"
                            EksternBegrunnelseDTO.AndreYtelserSvangerskapspenger -> "AndreYtelserSvangerskapspenger"
                            EksternBegrunnelseDTO.MinimumSykdomsgrad -> "MinimumSykdomsgrad"
                            EksternBegrunnelseDTO.EtterDødsdato -> "EtterDødsdato"
                            EksternBegrunnelseDTO.ManglerMedlemskap -> "ManglerMedlemskap"
                            EksternBegrunnelseDTO.ManglerOpptjening -> "ManglerOpptjening"
                            EksternBegrunnelseDTO.Over70 -> "Over70"
                        }
                    } ?: emptyList())
                )
        }
    }

    data class AvsluttetMedVedtakEvent(
        val yrkesaktivitetssporing: Behandlingsporing.Yrkesaktivitet,
        val vedtaksperiodeId: UUID,
        val behandlingId: UUID,
        val periode: Periode,
        val hendelseIder: Set<UUID>,
        val skjæringstidspunkt: LocalDate,
        val beregningsgrunnlag: Double,
        val `6G`: Double?,
        val sykepengegrunnlag: Double,
        val utbetalingId: UUID,
        val vedtakFattetTidspunkt: LocalDateTime,
        val sykepengegrunnlagsfakta: UtkastTilVedtakEvent.Sykepengegrunnlagsfakta
    ) : Event

    data class SelvstendigIngenDagerIgjenEvent(
        val behandlingId: UUID,
        val skjæringstidspunkt: LocalDate
    ) : Event

    data class SelvstendigUtbetaltEtterVentetidEvent(
        val behandlingId: UUID,
        val skjæringstidspunkt: LocalDate
    ) : Event

    data class OverstyringIgangsatt(
        val årsak: String,
        val skjæringstidspunkt: LocalDate,
        val periodeForEndring: Periode,
        val berørtePerioder: List<VedtaksperiodeData>,
        val meldingsreferanseId: UUID
    ) : Event {
        enum class TypeEndring {
            OVERSTYRING,
            REVURDERING
        }

        val typeEndring get() = if (berørtePerioder.any { it.typeEndring == TypeEndring.REVURDERING }) TypeEndring.REVURDERING else TypeEndring.OVERSTYRING

        data class VedtaksperiodeData(
            val yrkesaktivitetssporing: Behandlingsporing.Yrkesaktivitet,
            val vedtaksperiodeId: UUID,
            val periode: Periode,
            val skjæringstidspunkt: LocalDate,
            val typeEndring: TypeEndring
        )
    }

    data class VedtaksperiodeOpprettet(
        val vedtaksperiodeId: UUID,
        val yrkesaktivitetssporing: Behandlingsporing.Yrkesaktivitet,
        val periode: Periode,
        val skjæringstidspunkt: LocalDate,
        val opprettet: LocalDateTime
    ) : Event

    data class VedtaksperiodeAnnullertEvent(
        val fom: LocalDate,
        val tom: LocalDate,
        val vedtaksperiodeId: UUID,
        val yrkesaktivitetssporing: Behandlingsporing.Yrkesaktivitet,
        val behandlingId: UUID
    ) : Event

    data class BenyttetGrunnlagsdataForBeregningEvent(
        val behandlingId: UUID,
        val vedtaksperiodeId: UUID,
        val yrkesaktivitetssporing: Behandlingsporing.Yrkesaktivitet,
        val periode: Periode,
        val behandlingOpprettetTidspunkt: LocalDateTime,
        val forsikring: Forsikring?
    ) : Event

    data class TrengerInformasjonTilVilkårsprøvingEvent(
        val vedtaksperiodeId: UUID,
        val behandlingId: UUID,
        val yrkesaktivitetssporing: Behandlingsporing.Yrkesaktivitet,
        val skjæringstidspunkt: LocalDate,
        val periodeForMedlemskapsvurdering: Periode,
        val beregningsperiodeForOpptjeningsvurdering: Beregningsperiode,
        val beregningsperiodeForSykepengegrunnlagsvurdering: Beregningsperiode
    ) : Event {
        data class Beregningsperiode(val start: YearMonth, val slutt: YearMonth) {
            init {
                check(slutt >= start) { "Hæ? $start til $slutt er jo ikke nesten en gyldig beregningsperiode!" }
            }
        }
    }

    data class TrengerInformasjonTilBeregningEvent(
        val vedtaksperiodeId: UUID,
        val behandlingId: UUID,
        val yrkesaktivitetssporing: Behandlingsporing.Yrkesaktivitet,
        val periodeForForeldrepenger: Periode,
        val periodeForPleiepenger: Periode,
        val periodeForOmsorgspenger: Periode,
        val periodeForOpplæringspenger: Periode,
        val periodeForInstitusjonsopphold: Periode,
        val periodeForArbeidsavklaringspenger: Periode,
        val periodeForDagpenger: Periode,
        val beregningsperiode: Periode,
        // TODO: Fjern disse feltene, det brukes til selvstendig forsikring som skal flyttes til Vilkårsprøving
        val trengerInformasjonOmSelvstendigForsikring: Boolean,
        val skjæringstidspunkt: LocalDate
    ): Event

    data class TrengerInitiellHistorikkFraInfotrygdEvent(
        val periode: Periode,
        val vedtaksperiodeId: UUID,
        val yrkesaktivitetssporing: Behandlingsporing.Yrkesaktivitet
    ): Event

    data class TrengerOppdatertHistorikkFraInfotrygdEvent(
        val periode: Periode
    ): Event

    data class UtbetalFeriepengerEvent(
        val mottaker: String,
        val fagområde: String,
        val fagsystemId: String,
        val endringskode: String,
        val linje: Linje,
        val organisasjonsnummer: String,
        val utbetalingId: UUID
    ): Event {
        val saksbehandler = "SPLEIS"

        data class Linje(
            val periode: Periode,
            val sats: Int,
            val endringskode: String,
            val delytelseId: Int,
            val refDelytelseId: Int?,
            val refFagsystemId: String?,
            val statuskode: String?,
            val datoStatusFom: LocalDate?,
            val klassekode: String
        ) {
            val satstype = "ENG"
        }
    }

    data class Oppdragsdetaljer(
        val mottaker: String,
        val fagområde: String,
        val linjer: List<Linje>,
        val fagsystemId: String,
        val endringskode: String,
        val maksdato: LocalDate?
    ) {
        data class Linje(
            val periode: Periode,
            val sats: Int,
            val grad: Int,
            val stønadsdager: Int,
            val totalbeløp: Int,
            val endringskode: String,
            val delytelseId: Int,
            val refDelytelseId: Int?,
            val refFagsystemId: String?,
            val statuskode: String?,
            val datoStatusFom: LocalDate?,
            val klassekode: String,
        ) {
            val satstype = "DAG"
            val datoKlassifikFom = periode.start
        }
    }

    data class UtbetalingEvent(
        val yrkesaktivitetssporing: Behandlingsporing.Yrkesaktivitet,
        val vedtaksperiodeId: UUID,
        val behandlingId: UUID,
        val utbetalingId: UUID,
        val oppdragsdetaljer: Oppdragsdetaljer,
        val saksbehandler: String,
    ): Event

    data class SimuleringEvent(
        val yrkesaktivitetssporing: Behandlingsporing.Yrkesaktivitet,
        val vedtaksperiodeId: UUID,
        val behandlingId: UUID,
        val utbetalingId: UUID,
        val oppdragsdetaljer: Oppdragsdetaljer
    ): Event {
        val saksbehandler = "SPLEIS"
    }

    data class NyInformasjonIInfotrygdEvent(
        val fraOgMed: LocalDate
    ): Event

    fun inntektsmeldingReplay(event: TrengerInntektsmeldingReplayEvent) {}
    fun vedtaksperiodeOpprettet(event: VedtaksperiodeOpprettet) {}
    fun vedtaksperiodePåminnet(event: VedtaksperiodePåminnetEvent) {}
    fun vedtaksperiodeIkkePåminnet(event: VedtaksperiodeIkkePåminnetEvent) {}
    fun vedtaksperiodeEndret(event: VedtaksperiodeEndretEvent) {}
    fun vedtaksperioderVenter(event: VedtaksperioderVenterEvent) {}
    fun vedtaksperiodeForkastet(event: VedtaksperiodeForkastetEvent) {}
    fun sykefraværstilfelleIkkeFunnet(event: SykefraværstilfelleIkkeFunnet) {}
    fun trengerArbeidsgiveropplysninger(event: TrengerArbeidsgiveropplysningerEvent) {}
    fun trengerIkkeArbeidsgiveropplysninger(event: TrengerIkkeArbeidsgiveropplysningerEvent) {}
    fun utbetalingEndret(event: UtbetalingEndretEvent) {}
    fun utbetalingUtbetalt(event: UtbetalingUtbetaltEvent) {}
    fun utbetalingUtenUtbetaling(event: UtbetalingUtenUtbetalingEvent) {}
    fun feriepengerUtbetalt(event: FeriepengerUtbetaltEvent) {}
    fun annullering(event: UtbetalingAnnullertEvent) {}
    fun planlagtAnnullering(event: PlanlagtAnnulleringEvent) {}
    fun avsluttetMedVedtak(event: AvsluttetMedVedtakEvent) {}
    fun selvstendigIngenDagerIgjen(event: SelvstendigIngenDagerIgjenEvent) {}
    fun selvstendigUtbetaltEtterVentetid(event: SelvstendigUtbetaltEtterVentetidEvent) {}
    fun analytiskDatapakke(event: AnalytiskDatapakkeEvent) {}

    fun behandlingLukket(event: BehandlingLukketEvent) {}
    fun behandlingForkastet(event: BehandlingForkastetEvent) {}
    fun nyBehandling(event: BehandlingOpprettetEvent) {}
    fun avsluttetUtenVedtak(event: AvsluttetUtenVedtakEvent) {}
    fun nyVedtaksperiodeUtbetaling(event: VedtaksperiodeNyUtbetalingEvent) {}

    fun overstyringIgangsatt(event: OverstyringIgangsatt) {}
    fun overlappendeInfotrygdperioder(event: OverlappendeInfotrygdperioder) {}
    fun inntektsmeldingFørSøknad(event: InntektsmeldingFørSøknadEvent) {}
    fun inntektsmeldingIkkeHåndtert(event: InntektsmeldingIkkeHåndtertEvent) {}
    fun inntektsmeldingHåndtert(event: InntektsmeldingHåndtertEvent) {}
    fun skatteinntekterLagtTilGrunn(event: SkatteinntekterLagtTilGrunnEvent) {}
    fun søknadHåndtert(event: SøknadHåndtertEvent) {}
    fun behandlingUtført() {}
    fun vedtaksperiodeAnnullert(vedtaksperiodeAnnullertEvent: VedtaksperiodeAnnullertEvent) {}
    fun utkastTilVedtak(event: UtkastTilVedtakEvent) {}
    fun benyttetGrunnlagsdataForBeregning(event: BenyttetGrunnlagsdataForBeregningEvent) {}
    fun trengerInformasjonTilVilkårsprøving(event: TrengerInformasjonTilVilkårsprøvingEvent) {}
    fun trengerInformasjonTilBeregning(event: TrengerInformasjonTilBeregningEvent) {}
    fun trengerInitiellHistorikkFraInfotrygd(event: TrengerInitiellHistorikkFraInfotrygdEvent) {}
    fun trengerOppdatertHistorikkFraInfotrygd(event: TrengerOppdatertHistorikkFraInfotrygdEvent) {}
    fun utbetalFeriepenger(event: UtbetalFeriepengerEvent) {}
    fun utbetal(event: UtbetalingEvent) {}
    fun simuler(event: SimuleringEvent) {}
    fun trengerGodkjenning(event: GodkjenningEvent) {}
    fun nyInformasjonIInfotrygd(event: NyInformasjonIInfotrygdEvent) {}
}
