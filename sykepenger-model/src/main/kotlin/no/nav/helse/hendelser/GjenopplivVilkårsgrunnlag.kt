package no.nav.helse.hendelser

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.person.VilkårsgrunnlagHistorikk
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning
import no.nav.helse.person.inntekt.Inntektsmelding
import no.nav.helse.person.inntekt.Refusjonsopplysning
import no.nav.helse.person.inntekt.Refusjonsopplysning.Refusjonsopplysninger.Companion.refusjonsopplysninger
import no.nav.helse.økonomi.Inntekt

class GjenopplivVilkårsgrunnlag(
    meldingsreferanseId: UUID,
    aktørId: String,
    fødselsnummer: String,
    private val vilkårsgrunnlagId: UUID,
    private val nyttSkjæringstidspunkt: LocalDate?,
    private val arbeidsgiveropplysninger: Map<String, Inntekt>
): PersonHendelse(meldingsreferanseId, fødselsnummer, aktørId, Aktivitetslogg()) {

    internal fun gjenoppliv(vilkårsgrunnlagHistorikk: VilkårsgrunnlagHistorikk) {
        vilkårsgrunnlagHistorikk.gjenoppliv(this, vilkårsgrunnlagId, nyttSkjæringstidspunkt)
    }

    internal fun arbeidsgiverinntektsopplysninger(skjæringstidspunkt: LocalDate) = arbeidsgiveropplysninger.map { (organisasjonsnummer, inntekt) ->
        val inntektsmeldingInntekt = Inntektsmelding(skjæringstidspunkt, meldingsreferanseId(), inntekt)
        ArbeidsgiverInntektsopplysning(organisasjonsnummer, skjæringstidspunkt til LocalDate.MAX, inntektsmeldingInntekt, Refusjonsopplysning(meldingsreferanseId(), skjæringstidspunkt, null, inntekt).refusjonsopplysninger)
    }

    internal fun valider(organisasjonsnummere: List<String>) {
        if (arbeidsgiveropplysninger.isEmpty()) return
        check(organisasjonsnummere.containsAll(arbeidsgiveropplysninger.keys)) {
            "Det er forsøkt å legge til inntektsopplysnigner for arbeidsgiver som ikke finnes på personen."
        }
    }
}