<@compress_single_line>sqMMATRRiskBasedSizing(
            <@printSymbol block />,
            <@printOrderType block orderType directionParamName />,
            <@printPrice block orderType priceParamName />,
            <@printSLPTMethod block orderType "SL" slParamName directionParamName priceParamName />,
            mmRiskPercent,
            mmATRPeriod,
            mmATRMultiplier,
            mmDecimals,
            mmLotsIfNoMM,
            mmMaxLots,
            mmMultiplier,
            mmStep)

</@compress_single_line>