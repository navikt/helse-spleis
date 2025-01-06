package no.nav.helse.hendelser

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import no.nav.helse.hendelser.Avsender.ARBEIDSGIVER
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_8
import no.nav.helse.økonomi.Inntekt

sealed interface Arbeidsgiveropplysning {
    data class OppgittArbeidgiverperiode(val perioder: List<Periode>): Arbeidsgiveropplysning {
        init { check(perioder.isNotEmpty()) { "Må være minst en periode med agp!" }}
    }
    data class OppgittInntekt(val inntekt: Inntekt): Arbeidsgiveropplysning {
        init { check(inntekt >= Inntekt.INGEN) { "Inntekten må være minst 0 kroner!" }}
    }
    data class OppgittRefusjon(val beløp: Inntekt, val endringer: List<Refusjonsendring>): Arbeidsgiveropplysning {
        data class Refusjonsendring(val fom: LocalDate, val beløp: Inntekt)
    }
    data class IkkeUtbetaltArbeidsgiverperiode(private val begrunnelse: Begrunnelse): Arbeidsgiveropplysning {

        internal fun valider(aktivitetslogg: IAktivitetslogg) {
            aktivitetslogg.info("Arbeidsgiver har redusert utbetaling av arbeidsgiverperioden på grunn av: ${begrunnelse.name}")
            if (begrunnelse.støttes) return aktivitetslogg.varsel(RV_IM_8)
            aktivitetslogg.funksjonellFeil(RV_IM_8)
        }

        enum class Begrunnelse(internal val støttes: Boolean) {
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
            IkkeLoenn(støttes = false)
        }
    }
    data object IkkeNyArbeidsgiverperiode: Arbeidsgiveropplysning
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
        val opplysningstyperSomIkkeKanSendesSammen =
            opplysninger.filterIsInstance<Arbeidsgiveropplysning.OppgittArbeidgiverperiode>() +
            opplysninger.filterIsInstance<Arbeidsgiveropplysning.IkkeUtbetaltArbeidsgiverperiode>() +
            opplysninger.filterIsInstance<Arbeidsgiveropplysning.IkkeNyArbeidsgiverperiode>()

        check(opplysningstyperSomIkkeKanSendesSammen.size <= 1) {
            "Disse arbeidsgiveropplysningene kan ikke kombineres så lenge vi får svar på forsespørsler forkledd som en inntektsmelding."
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
}
