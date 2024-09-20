package no.nav.helse.hendelser

import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import no.nav.helse.Personidentifikator
import no.nav.helse.etterlevelse.Subsumsjonslogg
import no.nav.helse.hendelser.Vilkårsgrunnlag.Arbeidsforhold.Companion.opptjeningsgrunnlag
import no.nav.helse.person.Opptjening
import no.nav.helse.person.Person
import no.nav.helse.person.Vedtaksperiode.Companion.arbeidsgiverperioder
import no.nav.helse.person.VilkårsgrunnlagHistorikk
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.inntekt.AnsattPeriode
import no.nav.helse.person.inntekt.Inntektshistorikk
import no.nav.helse.person.inntekt.Refusjonshistorikk
import no.nav.helse.person.inntekt.Sammenligningsgrunnlag
import no.nav.helse.person.inntekt.SkattSykepengegrunnlag
import no.nav.helse.person.inntekt.Sykepengegrunnlag
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
