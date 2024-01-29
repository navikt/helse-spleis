package no.nav.helse.person

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.Personidentifikator
import no.nav.helse.hendelser.Avsender
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.hendelser.SimuleringResultat
import no.nav.helse.person.PersonObserver.ForespurtOpplysning.Companion.toJsonMap
import no.nav.helse.person.infotrygdhistorikk.Friperiode
import no.nav.helse.person.infotrygdhistorikk.Utbetalingsperiode
import no.nav.helse.person.inntekt.Refusjonsopplysning
import no.nav.helse.utbetalingslinjer.Endringskode
import no.nav.helse.utbetalingslinjer.Fagområde
import no.nav.helse.utbetalingslinjer.Klassekode
import no.nav.helse.utbetalingslinjer.Oppdrag
import no.nav.helse.utbetalingslinjer.OppdragVisitor
import no.nav.helse.utbetalingslinjer.Oppdragstatus
import no.nav.helse.utbetalingslinjer.Satstype
import no.nav.helse.utbetalingslinjer.Utbetalingslinje
import no.nav.helse.utbetalingstidslinje.Begrunnelse
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
        val harPeriodeInnenfor16Dager: Boolean,
        val trengerArbeidsgiveropplysninger: Boolean,
        val sykmeldingsperioder: List<Periode>
    ) {
        fun toJsonMap(): Map<String, Any> =
            mapOf(
                "organisasjonsnummer" to organisasjonsnummer,
                "vedtaksperiodeId" to vedtaksperiodeId,
                "tilstand" to gjeldendeTilstand,
                "hendelser" to hendelser,
                "fom" to fom,
                "tom" to tom,
                "forlengerPeriode" to forlengerPeriode,
                "harPeriodeInnenfor16Dager" to harPeriodeInnenfor16Dager,
                "trengerArbeidsgiveropplysninger" to trengerArbeidsgiveropplysninger,
                "sykmeldingsperioder" to sykmeldingsperioder.map {
                    mapOf(
                        "fom" to it.start,
                        "tom" to it.endInclusive
                    )
                }
            )
    }
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

    data class TrengerArbeidsgiveropplysningerEvent(
        val organisasjonsnummer: String,
        val vedtaksperiodeId: UUID,
        val skjæringstidspunkt: LocalDate,
        val sykmeldingsperioder: List<Periode>,
        val egenmeldingsperioder: List<Periode>,
        val førsteFraværsdager: List<Map<String, Any>>,
        val forespurteOpplysninger: List<ForespurtOpplysning>
    ) {
        fun toJsonMap(): Map<String, Any> =
            mapOf(
                "organisasjonsnummer" to organisasjonsnummer,
                "vedtaksperiodeId" to vedtaksperiodeId,
                "skjæringstidspunkt" to skjæringstidspunkt,
                "sykmeldingsperioder" to sykmeldingsperioder.map {
                    mapOf(
                        "fom" to it.start,
                        "tom" to it.endInclusive
                    )
                },
                "egenmeldingsperioder" to egenmeldingsperioder.map {
                    mapOf(
                        "fom" to it.start,
                        "tom" to it.endInclusive
                    )
                },
                "førsteFraværsdager" to førsteFraværsdager,
                "forespurteOpplysninger" to forespurteOpplysninger.toJsonMap()
            )
    }

    class TrengerIkkeArbeidsgiveropplysningerEvent(
        val organisasjonsnummer: String,
        val vedtaksperiodeId: UUID
    ) {
        fun toJsonMap(): Map<String, Any> =
            mapOf(
                "organisasjonsnummer" to organisasjonsnummer,
                "vedtaksperiodeId" to vedtaksperiodeId
            )
    }

    data class ArbeidsgiveropplysningerKorrigertEvent(
        val korrigertInntektsmeldingId: UUID,
        val korrigerendeInntektsopplysningId: UUID,
        val korrigerendeInntektektsopplysningstype: Inntektsopplysningstype
    ) {
        fun toJsonMap(): Map<String, Any> =
            mapOf(
                "korrigertInntektsmeldingId" to korrigertInntektsmeldingId,
                "korrigerendeInntektsopplysningId" to korrigerendeInntektsopplysningId,
                "korrigerendeInntektektsopplysningstype" to korrigerendeInntektektsopplysningstype
            )
    }

    sealed class ForespurtOpplysning {

        companion object {
            fun List<ForespurtOpplysning>.toJsonMap() = map { forespurtOpplysning ->
                when (forespurtOpplysning) {
                    is Arbeidsgiverperiode -> mapOf(
                        "opplysningstype" to "Arbeidsgiverperiode"
                    )

                    is Inntekt -> mapOf(
                        "opplysningstype" to "Inntekt",
                        "forslag" to mapOf(
                            "forrigeInntekt" to forespurtOpplysning.forslag?.let {
                                mapOf(
                                    "skjæringstidspunkt" to it.skjæringstidspunkt,
                                    "kilde" to it.kilde.name,
                                    "beløp" to it.beløp
                                )
                            }
                        )
                    )

                    is FastsattInntekt -> mapOf(
                        "opplysningstype" to "FastsattInntekt",
                        "fastsattInntekt" to forespurtOpplysning.fastsattInntekt.reflection { _, månedlig, _, _ -> månedlig }
                    )

                    is Refusjon -> mapOf(
                        "opplysningstype" to "Refusjon",
                        "forslag" to forespurtOpplysning.forslag.map { forslag ->
                            mapOf(
                                "fom" to forslag.fom(),
                                "tom" to forslag.tom(),
                                "beløp" to forslag.beløp().reflection {_, månedlig, _, _ -> månedlig}
                            )
                        }
                    )
                }
            }
        }
    }

    data class Inntektsdata(val skjæringstidspunkt: LocalDate, val kilde: Inntektsopplysningstype, val beløp: Double)
    enum class Inntektsopplysningstype{
        INNTEKTSMELDING,
        SAKSBEHANDLER
    }
    data class Inntekt(val forslag: Inntektsdata?) : ForespurtOpplysning()
    data class FastsattInntekt(val fastsattInntekt: no.nav.helse.økonomi.Inntekt) : ForespurtOpplysning()
    object Arbeidsgiverperiode : ForespurtOpplysning()
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
    )
    data class UtbetalingEndretEvent(
        val organisasjonsnummer: String,
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
                    fagsystemId = oppdrag.fagsystemId(),
                    mottaker = oppdrag.mottaker(),
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
        val arbeidsgiverOppdrag: OppdragEventDetaljer,
        val personOppdrag: OppdragEventDetaljer,
        val utbetalingsdager: List<Utbetalingsdag>,
        val vedtaksperiodeIder: List<UUID>,
        val ident: String
    ) {
        init {
            check(vedtaksperiodeIder.size <= 1) {
                "En utbetaling-melding kan ikke høre til flere enn én vedtaksperiode"
            }
        }
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
                fun mapOppdrag(oppdrag: Oppdrag): OppdragEventDetaljer {
                    val builder = Builder()
                    oppdrag.accept(builder)
                    return builder.build()
                }
            }
            private class Builder : OppdragVisitor {
                private lateinit var dto: OppdragEventDetaljer
                private val linjene = mutableListOf<OppdragEventLinjeDetaljer>()

                fun build() = dto

                override fun visitUtbetalingslinje(
                    linje: Utbetalingslinje,
                    fom: LocalDate,
                    tom: LocalDate,
                    stønadsdager: Int,
                    totalbeløp: Int,
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
                    linjene.add(OppdragEventLinjeDetaljer(fom, tom, beløp ?: error("mangler beløp for linje"), grad?.toDouble() ?: error("mangler grad for linje"), stønadsdager, totalbeløp, statuskode))
                }

                override fun postVisitOppdrag(
                    oppdrag: Oppdrag,
                    fagområde: Fagområde,
                    fagsystemId: String,
                    mottaker: String,
                    stønadsdager: Int,
                    totalBeløp: Int,
                    nettoBeløp: Int,
                    tidsstempel: LocalDateTime,
                    endringskode: Endringskode,
                    avstemmingsnøkkel: Long?,
                    status: Oppdragstatus?,
                    overføringstidspunkt: LocalDateTime?,
                    erSimulert: Boolean,
                    simuleringsResultat: SimuleringResultat?
                ) {
                    dto = OppdragEventDetaljer(fagsystemId, fagområde.verdi, mottaker, nettoBeløp, stønadsdager, linjene.firstOrNull()?.fom ?: LocalDate.MIN, linjene.lastOrNull()?.tom ?: LocalDate.MIN, linjene)
                }
            }
        }
    }

    data class Utbetalingsdag(
        val dato: LocalDate,
        val type: Dagtype,
        val begrunnelser: List<EksternBegrunnelseDTO>? = null
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
            Feriedag,
            ArbeidIkkeGjenopptattDag
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
        val organisasjonsnummer: String,
        val arbeidsgiverOppdrag: OppdragEventDetaljer,
        val personOppdrag: OppdragEventDetaljer
    ) {
        data class OppdragEventDetaljer(
            val fagsystemId: String,
            val mottaker: String,
            val fom: LocalDate,
            val tom: LocalDate,
            val linjer: List<OppdragEventLinjeDetaljer>
        ) {
            data class OppdragEventLinjeDetaljer(
                val fom: LocalDate,
                val tom: LocalDate,
                val totalbeløp: Int
            )

            companion object {
                fun mapOppdrag(oppdrag: Oppdrag): OppdragEventDetaljer {
                    val builder = Builder()
                    oppdrag.accept(builder)
                    return builder.build()
                }
            }
            private class Builder : OppdragVisitor {
                private lateinit var dto: OppdragEventDetaljer
                private val linjene = mutableListOf<OppdragEventLinjeDetaljer>()

                fun build() = dto

                override fun visitUtbetalingslinje(
                    linje: Utbetalingslinje,
                    fom: LocalDate,
                    tom: LocalDate,
                    stønadsdager: Int,
                    totalbeløp: Int,
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
                    linjene.add(OppdragEventLinjeDetaljer(fom, tom, totalbeløp))
                }

                override fun postVisitOppdrag(
                    oppdrag: Oppdrag,
                    fagområde: Fagområde,
                    fagsystemId: String,
                    mottaker: String,
                    stønadsdager: Int,
                    totalBeløp: Int,
                    nettoBeløp: Int,
                    tidsstempel: LocalDateTime,
                    endringskode: Endringskode,
                    avstemmingsnøkkel: Long?,
                    status: Oppdragstatus?,
                    overføringstidspunkt: LocalDateTime?,
                    erSimulert: Boolean,
                    simuleringsResultat: SimuleringResultat?
                ) {
                    dto = OppdragEventDetaljer(fagsystemId, mottaker, linjene.firstOrNull()?.fom ?: LocalDate.MIN, linjene.lastOrNull()?.tom ?: LocalDate.MIN, linjene)
                }
            }
        }
    }

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

    data class AvsluttetUtenVedtakEvent(
        val fødselsnummer: String,
        val aktørId: String,
        val organisasjonsnummer: String,
        val vedtaksperiodeId: UUID,
        val generasjonId: UUID,
        val periode: Periode,
        val hendelseIder: Set<UUID>,
        val skjæringstidspunkt: LocalDate,
        val avsluttetTidspunkt: LocalDateTime
    )

    data class GenerasjonLukketEvent(
        val fødselsnummer: String,
        val aktørId: String,
        val organisasjonsnummer: String,
        val vedtaksperiodeId: UUID,
        val generasjonId: UUID
    )

    data class GenerasjonForkastetEvent(
        val fødselsnummer: String,
        val aktørId: String,
        val organisasjonsnummer: String,
        val vedtaksperiodeId: UUID,
        val generasjonId: UUID
    )

    data class GenerasjonOpprettetEvent(
        val fødselsnummer: String,
        val aktørId: String,
        val organisasjonsnummer: String,
        val vedtaksperiodeId: UUID,
        val generasjonId: UUID,
        val type: Type,
        val kilde: Kilde
    ) {
        enum class Type {
            Førstegangsbehandling,
            TilInfotrygd,
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

    data class AvsluttetMedVedtakEvent(
        val fødselsnummer: String,
        val aktørId: String,
        val organisasjonsnummer: String,
        val vedtaksperiodeId: UUID,
        val generasjonId: UUID,
        val periode: Periode,
        val hendelseIder: Set<UUID>,
        val skjæringstidspunkt: LocalDate,
        val sykepengegrunnlag: Double,
        val beregningsgrunnlag: Double,
        val omregnetÅrsinntektPerArbeidsgiver: Map<String, Double>,
        val inntekt: Double,
        val utbetalingId: UUID?,
        val sykepengegrunnlagsbegrensning: String,
        val vedtakFattetTidspunkt: LocalDateTime,
        val sykepengegrunnlagsfakta: Sykepengegrunnlagsfakta?,
        val tags: Set<Tag>
    ) {
        enum class Fastsatt {
            EtterHovedregel,
            EtterSkjønn,
            IInfotrygd
        }

        enum class Tag {
            `6GBegrenset`,
            IngenNyArbeidsgiverperiode,
            SykepengegrunnlagUnder2G
        }

        sealed class Sykepengegrunnlagsfakta {
            abstract val fastsatt: Fastsatt
            abstract val omregnetÅrsinntekt: Double
        }
        data class FastsattIInfotrygd(override val omregnetÅrsinntekt: Double) : Sykepengegrunnlagsfakta() {
            override val fastsatt = Fastsatt.IInfotrygd
        }
        data class FastsattISpeil(
            override val omregnetÅrsinntekt: Double,
            val `6G`: Double,
            val tags: Set<Tag>,
            val arbeidsgivere: List<Arbeidsgiver>
        ) : Sykepengegrunnlagsfakta() {
            val skjønnsfastsatt: Double? = arbeidsgivere.mapNotNull { it.skjønnsfastsatt }.takeIf(List<*>::isNotEmpty)?.sum()
            override val fastsatt = if (skjønnsfastsatt == null) Fastsatt.EtterHovedregel else Fastsatt.EtterSkjønn
            data class Arbeidsgiver(val arbeidsgiver: String, val omregnetÅrsinntekt: Double, val skjønnsfastsatt: Double?)
        }
    }

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

    data class VedtaksperiodeAnnullertEvent (
        val fom: LocalDate,
        val tom: LocalDate,
        val vedtaksperiodeId: UUID,
        val organisasjonsnummer: String
    )

    fun inntektsmeldingReplay(personidentifikator: Personidentifikator, aktørId: String, organisasjonsnummer: String, vedtaksperiodeId: UUID, skjæringstidspunkt: LocalDate, sammenhengendePeriode: Periode) {}
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
    fun trengerIkkeArbeidsgiveropplysninger(event: TrengerIkkeArbeidsgiveropplysningerEvent) {}
    fun arbeidsgiveropplysningerKorrigert(event: ArbeidsgiveropplysningerKorrigertEvent) {}
    fun utbetalingEndret(event: UtbetalingEndretEvent) {}
    fun utbetalingUtbetalt(event: UtbetalingUtbetaltEvent) {}
    fun utbetalingUtenUtbetaling(event: UtbetalingUtbetaltEvent) {}
    fun feriepengerUtbetalt(event: FeriepengerUtbetaltEvent) {}
    fun annullering(event: UtbetalingAnnullertEvent) {}
    fun avstemt(result: Map<String, Any>) {}
    fun avsluttetMedVedtak(event: AvsluttetMedVedtakEvent) {}

    fun generasjonLukket(event: GenerasjonLukketEvent) {}
    fun generasjonForkastet(event: GenerasjonForkastetEvent) {}
    fun nyGenerasjon(event: GenerasjonOpprettetEvent) {}
    fun avsluttetUtenVedtak(event: AvsluttetUtenVedtakEvent) {}
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
    fun inntektsmeldingIkkeHåndtert(inntektsmeldingId: UUID, organisasjonsnummer: String, harPeriodeInnenfor16Dager: Boolean) {}
    fun inntektsmeldingHåndtert(inntektsmeldingId: UUID, vedtaksperiodeId: UUID, organisasjonsnummer: String) {}
    fun søknadHåndtert(søknadId: UUID, vedtaksperiodeId: UUID, organisasjonsnummer: String) {}
    fun behandlingUtført() {}
    fun vedtaksperiodeAnnullert(vedtaksperiodeAnnullertEvent: VedtaksperiodeAnnullertEvent) {}
}
