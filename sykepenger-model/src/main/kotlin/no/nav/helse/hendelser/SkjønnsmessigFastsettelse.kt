package no.nav.helse.hendelser

import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.helse.person.inntekt.Inntektsdata

class SkjønnsmessigFastsettelse(
    meldingsreferanseId: MeldingsreferanseId,
    private val skjæringstidspunkt: LocalDate,
    val arbeidsgiveropplysninger: List<SkjønnsfastsattInntekt>,
    opprettet: LocalDateTime
) : Hendelse, OverstyrInntektsgrunnlag {
    override val behandlingsporing = Behandlingsporing.IngenYrkesaktivitet
    override val metadata = HendelseMetadata(
        meldingsreferanseId = meldingsreferanseId,
        avsender = Avsender.SAKSBEHANDLER,
        innsendt = opprettet,
        registrert = LocalDateTime.now(),
        automatiskBehandling = false
    )

    override fun erRelevant(skjæringstidspunkt: LocalDate) = this.skjæringstidspunkt == skjæringstidspunkt

    data class SkjønnsfastsattInntekt(
        val orgnummer: String,
        val inntektsdata: Inntektsdata
    )
}
