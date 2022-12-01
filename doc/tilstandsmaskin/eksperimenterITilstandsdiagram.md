# The State of the Machine

Alle tilstandsendringer som er eksplisitt beskrevet i tilstandsmaskinen (at the time of writing...)

Det er to tilstander (TilInfotrygd og AvventerSimulering) som ikke har noen innkommende transisjoner. Disse tilstandene havner vi ofte i, så dermed vet vi at diagrammet mangler transisjoner. Dette er transisjoner som gjøres utenfor tilstandsmaskinen, men gjerne av Vedtaksperioden eller kanskje andre objekter.

```mermaid

stateDiagram-v2
    * --> Start
    
    Start --> AvventerInntektsmeldingEllerHistorikk
    Start --> AvventerBlokkerendePeriode
    
    AvventerBlokkerendePeriode --> AvsluttetUtenUtbetaling
    AvventerBlokkerendePeriode --> AvventerHistorikk
    AvventerBlokkerendePeriode --> AvventerVilkarsproving
    
    AvventerRevurdering --> AvventerGjennomfortRevurdering
    
    AvventerGjennomfortRevurdering --> RevurderingFeilet
    AvventerGjennomfortRevurdering --> Avsluttet
    
    AvventerHistorikkRevurdering --> AvventerVilkarsprovingRevurdering
    
    AvventerVilkarsprovingRevurdering --> AvventerHistorikkRevurdering
    AvventerVilkarsprovingRevurdering --> AvventerVilkarsprovingRevurdering
    
    AvventerInntektsmeldingEllerHistorikk --> AvsluttetUtenUtbetaling
    AvventerInntektsmeldingEllerHistorikk --> AvventerBlokkerendePeriode
    
    AvventerVilkarsproving --> AvventerBlokkerendePeriode
    AvventerVilkarsproving --> AvventerHistorikk
    
    AvventerHistorikk --> AvventerBlokkerendePeriode
    
    AvventerSimulering --> AvventerBlokkerendePeriode
    AvventerSimulering --> AvventerGodkjenning
    
    AvventerSimuleringRevurdering --> AvventerHistorikkRevurdering
    AvventerSimuleringRevurdering --> AvventerGodkjenningRevurdering

    AvventerGodkjenning --> AvventerBlokkerendePeriode   
    AvventerGodkjenning --> TilUtbetaling   
    AvventerGodkjenning --> Avsluttet
    
    AvventerGodkjenningRevurdering --> AvventerHistorikkRevurdering   
    AvventerGodkjenningRevurdering --> RevurderingFeilet   
    AvventerGodkjenningRevurdering --> Avsluttet
    
    TilUtbetaling --> UtbetalingFeilet
    TilUtbetaling --> Avsluttet   
    TilUtbetaling --> AvventerBlokkerendePeriode
    
    UtbetalingFeilet --> AvventerHistorikkRevurdering
    UtbetalingFeilet --> AvventerBlokkerendePeriode
    UtbetalingFeilet --> Avsluttet
    
    AvsluttetUtenUtbetaling --> AvventerRevurdering
    
    TilInfotrygd 

```