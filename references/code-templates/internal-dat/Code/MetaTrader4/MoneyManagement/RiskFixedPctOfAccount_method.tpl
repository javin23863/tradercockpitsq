<@compress_single_line>sqMMRiskFixedAccountPct(
            <@printSymbol block />,
            <@printOrderType block orderType directionParamName />,
            <@printPrice block orderType priceParamName />,
            <@printSLPTMethod block orderType "SL" slParamName directionParamName priceParamName />,
            mmRiskPercent,
            mmDecimals,
            mmStopLossPips,
            mmLotsIfNoMM,
            mmMaxLots,
            mmMultiplier,
            mmStep)

</@compress_single_line>
