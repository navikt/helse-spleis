package no.nav.helse.spleis

import java.time.format.DateTimeFormatter
import java.util.*
import no.nav.helse.Personidentifikator
import no.nav.helse.etterlevelse.Regelverkslogg
import no.nav.helse.etterlevelse.Regelverksporing
import no.nav.helse.spleis.SubsumsjonMediator.SubsumsjonEvent.Companion.paragrafVersjonFormaterer
import no.nav.helse.spleis.meldinger.model.HendelseMessage
import no.nav.helse.spleis.utboks.UtgåendeMelding

internal class SubsumsjonMediator(
    private val message: HendelseMessage,
    private val versjonAvKode: String
) : Regelverkslogg {

    private val subsumsjoner = mutableListOf<SubsumsjonEvent>()

    override fun logg(sporing: Regelverksporing) {
        subsumsjoner.add(SubsumsjonEvent(
            fødselsnummer = sporing.fødselsnummer,
            organisasjonsnummer = sporing.organisasjonsnummer,
            vedtaksperiodeId = sporing.vedtaksperiodeId,
            behandlingId = sporing.behandlingId,
            lovverk = sporing.subsumsjon.lovverk,
            ikrafttredelse = paragrafVersjonFormaterer.format(sporing.subsumsjon.versjon),
            paragraf = sporing.subsumsjon.paragraf.ref,
            ledd = sporing.subsumsjon.ledd?.nummer,
            punktum = sporing.subsumsjon.punktum?.nummer,
            bokstav = sporing.subsumsjon.bokstav?.ref,
            input = sporing.subsumsjon.input,
            output = sporing.subsumsjon.output,
            utfall = sporing.subsumsjon.utfall.name
        )
        )
    }

    fun leggIUtboks(context: BehandlingContext) {
        if (subsumsjoner.isEmpty()) return
        subsumsjoner.forEach { subsumsjon ->
            context.leggIUtboks {
                subsumsjonMelding(subsumsjon)
            }
        }
    }

    private fun subsumsjonMelding(event: SubsumsjonEvent): UtgåendeMelding {
        return UtgåendeMelding.nySubsumsjonsmelding(Personidentifikator(event.fødselsnummer)) { id, tidsstempel ->
            mapOf(
                "@forårsaket_av" to mapOf(
                    "id" to message.meldingsporing.id
                ),
                "subsumsjon" to buildMap {
                    this["id"] = id
                    this["eventName"] = "subsumsjon"
                    this["tidsstempel"] = tidsstempel
                    this["versjon"] = "1.1.0"
                    this["kilde"] = "spleis"
                    this["versjonAvKode"] = versjonAvKode
                    this["fodselsnummer"] = event.fødselsnummer
                    this["vedtaksperiodeId"] = event.vedtaksperiodeId
                    this["behandlingId"] = event.behandlingId
                    this["sporing"] = buildMap {
                        this["organisasjonsnummer"] = listOf(event.organisasjonsnummer)
                        if (event.vedtaksperiodeId != null) this["vedtaksperiode"] = listOf(event.vedtaksperiodeId.toString())
                    }
                    this["lovverk"] = event.lovverk
                    this["lovverksversjon"] = event.ikrafttredelse
                    this["paragraf"] = event.paragraf
                    this["input"] = event.input
                    this["output"] = event.output
                    this["utfall"] = event.utfall
                    if (event.ledd != null) {
                        this["ledd"] = event.ledd
                    }
                    if (event.punktum != null) {
                        this["punktum"] = event.punktum
                    }
                    if (event.bokstav != null) {
                        this["bokstav"] = event.bokstav
                    }
                }
            )
        }
    }

    data class SubsumsjonEvent(
        val fødselsnummer: String,
        val organisasjonsnummer: String,
        val vedtaksperiodeId: UUID?,
        val behandlingId: UUID?,
        val lovverk: String,
        val ikrafttredelse: String,
        val paragraf: String,
        val ledd: Int?,
        val punktum: Int?,
        val bokstav: Char?,
        val input: Map<String, Any>,
        val output: Map<String, Any>,
        val utfall: String,
    ) {
        companion object {
            val paragrafVersjonFormaterer = DateTimeFormatter.ISO_DATE
        }
    }
}
