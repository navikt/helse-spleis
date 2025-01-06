package no.nav.helse.hendelser

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import no.nav.helse.hendelser.Avsender.ARBEIDSGIVER
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
    data class IkkeUtbetaltArbeidsgiverperiode(val begrunnelse: String): Arbeidsgiveropplysning
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
