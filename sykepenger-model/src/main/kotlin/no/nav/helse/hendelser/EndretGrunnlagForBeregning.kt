package no.nav.helse.hendelser

import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.helse.hendelser.Avsender.SYSTEM

sealed interface Grunnlag {
    data object GraderteAndreYtelser: Grunnlag
    data object Inntektsendringer: Grunnlag
}

class EndretGrunnlagForBeregning(
    private val meldingsreferanseId: MeldingsreferanseId,
    internal val fom: LocalDate,
    internal val endretGrunnlag: Grunnlag,
    private val avsender: Avsender
): Hendelse {
    override val behandlingsporing = Behandlingsporing.IngenYrkesaktivitet

    override val metadata = LocalDateTime.now().let { nå ->
        HendelseMetadata(
            meldingsreferanseId = meldingsreferanseId,
            avsender = avsender,
            innsendt = nå,
            registrert = nå,
            automatiskBehandling = avsender == SYSTEM
        )
    }

    internal fun revurderingseventyr() = when (endretGrunnlag) {
        Grunnlag.GraderteAndreYtelser -> Revurderingseventyr.graderteAndreYtelserEndret(this, fom)
        Grunnlag.Inntektsendringer -> Revurderingseventyr.inntektsendringer(this, fom)
    }
}
