package no.nav.helse.person.vilkår

import no.nav.helse.hendelser.Periode.Companion.merge
import no.nav.helse.person.AktivitetsloggVisitor
import no.nav.helse.person.UtbetalingsdagVisitor
import no.nav.helse.person.vilkår.Etterlevelse.TidslinjegrunnlagVisitor.Periode.Companion.dager
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Prosent
import no.nav.helse.økonomi.Økonomi
import org.intellij.lang.annotations.Language
import java.time.LocalDate

class Etterlevelse {
    private val resultater = mutableListOf<Vurderingsresultat>()

    internal fun accept(visitor: EtterlevelseVisitor) {
        resultater.forEach { it.accept(visitor) }
    }

    fun `§2`(oppfylt: Boolean) {
    }

    fun `§8-2 ledd 1`(
        oppfylt: Boolean,
        skjæringstidspunkt: LocalDate,
        tilstrekkeligAntallOpptjeningsdager: Int,
        arbeidsforhold: List<Map<String, Any?>>
    ) {
        resultater.add(
            Vurderingsresultat(
                oppfylt = oppfylt,
                versjon = LocalDate.of(2020, 6, 12),
                paragraf = "8-2",
                ledd = "1",
                inputdata = mapOf(
                    "skjæringstidspunkt" to skjæringstidspunkt,
                    "tilstrekkeligAntallOpptjeningsdager" to tilstrekkeligAntallOpptjeningsdager,
                    "arbeidsforhold" to arbeidsforhold
                ),
                outputdata = null
            )
        )
    }

    fun `§8-3 ledd 2`(
        oppfylt: Boolean,
        skjæringstidspunkt: LocalDate,
        grunnlagForSykepengegrunnlag: Inntekt,
        minimumInntekt: Inntekt
    ) {
        resultater.add(
            Vurderingsresultat(
                oppfylt = oppfylt,
                versjon = LocalDate.of(2011, 12, 16),
                paragraf = "8-3",
                ledd = "2",
                inputdata = mapOf(
                    "skjæringstidspunkt" to skjæringstidspunkt,
                    "grunnlagForSykepengegrunnlag" to grunnlagForSykepengegrunnlag.reflection { årlig, _, _, _ -> årlig },
                    "minimumInntekt" to minimumInntekt.reflection { årlig, _, _, _ -> årlig }
                ),
                outputdata = null
            )
        )
    }

    internal fun `§8-12 ledd 1`(
        oppfylt: Boolean,
        fom: LocalDate,
        tom: LocalDate,
        tidslinjegrunnlag: List<Utbetalingstidslinje>,
        beregnetTidslinje: Utbetalingstidslinje,
        gjenståendeSykedager: Int,
        forbrukteSykedager: Int,
        maksdato: LocalDate,
        avvisteDager: List<LocalDate>
    ) {
        resultater.add(
            Vurderingsresultat(
                oppfylt = oppfylt,
                versjon = LocalDate.of(2021, 5, 21),
                paragraf = "8-12",
                ledd = "1",
                inputdata = mapOf(
                    "fom" to fom,
                    "tom" to tom,
                    "tidslinjegrunnlag" to tidslinjegrunnlag.map { TidslinjegrunnlagVisitor(it).dager() },
                    "beregnetTidslinje" to TidslinjegrunnlagVisitor(beregnetTidslinje).dager()
                ),
                outputdata = mapOf(
                    "gjenståendeSykedager" to gjenståendeSykedager,
                    "forbrukteSykedager" to forbrukteSykedager,
                    "maksdato" to maksdato,
                    "avvisteDager" to avvisteDager.merge()
                )
            )
        )
    }

    internal fun `§8-12 ledd 2`(
        dato: LocalDate,
        tilstrekkeligOppholdISykedager: Int,
        tidslinjegrunnlag: List<Utbetalingstidslinje>,
        beregnetTidslinje: Utbetalingstidslinje
    ) {
        resultater.add(
            Vurderingsresultat(
                oppfylt = true,
                versjon = LocalDate.of(2021, 5, 21),
                paragraf = "8-12",
                ledd = "2",
                inputdata = mapOf(
                    "dato" to dato,
                    "tilstrekkeligOppholdISykedager" to tilstrekkeligOppholdISykedager,
                    "tidslinjegrunnlag" to tidslinjegrunnlag.map { TidslinjegrunnlagVisitor(it).dager() },
                    "beregnetTidslinje" to TidslinjegrunnlagVisitor(beregnetTidslinje).dager()
                ),
                outputdata = null
            )
        )
    }

    fun `§8-30 ledd 2`(
        oppfylt: Boolean,
        maksimaltTillattAvvikPåÅrsinntekt: Prosent,
        grunnlagForSykepengegrunnlag: Inntekt,
        sammenligningsgrunnlag: Inntekt,
        avvik: Prosent
    ) {
        resultater.add(
            Vurderingsresultat(
                oppfylt = oppfylt,
                versjon = LocalDate.of(2017, 4, 5),
                paragraf = "8-30",
                ledd = "2",
                inputdata = mapOf(
                    "maksimaltTillattAvvikPåÅrsinntekt" to maksimaltTillattAvvikPåÅrsinntekt.prosent(),
                    "grunnlagForSykepengegrunnlag" to grunnlagForSykepengegrunnlag.reflection { årlig, _, _, _ -> årlig },
                    "sammenligningsgrunnlag" to sammenligningsgrunnlag.reflection { årlig, _, _, _ -> årlig }
                ),
                outputdata = mapOf(
                    "avvik" to avvik.prosent()
                )
            )
        )
    }

    fun `§8-51 ledd 2`(
        oppfylt: Boolean,
        skjæringstidspunkt: LocalDate,
        grunnlagForSykepengegrunnlag: Inntekt,
        minimumInntekt: Inntekt
    ) {
        resultater.add(
            Vurderingsresultat(
                oppfylt = oppfylt,
                versjon = LocalDate.of(2011, 12, 16),
                paragraf = "8-51",
                ledd = "2",
                inputdata = mapOf(
                    "skjæringstidspunkt" to skjæringstidspunkt,
                    "grunnlagForSykepengegrunnlag" to grunnlagForSykepengegrunnlag.reflection { årlig, _, _, _ -> årlig },
                    "minimumInntekt" to minimumInntekt.reflection { årlig, _, _, _ -> årlig }
                ),
                outputdata = null
            )
        )
    }

    internal interface EtterlevelseVisitor : AktivitetsloggVisitor {
        fun visitVurderingsresultat(oppfylt: Boolean, versjon: LocalDate, paragraf: String, ledd: String, inputdata: Any?, outputdata: Any?) {}
    }

    internal class Vurderingsresultat(
        private val oppfylt: Boolean,
        private val versjon: LocalDate,
        private val paragraf: String,
        private val ledd: String,
        private val inputdata: Any?,
        private val outputdata: Any?
    ) {
        internal fun accept(visitor: EtterlevelseVisitor) {
            visitor.visitVurderingsresultat(oppfylt, versjon, paragraf, ledd, inputdata, outputdata)
        }
    }

    private class TidslinjegrunnlagVisitor(utbetalingstidslinje: Utbetalingstidslinje) : UtbetalingsdagVisitor {
        private val navdager = mutableListOf<Periode>()
        private var forrigeDato: LocalDate? = null

        private class Periode(
            val fom: LocalDate,
            var tom: LocalDate,
            val dagtype: String
        ) {
            companion object {
                fun List<Periode>.dager() = map {
                    mapOf(
                        "fom" to it.fom,
                        "tom" to it.tom,
                        "dagtype" to it.dagtype
                    )
                }
            }
        }

        init {
            utbetalingstidslinje.accept(this)
        }

        fun dager() = navdager.dager()

        override fun visit(dag: Utbetalingstidslinje.Utbetalingsdag.NavDag, dato: LocalDate, økonomi: Økonomi) {
            visit(dato, "NAVDAG")
        }

        override fun visit(dag: Utbetalingstidslinje.Utbetalingsdag.NavHelgDag, dato: LocalDate, økonomi: Økonomi) {
            visit(dato, "NAVDAG")
        }

        override fun visit(dag: Utbetalingstidslinje.Utbetalingsdag.Fridag, dato: LocalDate, økonomi: Økonomi) {
            if (forrigeDato != null && forrigeDato?.plusDays(1) == dato) visit(dato, "FRIDAG")
        }

        private fun visit(dato: LocalDate, dagtype: String) {
            forrigeDato = dato
            if (navdager.isEmpty() || dagtype != navdager.last().dagtype || navdager.last().tom.plusDays(1) != dato) {
                navdager.add(Periode(dato, dato, dagtype))
            } else {
                navdager.last().tom = dato
            }
        }
    }
}

@Language("JSON")
private val test = """[{
        "fom": "2020-10-01",
        "tom": "2021-02-01",
        "oppfylt": false,
        "folketrygdlovenVersjon": "2021-06-01",
        "paragraf": "8-12",
        "ledd": 1,
        "data": {
            "type": "ARBEIDSTAKER",
            "sykedager": [
                {"fom": "2020-10-01", "tom": "2020-10-31", "dagtype": "NAVDAG"},
                {"fom": "2021-01-01", "tom": "2021-02-28", "dagtype": "NAVDAG"}
            ],
            "antallForbrukteDager": 90,
            "antallGjenståendeDager": 158,
            "maksdato": "2021-12-01"
        }
    },{
        "fom": "2021-02-02",
        "tom": "2021-02-28",
        "oppfylt": true,
        "folketrygdlovenVersjon": "2021-06-01",
        "paragraf": "8-12",
        "ledd": 1,
        "data": {
            "type": "ARBEIDSTAKER",
            "sykedager": [
                {"fom": "2020-10-01", "tom": "2020-10-31", "dagtype": "NAVDAG"},
                {"fom": "2021-01-01", "tom": "2021-02-28", "dagtype": "NAVDAG"}
            ],
            "antallForbrukteDager": 90,
            "antallGjenståendeDager": 158,
            "maksdato": "2021-12-01"
        }
    },
    {
        "fom": "2021-03-01",
        "tom": "2021-03-31",
        "oppfylt": false,
        "folketrygdlovenVersjon": "2021-06-01",
        "paragraf": "8-12",
        "ledd": 1,
        "data": {
            "type": "ARBEIDSTAKER",
            "sykedager": [
                {"fom": "2020-10-01", "tom": "2020-10-31", "dagtype": "NAVDAG"},
                {"fom": "2021-01-01", "tom": "2021-03-31", "dagtype": "NAVDAG"}
            ],
            "antallForbrukteDager": 148,
            "antallGjenståendeDager": 0,
            "maksdato": "2021-02-28"
        }
    }
]"""
