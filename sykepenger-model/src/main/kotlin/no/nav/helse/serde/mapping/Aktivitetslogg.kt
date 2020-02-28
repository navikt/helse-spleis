package no.nav.helse.serde.mapping

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.SpesifikkKontekst
import no.nav.helse.serde.PersonData.AktivitetsloggData
import no.nav.helse.serde.reflection.ReflectInstance.Companion.get

internal fun konverterTilAktivitetslogg(aktivitetsloggData: AktivitetsloggData): Aktivitetslogg {
    val aktivitetslogg = Aktivitetslogg()

    val aktiviteter = aktivitetslogg.get<Aktivitetslogg, MutableList<Any>>("aktiviteter")
    aktivitetsloggData.aktiviteter.forEach {
        val kontekster = it.kontekster.map { SpesifikkKontekst(it.kontekstType, it.kontekstMap) }
        aktiviteter.add(when (it.alvorlighetsgrad) {
            AktivitetsloggData.Alvorlighetsgrad.INFO -> Aktivitetslogg.Aktivitet.Info(
                kontekster,
                it.melding,
                it.tidsstempel
            )
            AktivitetsloggData.Alvorlighetsgrad.WARN -> Aktivitetslogg.Aktivitet.Warn(
                kontekster,
                it.melding,
                it.tidsstempel
            )
            AktivitetsloggData.Alvorlighetsgrad.BEHOV -> Aktivitetslogg.Aktivitet.Behov(
                Aktivitetslogg.Aktivitet.Behov.Behovtype.valueOf(it.behovtype!!),
                kontekster,
                it.melding,
                emptyMap(),
                it.tidsstempel
            )
            AktivitetsloggData.Alvorlighetsgrad.ERROR -> Aktivitetslogg.Aktivitet.Error(
                kontekster,
                it.melding,
                it.tidsstempel
            )
            AktivitetsloggData.Alvorlighetsgrad.SEVERE -> Aktivitetslogg.Aktivitet.Severe(
                kontekster,
                it.melding,
                it.tidsstempel
            )
        })
    }

    return aktivitetslogg
}
