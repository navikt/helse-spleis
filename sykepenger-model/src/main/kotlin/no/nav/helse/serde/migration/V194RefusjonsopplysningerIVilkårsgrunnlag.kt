package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.readValue
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.person.Refusjonshistorikk
import no.nav.helse.person.Refusjonshistorikk.Refusjon.EndringIRefusjon.Companion.refusjonsopplysninger
import no.nav.helse.person.Refusjonsopplysning.Refusjonsopplysninger
import no.nav.helse.person.RefusjonsopplysningerVisitor
import no.nav.helse.serde.PersonData.ArbeidsgiverData.RefusjonData
import no.nav.helse.serde.PersonData.ArbeidsgiverData.RefusjonData.Companion.parseRefusjon
import no.nav.helse.serde.serdeObjectMapper
import no.nav.helse.økonomi.Inntekt
import org.slf4j.LoggerFactory

internal class V194RefusjonsopplysningerIVilkårsgrunnlag: JsonMigration(version = 194) {

    override val description =
        "Legger til refusjonsopplysninger i eksisterende vilkårsgrunnlag basert på det som finnes i refusjonshistorikken for arbeidsgiverne i sykepengegrunnlaget"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        val aktørId = jsonNode.path("aktørId").asText()
        val vilkårsgrunnlagHistorikk = jsonNode.path("vilkårsgrunnlagHistorikk") as ArrayNode
        val gjeldendeVilkårsgrunnlagInnslag = vilkårsgrunnlagHistorikk.firstOrNull() ?: return
        val kopiAvGjeldendeVilkårsgrunnlag = gjeldendeVilkårsgrunnlagInnslag.path("vilkårsgrunnlag").takeUnless { it.isEmpty }?.deepCopy<ArrayNode>() ?: return

        val refusjonshistorikkPerArbeidsgiver = jsonNode.path("arbeidsgivere")
            .associateBy { it.path("organisasjonsnummer").asText() }
            .mapValues { (_, arbeidsgiver) ->
                serdeObjectMapper.readValue<Arbeidsgiver>(arbeidsgiver.toString()).refusjonshistorikk.parseRefusjon()
            }

        kopiAvGjeldendeVilkårsgrunnlag.forEach { vilkårsgrunnlag ->
            vilkårsgrunnlag as ObjectNode
            val skjæringstidspunkt = LocalDate.parse(vilkårsgrunnlag.path("skjæringstidspunkt").asText())
            val vilkårsgrunnlagType = vilkårsgrunnlag.path("type").asText()

            vilkårsgrunnlag.path("sykepengegrunnlag")
                .path("arbeidsgiverInntektsopplysninger")
                .forEach { arbeidsgiverInntektsopplysning ->
                    arbeidsgiverInntektsopplysning as ObjectNode
                    val organisasjonsnummer = arbeidsgiverInntektsopplysning.path("orgnummer").asText()
                    val refusjonsopplysninger = finnRefusjonsopplysninger(
                        aktørId = aktørId,
                        organisasjonsnummer = organisasjonsnummer,
                        refusjonshistorikk = refusjonshistorikkPerArbeidsgiver[organisasjonsnummer],
                        skjæringstidspunkt = skjæringstidspunkt,
                        vilkårsgrunnlagType = vilkårsgrunnlagType
                    )
                    arbeidsgiverInntektsopplysning.putArray("refusjonsopplysninger").addAll(refusjonsopplysninger.arrayNode)
                }
        }

        val nyttInnslag = serdeObjectMapper.createObjectNode().apply {
            put("id", "${UUID.randomUUID()}")
            put("opprettet", "${LocalDateTime.now()}")
            putArray("vilkårsgrunnlag").addAll(kopiAvGjeldendeVilkårsgrunnlag)
        }

        vilkårsgrunnlagHistorikk.insert(0, nyttInnslag)
    }

    private fun finnRefusjonsopplysninger(
        aktørId: String,
        organisasjonsnummer: String,
        refusjonshistorikk: Refusjonshistorikk?,
        skjæringstidspunkt: LocalDate,
        vilkårsgrunnlagType: String
    ): Refusjonsopplysninger {
        if (refusjonshistorikk == null) return Refusjonsopplysninger().also {
            sikkerlogg.info("Fant ikke refusjonsopplysninger for vilkårsgrunnlag. Ingen refusjonshistorikk for arbeidsgiver. {}, {}, skjæringstidspunkt=$skjæringstidspunkt, vilkårsgrunnlagType=$vilkårsgrunnlagType",
                keyValue("aktørId", aktørId), keyValue("organisasjonsnummer", organisasjonsnummer)
            )
        }
        val refusjonsopplysningerPåSkjæringstidspunkt = refusjonshistorikk.refusjonsopplysninger(skjæringstidspunkt)
        if (refusjonsopplysningerPåSkjæringstidspunkt.isNotEmpty()) return refusjonsopplysningerPåSkjæringstidspunkt.also {
            sikkerlogg.info("Fant refusjonsopplysninger på skjæringstidspunkt for vilkårsgrunnlag. {}, {}, skjæringstidspunkt=$skjæringstidspunkt, vilkårsgrunnlagType=$vilkårsgrunnlagType",
                keyValue("aktørId", aktørId), keyValue("organisasjonsnummer", organisasjonsnummer)
            )
        }

        (1..16).forEach { i ->
            val refusjonsopplysninger = refusjonshistorikk.refusjonsopplysninger(skjæringstidspunkt.minusDays(i.toLong()))
            if (refusjonsopplysninger.isNotEmpty()) return refusjonsopplysninger.also {
                sikkerlogg.info("Fant refusjonsopplysninger for vilkårsgrunnlag ved å gå $i dager tilbake fra skjæringstidspunktet. {}, {}, skjæringstidspunkt=$skjæringstidspunkt, vilkårsgrunnlagType=$vilkårsgrunnlagType",
                    keyValue("aktørId", aktørId), keyValue("organisasjonsnummer", organisasjonsnummer)
                )
            }
        }

        return Refusjonsopplysninger().also {
            sikkerlogg.info("Fant ikke refusjonsopplysninger for vilkårsgrunnlag. {}, {}, skjæringstidspunkt=$skjæringstidspunkt, vilkårsgrunnlagType=$vilkårsgrunnlagType",
                keyValue("aktørId", aktørId), keyValue("organisasjonsnummer", organisasjonsnummer)
            )
        }
    }

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")

        data class Arbeidsgiver(
            val refusjonshistorikk: List<RefusjonData>
        )

        val Refusjonsopplysninger.arrayNode get() = RefusjonsopplysningerToArrayNode(this).arrayNode
        class RefusjonsopplysningerToArrayNode(refusjonsopplysninger: Refusjonsopplysninger): RefusjonsopplysningerVisitor {
            val arrayNode = serdeObjectMapper.createArrayNode()

            init {
                refusjonsopplysninger.accept(this)
            }

            override fun visitRefusjonsopplysning(
                meldingsreferanseId: UUID,
                fom: LocalDate,
                tom: LocalDate?,
                beløp: Inntekt
            ) {
                arrayNode.add(serdeObjectMapper.createObjectNode().apply {
                    put("meldingsreferanseId", "$meldingsreferanseId")
                    put("fom", "$fom")
                    if (tom == null) putNull("tom")
                    else put("tom", "$tom")
                    put("beløp", beløp.reflection { _, månedlig, _, _ -> månedlig })
                })
            }
        }
    }
}