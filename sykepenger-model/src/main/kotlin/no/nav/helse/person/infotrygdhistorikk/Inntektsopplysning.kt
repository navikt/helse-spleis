package no.nav.helse.person.infotrygdhistorikk

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import net.logstash.logback.argument.StructuredArguments
import no.nav.helse.person.InfotrygdhistorikkVisitor
import no.nav.helse.økonomi.Inntekt
import org.slf4j.LoggerFactory

class Inntektsopplysning private constructor(
    private val orgnummer: String,
    private val sykepengerFom: LocalDate,
    private val inntekt: Inntekt,
    private val refusjonTilArbeidsgiver: Boolean,
    private val refusjonTom: LocalDate?,
    private var lagret: LocalDateTime?
) {
    constructor(
        orgnummer: String,
        sykepengerFom: LocalDate,
        inntekt: Inntekt,
        refusjonTilArbeidsgiver: Boolean,
        refusjonTom: LocalDate? = null
    ) : this(orgnummer, sykepengerFom, inntekt, refusjonTilArbeidsgiver, refusjonTom, null)

    internal fun accept(visitor: InfotrygdhistorikkVisitor) {
        visitor.visitInfotrygdhistorikkInntektsopplysning(orgnummer, sykepengerFom, inntekt, refusjonTilArbeidsgiver, refusjonTom, lagret)
    }

    internal fun funksjoneltLik(other: Inntektsopplysning): Boolean {
        if (this.orgnummer != other.orgnummer) return false
        if (this.sykepengerFom != other.sykepengerFom) return false
        if (this.inntekt != other.inntekt) return false
        if (this.refusjonTom != other.refusjonTom) return false
        return this.refusjonTilArbeidsgiver == other.refusjonTilArbeidsgiver
    }

    internal companion object {

        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")

        internal fun List<Inntektsopplysning>.loggSprøeInntektMigrertInnFraIT(
            dato: LocalDate,
            beløp: Inntekt,
            hendelseId: UUID,
            organisasjonsnummer: String
        ) {
            forEach { inntektsopplysning ->
                val treff = dato == inntektsopplysning.sykepengerFom && beløp == inntektsopplysning.inntekt && organisasjonsnummer == inntektsopplysning.orgnummer
                if (treff) {
                    sikkerlogg.info("Fant inntekt med {} og {} som er migert inn i inntektshistorikken fra IT",
                        StructuredArguments.keyValue("hendelseId", hendelseId),
                        StructuredArguments.keyValue("tidsstempel", dato.toString())
                    )
                }
            }
        }

        internal fun sorter(inntekter: List<Inntektsopplysning>) =
            inntekter.sortedWith(compareBy({ it.sykepengerFom }, { it.hashCode() }))

        internal fun ferdigInntektsopplysning(
            orgnummer: String,
            sykepengerFom: LocalDate,
            inntekt: Inntekt,
            refusjonTilArbeidsgiver: Boolean,
            refusjonTom: LocalDate?,
            lagret: LocalDateTime?
        ): Inntektsopplysning =
            Inntektsopplysning(
                orgnummer = orgnummer,
                sykepengerFom = sykepengerFom,
                inntekt = inntekt,
                refusjonTilArbeidsgiver = refusjonTilArbeidsgiver,
                refusjonTom = refusjonTom,
                lagret = lagret
            )
    }
}
