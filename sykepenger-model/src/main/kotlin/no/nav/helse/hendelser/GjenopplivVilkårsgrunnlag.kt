package no.nav.helse.hendelser

import java.util.UUID
import no.nav.helse.person.VilkårsgrunnlagHistorikk
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg

class GjenopplivVilkårsgrunnlag(
    meldingsreferanseId: UUID,
    aktørId: String,
    fødselsnummer: String,
    private val vilkårsgrunnlagId: UUID,
): PersonHendelse(meldingsreferanseId, fødselsnummer, aktørId, Aktivitetslogg()) {

    internal fun gjenoppliv(vilkårsgrunnlagHistorikk: VilkårsgrunnlagHistorikk) {
        vilkårsgrunnlagHistorikk.gjenoppliv(this, vilkårsgrunnlagId)
    }
}