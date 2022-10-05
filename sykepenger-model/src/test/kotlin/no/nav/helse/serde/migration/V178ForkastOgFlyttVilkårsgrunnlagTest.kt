package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.readResource
import org.junit.jupiter.api.Test

internal class V178ForkastOgFlyttVilkårsgrunnlagTest : MigrationTest(V178ForkastOgFlyttVilkårsgrunnlag()) {

    @Test
    fun `Ping-Pong, AUU - IT - AVSLUTTET`() {
        assertForkastetVilkårsgrunnlag(
            originalJson = "/migrations/178/ping-pong-auu-it-avsluttet_original.json",
            expectedJson = "/migrations/178/ping-pong-auu-it-avsluttet_expected.json"
        )
    }

    @Test
    fun `Kun vilkårsgrunnlag på skjæringstidspunkt skal ikke gi nytt innslag`() {
        val json = toNode("/migrations/178/kun-vilkårsgrunnlag-på-skjæringstidspunkt.json".readResource()) as ObjectNode
        assertMigrationRaw(
            originalJson = "${json.put("skjemaVersjon", 177)}",
            expectedJson = "${json.put("skjemaVersjon", 178)}"
        )
    }

    @Test
    fun `Tom vilkårsgrunnlaghistorikk`() {
        val json = toNode("/migrations/178/tom-vilkårsgrunnlaghistorikk.json".readResource()) as ObjectNode
        assertMigrationRaw(
            originalJson = "${json.put("skjemaVersjon", 177)}",
            expectedJson = "${json.put("skjemaVersjon", 178)}"
        )
    }

    @Test
    fun `Revurdering fører til nytt skjæringstidspunkt bakover`() {
        assertForkastetVilkårsgrunnlag(
            originalJson = "/migrations/178/revurder-skjæringstidspunkt-bakover_original.json",
            expectedJson = "/migrations/178/revurder-skjæringstidspunkt-bakover_expected.json"
        )
    }

    @Test
    fun `Et spleis vilkårsgrunnlag på skjæringstidspunktet og flere vilkårsgrunnlag fra infotrygd for samme sykefraværstilfelle - velger siste vilkårsgrunnlag fra infotrygd`() {
        assertForkastetVilkårsgrunnlag(
            originalJson = "/migrations/178/flere-vilkårsgrunnlag-fra-it_original.json",
            expectedJson = "/migrations/178/flere-vilkårsgrunnlag-fra-it_expected.json"
        )
    }

    @Test
    fun `Velger vilkårsgrunnlag fra infotrygd om vi har utbetalte perioder i Infotrygd rett før sykefraværsperioden`() {
        assertForkastetVilkårsgrunnlag(
            originalJson = "/migrations/178/vilkårsgrunnlag-fra-infotrygd-og-utbetalt-periode-rett-før_original.json",
            expectedJson = "/migrations/178/vilkårsgrunnlag-fra-infotrygd-og-utbetalt-periode-rett-før_expected.json"
        )
    }

    @Test
    fun `Velger vilkårsgrunnlag fra infotrygd om vi har utbetalte perioder i Infotrygd som overlapper sykefraværsperioden`() {
        assertForkastetVilkårsgrunnlag(
            originalJson = "/migrations/178/vilkårsgrunnlag-fra-infotrygd-og-utbetalt-periode-som-overlapper_original.json",
            expectedJson = "/migrations/178/vilkårsgrunnlag-fra-infotrygd-og-utbetalt-periode-som-overlapper_expected.json"
        )
    }

    @Test
    fun `Ignorerer vilkårsgrunnlag fra infotrygd om vi ikke har noen utbetalte perioder i infotrygd`() {
        assertForkastetVilkårsgrunnlag(
            originalJson = "/migrations/178/vilkårsgrunnlag-fra-infotrygd-men-ingen-utbetalinger-i-infotrygd_original.json",
            expectedJson = "/migrations/178/vilkårsgrunnlag-fra-infotrygd-men-ingen-utbetalinger-i-infotrygd_expected.json"
        )
    }

    @Test
    fun `Ignorerer vilkårsgrunnlag fra infotrygd om vi har en utbetalt periode i Infotrygd som hverken er kant-til-kant eller overlapper med sykefraværsperioden`() {
        assertForkastetVilkårsgrunnlag(
            originalJson = "/migrations/178/vilkårsgrunnlag-fra-infotrygd-og-utbetalt-periode-før_original.json",
            expectedJson = "/migrations/178/vilkårsgrunnlag-fra-infotrygd-og-utbetalt-periode-før_expected.json"
        )
    }

    @Test
    fun `Ignorerer vilkårsgrunnlag fra infotrygd hvor sykepengegrunnlaget er 0`() {
        assertForkastetVilkårsgrunnlag(
            originalJson = "/migrations/178/ignorerer-infotrygdgrunnlag-med-sykepengegrunnlag-0_original.json",
            expectedJson = "/migrations/178/ignorerer-infotrygdgrunnlag-med-sykepengegrunnlag-0_expected.json"
        )
    }

    @Test
    fun `Om vi mangler vilkårsgrunnlag fra Spleis skal vi bruke vilkårsgrunnlag fra Infotrygd selv om det ikke er noen relevante Infotrygdutbetalinger`() {
        assertForkastetVilkårsgrunnlag(
            originalJson = "/migrations/178/vilkårsgrunnlag-fra-infotrygd-om-spleis-mangler_original.json",
            expectedJson = "/migrations/178/vilkårsgrunnlag-fra-infotrygd-om-spleis-mangler_expected.json"
        )
    }

    @Test
    fun `Flere skjæringstidspunkt innenfor sykefraværstilfellet`() {
        assertForkastetVilkårsgrunnlag(
            originalJson = "/migrations/178/flere-skjæringstidspunkt-innenfor-sykefraværstilfellet_original.json",
            expectedJson = "/migrations/178/flere-skjæringstidspunkt-innenfor-sykefraværstilfellet_expected.json"
        )
    }

    @Test
    fun `kun vilkårsgrunnlag fra infotrygd med sykepengegrunnlag 0`() {
        assertForkastetVilkårsgrunnlag(
            originalJson = "/migrations/178/kun-vilkårsgrunnlag-fra-infotrygd-med-sykepengegrunnlag-0_original.json",
            expectedJson = "/migrations/178/kun-vilkårsgrunnlag-fra-infotrygd-med-sykepengegrunnlag-0_expected.json"
        )
    }

    @Test
    fun `velger vilkårsgrunnlag fra Spleis om vilkårsgrunnlaget fra infotrygd har sykepengegrunnlag 0`() {
        assertForkastetVilkårsgrunnlag(
            originalJson = "/migrations/178/vilkårsgrunnlag-fra-spleis-om-it-har-sykepengegrunnlag-0_original.json",
            expectedJson = "/migrations/178/vilkårsgrunnlag-fra-spleis-om-it-har-sykepengegrunnlag-0_expected.json"
        )
    }

    @Test
    fun `Vilkårsgrunnlaget fra Spleis ligger på et av flere skjæringstidspunkt innenfor sykefraværstilfellet (ikke første)`() {
        val json = toNode("/migrations/178/vilkårsgrunnlag-fra-spleis-på-at-annet-en-første-skjæringstidspunkt.json".readResource()) as ObjectNode
        assertMigrationRaw(
            originalJson = "${json.put("skjemaVersjon", 177)}",
            expectedJson = "${json.put("skjemaVersjon", 178)}"
        )
    }

    @Test
    fun `Forventet at vi ikke finner vilkårsgrunnlag for perioe i avventer_inntektsmelding_eller_historikk`() {
        val json = toNode("/migrations/178/avventer-inntektsmelding-eller-historikk.json".readResource()) as ObjectNode
        assertMigrationRaw(
            originalJson = "${json.put("skjemaVersjon", 177)}",
            expectedJson = "${json.put("skjemaVersjon", 178)}"
        )
    }

    @Test
    fun `Forventet at vi ikke finner vilkårsgrunnlag for perioe i avventer_inntektsmelding_eller_historikk uten historikk`() {
        val json = toNode("/migrations/178/avventer-inntektsmelding-eller-historikk-uten-historikk.json".readResource()) as ObjectNode
        assertMigrationRaw(
            originalJson = "${json.put("skjemaVersjon", 177)}",
            expectedJson = "${json.put("skjemaVersjon", 178)}"
        )
    }

    @Test
    fun `velger vilkårsgrunnlag fra Spleis om det fra Infotrygd er etter siste dag i Spleis`() {
        assertForkastetVilkårsgrunnlag(
            originalJson = "/migrations/178/leter-først-frem-til-siste-dag-i-spleis_original.json",
            expectedJson = "/migrations/178/leter-først-frem-til-siste-dag-i-spleis_expected.json"
        )
    }

    @Test
    fun `kun legge til et vilkårsgrunnlag per skjæringstidspunkt`() {
        assertForkastetVilkårsgrunnlag(
            originalJson = "/migrations/178/duplikate_skjæringstidspunkt_original.json",
            expectedJson = "/migrations/178/duplikate_skjæringstidspunkt_expected.json"
        )
    }
    @Test
    fun `velger vilkårsgrunnlag fra Infotrygd også om det ligger første ukedag etter sykefraværstilfellet opphører`() {
        assertForkastetVilkårsgrunnlag(
            originalJson = "/migrations/178/vilkårsgrunnlag-fra-infotrygd-dagen-etter-sykefraværstilfellet_original.json",
            expectedJson = "/migrations/178/vilkårsgrunnlag-fra-infotrygd-dagen-etter-sykefraværstilfellet_expected.json"
        )
    }

    private fun assertForkastetVilkårsgrunnlag(originalJson: String, expectedJson: String) {
        val migrert = migrer(originalJson.readResource())
        val sisteInnslag = migrert.path("vilkårsgrunnlagHistorikk")[0]
        val sisteGrunnlag = sisteInnslag.path("vilkårsgrunnlag").lastOrNull()
        val expected = expectedJson.readResource()
            .replace("{id}", sisteInnslag.path("id").asText())
            .replace("{opprettet}", sisteInnslag.path("opprettet").asText())
            .replace("{sisteVilkårsgrunnlagId}", sisteGrunnlag?.path("vilkårsgrunnlagId")?.asText()?:"")
        assertJson(migrert.toString(), expected)
    }
}

