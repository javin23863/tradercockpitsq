angular.module('app').service('RetestWithHigherPrecisionService', function($rootScope, SQEvents, $q, BackendService, SQConstants, AppService, CrossChecksService, L) {    
    this.loadConfig = function(config) {
        config.symbol = 'N/A';
        config.timeframe = 'N/A';
        config.precision = 'N/A';
        
        var dataElem = AppService.getCurrentTaskTabSettings("Data");

        if(dataElem) {
            var setupsElem = getChildElement(dataElem, 'Setups');
            if(setupsElem) {
                var setupElems = getChildElements(setupsElem, 'Setup');
                if(setupElems.length>0) {
                    var setupElem = setupElems[0];
                    config.precision = getPrecisionLabel(getAttrValue(setupElem, 'testPrecision'));

                    var chartElems = getChildElements(setupElem, 'Chart');
                    if(chartElems.length>0) {
                        var chartElem = chartElems[0];

                        config.symbol = getAttrValue(chartElem, 'symbol');
                        config.timeframe = getAttrValue(chartElem, 'timeframe');
                        config.spread = getAttrValue(chartElem, 'spread');
                    }
                }
            }
        }
    }

    function getPrecisionLabel(precision){
        var precisions = SQConstants.getConstants().precisions;
        for(var i=0; i<precisions.length; i++) {
            if(precisions[i].value == precision || precisions[i].name == precision) return precisions[i].name;
        }
    }

    this.loadSettings = function(settingsElem, config) {
        var xmlDoc = AppService.xmlDoc;

        addNode('Precision', config.crossCheckPrecision, settingsElem, xmlDoc);
        addNode('Spread', config.crossCheckSpread, settingsElem, xmlDoc);
        addNode('CustomSpread', config.customSpread, settingsElem, xmlDoc);
    }

    this.applySettings = function(settingsElem, config) {
        if(!settingsElem) {
            return;
        }

        config.crossCheckPrecision = getNodeIntValue(settingsElem, "Precision", SQConstants.getConstants().precisions[0].value);
        config.crossCheckSpread = getNodeFloatValue(settingsElem, "Spread", 1);
        config.customSpread = getNodeBooleanValue(settingsElem, "CustomSpread", false);
    }

    this.getInfo = function(settingsElem) {
        return L.tsq("Higher precision: %s", [instance.getShortInfo(settingsElem)]);
    }
    
    this.getShortInfo = function(settingsElem) {
        if(!settingsElem) {
            return L.tsq("N/A");
        }

        var config = {};
        instance.applySettings(settingsElem, config);

        return getPrecisionLabel(config.crossCheckPrecision);
    }

    this.getAverageDuration = function(settingsElem){
        var precision = getNodeIntValue(settingsElem, "Precision", -1);
        if(precision == -1) return 0;

        var duration = CrossChecksService.getAverageDuration(1, true, null, precision) * 0.5;
        return duration < 0.5 ? 0.5 : duration;
    }

    this.getSymbolData = function(symbol){
        var data = SQConstants.getConstants().data;
        var instruments = SQConstants.getConstants().instruments;
        if(!data) return;

        for(var i=0; i<data.length; i++){
            if(data[i].symbol == symbol){
                var dataObj = angular.copy(data[i]);
                dataObj.instrumentDetails = getItem(instruments, 'instrument', dataObj.instrument);
                
                return dataObj;
            }
        }
    }
    
    var instance = this;
    var listenerId = "RetestWithHigherPrecisionService";
    
    function getCurrentSymbol(){
        var dataElem = AppService.getCurrentTaskTabSettings("Data");

        if(dataElem) {
            var setupsElem = getChildElement(dataElem, 'Setups');
            if(setupsElem) {
                var setupElems = getChildElements(setupsElem, 'Setup');
                if(setupElems.length>0) {
                    var setupElem = setupElems[0];
                    config.precision = getPrecisionLabel(getAttrValue(setupElem, 'testPrecision'));

                    var chartElems = getChildElements(setupElem, 'Chart');
                    if(chartElems.length>0) {
                        var chartElem = chartElems[0];

                        return getAttrValue(chartElem, 'symbol');
                    }
                }
            }
        }

        return null;
    }

    SQEvents.addListener(listenerId, [SQEvents.get('MAIN_SETUP_CHANGED'), SQEvents.get('AR_MAIN_SETUP_CHANGED')], function(event, data){
        var precisions = SQConstants.getConstants().precisions;
        
        //for Tradestation and MultiCharts engine only SelectedTF is available
        if(SQConstants.limitPrecisions && (data.engine == "Tradestation" || data.engine == "MultiCharts")){
            var selectedTFPrecision = getItem(precisions, "value", 1);
            if(selectedTFPrecision){
                //save the correct precision type into XML
                var CrossChecksElem = AppService.getCurrentTaskTabSettings("CrossChecks");
                var RetestWithHigherPrecisionElem = getChildElement(CrossChecksElem, 'RetestWithHigherPrecision');
                var SettingsElem = getChildElement(RetestWithHigherPrecisionElem, 'Settings');
                var PrecisionElem = getChildElement(SettingsElem, 'Precision');
                
                if(PrecisionElem){
                    setNodeValue(PrecisionElem, selectedTFPrecision.value, AppService.xmlDoc);
                }
            }
        }
        
        var currentSymbol = getCurrentSymbol();
        var CrossChecksElem = AppService.getCurrentTaskTabSettings("CrossChecks");
        var RetestWithHigherPrecisionElem = getChildElement(CrossChecksElem, 'RetestWithHigherPrecision');
        var SettingsElem = getChildElement(RetestWithHigherPrecisionElem, 'Settings');
        var SpreadElem = getChildElement(SettingsElem, 'Spread');

        var customSpread = getNodeBooleanValue(SettingsElem, "CustomSpread", false);
        if(!customSpread){
            //its required to copy spread

            if (data.symbol!=currentSymbol){
                //symbol was changed - its necessary to take its spread (this must be done this way, because data.spread is not correclty set at this moment)
                var constants = angular.copy(SQConstants.getConstants());
                var symbolData = getItem(constants.data, "symbol", data.symbol);
                
                if(SpreadElem){
                    setNodeValue(SpreadElem, symbolData.spread, AppService.xmlDoc);
                }
            }else{
                //spread was changed directly 
                setNodeValue(SpreadElem, data.spread, AppService.xmlDoc);
            }
        }
    });

});