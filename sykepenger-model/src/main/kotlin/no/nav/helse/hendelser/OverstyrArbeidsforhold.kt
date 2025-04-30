package no.nav.helse.hendelser

import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.helse.etterlevelse.Subsumsjonslogg
import no.nav.helse.hendelser.Avsender.SAKSBEHANDLER
import no.nav.helse.person.Opptjening
import no.nav.helse.person.inntekt.Inntektsgrunnlag

class OverstyrArbeidsforhold(
    meldingsreferanseId: MeldingsreferanseId,
    private val skjæringstidspunkt: LocalDate,
    private val overstyrteArbeidsforhold: List<ArbeidsforholdOverstyrt>,
    opprettet: LocalDateTime
) : Hendelse, OverstyrInntektsgrunnlag {
    override val behandlingsporing = Behandlingsporing.IngenYrkesaktivitet
    override val metadata = HendelseMetadata(
        meldingsreferanseId = meldingsreferanseId,
        avsender = SAKSBEHANDLER,
        innsendt = opprettet,
        registrert = LocalDateTime.now(),
        automatiskBehandling = false
    )

    override fun erRelevant(skjæringstidspunkt: LocalDate) = this.skjæringstidspunkt == skjæringstidspunkt

    internal fun overstyr(inntektsgrunnlag: Inntektsgrunnlag, subsumsjonslogg: Subsumsjonslogg): Inntektsgrunnlag {
        return overstyrteArbeidsforhold.fold(inntektsgrunnlag) { acc, overstyring ->
            overstyring.overstyr(acc, subsumsjonslogg)
        }
    }

    internal fun overstyr(opptjening: Opptjening): Opptjening {
        return overstyrteArbeidsforhold.fold(opptjening) { acc, overstyring ->
            overstyring.overstyr(acc)
        }
    }

    class ArbeidsforholdOverstyrt(
        internal val orgnummer: String,
        private val deaktivert: Boolean,
        private val forklaring: String
    ) {
        internal fun overstyr(inntektsgrunnlag: Inntektsgrunnlag, subsumsjonslogg: Subsumsjonslogg) = when (deaktivert) {
            true -> inntektsgrunnlag.deaktiver(orgnummer, forklaring, subsumsjonslogg)
            else -> inntektsgrunnlag.aktiver(orgnummer, forklaring, subsumsjonslogg)
        }

        internal fun overstyr(opptjening: Opptjening) = when (deaktivert) {
            true -> opptjening.deaktiver(orgnummer)
            else -> opptjening.aktiver(orgnummer)
        }
    }
}
