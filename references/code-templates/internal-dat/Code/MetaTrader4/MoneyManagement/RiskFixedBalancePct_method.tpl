<@compress_single_line>sqMMRiskFixedBalancePct(
            <@printSymbol block />,
            <@printOrderType block orderType directionParamName />,
            <@printPrice block orderType priceParamName />,
            <@printSLPTMethod block orderType "SL" slParamName directionParamName priceParamName />,
            mmRiskPercent,
            mmDecimals,
            mmLotsIfNoMM,
            mmMaxLots,
            mmMultiplier,
            mmStep)

</@compress_single_line>
