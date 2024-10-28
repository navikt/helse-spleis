package no.nav.helse.hendelser

import no.nav.helse.person.aktivitetslogg.Aktivitetskontekst
import no.nav.helse.person.aktivitetslogg.SpesifikkKontekst

sealed class PersonHendelse : Hendelse, Aktivitetskontekst {

    override fun navn(): String = javaClass.simpleName

    final override fun toSpesifikkKontekst() = this.javaClass.canonicalName.split('.').last().let {
        SpesifikkKontekst(it, buildMap {
            put("meldingsreferanseId", metadata.meldingsreferanseId.toString())
            put("aktørId", behandlingsporing.aktørId)
            put("fødselsnummer", behandlingsporing.fødselsnummer)
            when (val sporing = behandlingsporing) {
                is Behandlingsporing.Arbeidsgiver -> {
                    put("organisasjonsnummer", sporing.organisasjonsnummer)
                }
                is Behandlingsporing.Person -> {}
            }
        })
    }
}