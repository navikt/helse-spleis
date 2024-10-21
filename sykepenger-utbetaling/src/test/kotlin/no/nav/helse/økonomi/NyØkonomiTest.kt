package no.nav.helse.økonomi

import no.nav.helse.Grunnbeløp.Companion.`6G`
import no.nav.helse.mai
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Inntekt.Companion.summer
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import no.nav.helse.økonomi.UberegnetØkonomi.Companion.beregn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NyØkonomiTest {

    @Test
    fun `en arbeidsgiver med tilkommet inntekt`() {
        val grunnbeløp = `6G`.beløp(1.mai(2024))
        val actual = UberegnetØkonomi(90000.månedlig, 90000.månedlig, 100.prosent)
        val tilkommet = 40451.månedlig
        val result = actual.beregn(grunnbeløp, tilkommet)
        val expected = BeregnetØkonomi(995.daglig, INGEN)
        assertEquals(expected, result)
    }

    @Test
    fun `to arbeidsgivere med tilkommet inntekt`() {
        val grunnbeløp = `6G`.beløp(1.mai(2024))
        val actual = listOf(
            UberegnetØkonomi(45000.månedlig, 45000.månedlig, 100.prosent),
            UberegnetØkonomi(45000.månedlig, 45000.månedlig, 100.prosent)
        )
        val tilkommet = 40451.månedlig
        val result = actual.beregn(grunnbeløp, tilkommet)
        val expected = listOf(
            BeregnetØkonomi(498.daglig, INGEN),
            BeregnetØkonomi(498.daglig, INGEN)
        )
        assertEquals(expected, result)
    }
}

data class UberegnetØkonomi(
    val inntektPåSkjæringstidspunktet: Inntekt,
    val ønsketRefusjon: Inntekt,
    val grad: Prosentdel
) {
    fun beregn(`6G`: Inntekt, tilkommet: Inntekt): BeregnetØkonomi {
        return listOf(this).beregn(`6G`, tilkommet).single()
    }

    fun begrensTil6G(inntektavkorting: Prosentdel, refusjonavkorting: Prosentdel, delAvTilkommet: Inntekt): `6GBegrensetØkonomi` {
        return `6GBegrensetØkonomi`(
            økonomi = this,
            inntektandel = this.inntektPåSkjæringstidspunktet * inntektavkorting,
            refusjonandel = this.ønsketRefusjon * refusjonavkorting,
            delAvTilkommet = delAvTilkommet
        )
    }

    companion object {
        fun List<UberegnetØkonomi>.beregn(`6G`: Inntekt, tilkommet: Inntekt): List<BeregnetØkonomi> {
            val inntektsgrunnlagavkorting = begrensTil6G(`6G`, UberegnetØkonomi::inntektPåSkjæringstidspunktet)
            val refusjonsgrunnlagavkorting = begrensTil6G(`6G`, UberegnetØkonomi::ønsketRefusjon)

            val tilkommetPerArbeidsgiver = tilkommet / size

            return this
                .map { it.begrensTil6G(inntektsgrunnlagavkorting, refusjonsgrunnlagavkorting, tilkommetPerArbeidsgiver) }
                .fordel()
        }

        private fun List<`6GBegrensetØkonomi`>.fordel(): List<BeregnetØkonomi> {
            val totalGradertRefusjon = map { it.gradertRefusjon }.summer()
            val totalGradertPersonbeløp = map { it.gradertPersonutbetaling }.summer()
            val totalTaptInntekt = map { it.taptInntekt }.summer()

            return this
                .fordelRefusjon(totalTaptInntekt, totalGradertRefusjon)
                .fordelPerson(totalTaptInntekt, totalGradertPersonbeløp)
        }

        private fun List<`6GBegrensetØkonomi`>.fordelRefusjon(totalTaptInntekt: Inntekt, totalGradertRefusjon: Inntekt): List<FordeltRefusjon> {
            // fordeler refusjonsbeløpene
            val fordeltRefusjon = fordel(totalGradertRefusjon, totalTaptInntekt, `6GBegrensetØkonomi`::fordelRefusjon)

            // TODO: <her må vi legge inn tildeling av 1kr pga øreavrunding-differanse-tingen>

            return fordeltRefusjon
        }

        private fun List<FordeltRefusjon>.fordelPerson(totalTaptInntekt: Inntekt, totalGradertPersonbeløp: Inntekt): List<BeregnetØkonomi> {
            val totalFordeltRefusjon = map { it.refusjonsbeløp }.summer()
            // pga avrunding så kjører vi først en runde hvor vi fordeler ut refusjon,
            // og så ser vi summen av avrundet refusjon etterpå
            val totalFordeltPersonbeløp = maxOf(INGEN, totalTaptInntekt - totalFordeltRefusjon)

            // fordeler personbeløpene
            return fordel(totalGradertPersonbeløp, totalFordeltPersonbeløp, FordeltRefusjon::fordelPersonutbetaling)
        }

        private fun <T, R> List<T>.fordel(budsjett: Inntekt, total: Inntekt, strategi: T.(Prosentdel) -> R): List<R> {
            val ratio = when {
                budsjett == INGEN -> 0.prosent
                total > budsjett -> 100.prosent
                else -> total ratio budsjett
            }

            // fordeler personbeløpene
            return map { it.strategi(ratio) }
        }

        private fun List<UberegnetØkonomi>.begrensTil6G(`6G`: Inntekt, strategi: (UberegnetØkonomi) -> Inntekt): Prosentdel {
            val total = map { strategi(it) }.summer()
            return minOf(`6G`, total) ratio total
        }
    }
}

data class `6GBegrensetØkonomi`(
    val økonomi: UberegnetØkonomi,
    val inntektandel: Inntekt,
    val refusjonandel: Inntekt,
    val delAvTilkommet: Inntekt
) {
    val gradertInntekt = inntektandel * økonomi.grad
    val taptInntekt = maxOf(INGEN, gradertInntekt - delAvTilkommet)

    val gradertRefusjon = refusjonandel * økonomi.grad
    val gradertPersonutbetaling = maxOf(INGEN, taptInntekt - gradertRefusjon)

    fun fordelRefusjon(refusjonavkorting: Prosentdel): FordeltRefusjon {
        return FordeltRefusjon(
            refusjonsbeløp = (gradertRefusjon * refusjonavkorting).rundTilDaglig(),
            ufordeltPersonbeløp = this.gradertPersonutbetaling
        )
    }

    companion object {
        // total sykdomgrad regnes ut ved å se på summen av gradert inntekt delt på summen av inntekter.
        // inntekt kan enten være begrenset av 6G eller ikke, graden blir lik uansett.
        // dvs. regnestykket:
        //      sumOf { it.inntektPåSkjæringstidspunktet * grad } / totalInntekt
        // er samme som
        //      sumOf { it.begrensetTil6GInntekt * grad } / totalInntektBegrensetTil6G
        fun List<`6GBegrensetØkonomi`>.totalSykdomsgrad() =
            map { it.gradertInntekt }.summer() ratio map { it.inntektandel }.summer()
    }
}

data class FordeltRefusjon(
    val refusjonsbeløp: Inntekt,
    val ufordeltPersonbeløp: Inntekt
) {
    fun fordelPersonutbetaling(personavkortingratio: Prosentdel) =
        BeregnetØkonomi(
            refusjonsbeløp = this.refusjonsbeløp,
            personbeløp = (ufordeltPersonbeløp * personavkortingratio).rundTilDaglig()
        )
}

data class BeregnetØkonomi(
    val refusjonsbeløp: Inntekt,
    val personbeløp: Inntekt
)