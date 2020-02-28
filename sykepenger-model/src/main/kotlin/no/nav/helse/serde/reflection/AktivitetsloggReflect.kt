package no.nav.helse.serde.reflection

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Aktivitetslogg.Aktivitet.*
import no.nav.helse.person.AktivitetsloggVisitor
import no.nav.helse.person.SpesifikkKontekst
import no.nav.helse.serde.PersonData.AktivitetsloggData.Alvorlighetsgrad
import no.nav.helse.serde.PersonData.AktivitetsloggData.Alvorlighetsgrad.*

internal class AktivitetsloggReflect(private val aktivitetslogg: Aktivitetslogg) {
    private val aktiviteter = Aktivitetslogginspektør(aktivitetslogg).aktiviteter

    internal fun toMap() = mutableMapOf(
        "aktiviteter" to aktiviteter
    )

    private inner class Aktivitetslogginspektør(aktivitetslogg: Aktivitetslogg) : AktivitetsloggVisitor {
        internal val aktiviteter = mutableListOf<Map<String, Any>>()

        init {
            aktivitetslogg.accept(this)
        }

        private fun leggTilMelding(
            kontekster: List<SpesifikkKontekst>,
            alvorlighetsgrad: Alvorlighetsgrad,
            melding: String,
            tidsstempel: String
        ) {
            aktiviteter.add(
                mutableMapOf<String, Any>(
                    "kontekster" to map(kontekster),
                    "alvorlighetsgrad" to alvorlighetsgrad.name,
                    "melding" to melding,
                    "tidsstempel" to tidsstempel
                )
            )
        }

        private fun leggTilMelding(
            kontekster: List<SpesifikkKontekst>,
            alvorlighetsgrad: Alvorlighetsgrad,
            type: Behov.Behovtype,
            melding: String,
            tidsstempel: String
        ) {
            aktiviteter.add(
                mutableMapOf<String, Any>(
                    "kontekster" to map(kontekster),
                    "alvorlighetsgrad" to alvorlighetsgrad.name,
                    "behovtype" to type.toString(),
                    "melding" to melding,
                    "tidsstempel" to tidsstempel
                )
            )
        }

        private fun map(kontekster: List<SpesifikkKontekst>): List<Map<String, Any>> {
            return kontekster.map {
                mutableMapOf(
                    "kontekstType" to it.kontekstType,
                    "kontekstMap" to it.kontekstMap
                )
            }
        }

        override fun visitInfo(
            kontekster: List<SpesifikkKontekst>,
            aktivitet: Info,
            melding: String,
            tidsstempel: String
        ) {
            leggTilMelding(kontekster, INFO, melding, tidsstempel)
        }

        override fun visitWarn(
            kontekster: List<SpesifikkKontekst>, aktivitet: Warn, melding: String, tidsstempel: String
        ) {
            leggTilMelding(kontekster, WARN, melding, tidsstempel)
        }

        override fun visitBehov(
            kontekster: List<SpesifikkKontekst>,
            aktivitet: Behov,
            type: Behov.Behovtype,
            melding: String,
            tidsstempel: String
        ) {
            leggTilMelding(kontekster, BEHOV, type, melding, tidsstempel)
        }

        override fun visitError(
            kontekster: List<SpesifikkKontekst>, aktivitet: Error, melding: String, tidsstempel: String
        ) {
            leggTilMelding(kontekster, ERROR, melding, tidsstempel)
        }

        override fun visitSevere(
            kontekster: List<SpesifikkKontekst>, aktivitet: Severe, melding: String, tidsstempel: String
        ) {
            leggTilMelding(kontekster, SEVERE, melding, tidsstempel)
        }
    }
}
