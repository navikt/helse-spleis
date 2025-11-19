package no.nav.helse.person.inntekt

import java.time.Year
import java.util.*
import no.nav.helse.Grunnbeløp.Companion.`1G`
import no.nav.helse.dto.deserialisering.SelvstendigFaktaavklartInntektInnDto
import no.nav.helse.dto.serialisering.SelvstendigFaktaavklartInntektUtDto
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.summer
import no.nav.helse.økonomi.Inntekt.Companion.årlig

internal data class SelvstendigFaktaavklartInntekt(
    override val id: UUID,
    override val inntektsdata: Inntektsdata,
    val pensjonsgivendeInntekter: List<PensjonsgivendeInntekt>,
    val anvendtGrunnbeløp: Inntekt
) : FaktaavklartInntekt {
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

    val normalinntekt = normalinntekt(anvendtGrunnbeløp)
    val beregningsgrunnlag = beregningsgrunnlag(anvendtGrunnbeløp)

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

    fun normalinntekt(anvendtGrunnbeløp: Inntekt) =
        normalinntekt(pensjonsgivendeInntekter, anvendtGrunnbeløp)

    fun beregningsgrunnlag(anvendtGrunnbeløp: Inntekt) =
        beregningsgrunnlag(pensjonsgivendeInntekter, anvendtGrunnbeløp)

    internal companion object {
        internal fun gjenopprett(dto: SelvstendigFaktaavklartInntektInnDto) = SelvstendigFaktaavklartInntekt(
            id = dto.id,
            inntektsdata = Inntektsdata.gjenopprett(dto.inntektsdata),
            pensjonsgivendeInntekter = dto.pensjonsgivendeInntekter.map {
                PensjonsgivendeInntekt(it.årstall, Inntekt.gjenopprett(it.beløp))
            },
            anvendtGrunnbeløp = Inntekt.gjenopprett(dto.anvendtGrunnbeløp)
        )

        fun normalinntekt(
            inntekter: List<PensjonsgivendeInntekt>,
            anvendtGrunnbeløp: Inntekt
        ) =
            inntekter
                .map { it.normalinntekt(anvendtGrunnbeløp) }
                .summer()
                .årlig.toInt()
                .årlig

        fun beregningsgrunnlag(
            inntekter: List<PensjonsgivendeInntekt>,
            anvendtGrunnbeløp: Inntekt
        ) =
            inntekter
                .map { it.beregningsgrunnlag(anvendtGrunnbeløp) }
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
        // alle inntekter mellom 6g og 12g
        private val inntekterMellom6gOg12g = antallG.coerceIn(SEKS_G, TOLV_G) - SEKS_G
        // alle inntekter over 12 g
        private val inntekterOver12g = antallG.coerceAtLeast(TOLV_G) - TOLV_G

        // 1/3 av inntekter mellom 6g og 12g
        private val enTredjedelAvInntekterMellom6gOg12g = inntekterMellom6gOg12g * EN_TREDJEDEL
        // 2/3 av inntekter mellom 6g og 12g
        private val toTredjedelAvInntekterMellom6gOg12g = inntekterMellom6gOg12g * TO_TREDJEDEL

        // alle intekter opp til 6g, 1/3 av inntekter mellom 6g og 12g, ingenting over 12g
        private val antallGKompensert = antallG - inntekterOver12g - toTredjedelAvInntekterMellom6gOg12g

        // normalinntekt basert på antall G over  tre år
        private val P = antallG / 3.0
        // snitter antall kompenserte G over tre år
        private val Q = antallGKompensert / 3.0

        init {
            check(antallGKompensert <= 8) { "antall kompenserte G kan ikke være over 8, var $antallGKompensert" }
        }

        fun normalinntekt(anvendtGrunnbeløp: Inntekt): Inntekt {
            return anvendtGrunnbeløp * P
        }

        fun beregningsgrunnlag(anvendtGrunnbeløp: Inntekt): Inntekt {
            return anvendtGrunnbeløp * Q
        }

        private companion object {
            private const val SEKS_G = 6.0
            private const val TOLV_G = 12.0
            private const val TO_TREDJEDEL = 2 / 3.0
            private const val EN_TREDJEDEL = 1 / 3.0
        }
    }

    internal fun view() = SelvstendigFaktaavklartInntektView(inntektsdata.hendelseId.id, normalinntekt)

    internal data class SelvstendigFaktaavklartInntektView(override val hendelseId: UUID, override val beløp: Inntekt) : FaktaavklartInntektView
}
