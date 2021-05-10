package no.nav.helse.serde.migration

import no.nav.helse.serde.serdeObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class V98SletterITCacheMedUtbetalingsperioderTest {
    @Test
    fun `historikk uten utbetalingsperioder`() {
        assertEquals(toNode(expectedJsonIngenUtbetalingsperioder), migrer(originalJsonIngenUtbetalingsperioder))
    }

    @Test
    fun `historikk med utbetalingsperioder`() {
        assertEquals(toNode(expectedJsonMedUtbetalingsperioder), migrer(originalJsonMedUtbetalingsperioder))
    }

    @Test
    fun `historikk både med og uten utbetalingsperioder`() {
        assertEquals(toNode(expectedJsonBådeMedOgUtenUtbetalingsperioder), migrer(originalJsonBådeMedOgUtenUtbetalingsperioder))
    }

    private fun toNode(json: String) = serdeObjectMapper.readTree(json)

    private fun migrer(json: String) = listOf(V98SletterITCacheMedUtbetalingsperioder()).migrate(toNode(json))
}

@Language("JSON")
private val originalJsonIngenUtbetalingsperioder = """
{
  "infotrygdhistorikk": [
    {
        "id": "b8d73ae5-5ca3-4553-aeed-755bd88b30a1",
        "tidsstempel": "2021-05-10T11:38:08.099234",
        "hendelseId": "9ce5967f-d335-4342-8530-116533465498",
        "ferieperioder": [],
        "utbetalingsperioder": [],
        "ukjenteperioder": [],
        "inntekter": [],
        "arbeidskategorikoder": {},
        "ugyldigePerioder": [],
        "harStatslønn": false,
        "lagretInntekter": false,
        "lagretVilkårsgrunnlag": false,
        "oppdatert": "2021-05-10T11:38:08.238542"
    }
  ]
}
"""

@Language("JSON")
private val expectedJsonIngenUtbetalingsperioder = """
{
  "infotrygdhistorikk": [
    {
        "id": "b8d73ae5-5ca3-4553-aeed-755bd88b30a1",
        "tidsstempel": "2021-05-10T11:38:08.099234",
        "hendelseId": "9ce5967f-d335-4342-8530-116533465498",
        "ferieperioder": [],
        "arbeidsgiverutbetalingsperioder": [],
        "personutbetalingsperioder": [],
        "ukjenteperioder": [],
        "inntekter": [],
        "arbeidskategorikoder": {},
        "ugyldigePerioder": [],
        "harStatslønn": false,
        "lagretInntekter": false,
        "lagretVilkårsgrunnlag": false,
        "oppdatert": "2021-05-10T11:38:08.238542"
    }
  ],
  "skjemaVersjon": 98
}
"""

@Language("JSON")
private val originalJsonMedUtbetalingsperioder = """
{
  "infotrygdhistorikk": [
    {
        "id": "b8d73ae5-5ca3-4553-aeed-755bd88b30a1",
        "tidsstempel": "2021-05-10T11:38:08.099234",
        "hendelseId": "9ce5967f-d335-4342-8530-116533465498",
        "ferieperioder": [],
        "utbetalingsperioder": [
          "ikke tom"
        ],
        "ukjenteperioder": [],
        "inntekter": [],
        "arbeidskategorikoder": {},
        "ugyldigePerioder": [],
        "harStatslønn": false,
        "lagretInntekter": false,
        "lagretVilkårsgrunnlag": false,
        "oppdatert": "2021-05-10T11:38:08.238542"
    }
  ]
}
"""

@Language("JSON")
private val expectedJsonMedUtbetalingsperioder = """
{
  "infotrygdhistorikk": [],
  "skjemaVersjon": 98
}
"""


@Language("JSON")
private val originalJsonBådeMedOgUtenUtbetalingsperioder = """
{
  "infotrygdhistorikk": [
    {
        "id": "b8d73ae5-5ca3-4553-aeed-755bd88b30a1",
        "tidsstempel": "2021-05-10T11:38:08.099234",
        "hendelseId": "9ce5967f-d335-4342-8530-116533465498",
        "ferieperioder": [],
        "utbetalingsperioder": [
          "ikke tom"
        ],
        "ukjenteperioder": [],
        "inntekter": [],
        "arbeidskategorikoder": {},
        "ugyldigePerioder": [],
        "harStatslønn": false,
        "lagretInntekter": false,
        "lagretVilkårsgrunnlag": false,
        "oppdatert": "2021-05-10T11:38:08.238542"
    },
    {
        "id": "b8d73ae5-5ca3-4553-aeed3-755bd88b30a1",
        "tidsstempel": "2021-05-11T11:38:08.099234",
        "hendelseId": "9ce5967f-d335-4342-8530-116533465498",
        "ferieperioder": [],
        "utbetalingsperioder": [
          "ikke tom"
        ],
        "ukjenteperioder": [],
        "inntekter": [],
        "arbeidskategorikoder": {},
        "ugyldigePerioder": [],
        "harStatslønn": true,
        "lagretInntekter": true,
        "lagretVilkårsgrunnlag": true,
        "oppdatert": "2021-05-11T11:38:08.238542"
    },
    {
        "id": "b8d73ae5-5ca3-4553-aeed-755bd88b30a1",
        "tidsstempel": "2021-05-12T11:38:08.099234",
        "hendelseId": "9ce5967f-d335-4342-8530-116533465498",
        "ferieperioder": [],
        "utbetalingsperioder": [],
        "ukjenteperioder": [],
        "inntekter": [],
        "arbeidskategorikoder": {},
        "ugyldigePerioder": [],
        "harStatslønn": false,
        "lagretInntekter": false,
        "lagretVilkårsgrunnlag": false,
        "oppdatert": "2021-05-12T11:38:08.238542"
    }
  ]
}
"""

@Language("JSON")
private val expectedJsonBådeMedOgUtenUtbetalingsperioder = """
{
  "infotrygdhistorikk": [
    {
        "id": "b8d73ae5-5ca3-4553-aeed-755bd88b30a1",
        "tidsstempel": "2021-05-12T11:38:08.099234",
        "hendelseId": "9ce5967f-d335-4342-8530-116533465498",
        "ferieperioder": [],
        "arbeidsgiverutbetalingsperioder": [],
        "personutbetalingsperioder": [],
        "ukjenteperioder": [],
        "inntekter": [],
        "arbeidskategorikoder": {},
        "ugyldigePerioder": [],
        "harStatslønn": false,
        "lagretInntekter": false,
        "lagretVilkårsgrunnlag": false,
        "oppdatert": "2021-05-12T11:38:08.238542"
    }
  ],
  "skjemaVersjon": 98
}
"""
