package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.readResource
import org.junit.jupiter.api.Test

internal class V177ForkastOgFlyttVilkårsgrunnlagTest : MigrationTest(V177ForkastOgFlyttVilkårsgrunnlag()) {

    @Test
    fun `Ping-Pong, AUU - IT - AVSLUTTET`() {
        assertForkastetVilkårsgrunnlag(
            originalJson = "/migrations/177/ping-pong-auu-it-avsluttet_original.json",
            expectedJson = "/migrations/177/ping-pong-auu-it-avsluttet_expected.json"
        )
    }

    @Test
    fun `Kun vilkårsgrunnlag på skjæringstidspunkt skal ikke gi nytt innslag`() {
        val json = toNode("/migrations/177/kun-vilkårsgrunnlag-på-skjæringstidspunkt.json".readResource()) as ObjectNode
        assertMigrationRaw(
            originalJson = "${json.put("skjemaVersjon", 176)}",
            expectedJson = "${json.put("skjemaVersjon", 177)}"
        )
    }

    @Test
    fun `Tom vilkårsgrunnlaghistorikk`() {
        val json = toNode("/migrations/177/tom-vilkårsgrunnlaghistorikk.json".readResource()) as ObjectNode
        assertMigrationRaw(
            originalJson = "${json.put("skjemaVersjon", 176)}",
            expectedJson = "${json.put("skjemaVersjon", 177)}"
        )
    }

    @Test
    fun `Revurdering fører til nytt skjæringstidspunkt bakover`() {
        assertForkastetVilkårsgrunnlag(
            originalJson = "/migrations/177/revurder-skjæringstidspunkt-bakover_original.json",
            expectedJson = "/migrations/177/revurder-skjæringstidspunkt-bakover_expected.json"
        )
    }

    @Test
    fun `Et spleis vilkårsgrunnlag på skjæringstidspunktet og flere vilkårsgrunnlag fra infotrygd for samme sykefraværstilfelle - velger siste vilkårsgrunnlag fra infotrygd`() {
        assertForkastetVilkårsgrunnlag(
            originalJson = "/migrations/177/flere-vilkårsgrunnlag-fra-it_original.json",
            expectedJson = "/migrations/177/flere-vilkårsgrunnlag-fra-it_expected.json"
        )
    }

    @Test
    fun `Velger vilkårsgrunnlag fra infotrygd om vi har forkastede perioder rett før sykefraværsperioden`() {
        assertForkastetVilkårsgrunnlag(
            originalJson = "/migrations/177/vilkårsgrunnlag-fra-infotrygd-og-forkastet-periode-rett-før_original.json",
            expectedJson = "/migrations/177/vilkårsgrunnlag-fra-infotrygd-og-forkastet-periode-rett-før_expected.json"
        )
    }

    @Test
    fun `Velger vilkårsgrunnlag fra infotrygd om vi har forkastede perioder som overlapper sykefraværsperioden`() {
        assertForkastetVilkårsgrunnlag(
            originalJson = "/migrations/177/vilkårsgrunnlag-fra-infotrygd-og-forkastet-periode-som-overlapper_original.json",
            expectedJson = "/migrations/177/vilkårsgrunnlag-fra-infotrygd-og-forkastet-periode-som-overlapper_expected.json"
        )
    }

    @Test
    fun `Ignorerer vilkårsgrunnlag fra infotrygd om vi ikke har noen forkastede perioder som overlapper med sykefraværsperioden`() {
        assertForkastetVilkårsgrunnlag(
            originalJson = "/migrations/177/vilkårsgrunnlag-fra-infotrygd-men-ingen-forkastet-periode_original.json",
            expectedJson = "/migrations/177/vilkårsgrunnlag-fra-infotrygd-men-ingen-forkastet-periode_expected.json"
        )
    }

    @Test
    fun `Ignorerer vilkårsgrunnlag fra infotrygd om vi har en forkastet periode som hverken er kant-til-kant eller overlapper med sykefraværsperioden`() {
        assertForkastetVilkårsgrunnlag(
            originalJson = "/migrations/177/vilkårsgrunnlag-fra-infotrygd-og-forkastet-periode-før_original.json",
            expectedJson = "/migrations/177/vilkårsgrunnlag-fra-infotrygd-og-forkastet-periode-før_expected.json"
        )
    }

    private fun assertForkastetVilkårsgrunnlag(originalJson: String, expectedJson: String) {
        val migrert = migrer(originalJson.readResource())
        val expected = expectedJson.readResource()
            .replace("{id}", migrert.path("vilkårsgrunnlagHistorikk")[0].path("id").asText())
            .replace("{opprettet}", migrert.path("vilkårsgrunnlagHistorikk")[0].path("opprettet").asText())
        assertJson(migrert.toString(), expected)
    }
}

