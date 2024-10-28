package no.nav.helse.hendelser

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.dto.AvsenderDto
import no.nav.helse.person.aktivitetslogg.Aktivitetskontekst
import no.nav.helse.person.aktivitetslogg.SpesifikkKontekst

enum class Avsender {
    SYKMELDT, ARBEIDSGIVER, SAKSBEHANDLER, SYSTEM;

    fun dto() = when (this) {
        SYKMELDT -> AvsenderDto.SYKMELDT
        ARBEIDSGIVER -> AvsenderDto.ARBEIDSGIVER
        SAKSBEHANDLER -> AvsenderDto.SAKSBEHANDLER
        SYSTEM -> AvsenderDto.SYSTEM
    }
    companion object {
        fun gjenopprett(dto: AvsenderDto): Avsender {
            return when (dto) {
                AvsenderDto.ARBEIDSGIVER -> ARBEIDSGIVER
                AvsenderDto.SAKSBEHANDLER -> SAKSBEHANDLER
                AvsenderDto.SYKMELDT -> SYKMELDT
                AvsenderDto.SYSTEM -> SYSTEM
            }
        }
    }
}

sealed interface Hendelse : Aktivitetskontekst {
    val behandlingsporing: Behandlingsporing
    val metadata: HendelseMetadata

    override fun toSpesifikkKontekst() = SpesifikkKontekst(kontekstnavn, buildMap {
        put("meldingsreferanseId", metadata.meldingsreferanseId.toString())
        put("aktørId", behandlingsporing.aktørId)
        put("fødselsnummer", behandlingsporing.fødselsnummer)
        when (val sporing = behandlingsporing) {
            is Behandlingsporing.Arbeidsgiver -> {
                put("organisasjonsnummer", sporing.organisasjonsnummer)
            }
            is Behandlingsporing.Person -> {}
        }
    })
}

private val Hendelse.kontekstnavn get() = when (this) {
    is Grunnbeløpsregulering -> "Grunnbeløpsregulering"
    is OverstyrArbeidsforhold -> "OverstyrArbeidsforhold"
    is OverstyrArbeidsgiveropplysninger -> "OverstyrArbeidsgiveropplysninger"
    is SkjønnsmessigFastsettelse -> "SkjønnsmessigFastsettelse"
    is AnmodningOmForkasting -> "AnmodningOmForkasting"
    is AnnullerUtbetaling -> "AnnullerUtbetaling"
    is AvbruttSøknad -> "AvbruttSøknad"
    is Dødsmelding -> "Dødsmelding"
    is ForkastSykmeldingsperioder -> "ForkastSykmeldingsperioder"
    is GjenopplivVilkårsgrunnlag -> "GjenopplivVilkårsgrunnlag"
    is IdentOpphørt -> "IdentOpphørt"
    is Infotrygdendring -> "Infotrygdendring"
    is Inntektsmelding -> "Inntektsmelding"
    is InntektsmeldingerReplay -> "InntektsmeldingerReplay"
    is KanIkkeBehandlesHer -> "KanIkkeBehandlesHer"
    is Migrate -> "Migrate"
    is MinimumSykdomsgradsvurderingMelding -> "MinimumSykdomsgradsvurderingMelding"
    is OmfordelRefusjonsopplysninger -> "OmfordelRefusjonsopplysninger"
    is PersonPåminnelse -> "PersonPåminnelse"
    is Påminnelse -> "Påminnelse"
    is Simulering -> "Simulering"
    is OverstyrTidslinje -> "OverstyrTidslinje"
    is Søknad -> "Søknad"
    is SykepengegrunnlagForArbeidsgiver -> "SykepengegrunnlagForArbeidsgiver"
    is Sykmelding -> "Sykmelding"
    is UtbetalingHendelse -> "UtbetalingHendelse"
    is Utbetalingpåminnelse -> "Utbetalingpåminnelse"
    is Utbetalingsgodkjenning -> "Utbetalingsgodkjenning"
    is Utbetalingshistorikk -> "Utbetalingshistorikk"
    is UtbetalingshistorikkEtterInfotrygdendring -> "UtbetalingshistorikkEtterInfotrygdendring"
    is UtbetalingshistorikkForFeriepenger -> "UtbetalingshistorikkForFeriepenger"
    is VedtakFattet -> "VedtakFattet"
    is Vilkårsgrunnlag -> "Vilkårsgrunnlag"
    is Ytelser -> "Ytelser"
    is DagerFraInntektsmelding,
    is Revurderingseventyr,
    is Behandlingsavgjørelse -> error("Har ikke kontekstnavn definert for ${this::class.simpleName}")
}

sealed interface Behandlingsporing {
    val fødselsnummer: String
    val aktørId: String

    data class Person(override val fødselsnummer: String, override val aktørId: String) : Behandlingsporing
    data class Arbeidsgiver(override val fødselsnummer: String, override val aktørId: String, val organisasjonsnummer: String) : Behandlingsporing
}

data class HendelseMetadata(
    val meldingsreferanseId: UUID,
    val avsender: Avsender,

    // tidspunktet meldingen ble registrert (lest inn) av fagsystemet
    val registrert: LocalDateTime,

    // tidspunktet for når meldingen ble sendt inn av avsender.
    // kan være når bruker sendte søknaden sin, eller arbeidsgiver sendte inntektsmelding.
    val innsendt: LocalDateTime,

    // sann hvis et system har sendt meldingen på eget initiativ
    val automatiskBehandling: Boolean
)