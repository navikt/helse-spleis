package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.util.UUID
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.serde.serdeObjectMapper
import org.slf4j.LoggerFactory

internal class V144TyperPåHendelserIVedtaksperiode : JsonMigration(version = 144) {
    override val description: String = "Legger til typer på hendelses-ider i vedtaksperioden"

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        val meldinger: Map<UUID, Pair<Navn, Json>> = meldingerSupplier.hentMeldinger()
        jsonNode["arbeidsgivere"]
            .flatMap { it["vedtaksperioder"] }
            .migrerInnHendelseNavn(meldinger)

        jsonNode["arbeidsgivere"]
            .flatMap { it["forkastede"] }
            .map { it["vedtaksperiode"] }
            .migrerInnHendelseNavn(meldinger)
    }


    private fun List<JsonNode>.migrerInnHendelseNavn(meldinger: Map<UUID, Pair<Navn, Json>>) {
        this.map { it as ObjectNode }
            .forEach {
                val vedtaksperiodeId = UUID.fromString(it["id"].asText())
                val hendelser: ObjectNode = it.remove("hendelseIder").fold(serdeObjectMapper.createObjectNode()) { acc, idNode ->
                    val id = UUID.fromString(idNode.asText())
                    val hendelseType = meldinger[id]?.let { (type, _) -> type }

                    if (hendelseType == null) {
                        sikkerlogg.warn("Fjerner hendelse med id=$id fra vedtaksperiode med id=$vedtaksperiodeId, da hendelsen ikke finnes i meldingstabellen i spleisdb")
                        return@fold acc
                    }
                    val sporingsType = hendelseTypeTilSporing(hendelseType, vedtaksperiodeId)
                    acc.put(idNode.asText(), sporingsType.name)
                    acc
                }
                it.set<ObjectNode>("hendelseIder", hendelser)
            }
    }

    private fun hendelseTypeTilSporing(hendelseType: String, vedtaksperiodeId: UUID): Dokumentsporing.DokumentType {
        return when (hendelseType) {
            "NY_SØKNAD" -> Dokumentsporing.DokumentType.Sykmelding
            "SENDT_SØKNAD_ARBEIDSGIVER" -> Dokumentsporing.DokumentType.Søknad
            "SENDT_SØKNAD_NAV" -> Dokumentsporing.DokumentType.Søknad
            "INNTEKTSMELDING" -> Dokumentsporing.DokumentType.Inntektsmelding
            "OVERSTYRTIDSLINJE" -> Dokumentsporing.DokumentType.OverstyrTidslinje
            "OVERSTYRINNTEKT" -> Dokumentsporing.DokumentType.OverstyrInntekt
            "OVERSTYRARBEIDSFORHOLD" -> Dokumentsporing.DokumentType.OverstyrArbeidsforhold
            else -> throw IllegalArgumentException("Hendelse med type=$hendelseType var ikke forventet i migrering. VedtaksperiodeId=$vedtaksperiodeId")
        }
    }
}

