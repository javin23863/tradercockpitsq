angular.module('app').service('RetestOnAdditionalMarketsService', function(SQEvents, AppService, SQConstants, DataService, AutoRetestDataService, CrossChecksService, L) {

    this.getMainSetup = function(){
        if(AppService.getTask().type == "AutomaticRetest"){
            AutoRetestDataService.loadDataConfig();
            return AutoRetestDataService.setup;
        }
        else {
            var dataConfig = { setups : []};
            DataService.loadDataConfig(dataConfig);
            return DataService.config.setups[0];
        }
    }

    this.applySettings = function(settingsElem, config) {
        if(!settingsElem) {
            return;
        }

        var mainSetup;

        if(AppService.getTask().type == "AutomaticRetest"){
            AutoRetestDataService.loadDataConfig();
            mainSetup = AutoRetestDataService.setup;
        }
        else {
            mainSetup = DataService.config.setups[0];
        }

        var setupsElem = getChildElement(settingsElem, 'Setups', true);
        if(setupsElem) {
            config.detailed = getAttrBooleanValue(setupsElem, "detailed");

            var setupElems = getChildElements(setupsElem, 'Setup');
            if(setupElems.length > 0){
                config.setups.length = 0;
            }
            
            for(var i=0; i<setupElems.length; i++){
                var setupElem = setupElems[i];
                var setup = DataService.loadSetup(config.setups, setupElem);

                if(mainSetup && mainSetup.engine){
                    setup.engine = mainSetup.engine;
                    
                    if(SQConstants.limitPrecisions && (setup.engine == "Tradestation" || setup.engine == "MultiCharts")){
                        setup.precision = mainSetup.precision;
                    }
                }
                
                var mainTestValuesElem = getChildElement(setupElem, "MainTestValues");
                setup.useCustomTF = !getAttrBooleanValue(mainTestValuesElem, "timeframe");
                setup.useCustomCommission = !getAttrBooleanValue(mainTestValuesElem, "commissions");
                setup.useCustomDates = config.detailed && !getAttrBooleanValue(mainTestValuesElem, "dates");
                setup.useCustomSubCharts = config.detailed && !getAttrBooleanValue(mainTestValuesElem, "subcharts");
                setup.useCustomPrecision = config.detailed && !getAttrBooleanValue(mainTestValuesElem, "precision");
                setup.useCustomDistance = config.detailed && !getAttrBooleanValue(mainTestValuesElem, "distance");
                setup.useCustomSpread = config.detailed && !getAttrBooleanValue(mainTestValuesElem, "spread");
                setup.useCustomSlippage = config.detailed && !getAttrBooleanValue(mainTestValuesElem, "slippage");
                setup.useCustomSession = !getAttrBooleanValue(mainTestValuesElem, "session");
                setup.useCustomSwap = !getAttrBooleanValue(mainTestValuesElem, "swap");

                DataService.correctSubcharts(setup);
            }
        }
    }

    this.loadSettings = function(settingsElem, config) {
        if(!settingsElem) {
            return;
        }

        var xmlDoc = AppService.xmlDoc;
        var setupsElem = createChild(settingsElem, 'Setups', xmlDoc);

        setupsElem.setAttribute("detailed", config.detailed);

        removeAllChildren(setupsElem);

        if (arrayNotEmpty(config.setups)) {
            for (var i = 0; i < config.setups.length; i++) {
                var setup = config.setups[i];
                var setupElem = DataService.saveSetup(setupsElem, setup, xmlDoc);

                var mainTestValuesElem = createChild(setupElem, "MainTestValues", xmlDoc);
                mainTestValuesElem.setAttribute("timeframe", !setup.useCustomTF);
                mainTestValuesElem.setAttribute("dates", !setup.useCustomDates);
                mainTestValuesElem.setAttribute("subcharts", !setup.useCustomSubCharts);
                mainTestValuesElem.setAttribute("precision", !setup.useCustomPrecision);
                mainTestValuesElem.setAttribute("distance", !setup.useCustomDistance);
                mainTestValuesElem.setAttribute("spread", !setup.useCustomSpread);
                mainTestValuesElem.setAttribute("slippage", !setup.useCustomSlippage);
                mainTestValuesElem.setAttribute("commissions", !setup.useCustomCommission);
                mainTestValuesElem.setAttribute("swap", !setup.useCustomSwap);
                mainTestValuesElem.setAttribute("session", !setup.useCustomSession);
            }
        }
    }

    this.getInfo = function(settingsElem) {
        return L.tsq("Retest on:") + " " + instance.getShortInfo(settingsElem, true);
    }

    this.getShortInfo = function(settingsElem, dontCutSymbolNames) {
        if(!settingsElem) {
            return L.tsq("N/A");
        }

        var config = {
            setups: []
        };

        instance.applySettings(settingsElem, config);

        var mainTF = getMainDataTF();
        var symbol1 = "";
        var others = 0; //" and 4 more";

        for(var i=0; i<config.setups.length; i++) {
            var setup = config.setups[i];
            
            if(i == 0) {
                var timeframe = setup.timeframe == "Same as main data" ? mainTF : setup.timeframe;
                symbol1 = (dontCutSymbolNames ? setup.symbol : limitString2(setup.symbol, 12)) + "/" + timeframe;
            } else {
                others++;
            }
        }
        
        others = others > 0 ? " + " + L.tsq("%d more", [others]) : "";

        return symbol1 + others;
    }

    this.getAverageDuration = function(settingsElem){
        var duration = CrossChecksService.getAverageDuration(1, false, getChildElement(settingsElem, 'Setups', true)) * 2;
        return duration < 0.5 ? 0.5 : duration;
    }

    function getMainDataTF(){
        var elData = AppService.getCurrentTaskTabSettings("Data");
        var elSetups = getChildElement(elData, "Setups");
        var elSetup = getChildElement(elSetups, "Setup");
        var elChart = getChildElement(elSetup, "Chart");

        return getAttrStringValue(elChart, "timeframe", "???");
    }

    function getTodayDate(){
        var date = new Date();
        return date.getFullYear() + ".0" + (date.getMonth() + 1) + "." + date.getDate();
    }

    function get10YearsAgoDate(){
        var date = new Date();
        return (date.getFullYear() - 10) + ".0" + (date.getMonth() + 1) + "." + date.getDate();
    }

    function onEvent(event, data) {
        if (event == SQEvents.get('DATA_CHANGED')) {
            let elSettings = getSettingsElem();

            instance.applySettings(elSettings, instance.config);
            if(!instance.config.setups.length) return;

            var renameAction = data && data.action == SQConstants.getConstants().dataUpdateTypes.rename;
            var symbolNames = renameAction ? data.symbols.split("##") : null;

            for(let i=0; i<instance.config.setups.length; i++){
                let setup = instance.config.setups[i];
                if(renameAction){
                    DataService.correctSetupSymbolNames(setup, symbolNames[0], symbolNames[1]);
                }
                else DataService.changeSetupDetails(setup, []);
            }

            instance.loadSettings(elSettings, instance.config);
        } 
        else if (event == SQEvents.get('SETTINGS_TAB_RELOAD')) {
            if (data == 'Data' || data == 'all') {
                let elSettings = getSettingsElem();

                instance.applySettings(elSettings, instance.config);
                instance.loadSettings(elSettings, instance.config);
            }
        } 
        else if (event == SQEvents.get('MAIN_SETUP_CHANGED') || event == SQEvents.get('AR_MAIN_SETUP_CHANGED')) {
            var crossChecksElem = AppService.getCurrentTaskTabSettings("CrossChecks");
            var roadElem = getChildElement(crossChecksElem, "RetestOnAdditionalMarkets");
            var settingsElem = getChildElement(roadElem, "Settings");

            var mainSetup = data;

            instance.applySettings(settingsElem, instance.config);

            for(let i=0; i<instance.config.setups.length; i++){
                let setup = instance.config.setups[i];
                setup.timeframe = setup.useCustomTF ? setup.timeframe : mainSetup.timeframe;
                setup.minDistance = setup.useCustomDistance ? setup.minDistance : mainSetup.minDistance;
                setup.slippage = setup.useCustomSlippage ? setup.slippage : mainSetup.slippage;

                if(!setup.useCustomSpread){
                    setup.spread = setup.symbol == mainSetup.symbol ? mainSetup.spread : setup.spread;
                }

                if(!setup.useCustomDates) {
                    var symbolData = DataService.getSymbolData(setup.symbol);

                    if(DataService.currentDateRangeCanBeUsed(symbolData, mainSetup)){
                        setup.dateFrom = timeToDateString(mainSetup.dateFrom);
                        setup.dateTo = timeToDateString(mainSetup.dateTo);
                        
                    } else {
                        try {
                            mainSetup.dateFrom = mainSetup.dateFrom ? mainSetup.dateFrom : get10YearsAgoDate()
                            mainSetup.dateTo = mainSetup.dateTo ? mainSetup.dateTo : getTodayDate()
                            var mainSetupDateFrom = getDate(mainSetup.dateFrom, true);
                            var mainSetupDateTo = getDate(mainSetup.dateTo, true);
                            var symbolDateFrom = new Date(symbolData.dateFrom);
                            var symbolDateTo = new Date(symbolData.dateTo);
                
                            if(dateIsAfter(mainSetupDateFrom, symbolDateFrom, true)) {
                                setup.dateFrom = timeToDateString(mainSetupDateFrom.getTime());
                            } else {
                                setup.dateFrom = timeToDateString(symbolDateFrom.getTime());
                            }

                            if(dateIsAfter(symbolDateTo, mainSetupDateTo, true)) {
                                setup.dateTo = timeToDateString(mainSetupDateTo.getTime());
                            } else {
                                setup.dateTo = timeToDateString(symbolDateTo.getTime());
                            }
                        } catch(err){
                            console.error("CC RetestOnAddMarkets - failed to apply date range", err);
                            return false;
                        }
                    }
                }

                var precisions = [];
                DataService.changeSetupDetails(setup, precisions);

                //for Tradestation and MultiCharts engine only SelectedTF is available
                if((SQConstants.limitPrecisions && (mainSetup.engine == "Tradestation" || mainSetup.engine == "MultiCharts")) || (!setup.useCustomPrecision && getItem(precisions, "value", mainSetup.precision))){
                    setup.precision = mainSetup.precision;
                }

                if(!setup.useCustomCommission){
                    setup.commissions = angular.copy(mainSetup.commissions);
                }
                
                if(!setup.useCustomSwap){
                    setup.swap = angular.copy(mainSetup.swap);
                }
            }

            instance.loadSettings(settingsElem, instance.config);
        } 
    }

    function getSettingsElem(){
        var elCrossChecks = AppService.getCurrentTaskTabSettings("CrossChecks");
        var elRetestOnAdditionalMarkets = getChildElement(elCrossChecks, "RetestOnAdditionalMarkets");
        return getChildElement(elRetestOnAdditionalMarkets, "Settings");
    }

    var instance = this;

    this.config = {
        setups: []
    };

    SQEvents.addListener("RetestOnAdditionalMarketsService", [
        SQEvents.get('DATA_CHANGED'), 
        SQEvents.get('SETTINGS_TAB_RELOAD'), 
        SQEvents.get('MAIN_SETUP_CHANGED'),
        SQEvents.get('AR_MAIN_SETUP_CHANGED')
    ], onEvent);
});