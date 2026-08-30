<@compress_single_line>sqMMFixedAmount(
            "<@printParam block symbolParamName />",
            <@printOrderType block orderType directionParamName />,
            <@printPrice block orderType priceParamName />,
            <@printSLPTMethod block orderType "SL" slParamName directionParamName priceParamName symbolParamName />,
            mmRiskedMoney,
            mmDecimals,
            mmLotsIfNoMM,
            mmMaxLots)
            
</@compress_single_line>