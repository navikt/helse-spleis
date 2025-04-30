package no.nav.helse.hendelser

import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.helse.hendelser.Avsender.SYSTEM
import no.nav.helse.person.PersonObserver

class Grunnbeløpsregulering(
    meldingsreferanseId: MeldingsreferanseId,
    val skjæringstidspunkt: LocalDate,
    opprettet: LocalDateTime
) : Hendelse, OverstyrInntektsgrunnlag {
    override val behandlingsporing = Behandlingsporing.IngenYrkesaktivitet

    override val metadata = HendelseMetadata(
        meldingsreferanseId = meldingsreferanseId,
        avsender = SYSTEM,
        innsendt = opprettet,
        registrert = LocalDateTime.now(),
        automatiskBehandling = true
    )

    override fun erRelevant(skjæringstidspunkt: LocalDate) =
        this.skjæringstidspunkt == skjæringstidspunkt

    internal fun sykefraværstilfelleIkkeFunnet(observer: PersonObserver) {
        observer.sykefraværstilfelleIkkeFunnet(
            PersonObserver.SykefraværstilfelleIkkeFunnet(
                skjæringstidspunkt = skjæringstidspunkt
            )
        )
    }
}
