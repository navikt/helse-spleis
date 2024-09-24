package no.nav.helse.hendelser

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.Personidentifikator
import no.nav.helse.person.inntekt.Inntektshistorikk
import no.nav.helse.person.inntekt.Refusjonshistorikk
import no.nav.helse.økonomi.Inntekt.Companion.INGEN

class SykepengegrunnlagForArbeidsgiver(
    meldingsreferanseId: UUID,
    private val vedtaksperiodeId: String,
    private val skjæringstidspunkt: LocalDate,
    aktørId: String,
    personidentifikator: Personidentifikator,
    orgnummer: String,
    private val inntekter: ArbeidsgiverInntekt
) : ArbeidstakerHendelse(meldingsreferanseId, personidentifikator.toString(), aktørId, orgnummer) {

    internal fun erRelevant(other: UUID, skjæringstidspunktVedtaksperiode: LocalDate): Boolean {
        if (other.toString() != vedtaksperiodeId) return false
        if (skjæringstidspunktVedtaksperiode == skjæringstidspunkt) return true
        info("Vilkårsgrunnlag var relevant for Vedtaksperiode, men skjæringstidspunktene var ulikte: [$skjæringstidspunkt, $skjæringstidspunktVedtaksperiode]")
        return false
    }

    internal fun lagreInntekt(inntektshistorikk: Inntektshistorikk, refusjonshistorikk: Refusjonshistorikk) {
        inntektshistorikk.leggTil(inntekter.somInntektsmelding(skjæringstidspunkt, meldingsreferanseId()))
        val refusjon = Refusjonshistorikk.Refusjon(meldingsreferanseId(), skjæringstidspunkt, emptyList(), INGEN, null, emptyList())
        refusjonshistorikk.leggTilRefusjon(refusjon)
    }
}
