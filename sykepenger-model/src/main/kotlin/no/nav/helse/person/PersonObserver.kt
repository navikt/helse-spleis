package no.nav.helse.person

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID
import no.nav.helse.Personidentifikator
import no.nav.helse.feriepenger.Feriepengeoppdrag
import no.nav.helse.hendelser.Avsender
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.person.tilstandsmaskin.TilstandType
import no.nav.helse.utbetalingslinjer.Oppdrag
import no.nav.helse.utbetalingslinjer.OppdragDetaljer
import no.nav.helse.utbetalingstidslinje.Begrunnelse

interface PersonObserver {

    data class PlanlagtAnnulleringEvent(
        val yrkesaktivitet: String,
        val vedtaksperioder: List<UUID>,
        val fom: LocalDate,
        val tom: LocalDate,
        val saksbehandlerIdent: String,
        val årsaker: List<String>,
        val begrunnelse: String
    )

    data class SykefraværstilfelleIkkeFunnet(
        val skjæringstidspunkt: LocalDate
    )

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
    )

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
    ) {
        data class Pengeinformasjon (
            val totalBeløp: Double,
            val nettoBeløp: Double
        )
        data class Daginformasjon(
            val antallDager: Int,
            val nettoDager: Int
        )
    }


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
    ) {
        val trengerArbeidsgiveropplysninger = sykmeldingsperioder.isNotEmpty()
    }

    data class InntektsmeldingFørSøknadEvent(
        val inntektsmeldingId: UUID,
        val yrkesaktivitetssporing: Behandlingsporing.Yrkesaktivitet
    )

    data class SkatteinntekterLagtTilGrunnEvent(
        val yrkesaktivitetssporing: Behandlingsporing.Yrkesaktivitet,
        val vedtaksperiodeId: UUID,
        val behandlingId: UUID,
        val skjæringstidspunkt: LocalDate,
        val skatteinntekter: List<Skatteinntekt>,
        val omregnetÅrsinntekt: Double
    ) {
        data class Skatteinntekt(
            val måned: YearMonth,
            val beløp: Double
        )
    }

    data class TrengerArbeidsgiveropplysningerEvent(
        val personidentifikator: Personidentifikator,
        val yrkesaktivitetssporing: Behandlingsporing.Yrkesaktivitet,
        val vedtaksperiodeId: UUID,
        val skjæringstidspunkt: LocalDate,
        val sykmeldingsperioder: List<Periode>,
        val egenmeldingsperioder: List<Periode>,
        val førsteFraværsdager: List<FørsteFraværsdag>,
        val forespurteOpplysninger: Set<ForespurtOpplysning>
    )

    class TrengerIkkeArbeidsgiveropplysningerEvent(
        val yrkesaktivitetssporing: Behandlingsporing.Yrkesaktivitet,
        val vedtaksperiodeId: UUID
    ) {
    }

    data class FørsteFraværsdag(
        val yrkesaktivitetssporing: Behandlingsporing.Yrkesaktivitet,
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
    )

    data class UtbetalingEndretEvent(
        val yrkesaktivitetssporing: Behandlingsporing.Yrkesaktivitet,
        val utbetalingId: UUID,
        val type: String,
        val forrigeStatus: String,
        val gjeldendeStatus: String,
        val arbeidsgiverOppdrag: OppdragEventDetaljer,
        val personOppdrag: OppdragEventDetaljer,
        val korrelasjonsId: UUID
    ) {
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
    ) {
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
    }

    data class Utbetalingsdag(
        val dato: LocalDate,
        val type: Dagtype,
        val beløpTilArbeidsgiver: Int,
        val beløpTilBruker: Int,
        val sykdomsgrad: Int,
        val begrunnelser: List<EksternBegrunnelseDTO>?
    ) {
        constructor(dato: LocalDate, type: Dagtype) : this(dato, type, 0, 0, 0, null)

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
        val yrkesaktivitetssporing: Behandlingsporing.Yrkesaktivitet,
        val fom: LocalDate,
        val tom: LocalDate,
        val arbeidsgiverOppdrag: FeriepengeoppdragEventDetaljer,
        val personOppdrag: FeriepengeoppdragEventDetaljer
    ) {
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
    )

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
    )

    data class BehandlingLukketEvent(
        val yrkesaktivitetssporing: Behandlingsporing.Yrkesaktivitet,
        val vedtaksperiodeId: UUID,
        val behandlingId: UUID
    )

    data class BehandlingForkastetEvent(
        val yrkesaktivitetssporing: Behandlingsporing.Yrkesaktivitet,
        val vedtaksperiodeId: UUID,
        val behandlingId: UUID,
        val automatiskBehandling: Boolean
    )

    data class BehandlingOpprettetEvent(
        val yrkesaktivitetssporing: Behandlingsporing.Yrkesaktivitet,
        val vedtaksperiodeId: UUID,
        val behandlingId: UUID,
        val søknadIder: Set<UUID>,
        val fom: LocalDate,
        val tom: LocalDate,
        val type: Type,
        val kilde: Kilde
    ) {
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
    ) {
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

    data class AvsluttetMedVedtakEvent(
        val yrkesaktivitetssporing: Behandlingsporing.Yrkesaktivitet,
        val vedtaksperiodeId: UUID,
        val behandlingId: UUID,
        val periode: Periode,
        val hendelseIder: Set<UUID>,
        val skjæringstidspunkt: LocalDate,
        val sykepengegrunnlag: Double,
        val utbetalingId: UUID,
        val vedtakFattetTidspunkt: LocalDateTime,
        val sykepengegrunnlagsfakta: UtkastTilVedtakEvent.Sykepengegrunnlagsfakta
    )

    data class OverstyringIgangsatt(
        val årsak: String,
        val skjæringstidspunkt: LocalDate,
        val periodeForEndring: Periode,
        val berørtePerioder: List<VedtaksperiodeData>,
        val meldingsreferanseId: UUID
    ) {
        val typeEndring get() = if (berørtePerioder.any { it.typeEndring == "REVURDERING" }) "REVURDERING" else "OVERSTYRING"

        data class VedtaksperiodeData(
            val yrkesaktivitetssporing: Behandlingsporing.Yrkesaktivitet,
            val vedtaksperiodeId: UUID,
            val periode: Periode,
            val skjæringstidspunkt: LocalDate,
            val typeEndring: String
        )
    }

    data class VedtaksperiodeOpprettet(
        val vedtaksperiodeId: UUID,
        val yrkesaktivitetssporing: Behandlingsporing.Yrkesaktivitet,
        val periode: Periode,
        val skjæringstidspunkt: LocalDate,
        val opprettet: LocalDateTime
    )

    data class VedtaksperiodeAnnullertEvent(
        val fom: LocalDate,
        val tom: LocalDate,
        val vedtaksperiodeId: UUID,
        val yrkesaktivitetssporing: Behandlingsporing.Yrkesaktivitet,
        val behandlingId: UUID
    )

    fun inntektsmeldingReplay(event: TrengerArbeidsgiveropplysningerEvent) {}
    fun vedtaksperiodeOpprettet(event: VedtaksperiodeOpprettet) {}
    fun vedtaksperiodePåminnet(vedtaksperiodeId: UUID, organisasjonsnummer: String, påminnelse: Påminnelse) {}
    fun vedtaksperiodeIkkePåminnet(vedtaksperiodeId: UUID, organisasjonsnummer: String, nåværendeTilstand: TilstandType) {}
    fun vedtaksperiodeEndret(event: VedtaksperiodeEndretEvent) {}
    fun vedtaksperioderVenter(eventer: List<VedtaksperiodeVenterEvent>) {}
    fun vedtaksperiodeForkastet(event: VedtaksperiodeForkastetEvent) {}
    fun sykefraværstilfelleIkkeFunnet(event: SykefraværstilfelleIkkeFunnet) {}
    fun trengerArbeidsgiveropplysninger(event: TrengerArbeidsgiveropplysningerEvent) {}
    fun trengerIkkeArbeidsgiveropplysninger(event: TrengerIkkeArbeidsgiveropplysningerEvent) {}
    fun utbetalingEndret(event: UtbetalingEndretEvent) {}
    fun utbetalingUtbetalt(event: UtbetalingUtbetaltEvent) {}
    fun utbetalingUtenUtbetaling(event: UtbetalingUtbetaltEvent) {}
    fun feriepengerUtbetalt(event: FeriepengerUtbetaltEvent) {}
    fun annullering(event: UtbetalingAnnullertEvent) {}
    fun planlagtAnnullering(event: PlanlagtAnnulleringEvent) {}
    fun avsluttetMedVedtak(event: AvsluttetMedVedtakEvent) {}
    fun analytiskDatapakke(event: AnalytiskDatapakkeEvent) {}

    fun behandlingLukket(event: BehandlingLukketEvent) {}
    fun behandlingForkastet(event: BehandlingForkastetEvent) {}
    fun nyBehandling(event: BehandlingOpprettetEvent) {}
    fun avsluttetUtenVedtak(event: AvsluttetUtenVedtakEvent) {}
    fun nyVedtaksperiodeUtbetaling(
        organisasjonsnummer: String,
        utbetalingId: UUID,
        vedtaksperiodeId: UUID
    ) {
    }

    fun overstyringIgangsatt(event: OverstyringIgangsatt) {}
    fun overlappendeInfotrygdperioder(event: OverlappendeInfotrygdperioder) {}
    fun inntektsmeldingFørSøknad(event: InntektsmeldingFørSøknadEvent) {}
    fun inntektsmeldingIkkeHåndtert(inntektsmeldingId: UUID, organisasjonsnummer: String, speilrelatert: Boolean) {}
    fun inntektsmeldingHåndtert(inntektsmeldingId: UUID, vedtaksperiodeId: UUID, organisasjonsnummer: String) {}
    fun skatteinntekterLagtTilGrunn(event: SkatteinntekterLagtTilGrunnEvent) {}
    fun søknadHåndtert(søknadId: UUID, vedtaksperiodeId: UUID, organisasjonsnummer: String) {}
    fun behandlingUtført() {}
    fun vedtaksperiodeAnnullert(vedtaksperiodeAnnullertEvent: VedtaksperiodeAnnullertEvent) {}
    fun utkastTilVedtak(event: UtkastTilVedtakEvent) {}
}
