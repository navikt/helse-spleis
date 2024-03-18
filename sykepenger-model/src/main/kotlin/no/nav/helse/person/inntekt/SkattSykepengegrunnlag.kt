package no.nav.helse.person.inntekt

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.etterlevelse.SubsumsjonObserver
import no.nav.helse.dto.AnsattPeriodeDto
import no.nav.helse.dto.deserialisering.InntektsopplysningInnDto
import no.nav.helse.dto.serialisering.InntektsopplysningUtDto
import no.nav.helse.person.inntekt.AnsattPeriode.Companion.harArbeidsforholdNyereEnn
import no.nav.helse.person.inntekt.Skatteopplysning.Companion.sisteMåneder
import no.nav.helse.person.inntekt.Skatteopplysning.Companion.subsumsjonsformat
import no.nav.helse.økonomi.Inntekt

internal class SkattSykepengegrunnlag private constructor(
    id: UUID,
    hendelseId: UUID,
    dato: LocalDate,
    beløp: Inntekt,
    private val inntektsopplysninger: List<Skatteopplysning>,
    private val ansattPerioder: List<AnsattPeriode>,
    tidsstempel: LocalDateTime
) : AvklarbarSykepengegrunnlag(id, hendelseId, dato, beløp, tidsstempel) {
    internal companion object {
        private const val MAKS_INNTEKT_GAP = 2

        internal fun gjenopprett(dto: InntektsopplysningInnDto.SkattSykepengegrunnlagDto): SkattSykepengegrunnlag {
            return SkattSykepengegrunnlag(
                id = dto.id,
                hendelseId = dto.hendelseId,
                dato = dto.dato,
                inntektsopplysninger = dto.inntektsopplysninger.map { Skatteopplysning.gjenopprett(it) },
                ansattPerioder = dto.ansattPerioder.map { AnsattPeriode.gjenopprett(it) },
                tidsstempel = dto.tidsstempel
            )
        }
    }

    private constructor(
        id: UUID,
        hendelseId: UUID,
        dato: LocalDate,
        inntektsopplysninger: List<Skatteopplysning>,
        ansattPerioder: List<AnsattPeriode>,
        tidsstempel: LocalDateTime = LocalDateTime.now()
    ) : this(id, hendelseId, dato, Skatteopplysning.omregnetÅrsinntekt(inntektsopplysninger), inntektsopplysninger, ansattPerioder, tidsstempel)

    internal constructor(
        hendelseId: UUID,
        dato: LocalDate,
        inntektsopplysninger: List<Skatteopplysning>,
        ansattPerioder: List<AnsattPeriode>,
        tidsstempel: LocalDateTime = LocalDateTime.now()
    ) : this(UUID.randomUUID(), hendelseId, dato, Skatteopplysning.sisteTreMåneder(dato, inntektsopplysninger), ansattPerioder, tidsstempel)

    override fun accept(visitor: InntektsopplysningVisitor) {
        visitor.preVisitSkattSykepengegrunnlag(this, id, hendelseId, dato, beløp, tidsstempel)
        inntektsopplysninger.forEach { it.accept(visitor) }
        visitor.postVisitSkattSykepengegrunnlag(this, id, hendelseId, dato, beløp, tidsstempel)
    }

    override fun avklarSykepengegrunnlag(skjæringstidspunkt: LocalDate, førsteFraværsdag: LocalDate?): AvklarbarSykepengegrunnlag? {
        if (this.dato != skjæringstidspunkt) return null
        if (ansattPerioder.isEmpty()) return null
        // ser bort fra skatteinntekter om man ikke er ansatt på skjæringstidspunktet:
        if (!ansattVedSkjæringstidspunkt(skjæringstidspunkt)) return null
        // bruker skatteinntekter om det foreligger inntekter innenfor 2 mnd fra skjæringstidspunktet:
        if (sisteMåneder(skjæringstidspunkt, MAKS_INNTEKT_GAP, inntektsopplysninger).isNotEmpty()) return this
        // ser bort fra skatteinntekter om man er ansatt på skjæringstidspunktet, men inntektene er eldre enn 2 mnd fra skjæringstidspunktet (avsluttet arb.forhold?):
        if (inntektsopplysninger.isNotEmpty()) return null
        // ser bort fra skatteinntekter om arb.forholdet er eldre enn 2 mnd fra skjæringstidspunktet:
        if (!nyoppstartetArbeidsforhold(skjæringstidspunkt)) return null
        // nyoppstartet arbeidsforhold (startdato innen 2 mnd fra skjæringstidspunktet), og ingen inntekter foreligger:
        // todo bare returnere "this" og mappe ut IKKE_RAPPORTERT i SpeilBuilder?
        return IkkeRapportert(
            hendelseId = this.hendelseId,
            dato = this.dato,
            tidsstempel = this.tidsstempel
        )
    }

    private fun nyoppstartetArbeidsforhold(skjæringstidspunkt: LocalDate) =
        ansattPerioder.harArbeidsforholdNyereEnn(skjæringstidspunkt, MAKS_INNTEKT_GAP)

    private fun ansattVedSkjæringstidspunkt(dato: LocalDate) =
        ansattPerioder.any { ansattPeriode -> ansattPeriode.gjelder(dato) }

    override fun kanOverstyresAv(ny: Inntektsopplysning): Boolean {
        if (ny !is Inntektsmelding) return true
        return super.kanOverstyresAv(ny)
    }

    override fun blirOverstyrtAv(ny: Inntektsopplysning): Inntektsopplysning {
        return ny.overstyrer(this)
    }

    override fun subsumerSykepengegrunnlag(subsumsjonObserver: SubsumsjonObserver, organisasjonsnummer: String, startdatoArbeidsforhold: LocalDate?) {
        subsumsjonObserver.`§ 8-28 ledd 3 bokstav a`(
            organisasjonsnummer = organisasjonsnummer,
            skjæringstidspunkt = dato,
            inntekterSisteTreMåneder = inntektsopplysninger.subsumsjonsformat(),
            grunnlagForSykepengegrunnlagÅrlig = fastsattÅrsinntekt().reflection { årlig, _, _, _ -> årlig },
            grunnlagForSykepengegrunnlagMånedlig = fastsattÅrsinntekt().reflection { _, månedlig, _, _ -> månedlig }
        )
        subsumsjonObserver.`§ 8-29`(
            skjæringstidspunkt = dato,
            grunnlagForSykepengegrunnlagÅrlig = fastsattÅrsinntekt().reflection { årlig, _, _, _ -> årlig },
            inntektsopplysninger = inntektsopplysninger.subsumsjonsformat(),
            organisasjonsnummer = organisasjonsnummer
        )
    }

    override fun subsumerArbeidsforhold(
        subsumsjonObserver: SubsumsjonObserver,
        organisasjonsnummer: String,
        forklaring: String,
        oppfylt: Boolean
    ) = apply {
        subsumsjonObserver.`§ 8-15`(
            skjæringstidspunkt = dato,
            organisasjonsnummer = organisasjonsnummer,
            inntekterSisteTreMåneder = inntektsopplysninger.subsumsjonsformat(),
            forklaring = forklaring,
            oppfylt = oppfylt
        )
    }

    override fun erSamme(other: Inntektsopplysning): Boolean {
        return other is SkattSykepengegrunnlag && this.dato == other.dato && this.inntektsopplysninger == other.inntektsopplysninger
    }

    internal operator fun plus(other: SkattSykepengegrunnlag?): SkattSykepengegrunnlag {
        if (other == null) return this
        return SkattSykepengegrunnlag(
            id = this.id,
            hendelseId = this.hendelseId,
            dato = this.dato,
            inntektsopplysninger = this.inntektsopplysninger + other.inntektsopplysninger,
            ansattPerioder = this.ansattPerioder + other.ansattPerioder,
            tidsstempel = this.tidsstempel
        )
    }
    override fun dto() =
        InntektsopplysningUtDto.SkattSykepengegrunnlagDto(
            id = id,
            hendelseId = hendelseId,
            dato = dato,
            tidsstempel = tidsstempel,
            inntektsopplysninger = inntektsopplysninger.map { it.dto() },
            ansattPerioder = ansattPerioder.map { it.dto() })
}

class AnsattPeriode(
    private val ansattFom: LocalDate,
    private val ansattTom: LocalDate?
) {
    fun gjelder(skjæringstidspunkt: LocalDate) = ansattFom <= skjæringstidspunkt && (ansattTom == null || ansattTom >= skjæringstidspunkt)
    fun harArbeidetMindreEnn(skjæringstidspunkt: LocalDate, antallMåneder: Int) =
        ansattFom >= skjæringstidspunkt.withDayOfMonth(1).minusMonths(antallMåneder.toLong())
    companion object {
        fun List<AnsattPeriode>.harArbeidsforholdNyereEnn(skjæringstidspunkt: LocalDate, antallMåneder: Int) =
            harArbeidetMindreEnn(skjæringstidspunkt, antallMåneder).isNotEmpty()

        private fun List<AnsattPeriode>.harArbeidetMindreEnn(skjæringstidspunkt: LocalDate, antallMåneder: Int) = this
            .filter { it.harArbeidetMindreEnn(skjæringstidspunkt, antallMåneder) }
            .filter { it.gjelder(skjæringstidspunkt) }

        fun gjenopprett(dto: AnsattPeriodeDto) = AnsattPeriode(
            ansattFom = dto.fom,
            ansattTom = dto.tom
        )
    }

    internal fun dto() = AnsattPeriodeDto(ansattFom, ansattTom)
}