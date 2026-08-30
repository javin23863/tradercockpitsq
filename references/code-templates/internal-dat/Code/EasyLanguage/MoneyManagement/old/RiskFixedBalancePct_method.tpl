<@compress_single_line>sqMMRiskFixedBalancePct(
            "<@printParam block symbolParamName />",
            <@printOrderType block orderType directionParamName />,
            <@printPrice block orderType priceParamName />,
            <@printSLPTMethod block orderType "SL" slParamName directionParamName priceParamName symbolParamName />,
            mmRiskPercent,
            mmDecimals,
            mmLotsIfNoMM,
            mmMaxLots,
            <@printParam mm.params "#BaseCurrencyExchange#" />)

</@compress_single_line>