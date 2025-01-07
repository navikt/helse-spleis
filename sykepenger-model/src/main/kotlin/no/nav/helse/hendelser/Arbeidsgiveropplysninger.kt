package no.nav.helse.hendelser

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import no.nav.helse.hendelser.Avsender.ARBEIDSGIVER
import no.nav.helse.nesteDag
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_8
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN

sealed interface Arbeidsgiveropplysning {
    data class OppgittArbeidgiverperiode(val perioder: List<Periode>): Arbeidsgiveropplysning {
        init { check(perioder.isNotEmpty()) { "Må være minst en periode med agp!" }}
    }
    data class OppgittInntekt(val inntekt: Inntekt): Arbeidsgiveropplysning {
        init { check(inntekt >= INGEN) { "Inntekten må være minst 0 kroner!" }}
    }
    data class OppgittRefusjon(val beløp: Inntekt, val endringer: List<Refusjonsendring>): Arbeidsgiveropplysning {
        data class Refusjonsendring(val fom: LocalDate, val beløp: Inntekt)
    }
    data class IkkeUtbetaltArbeidsgiverperiode(private val begrunnelse: Begrunnelse): Arbeidsgiveropplysning {
        internal fun valider(aktivitetslogg: IAktivitetslogg) = begrunnelse.valider(aktivitetslogg)
    }
    data class RedusertUtbetaltBeløpIArbeidsgiverperioden(private val begrunnelse: Begrunnelse): Arbeidsgiveropplysning {
        internal fun valider(aktivitetslogg: IAktivitetslogg) = begrunnelse.valider(aktivitetslogg)
    }
    data class UtbetaltDelerAvArbeidsgiverperioden(private val begrunnelse: Begrunnelse, val utbetaltTilOgMed: LocalDate): Arbeidsgiveropplysning {
        internal fun valider(aktivitetslogg: IAktivitetslogg) = begrunnelse.valider(aktivitetslogg)
    }
    data object IkkeNyArbeidsgiverperiode: Arbeidsgiveropplysning

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
}

class Arbeidsgiveropplysninger(
    meldingsreferanseId: UUID,
    innsendt: LocalDateTime,
    registrert: LocalDateTime,
    organisasjonsnummer: String,
    val vedtaksperiodeId: UUID,
    val opplysninger: List<Arbeidsgiveropplysning>
) : Collection<Arbeidsgiveropplysning> by opplysninger, Hendelse {

    init {
        val opplysningerFraBegrunnelse =
            opplysninger.filterIsInstance<Arbeidsgiveropplysning.RedusertUtbetaltBeløpIArbeidsgiverperioden>() +
            opplysninger.filterIsInstance<Arbeidsgiveropplysning.UtbetaltDelerAvArbeidsgiverperioden>() +
            opplysninger.filterIsInstance<Arbeidsgiveropplysning.IkkeUtbetaltArbeidsgiverperiode>() +
            opplysninger.filterIsInstance<Arbeidsgiveropplysning.IkkeNyArbeidsgiverperiode>()

        check(opplysningerFraBegrunnelse.size <= 1) {
            "Disse arbeidsgiveropplysningene kan ikke kombineres så lenge vi får svar på forsespørsler forkledd som en inntektsmelding."
        }

        val antallDager = (opplysninger
            .filterIsInstance<Arbeidsgiveropplysning.OppgittArbeidgiverperiode>()
            .singleOrNull()?.perioder?.flatten() ?: emptyList())
            .size

        when (opplysningerFraBegrunnelse.singleOrNull()) {
            is Arbeidsgiveropplysning.IkkeUtbetaltArbeidsgiverperiode -> check(antallDager == 0) {
                "Disse arbeidsgiveropplysningene kan ikke kombineres så lenge vi får svar på forsespørsler forkledd som en inntektsmelding."
            }
            is Arbeidsgiveropplysning.UtbetaltDelerAvArbeidsgiverperioden -> check(antallDager in 1 .. 15) {
                "Disse arbeidsgiveropplysningene kan ikke kombineres så lenge vi får svar på forsespørsler forkledd som en inntektsmelding."
            }
            is Arbeidsgiveropplysning.RedusertUtbetaltBeløpIArbeidsgiverperioden -> check(antallDager == 16) {
                "Disse arbeidsgiveropplysningene kan ikke kombineres så lenge vi får svar på forsespørsler forkledd som en inntektsmelding."
            }
            else -> {}
        }
    }

    override val behandlingsporing = Behandlingsporing.Arbeidsgiver(organisasjonsnummer)

    override val metadata = HendelseMetadata(
        meldingsreferanseId = meldingsreferanseId,
        avsender = ARBEIDSGIVER,
        innsendt = innsendt,
        registrert = registrert,
        automatiskBehandling = false
    )

    companion object {
        private fun Inntektsmelding.Refusjon.somOppgittRefusjon(): Arbeidsgiveropplysning.OppgittRefusjon {
            val endringerIRefusjon = endringerIRefusjon.map { Arbeidsgiveropplysning.OppgittRefusjon.Refusjonsendring(it.endringsdato, it.beløp) }
            val opphørAvRefusjon = listOfNotNull(opphørsdato?.let { Arbeidsgiveropplysning.OppgittRefusjon.Refusjonsendring(it.nesteDag, INGEN) })
            return Arbeidsgiveropplysning.OppgittRefusjon(beløp ?: INGEN, endringerIRefusjon + opphørAvRefusjon)
        }

        private fun String.somArbeidsgiveropplysning(arbeidsgiverperioder: List<Periode>): Arbeidsgiveropplysning? {
            if (this.isBlank()) return null
            if (this == "FerieEllerAvspasering") {
                return Arbeidsgiveropplysning.IkkeNyArbeidsgiverperiode
            }
            val begrunnelse = Arbeidsgiveropplysning.Begrunnelse.valueOf(this)
            val antallDager = arbeidsgiverperioder.flatten().size
            return when (antallDager) {
                0 -> Arbeidsgiveropplysning.IkkeUtbetaltArbeidsgiverperiode(begrunnelse)
                in 1 .. 15 -> Arbeidsgiveropplysning.UtbetaltDelerAvArbeidsgiverperioden(begrunnelse, utbetaltTilOgMed = arbeidsgiverperioder.last().endInclusive)
                16 -> Arbeidsgiveropplysning.RedusertUtbetaltBeløpIArbeidsgiverperioden(begrunnelse)
                else -> error("Forventer ikke arbeidsgierperioder på $antallDager dager: $arbeidsgiverperioder")
            }
        }

        fun fraInntektsmelding(
            meldingsreferanseId: UUID,
            innsendt: LocalDateTime,
            registrert: LocalDateTime,
            organisasjonsnummer: String,
            vedtaksperiodeId: UUID,
            beregnetInntekt: Inntekt?,
            refusjon: Inntektsmelding.Refusjon?,
            arbeidsgiverperioder: List<Periode>?,
            begrunnelseForReduksjonEllerIkkeUtbetalt: String?,
        ): Arbeidsgiveropplysninger {
            val oppgittInntekt = beregnetInntekt
                ?.takeUnless { it < INGEN }
                ?.let { Arbeidsgiveropplysning.OppgittInntekt(it) }

            val oppgittArbeidsgiverperiode = arbeidsgiverperioder
                ?.takeUnless { it.isEmpty() }
                ?.let { Arbeidsgiveropplysning.OppgittArbeidgiverperiode(it) }

            val opplysninger = listOfNotNull(
                oppgittInntekt,
                oppgittArbeidsgiverperiode,
                refusjon?.somOppgittRefusjon(),
                begrunnelseForReduksjonEllerIkkeUtbetalt?.somArbeidsgiveropplysning(arbeidsgiverperioder ?: emptyList())
            )

            return Arbeidsgiveropplysninger(
                meldingsreferanseId = meldingsreferanseId,
                innsendt = innsendt,
                registrert = registrert,
                organisasjonsnummer = organisasjonsnummer,
                vedtaksperiodeId = vedtaksperiodeId,
                opplysninger = opplysninger
            )
        }
    }
}
