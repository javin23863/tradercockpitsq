<@compress_single_line>sqMMFixedAmount(
            <@printSymbol block />,
            <@printOrderType block orderType directionParamName />,
            <@printPrice block orderType priceParamName />,
            <@printSLPTMethod block orderType "SL" slParamName directionParamName priceParamName />,
            mmRiskedMoney,
            mmDecimals,
            mmLotsIfNoMM,
            mmMaxLots,
            mmMultiplier,
            mmStep)
            
</@compress_single_line>
