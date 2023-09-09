package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDate
import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.keyValue
import org.slf4j.LoggerFactory

internal class V262FikseVilkårsgrunnlagForVedtaksperioder : JsonMigration(version = 262) {
    override val description = "sørger for at alle vedtaksperioder med utbetaling har et aktivt vilkårsgrunnlag"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        val aktørId = jsonNode.path("aktørId").asText()
        val vilkårsgrunnlaghistorikk = jsonNode.path("vilkårsgrunnlagHistorikk") as ArrayNode

        val aktivtVilkårsgrunnlagelement = vilkårsgrunnlaghistorikk.path(0)
        if (aktivtVilkårsgrunnlagelement !is ObjectNode) return

        val aktiveVilkårsgrunnlag = aktivtVilkårsgrunnlagelement
            .path("vilkårsgrunnlag")
            .map { it.path("skjæringstidspunkt").asLocalDate() }
            .toSet()

        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("vedtaksperioder")
                .filter { vedtaksperiode -> vedtaksperiode.path("tilstand").asText() == "AVSLUTTET" }
                .filter { vedtaksperiode -> vedtaksperiode.path("skjæringstidspunkt").asLocalDate() !in aktiveVilkårsgrunnlag }
                .forEach { vedtaksperiode ->
                    val forrigeVilkårsgrunnlag = vedtaksperiode.path("utbetalinger").last().path("vilkårsgrunnlagId").asUUID()
                    gjenopplivVilkårsgrunnlag(aktørId, vilkårsgrunnlaghistorikk, vedtaksperiode.path("skjæringstidspunkt").asLocalDate(), forrigeVilkårsgrunnlag)
                }
        }
    }

    private fun gjenopplivVilkårsgrunnlag(aktørId: String, vilkårsgrunnlaghistorikk: ArrayNode, nyttSkjæringstidspunkt: LocalDate, forrigeVilkårsgrunnlagId: UUID) {
        if (erMigrertFraFør(vilkårsgrunnlaghistorikk, nyttSkjæringstidspunkt)) return

        val forrigeVilkårsgrunnlag = finnVilkårsgrunnlaget(vilkårsgrunnlaghistorikk, forrigeVilkårsgrunnlagId)

        sikkerlogg.info("V262 {} gjenoppliver skjæringstidspunkt=${forrigeVilkårsgrunnlag.path("skjæringstidspunkt").asText()} til=$nyttSkjæringstidspunkt fra vilkårsgrunnlagId=$forrigeVilkårsgrunnlagId", keyValue("aktørId", aktørId))

        val nytt = kopierVilkårsgrunnlagMedNyttSkjæringtsidspunkt(nyttSkjæringstidspunkt, forrigeVilkårsgrunnlag)
        leggTilNyttVilkårsgrunnlag(vilkårsgrunnlaghistorikk, nytt)
    }

    private fun leggTilNyttVilkårsgrunnlag(vilkårsgrunnlaghistorikk: ArrayNode, nytt: ObjectNode) {
        (vilkårsgrunnlaghistorikk[0].path("vilkårsgrunnlag") as ArrayNode).add(nytt)
    }

    private fun erMigrertFraFør(vilkårsgrunnlaghistorikk: ArrayNode, nyttSkjæringstidspunkt: LocalDate) =
        vilkårsgrunnlaghistorikk.path(0).any { grunnlag ->
            grunnlag.any { it.path("skjæringstidspunkt").asLocalDate() == nyttSkjæringstidspunkt }
        }

    private fun finnVilkårsgrunnlaget(vilkårsgrunnlaghistorikk: ArrayNode, forrigeVilkårsgrunnlagId: UUID) =
        vilkårsgrunnlaghistorikk.firstNotNullOf { vilkårsgrunnlagelement ->
            vilkårsgrunnlagelement.path("vilkårsgrunnlag").firstOrNull { grunnlag ->
                grunnlag.path("vilkårsgrunnlagId").asUUID() == forrigeVilkårsgrunnlagId
            }
        }

    private fun kopierVilkårsgrunnlagMedNyttSkjæringtsidspunkt(nyttSkjæringstidspunkt: LocalDate, vilkårsgrunnlag: JsonNode): ObjectNode {
        val nytt = vilkårsgrunnlag.deepCopy<ObjectNode>()
        nytt.put("skjæringstidspunkt", nyttSkjæringstidspunkt.toString())
        nytt.put("vilkårsgrunnlagId", UUID.randomUUID().toString())

        justerOpptjeningsperiode(nytt, nyttSkjæringstidspunkt)
        justerRefusjonsopplysninger(nytt, nyttSkjæringstidspunkt)

        return nytt
    }

    private fun justerRefusjonsopplysninger(nytt: ObjectNode, nyttSkjæringstidspunkt: LocalDate) {
        nytt.path("sykepengegrunnlag").path("arbeidsgiverInntektsopplysninger").forEach { opplysning ->
            val refusjonsopplysninger = opplysning.path("refusjonsopplysninger") as ArrayNode
            if (refusjonsopplysninger.isEmpty) {
                leggTilNyRefusjonsopplysning(opplysning, refusjonsopplysninger, nyttSkjæringstidspunkt)
            } else {
                justerFomPåEksisterendeRefusjonsopplysning(refusjonsopplysninger, nyttSkjæringstidspunkt)
            }
        }
    }

    private fun justerFomPåEksisterendeRefusjonsopplysning(refusjonsopplysninger: ArrayNode, nyttSkjæringstidspunkt: LocalDate) {
        (refusjonsopplysninger[0] as ObjectNode).put("fom", nyttSkjæringstidspunkt.toString())
    }

    private fun leggTilNyRefusjonsopplysning(opplysning: JsonNode, refusjonsopplysninger: ArrayNode, nyttSkjæringstidspunkt: LocalDate) {
        refusjonsopplysninger.addObject().apply {
            put("meldingsreferanseId", opplysning.path("inntektsopplysning").path("hendelseId").asText())
            put("beløp", opplysning.path("inntektsopplysning").path("beløp").asDouble())
            put("fom", nyttSkjæringstidspunkt.toString())
            putNull("tom")
        }
    }

    private fun justerOpptjeningsperiode(nytt: ObjectNode, nyttSkjæringstidspunkt: LocalDate) {
        if (nytt.path("type").asText() != "Vilkårsprøving") return
        (nytt.path("opptjening") as ObjectNode).put("opptjeningTom", nyttSkjæringstidspunkt.minusDays(1).toString())
    }

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }
}

private fun JsonNode.asLocalDate() = LocalDate.parse(asText())
private fun JsonNode.asUUID() = UUID.fromString(asText())