package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.readValue
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.person.Refusjonshistorikk.Refusjon.EndringIRefusjon.Companion.refusjonsopplysninger
import no.nav.helse.person.Refusjonsopplysning.Refusjonsopplysninger
import no.nav.helse.person.RefusjonsopplysningerVisitor
import no.nav.helse.serde.PersonData.ArbeidsgiverData.RefusjonData
import no.nav.helse.serde.PersonData.ArbeidsgiverData.RefusjonData.Companion.parseRefusjon
import no.nav.helse.serde.serdeObjectMapper
import no.nav.helse.økonomi.Inntekt

internal class V194RefusjonsopplysningerIVilkårsgrunnlag: JsonMigration(version = 194) {

    override val description =
        "Legger til refusjonsopplysninger i eksisterende vilkårsgrunnlag basert på det som finnes i refusjonshistorikken for arbeidsgiverne i sykepengegrunnlaget"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
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
            val type = vilkårsgrunnlag.path("type").asText()
            check(type == "Vilkårsprøving") { "Avklare hvordan vi skal grave frem info for Infotrygd-vilkårsgrunnlag" }

            vilkårsgrunnlag.path("sykepengegrunnlag")
                .path("arbeidsgiverInntektsopplysninger")
                .forEach { arbeidsgiverInntektsopplysning ->
                    arbeidsgiverInntektsopplysning as ObjectNode
                    val organisasjonsnummer = arbeidsgiverInntektsopplysning.path("orgnummer").asText()
                    val refusjonsopplysninger = refusjonshistorikkPerArbeidsgiver[organisasjonsnummer]?.refusjonsopplysninger(skjæringstidspunkt) ?: Refusjonsopplysninger()
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


    private companion object {
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