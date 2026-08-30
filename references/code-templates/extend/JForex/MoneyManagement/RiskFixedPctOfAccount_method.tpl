<@compress_single_line>sqMMRiskFixedAccountPct(
            "<@printParam block symbolParamName />",
            <@printOrderType block orderType directionParamName />,
            <@printPrice block orderType priceParamName />,
            <@printSLPTMethod block orderType "SL" slParamName directionParamName priceParamName symbolParamName />,
            mmRiskPercent,
            mmDecimals,
            mmStopLossPips,
            mmLotsIfNoMM,
            mmMaxLots)

</@compress_single_line>