package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.readValue
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.RefusjonsopplysningerVisitor
import no.nav.helse.person.inntekt.Refusjonshistorikk
import no.nav.helse.person.inntekt.Refusjonshistorikk.Refusjon.EndringIRefusjon.Companion.erTom
import no.nav.helse.person.inntekt.Refusjonshistorikk.Refusjon.EndringIRefusjon.Companion.refusjonsopplysninger
import no.nav.helse.person.inntekt.Refusjonsopplysning
import no.nav.helse.person.inntekt.Refusjonsopplysning.Refusjonsopplysninger
import no.nav.helse.person.inntekt.Refusjonsopplysning.Refusjonsopplysninger.Companion.refusjonsopplysninger
import no.nav.helse.serde.migration.RefusjonData.Companion.parseRefusjon
import no.nav.helse.serde.migration.RefusjonData.EndringIRefusjonData.Companion.parseEndringerIRefusjon
import no.nav.helse.serde.serdeObjectMapper
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.slf4j.LoggerFactory

internal object RefusjonsopplysningerIVilkårsgrunnlag {

    internal fun vilkårsgrunnlagMedRefusjonsopplysninger(jsonNode: JsonNode): ArrayNode? {
        val aktørId = jsonNode.path("aktørId").asText()
        val vilkårsgrunnlagHistorikk = jsonNode.path("vilkårsgrunnlagHistorikk") as ArrayNode
        val gjeldendeVilkårsgrunnlagInnslag = vilkårsgrunnlagHistorikk.firstOrNull() ?: return null
        val kopiAvGjeldendeVilkårsgrunnlag = gjeldendeVilkårsgrunnlagInnslag.path("vilkårsgrunnlag").takeUnless { it.isEmpty }?.deepCopy<ArrayNode>() ?: return null

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
                    val inntekt = arbeidsgiverInntektsopplysning.path("inntektsopplysning")
                    val refusjonsopplysninger = finnRefusjonsopplysninger(
                        aktørId = aktørId,
                        organisasjonsnummer = organisasjonsnummer,
                        refusjonshistorikk = refusjonshistorikkPerArbeidsgiver[organisasjonsnummer],
                        skjæringstidspunkt = skjæringstidspunkt,
                        vilkårsgrunnlagType = vilkårsgrunnlagType,
                        fallback = { inntekt.fallbackRefusjonsopplysninger() }
                    )
                    arbeidsgiverInntektsopplysning.putArray("refusjonsopplysninger").addAll(refusjonsopplysninger.arrayNode)
                }

            vilkårsgrunnlag.path("sykepengegrunnlag")
                .path("deaktiverteArbeidsforhold")
                .forEach { deaktivertArbeidsforhold ->
                    deaktivertArbeidsforhold as ObjectNode
                    deaktivertArbeidsforhold.putArray("refusjonsopplysninger")
                }
        }
        return kopiAvGjeldendeVilkårsgrunnlag
    }

    private fun JsonNode.erSkatt() = path("skatteopplysninger").isArray
    private fun JsonNode.erIkkeRapportert() = path("kilde").asText() == "IKKE_RAPPORTERT"
    private fun JsonNode.fallbackRefusjonsopplysninger(): Refusjonsopplysninger {
        // Ghosts hvor vi ikke har noe refusjonshistorikk på skjæringstidspunktet. Skal ha tomme refusjonsopplysninger
        if (erSkatt() || erIkkeRapportert()) return Refusjonsopplysninger()
        return Refusjonsopplysning(
            meldingsreferanseId = UUID.fromString(path("hendelseId").asText()),
            fom = LocalDate.parse(path("dato").asText()),
            tom = null,
            beløp = path("beløp").asDouble().månedlig
        ).refusjonsopplysninger
    }

    private fun finnRefusjonsopplysninger(
        aktørId: String,
        organisasjonsnummer: String,
        refusjonshistorikk: Refusjonshistorikk?,
        skjæringstidspunkt: LocalDate,
        vilkårsgrunnlagType: String,
        fallback: () -> Refusjonsopplysninger
    ): Refusjonsopplysninger {
        if (refusjonshistorikk == null || refusjonshistorikk.erTom()) {
            val refusjonsopplysninger = if (vilkårsgrunnlagType == SPLEIS) Refusjonsopplysninger() else fallback()
            return refusjonsopplysninger.also {
                sikkerlogg.info("Fant ikke refusjonsopplysninger for vilkårsgrunnlag. Ingen refusjonshistorikk for arbeidsgiver. {}, {}, skjæringstidspunkt=$skjæringstidspunkt, vilkårsgrunnlagType=$vilkårsgrunnlagType",
                    keyValue("aktørId", aktørId),
                    keyValue("organisasjonsnummer", organisasjonsnummer)
                )
            }
        }
        val refusjonsopplysningerPåSkjæringstidspunkt = refusjonshistorikk.refusjonsopplysninger(skjæringstidspunkt)
        if (refusjonsopplysningerPåSkjæringstidspunkt.harOpplysninger) return refusjonsopplysningerPåSkjæringstidspunkt.also {
            sikkerlogg.info("Fant refusjonsopplysninger på skjæringstidspunkt for vilkårsgrunnlag. {}, {}, skjæringstidspunkt=$skjæringstidspunkt, vilkårsgrunnlagType=$vilkårsgrunnlagType",
                keyValue("aktørId", aktørId),
                keyValue("organisasjonsnummer", organisasjonsnummer)
            )
        }

        if (vilkårsgrunnlagType == SPLEIS) return fallback().also {
            sikkerlogg.info("Fant ikke refusjonsopplysninger for vilkårsgrunnlag. {}, {}, skjæringstidspunkt=$skjæringstidspunkt, vilkårsgrunnlagType=$vilkårsgrunnlagType",
                keyValue("aktørId", aktørId),
                keyValue("organisasjonsnummer", organisasjonsnummer)
            )
        }

        (1..19).forEach { i ->
            val refusjonsopplysninger = refusjonshistorikk.refusjonsopplysninger(skjæringstidspunkt.minusDays(i.toLong()))
            if (refusjonsopplysninger.harOpplysninger) return refusjonsopplysninger.also {
                sikkerlogg.info("Fant refusjonsopplysninger for vilkårsgrunnlag ved å gå $i dager tilbake fra skjæringstidspunktet. {}, {}, skjæringstidspunkt=$skjæringstidspunkt, vilkårsgrunnlagType=$vilkårsgrunnlagType",
                    keyValue("aktørId", aktørId),
                    keyValue("organisasjonsnummer", organisasjonsnummer)
                )
            }
        }

        return fallback().also {
            sikkerlogg.info("Fant ikke refusjonsopplysninger for vilkårsgrunnlag. {}, {}, skjæringstidspunkt=$skjæringstidspunkt, vilkårsgrunnlagType=$vilkårsgrunnlagType",
                keyValue("aktørId", aktørId),
                keyValue("organisasjonsnummer", organisasjonsnummer)
            )
        }
    }

    private val SPLEIS = "Vilkårsprøving"
    private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    private val Refusjonsopplysninger.harOpplysninger get() = this != Refusjonsopplysninger()

    internal val Refusjonsopplysninger.arrayNode get() = RefusjonsopplysningerToArrayNode(this).arrayNode
    private class RefusjonsopplysningerToArrayNode(refusjonsopplysninger: Refusjonsopplysninger): RefusjonsopplysningerVisitor {
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

data class RefusjonData(
    private val meldingsreferanseId: UUID,
    private val førsteFraværsdag: LocalDate?,
    private val arbeidsgiverperioder: List<Periode>,
    private val beløp: Double?,
    private val sisteRefusjonsdag: LocalDate?,
    private val endringerIRefusjon: List<EndringIRefusjonData>,
    private val tidsstempel: LocalDateTime
) {
    internal companion object {
        internal fun List<RefusjonData>.parseRefusjon() = Refusjonshistorikk().apply {
            forEach {
                leggTilRefusjon(
                    Refusjonshistorikk.Refusjon(
                        meldingsreferanseId = it.meldingsreferanseId,
                        førsteFraværsdag = it.førsteFraværsdag,
                        arbeidsgiverperioder = it.arbeidsgiverperioder,
                        beløp = it.beløp?.månedlig,
                        sisteRefusjonsdag = it.sisteRefusjonsdag,
                        endringerIRefusjon = it.endringerIRefusjon.parseEndringerIRefusjon(),
                        tidsstempel = it.tidsstempel
                    )
                )
            }
        }
    }

    data class EndringIRefusjonData(
        private val beløp: Double,
        private val endringsdato: LocalDate
    ) {
        internal companion object {
            internal fun List<EndringIRefusjonData>.parseEndringerIRefusjon() = map {
                Refusjonshistorikk.Refusjon.EndringIRefusjon(
                    beløp = it.beløp.månedlig,
                    endringsdato = it.endringsdato
                )
            }
        }
    }
}

data class Arbeidsgiver(
    val refusjonshistorikk: List<RefusjonData>
)