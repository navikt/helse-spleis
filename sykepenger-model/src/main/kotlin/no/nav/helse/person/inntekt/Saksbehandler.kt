package no.nav.helse.person.inntekt

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Subsumsjon
import no.nav.helse.person.etterlevelse.Bokstav
import no.nav.helse.person.InntekthistorikkVisitor
import no.nav.helse.person.InntektsopplysningVisitor
import no.nav.helse.person.etterlevelse.Ledd
import no.nav.helse.person.etterlevelse.Paragraf
import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import no.nav.helse.økonomi.Inntekt
import org.slf4j.LoggerFactory

class Saksbehandler internal constructor(
    private val id: UUID,
    dato: LocalDate,
    private val hendelseId: UUID,
    private val beløp: Inntekt,
    private val forklaring: String?,
    private val subsumsjon: Subsumsjon?,
    private val tidsstempel: LocalDateTime = LocalDateTime.now()
) : Inntektsopplysning(dato, 100) {

    constructor(dato: LocalDate, hendelseId: UUID, beløp: Inntekt, forklaring: String, subsumsjon: Subsumsjon?, tidsstempel: LocalDateTime) : this(UUID.randomUUID(), dato, hendelseId, beløp, forklaring, subsumsjon, tidsstempel)

    override fun accept(visitor: InntektsopplysningVisitor) {
        visitor.visitSaksbehandler(this, id, dato, hendelseId, beløp, forklaring, subsumsjon, tidsstempel)
    }

    override fun overstyres(ny: Inntektsopplysning): Inntektsopplysning {
        if (ny !is Saksbehandler) return this
        if (ny.beløp == this.beløp) return this
        return ny
    }

    override fun omregnetÅrsinntekt(skjæringstidspunkt: LocalDate, førsteFraværsdag: LocalDate?) = takeIf { it.dato == skjæringstidspunkt }
    override fun omregnetÅrsinntekt(): Inntekt = beløp

    override fun skalErstattesAv(other: Inntektsopplysning) =
        other is Saksbehandler && this.dato == other.dato

    override fun erSamme(other: Inntektsopplysning) =
        other is Saksbehandler && this.dato == other.dato && this.beløp == other.beløp

    override fun subsumerSykepengegrunnlag(subsumsjonObserver: SubsumsjonObserver, organisasjonsnummer: String, startdatoArbeidsforhold: LocalDate?) {
        if(subsumsjon == null) return
        requireNotNull(forklaring) { "Det skal være en forklaring fra saksbehandler ved overstyring av inntekt" }
        if (subsumsjon.paragraf == Paragraf.PARAGRAF_8_28.ref
            && subsumsjon.ledd == Ledd.LEDD_3.nummer
            && subsumsjon.bokstav == Bokstav.BOKSTAV_B.ref.toString()
        ) {
            requireNotNull(startdatoArbeidsforhold) { "Fant ikke aktivt arbeidsforhold for skjæringstidspunktet i arbeidsforholdshistorikken" }
            subsumsjonObserver.`§ 8-28 ledd 3 bokstav b`(
                organisasjonsnummer = organisasjonsnummer,
                startdatoArbeidsforhold = startdatoArbeidsforhold,
                overstyrtInntektFraSaksbehandler = mapOf("dato" to dato, "beløp" to beløp.reflection { _, månedlig, _, _ -> månedlig }),
                skjæringstidspunkt = dato,
                forklaring = forklaring,
                grunnlagForSykepengegrunnlag = omregnetÅrsinntekt()
            )
        } else if (subsumsjon.paragraf == Paragraf.PARAGRAF_8_28.ref
            && subsumsjon.ledd == Ledd.LEDD_3.nummer
            && subsumsjon.bokstav == Bokstav.BOKSTAV_C.ref.toString()
        ) {
            subsumsjonObserver.`§ 8-28 ledd 3 bokstav c`(
                organisasjonsnummer = organisasjonsnummer,
                overstyrtInntektFraSaksbehandler = mapOf("dato" to dato, "beløp" to beløp.reflection { _, månedlig, _, _ -> månedlig }),
                skjæringstidspunkt = dato,
                forklaring = forklaring,
                grunnlagForSykepengegrunnlag = omregnetÅrsinntekt()
            )
        } else if (subsumsjon.paragraf == Paragraf.PARAGRAF_8_28.ref && subsumsjon.ledd == Ledd.LEDD_5.nummer) {
            subsumsjonObserver.`§ 8-28 ledd 5`(
                organisasjonsnummer = organisasjonsnummer,
                overstyrtInntektFraSaksbehandler = mapOf("dato" to dato, "beløp" to beløp.reflection { _, månedlig, _, _ -> månedlig }),
                skjæringstidspunkt = dato,
                forklaring = forklaring,
                grunnlagForSykepengegrunnlag = omregnetÅrsinntekt()
            )
        } else {
            sikkerLogg.warn("Overstyring av ghost: inntekt ble overstyrt med ukjent årsak: $forklaring")
        }

    }

    override fun subsumerArbeidsforhold(
        subsumsjonObserver: SubsumsjonObserver,
        organisasjonsnummer: String,
        forklaring: String,
        oppfylt: Boolean
    ) {
        subsumsjonObserver.`§ 8-15`(
            dato,
            organisasjonsnummer,
            emptyList(),
            forklaring,
            oppfylt
        )
    }

    private companion object {
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
    }
}