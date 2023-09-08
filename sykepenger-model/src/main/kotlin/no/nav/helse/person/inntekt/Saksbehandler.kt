package no.nav.helse.person.inntekt

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.etterlevelse.Bokstav
import no.nav.helse.etterlevelse.Ledd
import no.nav.helse.etterlevelse.Paragraf
import no.nav.helse.etterlevelse.SubsumsjonObserver
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Subsumsjon
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.økonomi.Inntekt
import org.slf4j.LoggerFactory

class Saksbehandler internal constructor(
    id: UUID,
    dato: LocalDate,
    hendelseId: UUID,
    beløp: Inntekt,
    private val forklaring: String?,
    private val subsumsjon: Subsumsjon?,
    private val overstyrtInntekt: Inntektsopplysning?,
    tidsstempel: LocalDateTime
) : Inntektsopplysning(id, hendelseId, dato, beløp, tidsstempel) {
    constructor(dato: LocalDate, hendelseId: UUID, beløp: Inntekt, forklaring: String, subsumsjon: Subsumsjon?, tidsstempel: LocalDateTime) : this(UUID.randomUUID(), dato, hendelseId, beløp, forklaring, subsumsjon, null, tidsstempel)

    override fun accept(visitor: InntektsopplysningVisitor) {
        visitor.preVisitSaksbehandler(this, id, dato, hendelseId, beløp, forklaring, subsumsjon, tidsstempel)
        overstyrtInntekt?.accept(visitor)
        visitor.postVisitSaksbehandler(this, id, dato, hendelseId, beløp, forklaring, subsumsjon, tidsstempel)
    }

    override fun lagreTidsnærInntekt(
        skjæringstidspunkt: LocalDate,
        arbeidsgiver: Arbeidsgiver,
        hendelse: IAktivitetslogg,
        oppholdsperiodeMellom: Periode?,
        refusjonsopplysninger: Refusjonsopplysning.Refusjonsopplysninger,
        orgnummer: String,
        beløp: Inntekt?
    ) {
        overstyrtInntekt?.lagreTidsnærInntekt(
            skjæringstidspunkt,
            arbeidsgiver,
            hendelse,
            oppholdsperiodeMellom,
            refusjonsopplysninger,
            orgnummer,
            beløp ?: this.beløp
        )
    }

    override fun blirOverstyrtAv(ny: Inntektsopplysning): Inntektsopplysning {
        return ny.overstyrer(this)
    }

    override fun overstyrer(gammel: Saksbehandler) = kopierMed(gammel)
    override fun overstyrer(gammel: IkkeRapportert) = kopierMed(gammel)
    override fun overstyrer(gammel: SkattSykepengegrunnlag) = kopierMed(gammel)
    override fun overstyrer(gammel: Inntektsmelding) = kopierMed(gammel)
    override fun overstyrer(gammel: SkjønnsmessigFastsatt) = kopierMed(gammel)

    private fun kopierMed(overstyrtInntekt: Inntektsopplysning) =
        Saksbehandler(id, dato, hendelseId, beløp, forklaring, subsumsjon, overstyrtInntekt, tidsstempel)

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
                grunnlagForSykepengegrunnlagÅrlig = fastsattÅrsinntekt().reflection { årlig, _, _, _ -> årlig },
                grunnlagForSykepengegrunnlagMånedlig = fastsattÅrsinntekt().reflection { _, månedlig, _, _ -> månedlig }
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
                grunnlagForSykepengegrunnlagÅrlig = fastsattÅrsinntekt().reflection { årlig, _, _, _ -> årlig },
                grunnlagForSykepengegrunnlagMånedlig = fastsattÅrsinntekt().reflection { _, månedlig, _, _ -> månedlig }
            )
        } else if (subsumsjon.paragraf == Paragraf.PARAGRAF_8_28.ref && subsumsjon.ledd == Ledd.LEDD_5.nummer) {
            subsumsjonObserver.`§ 8-28 ledd 5`(
                organisasjonsnummer = organisasjonsnummer,
                overstyrtInntektFraSaksbehandler = mapOf("dato" to dato, "beløp" to beløp.reflection { _, månedlig, _, _ -> månedlig }),
                skjæringstidspunkt = dato,
                forklaring = forklaring,
                grunnlagForSykepengegrunnlagÅrlig = fastsattÅrsinntekt().reflection { årlig, _, _, _ -> årlig },
                grunnlagForSykepengegrunnlagMånedlig = fastsattÅrsinntekt().reflection { _, månedlig, _, _ -> månedlig }
            )
        } else {
            sikkerLogg.warn("Overstyring av ghost: inntekt ble overstyrt med ukjent årsak: $forklaring")
        }

    }

    private companion object {
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
    }
}