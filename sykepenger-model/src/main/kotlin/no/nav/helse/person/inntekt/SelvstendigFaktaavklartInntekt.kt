package no.nav.helse.person.inntekt

import java.time.Year
import java.util.UUID
import no.nav.helse.Grunnbeløp.Companion.`1G`
import no.nav.helse.dto.deserialisering.SelvstendigFaktaavklartInntektInnDto
import no.nav.helse.dto.serialisering.SelvstendigFaktaavklartInntektUtDto
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.summer
import no.nav.helse.økonomi.Inntekt.Companion.årlig

internal data class SelvstendigFaktaavklartInntekt(
    val id: UUID,
    val inntektsdata: Inntektsdata,
    val pensjonsgivendeInntekter: List<PensjonsgivendeInntekt>,
    val anvendtGrunnbeløp: Inntekt
) {
    init {
        check(pensjonsgivendeInntekter.size <= 3) {
            "Selvstendig kan ikke ha mer enn tre inntekter"
        }
        check(pensjonsgivendeInntekter.distinctBy { it.årstall }.size == pensjonsgivendeInntekter.size) {
            "Selvstendig kan ikke ha flere inntekter med samme årstall"
        }
        pensjonsgivendeInntekter
            .sortedBy { it.årstall }
            .also { sortertListe ->
                sortertListe.forEachIndexed { index, pgi ->
                    if (index > 0) {
                        check(sortertListe[index - 1].årstall == pgi.årstall.minusYears(1)) {
                            "inntektene må være i sekvens; årstallet må øke med 1 for hvert år"
                        }
                    }
                }
            }
    }

    val inntektsgrunnlag = beregnInntektsgrunnlag(anvendtGrunnbeløp)

    internal fun funksjoneltLik(other: SelvstendigFaktaavklartInntekt): Boolean {
        if (!this.inntektsdata.funksjoneltLik(other.inntektsdata)) return false
        return this.pensjonsgivendeInntekter == other.pensjonsgivendeInntekter
    }

    internal fun dto() = SelvstendigFaktaavklartInntektUtDto(
        id = this.id,
        inntektsdata = this.inntektsdata.dto(),
        pensjonsgivendeInntekter = this.pensjonsgivendeInntekter.map {
            SelvstendigFaktaavklartInntektUtDto.PensjonsgivendeInntektDto(it.årstall, it.beløp.dto())
        },
        anvendtGrunnbeløp = this.anvendtGrunnbeløp.dto()
    )

    fun beregnInntektsgrunnlag(anvendtGrunnbeløp: Inntekt) =
        Companion.beregnInntektsgrunnlag(pensjonsgivendeInntekter, anvendtGrunnbeløp)

    internal companion object {
        internal fun gjenopprett(dto: SelvstendigFaktaavklartInntektInnDto) = SelvstendigFaktaavklartInntekt(
            id = dto.id,
            inntektsdata = Inntektsdata.gjenopprett(dto.inntektsdata),
            pensjonsgivendeInntekter = dto.pensjonsgivendeInntekter.map {
                PensjonsgivendeInntekt(it.årstall, Inntekt.gjenopprett(it.beløp))
            },
            anvendtGrunnbeløp = Inntekt.gjenopprett(dto.anvendtGrunnbeløp)
        )

        fun beregnInntektsgrunnlag(
            inntekter: List<PensjonsgivendeInntekt>,
            anvendtGrunnbeløp: Inntekt
        ) =
            inntekter
                .map { it.justertÅrsgrunnlag(anvendtGrunnbeløp) }
                .summer()
                .årlig.toInt()
                .årlig
    }

    data class PensjonsgivendeInntekt(val årstall: Year, val beløp: Inntekt) {
        internal val snitt = `1G`.snitt(årstall.value)

        // hvor mange G inntekten utgjør
        private val antallG = beløp.årlig / snitt.årlig

        // alle inntekter opp til 6g
        private val inntekterOppTil6g = antallG.coerceAtMost(SEKS_G)

        // 1/3 av inntekter mellom 6g og 12g
        private val enTredjedelAvInntekterMellom6gOg12g = (antallG.coerceIn(SEKS_G, TOLV_G) - SEKS_G) * EN_TREDJEDEL

        private val antallGKompensert = inntekterOppTil6g + enTredjedelAvInntekterMellom6gOg12g

        // snitter antall kompenserte G over tre år
        private val Q = antallGKompensert / 3.0

        fun justertÅrsgrunnlag(anvendtGrunnbeløp: Inntekt): Inntekt {
            return anvendtGrunnbeløp * Q
        }

        private companion object {
            private const val SEKS_G = 6.0
            private const val TOLV_G = 12.0
            private const val EN_TREDJEDEL = 1 / 3.0
        }
    }

    internal fun view() = SelvstendigFaktaavklartInntektView(id, inntektsgrunnlag)

    internal data class SelvstendigFaktaavklartInntektView(
        val id: UUID,
        val inntektsgrunnlag: Inntekt
    )

}
