package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDate
import java.time.LocalDateTime

internal class V120BrukeInntektsmeldingOverSkattIVilkårsgrunnlag : JsonMigration(version = 120){

    private val dagenV114BleInnført = LocalDate.parse("2021-09-07")

    override val description = "Rekjører migrering V114 for alle vilkårsgrunnlag som ble opprettet i migreringen på grunn av en bug i V114"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        val (_, sisteVilkårsgrunnlagHisorikkInnslagFørV114BleInnført) = jsonNode["vilkårsgrunnlagHistorikk"]
            .map { LocalDateTime.parse(it["opprettet"].asText()) to it }
            .filter { (opprettet, _) -> opprettet.toLocalDate() < dagenV114BleInnført }
            .maxByOrNull { (opprettet, _) -> opprettet }
            ?: return

        val gjeldendeVilkårsgrunnlagIder = sisteVilkårsgrunnlagHisorikkInnslagFørV114BleInnført["vilkårsgrunnlag"]
            .mapNotNull { it["meldingsreferanseId"]?.textValue() }

        jsonNode["vilkårsgrunnlagHistorikk"]
            .flatMap { it["vilkårsgrunnlag"] }
            .filter { it["meldingsreferanseId"]?.textValue() in gjeldendeVilkårsgrunnlagIder }
            .forEach {
                V114LagreSykepengegrunnlag.genererSykepengegrunnlag((it as ObjectNode), jsonNode)
            }
    }
}
