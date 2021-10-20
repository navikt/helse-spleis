package no.nav.helse.serde.migration

import no.nav.helse.serde.serdeObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class V117LeggTilGradPåUgyldigeInfotrygdHistorikkPerioderTest {
    @Test
    fun `Endrer ingenting hvis ugyldige perioder er tom`() {
        val migrering = migrer(originalUtenUgyldigePerioder)
        val forventetResultat = toNode(expectedUtenUgyldigePerioder)
        assertEquals(forventetResultat, migrering)
    }

    @Test
    fun `Migrerer IT historikk med en ugyldig periode`() {
        val migrering = migrer(originalMedEnUgyldigPeriode)
        val forventetResultat = toNode(expectedMedEnUgyldigPeriode)
        assertEquals(forventetResultat, migrering)
    }

    @Test
    fun `Migrerer IT historikk med flere ugyldige perioder`() {
        val migrering = migrer(originalMedFlereUgyldigPerioder)
        val forventetResultat = toNode(expectedMedFlereUgyldigPerioder)
        assertEquals(forventetResultat, migrering)
    }

    @Test
    fun `Migrerer IT historikk med flere innslag i historikken`() {
        val migrering = migrer(originalMedFlereHistorikker)
        val forventetResultat = toNode(expectedMedFlereHistorikker)
        assertEquals(forventetResultat, migrering)
    }

    private fun toNode(json: String) = serdeObjectMapper.readTree(json)

    private fun migrer(json: String) = listOf(V117LeggTilGradPåUgyldigeInfotrygdHistorikkPerioder()).migrate(toNode(json))

    @Language("JSON")
    private val originalUtenUgyldigePerioder = """{
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
        ],
        "skjemaVersjon": 116
    }
    """

    @Language("JSON")
    private val expectedUtenUgyldigePerioder = """{
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
        ],
        "skjemaVersjon": 117
    }
    """

    @Language("JSON")
    private val originalMedEnUgyldigPeriode = """{
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
                "ugyldigePerioder": [
                    {
                        "first": "2017-07-21",
                        "second": "2017-07-20"
                    }
                ],
                "harStatslønn": false,
                "lagretInntekter": false,
                "lagretVilkårsgrunnlag": false,
                "oppdatert": "2021-05-10T11:38:08.238542"
            }
        ],
        "skjemaVersjon": 116
    }
    """

    @Language("JSON")
    private val expectedMedEnUgyldigPeriode = """{
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
                "ugyldigePerioder": [
                    {
                        "fom": "2017-07-21",
                        "tom": "2017-07-20",
                        "utbetalingsgrad": null
                    }
                ],
                "harStatslønn": false,
                "lagretInntekter": false,
                "lagretVilkårsgrunnlag": false,
                "oppdatert": "2021-05-10T11:38:08.238542"
            }
        ],
        "skjemaVersjon": 117
    }
    """

    @Language("JSON")
    private val originalMedFlereUgyldigPerioder = """{
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
                "ugyldigePerioder": [
                    {
                        "first": "2017-07-21",
                        "second": "2017-07-20"
                    },
                    {
                        "first": "2017-09-04",
                        "second": "2017-09-01"
                    },
                    {
                        "first": "2019-03-21",
                        "second": "2018-04-20"
                    }
                ],
                "harStatslønn": false,
                "lagretInntekter": false,
                "lagretVilkårsgrunnlag": false,
                "oppdatert": "2021-05-10T11:38:08.238542"
            }
        ],
        "skjemaVersjon": 116
    }
    """

    @Language("JSON")
    private val expectedMedFlereUgyldigPerioder = """{
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
                "ugyldigePerioder": [
                    {
                        "fom": "2017-07-21",
                        "tom": "2017-07-20",
                        "utbetalingsgrad": null
                    },
                    {
                        "fom": "2017-09-04",
                        "tom": "2017-09-01",
                        "utbetalingsgrad": null
                    },
                    {
                        "fom": "2019-03-21",
                        "tom": "2018-04-20",
                        "utbetalingsgrad": null
                    }
                ],
                "harStatslønn": false,
                "lagretInntekter": false,
                "lagretVilkårsgrunnlag": false,
                "oppdatert": "2021-05-10T11:38:08.238542"
            }
        ],
        "skjemaVersjon": 117
    }
    """

    @Language("JSON")
    private val originalMedFlereHistorikker = """{
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
                "ugyldigePerioder": [
                    {
                        "first": "2017-07-21",
                        "second": "2017-07-20"
                    }
                ],
                "harStatslønn": false,
                "lagretInntekter": false,
                "lagretVilkårsgrunnlag": false,
                "oppdatert": "2021-05-10T11:38:08.238542"
            },
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
            },
            {
                "id": "b8d73ae5-5ca3-4553-aeed-755bd88b30a1",
                "tidsstempel": "2021-05-10T11:38:08.099234",
                "hendelseId": "9ce5967f-d335-4342-8530-116533465498",
                "ferieperioder": [],
                "utbetalingsperioder": [],
                "ukjenteperioder": [],
                "inntekter": [],
                "arbeidskategorikoder": {},
                "ugyldigePerioder": [
                    {
                        "first": "2017-09-04",
                        "second": "2017-09-01"
                    },
                    {
                        "first": "2019-03-21",
                        "second": "2018-04-20"
                    }
                ],
                "harStatslønn": false,
                "lagretInntekter": false,
                "lagretVilkårsgrunnlag": false,
                "oppdatert": "2021-05-10T11:38:08.238542"
            }
        ],
        "skjemaVersjon": 116
    }
    """

    @Language("JSON")
    private val expectedMedFlereHistorikker = """{
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
                "ugyldigePerioder": [
                    {
                        "fom": "2017-07-21",
                        "tom": "2017-07-20",
                        "utbetalingsgrad": null
                    }
                ],
                "harStatslønn": false,
                "lagretInntekter": false,
                "lagretVilkårsgrunnlag": false,
                "oppdatert": "2021-05-10T11:38:08.238542"
            },
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
            },
            {
                "id": "b8d73ae5-5ca3-4553-aeed-755bd88b30a1",
                "tidsstempel": "2021-05-10T11:38:08.099234",
                "hendelseId": "9ce5967f-d335-4342-8530-116533465498",
                "ferieperioder": [],
                "utbetalingsperioder": [],
                "ukjenteperioder": [],
                "inntekter": [],
                "arbeidskategorikoder": {},
                "ugyldigePerioder": [
                    {
                        "fom": "2017-09-04",
                        "tom": "2017-09-01",
                        "utbetalingsgrad": null
                    },
                    {
                        "fom": "2019-03-21",
                        "tom": "2018-04-20",
                        "utbetalingsgrad":  null
                    }
                ],
                "harStatslønn": false,
                "lagretInntekter": false,
                "lagretVilkårsgrunnlag": false,
                "oppdatert": "2021-05-10T11:38:08.238542"
            }
        ],
        "skjemaVersjon": 117
    }
    """
}
