package no.nav.helse.hendelser

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Avsender.SYSTEM
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.inntekt.Inntektshistorikk
import no.nav.helse.person.inntekt.Refusjonshistorikk
import no.nav.helse.økonomi.Inntekt.Companion.INGEN

class SykepengegrunnlagForArbeidsgiver(
    meldingsreferanseId: UUID,
    private val vedtaksperiodeId: UUID,
    private val skjæringstidspunkt: LocalDate,
    private val orgnummer: String,
    private val inntekter: ArbeidsgiverInntekt,
) : Hendelse {
    override val behandlingsporing = Behandlingsporing.Arbeidsgiver(organisasjonsnummer = orgnummer)
    override val metadata =
        LocalDateTime.now().let { nå ->
            HendelseMetadata(
                meldingsreferanseId = meldingsreferanseId,
                avsender = SYSTEM,
                innsendt = nå,
                registrert = nå,
                automatiskBehandling = true,
            )
        }

    internal fun erRelevant(
        aktivitetslogg: IAktivitetslogg,
        other: UUID,
        skjæringstidspunktVedtaksperiode: LocalDate,
    ): Boolean {
        if (other != vedtaksperiodeId) return false
        if (skjæringstidspunktVedtaksperiode == skjæringstidspunkt) return true
        aktivitetslogg.info(
            "Vilkårsgrunnlag var relevant for Vedtaksperiode, men skjæringstidspunktene var ulikte: [$skjæringstidspunkt, $skjæringstidspunktVedtaksperiode]"
        )
        return false
    }

    internal fun lagreInntekt(
        inntektshistorikk: Inntektshistorikk,
        refusjonshistorikk: Refusjonshistorikk,
    ) {
        inntektshistorikk.leggTil(
            inntekter.somInntektsmelding(skjæringstidspunkt, metadata.meldingsreferanseId)
        )
        val refusjon =
            Refusjonshistorikk.Refusjon(
                metadata.meldingsreferanseId,
                skjæringstidspunkt,
                emptyList(),
                INGEN,
                null,
                emptyList(),
            )
        refusjonshistorikk.leggTilRefusjon(refusjon)
    }

    internal fun skatteinntekterLagtTilGrunnEvent(
        behandlingId: UUID
    ): PersonObserver.SkatteinntekterLagtTilGrunnEvent {
        return PersonObserver.SkatteinntekterLagtTilGrunnEvent(
            organisasjonsnummer = orgnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            behandlingId = behandlingId,
            skjæringstidspunkt = skjæringstidspunkt,
            skatteinntekter = inntekter.somEksterneSkatteinntekter(),
            omregnetÅrsinntekt = inntekter.omregnetÅrsinntekt(metadata.meldingsreferanseId),
        )
    }
}
