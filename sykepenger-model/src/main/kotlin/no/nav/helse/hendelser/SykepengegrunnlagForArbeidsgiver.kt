package no.nav.helse.hendelser

import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.helse.hendelser.Avsender.SYSTEM
import no.nav.helse.person.inntekt.Skatteopplysning

class SykepengegrunnlagForArbeidsgiver(
    meldingsreferanseId: MeldingsreferanseId,
    internal val skjæringstidspunkt: LocalDate,
    orgnummer: String,
    private val inntekter: ArbeidsgiverInntekt
) : Hendelse {
    override val behandlingsporing = Behandlingsporing.Arbeidsgiver(
        organisasjonsnummer = orgnummer
    )
    override val metadata = LocalDateTime.now().let { nå ->
        HendelseMetadata(
            meldingsreferanseId = meldingsreferanseId,
            avsender = SYSTEM,
            innsendt = nå,
            registrert = nå,
            automatiskBehandling = true
        )
    }

    internal fun inntekter(): List<Skatteopplysning> {
        return inntekter.inntekter.map { it.somInntekt(metadata.meldingsreferanseId) }
    }
}
