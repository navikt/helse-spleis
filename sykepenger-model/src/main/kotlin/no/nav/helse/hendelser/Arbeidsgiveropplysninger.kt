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
        init { check(inntekt > Inntekt.INGEN) { "Inntekten må settes til noe mer enn 0." }}
    }
    data class OppgittRefusjon(val beløp: Inntekt, val endringer: List<Refusjonsendring>): Arbeidsgiveropplysning {
        data class Refusjonsendring(val fom: LocalDate, val beløp: Inntekt)
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

    override val behandlingsporing = Behandlingsporing.Arbeidsgiver(organisasjonsnummer)

    override val metadata = HendelseMetadata(
        meldingsreferanseId = meldingsreferanseId,
        avsender = ARBEIDSGIVER,
        innsendt = innsendt,
        registrert = registrert,
        automatiskBehandling = false
    )
}
