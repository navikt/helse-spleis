package no.nav.helse.hendelser

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Arbeidsgiveropplysning.Companion.valider
import no.nav.helse.hendelser.Avsender.ARBEIDSGIVER
import no.nav.helse.nesteDag
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_8
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN

sealed interface Arbeidsgiveropplysning {
    data class OppgittArbeidgiverperiode(val perioder: List<Periode>) : Arbeidsgiveropplysning {
        init {
            check(perioder.isNotEmpty()) { "Må være minst en periode med agp!" }
        }
    }

    data class OppgittInntekt(val inntekt: Inntekt) : Arbeidsgiveropplysning {
        init {
            check(inntekt >= INGEN) { "Inntekten må være minst 0 kroner!" }
        }
    }

    data object OpphørAvNaturalytelser : Arbeidsgiveropplysning
    data class OppgittRefusjon(val beløp: Inntekt, val endringer: List<Refusjonsendring>) : Arbeidsgiveropplysning {
        data class Refusjonsendring(val fom: LocalDate, val beløp: Inntekt)
    }

    data class IkkeUtbetaltArbeidsgiverperiode(private val begrunnelse: Begrunnelse) : Arbeidsgiveropplysning {
        internal fun valider(aktivitetslogg: IAktivitetslogg) = begrunnelse.valider(aktivitetslogg)
    }

    data class RedusertUtbetaltBeløpIArbeidsgiverperioden(private val begrunnelse: Begrunnelse) : Arbeidsgiveropplysning {
        internal fun valider(aktivitetslogg: IAktivitetslogg) = begrunnelse.valider(aktivitetslogg)
    }

    data class UtbetaltDelerAvArbeidsgiverperioden(private val begrunnelse: Begrunnelse, val utbetaltTilOgMed: LocalDate) : Arbeidsgiveropplysning {
        internal fun valider(aktivitetslogg: IAktivitetslogg) = begrunnelse.valider(aktivitetslogg)
    }

    data object IkkeNyArbeidsgiverperiode : Arbeidsgiveropplysning

    enum class Begrunnelse(private val støttes: Boolean) {
        LovligFravaer(støttes = true),
        ArbeidOpphoert(støttes = true),
        ManglerOpptjening(støttes = true),
        IkkeFravaer(støttes = true),
        Permittering(støttes = true),
        Saerregler(støttes = true),
        IkkeFullStillingsandel(støttes = true),
        TidligereVirksomhet(støttes = true),

        // Dette er potensielt andre opplysningstyper om det faktisk skal støttes
        // Men så lenge det ikke støttes legger vi inn et ørlite hack her for å gi dem error
        BetvilerArbeidsufoerhet(støttes = false),
        FiskerMedHyre(støttes = false),
        StreikEllerLockout(støttes = false),
        FravaerUtenGyldigGrunn(støttes = false),
        BeskjedGittForSent(støttes = false),
        IkkeLoenn(støttes = false);

        internal fun valider(aktivitetslogg: IAktivitetslogg) {
            aktivitetslogg.info("Arbeidsgiver har redusert utbetaling av arbeidsgiverperioden på grunn av: $name")
            if (støttes) return aktivitetslogg.varsel(RV_IM_8)
            aktivitetslogg.funksjonellFeil(RV_IM_8)
        }
    }

    companion object {
        fun List<Arbeidsgiveropplysning>.valider() {
            check(isNotEmpty()) { "Ingen opplysninger? Hva skal det bety?" }

            val opplysningerFraBegrunnelse =
                filterIsInstance<RedusertUtbetaltBeløpIArbeidsgiverperioden>() +
                    filterIsInstance<UtbetaltDelerAvArbeidsgiverperioden>() +
                    filterIsInstance<IkkeUtbetaltArbeidsgiverperiode>() +
                    filterIsInstance<IkkeNyArbeidsgiverperiode>()

            check(opplysningerFraBegrunnelse.size <= 1) {
                "Det kan maks oppgis én opplysning relatert til begrunnelse. Fant ${opplysningerFraBegrunnelse.size}: ${map { it::class.simpleName }.joinToString()}"
            }

            val arbeidsgiverperiode = this
                .filterIsInstance<OppgittArbeidgiverperiode>()
                .singleOrNull()?.perioder ?: emptyList()

            val antallDager = arbeidsgiverperiode.flatten().size

            when (val opplysning = opplysningerFraBegrunnelse.singleOrNull()) {
                is IkkeUtbetaltArbeidsgiverperiode -> check(antallDager == 0) {
                    "IkkeUtbetaltArbeidsgiverperiode kan ikke kombineres med arbeidsgiverperiode på $antallDager dager: $arbeidsgiverperiode. Kan ikke være oppgitt noen arbeidsgiverperiode med denne opplysningstypen."
                }

                is UtbetaltDelerAvArbeidsgiverperioden -> {
                    check(antallDager in 1..15) {
                        "UtbetaltDelerAvArbeidsgiverperioden kan ikke kombineres med arbeidsgiverperiode på $antallDager dager: $arbeidsgiverperiode. Må være oppgitt mellom 1 og 15 dager med arbeidsgiverperiode med denne opplysningstypen."
                    }
                    val sisteDagIOppgittArbeidsgiverperiode = arbeidsgiverperiode.last().endInclusive
                    check(opplysning.utbetaltTilOgMed == sisteDagIOppgittArbeidsgiverperiode) {
                        "UtbetaltDelerAvArbeidsgiverperioden må ha utbetaltTilOgMed satt til siste dag i arbeidsgiverperioden ($sisteDagIOppgittArbeidsgiverperiode), men var ${opplysning.utbetaltTilOgMed}"
                    }
                }

                is RedusertUtbetaltBeløpIArbeidsgiverperioden -> check(antallDager == 16) {
                    "RedusertUtbetaltBeløpIArbeidsgiverperioden kan ikke kombineres med arbeidsgiverperiode på $antallDager dager: $arbeidsgiverperiode. Må være oppgitt en full arbeidsigverperiode på 16 dager med denne opplysningstypen."
                }

                else -> {}
            }
        }

        fun fraInntektsmelding(
            beregnetInntekt: Inntekt?,
            refusjon: Inntektsmelding.Refusjon?,
            arbeidsgiverperioder: List<Periode>?,
            begrunnelseForReduksjonEllerIkkeUtbetalt: String?,
            opphørAvNaturalytelser: List<Inntektsmelding.OpphørAvNaturalytelse>
        ): List<Arbeidsgiveropplysning> {
            val oppgittInntekt = beregnetInntekt
                ?.takeUnless { it < INGEN }
                ?.let { OppgittInntekt(it) }

            val oppgittArbeidsgiverperiode = arbeidsgiverperioder
                ?.takeUnless { it.isEmpty() }
                ?.let { OppgittArbeidgiverperiode(it) }

            val oppgittOpphørAvNaturalytelser = OpphørAvNaturalytelser
                .takeIf { opphørAvNaturalytelser.isNotEmpty() }

            return listOfNotNull(
                oppgittInntekt,
                oppgittArbeidsgiverperiode,
                refusjon?.somOppgittRefusjon(),
                begrunnelseForReduksjonEllerIkkeUtbetalt?.somArbeidsgiveropplysning(arbeidsgiverperioder ?: emptyList()),
                oppgittOpphørAvNaturalytelser
            )
        }

        private fun Inntektsmelding.Refusjon.somOppgittRefusjon(): OppgittRefusjon {
            val endringerIRefusjon = endringerIRefusjon.map { OppgittRefusjon.Refusjonsendring(it.endringsdato, it.beløp) }
            val opphørAvRefusjon = listOfNotNull(opphørsdato?.let { OppgittRefusjon.Refusjonsendring(it.nesteDag, INGEN) })
            return OppgittRefusjon(beløp ?: INGEN, endringerIRefusjon + opphørAvRefusjon)
        }

        private fun String.somArbeidsgiveropplysning(arbeidsgiverperioder: List<Periode>): Arbeidsgiveropplysning? {
            if (this.isBlank()) return null
            if (this == "FerieEllerAvspasering") {
                return IkkeNyArbeidsgiverperiode
            }
            val begrunnelse = Arbeidsgiveropplysning.Begrunnelse.valueOf(this)
            val antallDager = arbeidsgiverperioder.flatten().size
            return when (antallDager) {
                0 -> IkkeUtbetaltArbeidsgiverperiode(begrunnelse)
                in 1..15 -> UtbetaltDelerAvArbeidsgiverperioden(begrunnelse, utbetaltTilOgMed = arbeidsgiverperioder.last().endInclusive)
                16 -> RedusertUtbetaltBeløpIArbeidsgiverperioden(begrunnelse)
                else -> error("Forventer ikke arbeidsgierperioder på $antallDager dager: $arbeidsgiverperioder")
            }
        }
    }
}

class Arbeidsgiveropplysninger(
    meldingsreferanseId: MeldingsreferanseId,
    innsendt: LocalDateTime,
    registrert: LocalDateTime,
    override val behandlingsporing: Behandlingsporing.Yrkesaktivitet,
    val vedtaksperiodeId: UUID,
    val opplysninger: List<Arbeidsgiveropplysning>
) : Collection<Arbeidsgiveropplysning> by opplysninger, Hendelse {

    init {
        opplysninger.valider()
    }

    override val metadata = HendelseMetadata(
        meldingsreferanseId = meldingsreferanseId,
        avsender = ARBEIDSGIVER,
        innsendt = innsendt,
        registrert = registrert,
        automatiskBehandling = false
    )
}

class KorrigerteArbeidsgiveropplysninger(
    meldingsreferanseId: MeldingsreferanseId,
    innsendt: LocalDateTime,
    registrert: LocalDateTime,
    override val behandlingsporing: Behandlingsporing.Yrkesaktivitet.Arbeidstaker,
    val vedtaksperiodeId: UUID,
    val opplysninger: List<Arbeidsgiveropplysning>
) : Collection<Arbeidsgiveropplysning> by opplysninger, Hendelse {

    init {
        opplysninger.valider()
    }

    override val metadata = HendelseMetadata(
        meldingsreferanseId = meldingsreferanseId,
        avsender = ARBEIDSGIVER,
        innsendt = innsendt,
        registrert = registrert,
        automatiskBehandling = false
    )
}
