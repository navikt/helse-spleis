package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

internal class V315AuuerSomIkkeRapportertInntekt : JsonMigration(version = 315) {
    override val description = "legger til en deaktivert IkkeRapportert inntekt for alle auuer som har et vilkårsgrunnlag"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        if (jsonNode.path("vilkårsgrunnlagHistorikk").isEmpty) return

        val tidsstempel = LocalDateTime.now().toString()
        val sisteInnslag = jsonNode.path("vilkårsgrunnlagHistorikk").path(0).deepCopy<ObjectNode>()

        val aktiveVilkårsgrunnlag = sisteInnslag.path("vilkårsgrunnlag").associate { element ->
            element.path("skjæringstidspunkt").asText().dato to element
        }

        val arbeidsgiverePerSkjæringstidspunkt = jsonNode.path("arbeidsgivere").mapNotNull { arbeidsgiver ->
            val orgnr = arbeidsgiver.path("organisasjonsnummer").asText()
            val skjæringstidspunkter = arbeidsgiver.path("vedtaksperioder")
                .map { vedtaksperiode -> vedtaksperiode.path("skjæringstidspunkt").asText().dato }
                .filter { it in aktiveVilkårsgrunnlag }
                .toSet()
            if (skjæringstidspunkter.isEmpty()) null
            else orgnr to skjæringstidspunkter
        }.toMap()

        val endredeSkjæringstidspunkter = mutableSetOf<LocalDate>()

        arbeidsgiverePerSkjæringstidspunkt.forEach { (orgnr, skjæringstidspunkter) ->
            skjæringstidspunkter.forEach { skjæringstidspunkt ->
                val nodeForSkjæringstidspunkt = aktiveVilkårsgrunnlag.getValue(skjæringstidspunkt)

                val inntekter = nodeForSkjæringstidspunkt.path("inntektsgrunnlag").path("arbeidsgiverInntektsopplysninger") as ArrayNode
                val deaktiverteArbeidsforhold = nodeForSkjæringstidspunkt.path("inntektsgrunnlag").path("deaktiverteArbeidsforhold") as ArrayNode

                if (inntekter.none { opplysning -> opplysning.path("orgnummer").asText() == orgnr } && deaktiverteArbeidsforhold.none { opplysning -> opplysning.path("orgnummer").asText() == orgnr }) {
                    endredeSkjæringstidspunkter.add(skjæringstidspunkt)
                    deaktiverteArbeidsforhold.addObject().apply {
                        put("orgnummer", orgnr)
                        put("fom", skjæringstidspunkt.toString())
                        put("tom", LocalDate.MAX.toString())
                        putArray("refusjonsopplysninger")
                        putObject("inntektsopplysning").apply {
                            put("id", UUID.randomUUID().toString())
                            put("dato", skjæringstidspunkt.toString())
                            put("hendelseId", "00000000-0000-0000-0000-000000000000")
                            put("kilde", "IKKE_RAPPORTERT")
                            put("tidsstempel", tidsstempel.toString())
                        }
                    }
                }
            }
        }

        if (endredeSkjæringstidspunkter.isEmpty()) return

        endredeSkjæringstidspunkter.forEach { skjæringstidspunkt ->
            val nodeForSkjæringstidspunkt = aktiveVilkårsgrunnlag.getValue(skjæringstidspunkt)

            nodeForSkjæringstidspunkt as ObjectNode
            nodeForSkjæringstidspunkt.put("meldingsreferanseId", "00000000-0000-0000-0000-000000000000")
            nodeForSkjæringstidspunkt.put("vilkårsgrunnlagId", UUID.randomUUID().toString())
        }

        sisteInnslag.put("id", UUID.randomUUID().toString())
        sisteInnslag.put("opprettet", tidsstempel.toString())

        // lager et nytt innslag som erstatter det forrige vilkårsgrunnlaget.
        // modifiserer ikke det eksisterende
        (jsonNode.path("vilkårsgrunnlagHistorikk") as ArrayNode).insert(0, sisteInnslag)
    }
}
