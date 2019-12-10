package no.nav.helse.testhelpers

import no.nav.helse.hendelser.søknad.NySøknadHendelse
import no.nav.helse.hendelser.søknad.SendtSøknadHendelse
import no.nav.helse.hendelser.søknad.Sykepengesøknad
import no.nav.helse.toJsonNode
import no.nav.syfo.kafka.sykepengesoknad.dto.PeriodeDTO
import no.nav.syfo.kafka.sykepengesoknad.dto.SoknadsperiodeDTO
import no.nav.syfo.kafka.sykepengesoknad.dto.SoknadsstatusDTO
import no.nav.syfo.kafka.sykepengesoknad.dto.SykepengesoknadDTO
import java.time.LocalDate

/* Dette er en hjelpeklasse for å generere testdata. Det er viktig at defaults her holdes til absolutt minimum, slik at
 * en ikke ender opp med tester som er avhengig av sære defaults i søknaden
 */
internal fun <Type, Builder> søknad(buildertype: Buildertype<Type, Builder>, block: Builder.() -> Unit) =
    build(buildertype, block)

internal object NySøknadHendelseWrapper :
    Buildertype<NySøknadHendelse, SykepengesoknadDTOBuilder> {
    override fun build(block: SykepengesoknadDTOBuilder.() -> Unit) =
        NySøknadHendelseBuilder.build {
            sykepengesøknad {
                søknad {
                    status = SoknadsstatusDTO.NY
                    apply(block)
                }
            }
        }
}

internal object SendtSøknadHendelseWrapper :
    Buildertype<SendtSøknadHendelse, SykepengesoknadDTOBuilder> {
    override fun build(block: SykepengesoknadDTOBuilder.() -> Unit) =
        SendtSøknadHendelseBuilder.build {
            sykepengesøknad {
                søknad {
                    status = SoknadsstatusDTO.SENDT
                    apply(block)
                }
            }
        }

}

internal object SykepengesøknadWrapper :
    Buildertype<Sykepengesøknad, SykepengesoknadDTOBuilder> {
    override fun build(block: SykepengesoknadDTOBuilder.() -> Unit) =
        SykepengesøknadBuilder.build { søknad(block) }
}

internal class NySøknadHendelseBuilder {
    private lateinit var sykepengesøknad: Sykepengesøknad

    internal fun sykepengesøknad(block: SykepengesøknadBuilder.() -> Unit) {
        sykepengesøknad = SykepengesøknadBuilder.build(block)
    }

    private fun build() = NySøknadHendelse(sykepengesøknad)

    internal companion object Type :
        Buildertype<NySøknadHendelse, NySøknadHendelseBuilder> {
        override fun build(block: NySøknadHendelseBuilder.() -> Unit) =
            NySøknadHendelseBuilder().apply(block).build()
    }
}

internal class SendtSøknadHendelseBuilder {
    private lateinit var sykepengesøknad: Sykepengesøknad

    internal fun sykepengesøknad(block: SykepengesøknadBuilder.() -> Unit) {
        sykepengesøknad = SykepengesøknadBuilder.build(block)
    }

    private fun build() = SendtSøknadHendelse(sykepengesøknad)

    internal companion object Type :
        Buildertype<SendtSøknadHendelse, SendtSøknadHendelseBuilder> {
        override fun build(block: SendtSøknadHendelseBuilder.() -> Unit) =
            SendtSøknadHendelseBuilder().apply(block).build()
    }
}

internal class SykepengesøknadBuilder {
    private lateinit var søknad: SykepengesoknadDTO

    internal fun søknad(block: SykepengesoknadDTOBuilder.() -> Unit) {
        søknad = SykepengesoknadDTOBuilder.build(block)
    }

    private fun build() = Sykepengesøknad(søknad.toJsonNode())

    internal companion object Type :
        Buildertype<Sykepengesøknad, SykepengesøknadBuilder> {
        override fun build(block: SykepengesøknadBuilder.() -> Unit) =
            SykepengesøknadBuilder().apply(block).build()
    }
}

internal class SykepengesoknadDTOBuilder {
    internal lateinit var fom: LocalDate
    internal lateinit var tom: LocalDate
    internal lateinit var status: SoknadsstatusDTO
    private val perioder = mutableListOf<SoknadsperiodeDTO>()
    private val egenmeldinger = mutableListOf<PeriodeDTO>()

    internal fun søknadsperiode(block: SøknadsperiodeBuilder.() -> Unit) {
        perioder.add(SøknadsperiodeBuilder.søknadsperiode(block))
    }

    internal fun egenmelding(block: PeriodeBuilder.() -> Unit) {
        egenmeldinger.add(PeriodeBuilder.periode(block))
    }

    private fun build(): SykepengesoknadDTO {
        perioder.takeIf { it.isEmpty() }?.add(SoknadsperiodeDTO(fom = fom, tom = tom))
        return SykepengesoknadDTO(
            fom = fom,
            tom = tom,
            soknadsperioder = perioder,
            egenmeldinger = egenmeldinger,
            status = status
        )
    }

    internal companion object Type :
        Buildertype<SykepengesoknadDTO, SykepengesoknadDTOBuilder> {
        override fun build(block: SykepengesoknadDTOBuilder.() -> Unit) =
            SykepengesoknadDTOBuilder().apply(block).build()
    }
}

internal class SøknadsperiodeBuilder {
    internal lateinit var fom: LocalDate
    internal lateinit var tom: LocalDate
    internal var periode
        set(value) {
            fom = value.first
            tom = value.second
        }
        get() = fom to tom

    private fun build() = SoknadsperiodeDTO(fom, tom)

    internal companion object {
        internal fun søknadsperiode(block: SøknadsperiodeBuilder.() -> Unit) =
            SøknadsperiodeBuilder().apply(block).build()
    }
}

internal class PeriodeBuilder {
    internal lateinit var fom: LocalDate
    internal lateinit var tom: LocalDate
    internal var periode
        set(value) {
            fom = value.first
            tom = value.second
        }
        get() = fom to tom

    private fun build() = PeriodeDTO(fom, tom)

    internal companion object {
        fun periode(block: PeriodeBuilder.() -> Unit) = PeriodeBuilder().apply(block).build()
    }
}
