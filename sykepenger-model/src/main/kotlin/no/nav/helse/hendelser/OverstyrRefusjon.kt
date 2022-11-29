package no.nav.helse.hendelser

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.NyeRefusjonsopplysninger
import no.nav.helse.person.PersonHendelse
import no.nav.helse.person.Refusjonsopplysning

class OverstyrRefusjon(
    meldingsreferanseId: UUID,
    fødselsnummer: String,
    aktørId: String,
    aktivitetslogg: Aktivitetslogg,
    private val refusjon: Map<String, Refusjonsopplysning.Refusjonsopplysninger>,
    private val skjæringstidspunkt: LocalDate
) : PersonHendelse(meldingsreferanseId, fødselsnummer, aktørId, aktivitetslogg) {
    internal fun erRelevant(skjæringstidspunkt: LocalDate) = this.skjæringstidspunkt == skjæringstidspunkt

    internal fun leggTil(hendelseIder: MutableSet<Dokumentsporing>) {
        hendelseIder.add(Dokumentsporing.overstyrRefusjon(meldingsreferanseId()))
    }

    internal fun overstyr(builder: NyeRefusjonsopplysninger) {
        refusjon.forEach { (orgnummer, refusjonsopplysninger) ->
            builder.leggTilRefusjonsopplysninger(orgnummer, refusjonsopplysninger)
        }
    }
}
