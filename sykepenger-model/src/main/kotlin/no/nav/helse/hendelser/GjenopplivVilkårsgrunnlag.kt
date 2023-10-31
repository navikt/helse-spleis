package no.nav.helse.hendelser

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.person.VilkårsgrunnlagHistorikk
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning
import no.nav.helse.person.inntekt.Sykepengegrunnlag.ArbeidsgiverInntektsopplysningerOverstyringer

class GjenopplivVilkårsgrunnlag(
    meldingsreferanseId: UUID,
    aktørId: String,
    fødselsnummer: String,
    private val vilkårsgrunnlagId: UUID,
    private val nyttSkjæringstidspunkt: LocalDate?,
    private val arbeidsgiveropplysninger: List<ArbeidsgiverInntektsopplysning>?
): PersonHendelse(meldingsreferanseId, fødselsnummer, aktørId, Aktivitetslogg()) {

    internal fun gjenoppliv(vilkårsgrunnlagHistorikk: VilkårsgrunnlagHistorikk) {
        vilkårsgrunnlagHistorikk.gjenoppliv(this, vilkårsgrunnlagId, nyttSkjæringstidspunkt)
    }

    internal fun overstyr(builder: ArbeidsgiverInntektsopplysningerOverstyringer) {
        arbeidsgiveropplysninger?.forEach {
            builder.leggTilInntekt(it)
        }
    }

    internal fun valider(organisasjonsnummere: List<String>) {
        if (arbeidsgiveropplysninger.isNullOrEmpty()) return
        check(arbeidsgiveropplysninger.all { arbeidsgiveropplysning ->
            organisasjonsnummere.any { arbeidsgiveropplysning.gjelder(it) }
        }) { "Det er forsøkt å legge til inntektsopplysnigner for arbeidsgiver som ikke finnes på personen." }
    }
}