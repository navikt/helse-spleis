package no.nav.helse.hendelser

import no.nav.helse.person.Sykmeldingsperioder
import java.time.LocalDateTime
import java.util.UUID

class AvbruttSøknad(
    private val periode: Periode,
    meldingsreferanseId: UUID,
    orgnummer: String
) : Hendelse {
    override val behandlingsporing =
        Behandlingsporing.Arbeidsgiver(
            organisasjonsnummer = orgnummer
        )
    override val metadata =
        LocalDateTime.now().let { nå ->
            HendelseMetadata(
                meldingsreferanseId = meldingsreferanseId,
                avsender = Avsender.SYKMELDT,
                innsendt = nå,
                registrert = nå,
                automatiskBehandling = false
            )
        }

    internal fun avbryt(sykmeldingsperioder: Sykmeldingsperioder) {
        sykmeldingsperioder.fjern(periode)
    }
}
