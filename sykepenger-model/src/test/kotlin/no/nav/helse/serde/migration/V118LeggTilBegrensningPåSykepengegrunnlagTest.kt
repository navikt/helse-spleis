package no.nav.helse.serde.migration

import no.nav.helse.serde.serdeObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class V118LeggTilBegrensningPåSykepengegrunnlagTest {

    private val ER_6G_BEGRENSET = "ER_6G_BEGRENSET"
    private val ER_IKKE_6G_BEGRENSET = "ER_IKKE_6G_BEGRENSET"
    private val VURDERT_I_INFOTRYGD = "VURDERT_I_INFOTRYGD"

    @Test
    fun `migreres til ER_IKKE_6G_BEGRENSET dersom grunnlag for sykepengegrunnlag ikke er større enn 6G`() {
        val grunnlagForSykepengegrunnlag = 200000
        val type = "Vilkårsprøving"

        val migrert = migrer(originalMelding(grunnlagForSykepengegrunnlag, type))

        val expected = toNode(forventetMelding(grunnlagForSykepengegrunnlag, type, ER_IKKE_6G_BEGRENSET))

        Assertions.assertEquals(expected, migrert)
    }

    @Test
    fun `migreres til ER_6G_BEGRENSET dersom grunnlag for sykepengegrunnlag er større enn 6G`() {
        val grunnlagForSykepengegrunnlag = 800000
        val type = "Vilkårsprøving"

        val migrert = migrer(originalMelding(grunnlagForSykepengegrunnlag, type))

        val expected = toNode(forventetMelding(grunnlagForSykepengegrunnlag, type, ER_6G_BEGRENSET))

        Assertions.assertEquals(expected, migrert)
    }

    @Test
    fun `migreres til VURDERT_I_INFOTRYGD dersom vilkårsprøvingen er gjort i Infotrygd og grunnlag for sykepengegrunnlag er over 6G`() {
        val grunnlagForSykepengegrunnlag = 800000
        val type = "Infotrygd"

        val migrert = migrer(originalMelding(grunnlagForSykepengegrunnlag, type))


        val expected = toNode(forventetMelding(grunnlagForSykepengegrunnlag, type, VURDERT_I_INFOTRYGD))

        Assertions.assertEquals(expected, migrert)
    }

    @Test
    fun `migreres til VURDERT_I_INFOTRYGD dersom vilkårsprøvingen er gjort i Infotrygd`() {
        val grunnlagForSykepengegrunnlag = 200000
        val type = "Infotrygd"

        val migrert = migrer(originalMelding(grunnlagForSykepengegrunnlag, type))


        val expected = toNode(forventetMelding(grunnlagForSykepengegrunnlag, type, VURDERT_I_INFOTRYGD))

        Assertions.assertEquals(expected, migrert)
    }


    private fun toNode(json: String) = serdeObjectMapper.readTree(json)
    private fun migrer(json: String) = listOf(V118LeggTilBegrensningPåSykepengegrunnlag()).migrate(toNode(json))

    @Language("JSON")
    fun originalMelding(grunnlagForSykepengegrunnlag: Int, type: String) = """{
        "vilkårsgrunnlagHistorikk": [
            {
                "vilkårsgrunnlag": [
                    {
                        "skjæringstidspunkt": "2021-01-01",
                        "type": "$type",
                        "sykepengegrunnlag": {
                            "grunnlagForSykepengegrunnlag": $grunnlagForSykepengegrunnlag
                        }
                    },
                    {
                        "skjæringstidspunkt": "2021-06-01",
                        "type": "$type",
                        "sykepengegrunnlag": {
                            "grunnlagForSykepengegrunnlag": $grunnlagForSykepengegrunnlag
                        }
                    }
                ]
            },
            {
                "vilkårsgrunnlag": [
                    {
                        "skjæringstidspunkt": "2021-01-01",
                        "type": "$type",
                        "sykepengegrunnlag": {
                            "grunnlagForSykepengegrunnlag": $grunnlagForSykepengegrunnlag
                        }
                    },
                    {
                        "skjæringstidspunkt": "2021-06-01",
                        "type": "$type",
                        "sykepengegrunnlag": {
                            "grunnlagForSykepengegrunnlag": $grunnlagForSykepengegrunnlag
                        }
                    }
                ]
            }
        ],
        "skjemaVersjon": 117
    }"""

    @Language("JSON")
    fun forventetMelding(grunnlagForSykepengegrunnlag: Int, type: String, begrensning: String) = """{
        "vilkårsgrunnlagHistorikk": [
            {
                "vilkårsgrunnlag": [
                    {
                        "skjæringstidspunkt": "2021-01-01",
                        "type": "$type",
                        "sykepengegrunnlag": {
                            "grunnlagForSykepengegrunnlag": $grunnlagForSykepengegrunnlag,
                            "begrensning": "$begrensning"
                        }
                    },
                    {
                        "skjæringstidspunkt": "2021-06-01",
                        "type": "$type",
                        "sykepengegrunnlag": {
                            "grunnlagForSykepengegrunnlag": $grunnlagForSykepengegrunnlag,
                            "begrensning": "$begrensning"
                        }
                    }
                ]
            },
            {
                "vilkårsgrunnlag": [
                    {
                        "skjæringstidspunkt": "2021-01-01",
                        "type": "$type",
                        "sykepengegrunnlag": {
                            "grunnlagForSykepengegrunnlag": $grunnlagForSykepengegrunnlag,
                            "begrensning": "$begrensning"
                        }
                    },
                    {
                        "skjæringstidspunkt": "2021-06-01",
                        "type": "$type",
                        "sykepengegrunnlag": {
                            "grunnlagForSykepengegrunnlag": $grunnlagForSykepengegrunnlag,
                            "begrensning": "$begrensning"
                        }
                    }
                ]
            }
        ],
        "skjemaVersjon": 118
    }"""
}

