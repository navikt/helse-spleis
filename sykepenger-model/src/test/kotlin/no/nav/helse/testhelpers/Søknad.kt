package no.nav.helse.testhelpers

import no.nav.helse.hendelser.NySøknad
import no.nav.helse.hendelser.SendtSøknad
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
    Buildertype<NySøknad, SykepengesoknadDTOBuilder> {
    override fun build(block: SykepengesoknadDTOBuilder.() -> Unit) =
        NySøknadHendelseBuilder.build {
            sykepengesøknad {
                status = SoknadsstatusDTO.NY
                apply(block)
            }
        }
}

internal object SendtSøknadHendelseWrapper :
    Buildertype<SendtSøknad, SykepengesoknadDTOBuilder> {
    override fun build(block: SykepengesoknadDTOBuilder.() -> Unit) =
        SendtSøknadHendelseBuilder.build {
            sykepengesøknad {
                status = SoknadsstatusDTO.SENDT
                apply(block)
            }
        }
}

internal class NySøknadHendelseBuilder {
    private lateinit var sykepengesøknad: SykepengesoknadDTO

    internal fun sykepengesøknad(block: SykepengesoknadDTOBuilder.() -> Unit) {
        sykepengesøknad = SykepengesoknadDTOBuilder.build(block)
    }

    private fun build() = NySøknad.Builder().build(sykepengesøknad.toJsonNode().toString())!!

    internal companion object Type :
        Buildertype<NySøknad, NySøknadHendelseBuilder> {
        override fun build(block: NySøknadHendelseBuilder.() -> Unit) =
            NySøknadHendelseBuilder().apply(block).build()
    }
}

internal class SendtSøknadHendelseBuilder {
    private lateinit var sykepengesøknad: SykepengesoknadDTO

    internal fun sykepengesøknad(block: SykepengesoknadDTOBuilder.() -> Unit) {
        sykepengesøknad = SykepengesoknadDTOBuilder.build(block)
    }

    private fun build() = SendtSøknad.Builder().build(sykepengesøknad.toJsonNode().toString())!!

    internal companion object Type :
        Buildertype<SendtSøknad, SendtSøknadHendelseBuilder> {
        override fun build(block: SendtSøknadHendelseBuilder.() -> Unit) =
            SendtSøknadHendelseBuilder().apply(block).build()
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
