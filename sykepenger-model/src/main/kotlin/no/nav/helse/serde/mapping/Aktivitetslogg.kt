package no.nav.helse.serde.mapping

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.NeedType
import no.nav.helse.person.SpesifikkKontekst
import no.nav.helse.serde.PersonData.AktivitetsloggData
import no.nav.helse.serde.reflection.ReflectClass
import no.nav.helse.serde.reflection.ReflectInstance.Companion.get

internal fun konverterTilAktivitetslogg(aktivitetsloggData: AktivitetsloggData): Aktivitetslogg {
    val aktivitetslogg = Aktivitetslogg()

    val aktivitetClass: ReflectClass = ReflectClass.getNestedClass<Aktivitetslogg>("Aktivitet")

    val aktiviteter = aktivitetslogg.get<Aktivitetslogg, MutableList<Any>>("aktiviteter")
    aktivitetsloggData.aktiviteter.forEach {
        val kontekster = it.kontekster.map { SpesifikkKontekst(it.kontekstType, it.melding) }
        aktiviteter.add(when (it.alvorlighetsgrad) {
            AktivitetsloggData.Alvorlighetsgrad.INFO -> aktivitetClass.getNestedClass("Info").getInstance(
                kontekster,
                it.melding,
                it.tidsstempel
            )
            AktivitetsloggData.Alvorlighetsgrad.WARN -> aktivitetClass.getNestedClass("Warn").getInstance(
                kontekster,
                it.melding,
                it.tidsstempel
            )
            AktivitetsloggData.Alvorlighetsgrad.NEED -> aktivitetClass.getNestedClass("Need").getInstance(
                kontekster,
                NeedType.valueOf(requireNotNull(it.needType)),
                it.melding,
                it.tidsstempel
            )
            AktivitetsloggData.Alvorlighetsgrad.ERROR -> aktivitetClass.getNestedClass("Error").getInstance(
                kontekster,
                it.melding,
                it.tidsstempel
            )
            AktivitetsloggData.Alvorlighetsgrad.SEVERE -> aktivitetClass.getNestedClass("Severe").getInstance(
                kontekster,
                it.melding,
                it.tidsstempel
            )
        })
    }

    return aktivitetslogg
}
