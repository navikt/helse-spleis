package no.nav.helse.hendelser

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import no.nav.helse.person.inntekt.Inntektsdata

class SkjønnsmessigFastsettelse(
    meldingsreferanseId: UUID,
    private val skjæringstidspunkt: LocalDate,
    val arbeidsgiveropplysninger: List<SkjønnsfastsattInntekt>,
    opprettet: LocalDateTime
) : Hendelse, OverstyrInntektsgrunnlag {
    override val behandlingsporing = Behandlingsporing.IngenArbeidsgiver
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
