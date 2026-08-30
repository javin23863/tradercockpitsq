angular.module('app.settings').service('AutoRetestDataService', function ($rootScope, $q, AppService, BackendService, SQConstants, DataService) {

    this.loadCommissionMethods = DataService.loadCommissionMethods;
    this.getCommissionSettings = DataService.getCommissionSettings;
    this.updateCommissionSettings = DataService.updateCommissionSettings;    
    this.getCommissionConfig = DataService.getCommissionConfig;
    this.currentDateRangeCanBeUsed = DataService.currentDateRangeCanBeUsed;
    this.loadAvailablePrecisions = DataService.loadAvailablePrecisions;
    this.getSymbolData = DataService.getSymbolData;

    this.loadDataConfig = function (forceReload) {
        if (configLoaded && !forceReload) return;

        configLoaded = false;

        var dataElem = AppService.getCurrentTaskTabSettings("CustomData");

        setDefaultSettings();

        if (dataElem) {
            var setupsElem = getChildElement(dataElem, 'Setups', true);
            if (setupsElem) {
                var elSetup = getChildElement(setupsElem, 'Setup');
                loadSetup(elSetup);
                
                var mainTestValuesElem = getChildElement(elSetup, "MainTestValues");
                instance.setup.useCustomEngine = !getAttrBooleanValue(mainTestValuesElem, "engine");
                instance.setup.useCustomSymbol = !getAttrBooleanValue(mainTestValuesElem, "symbol");
                instance.setup.useCustomTF = !getAttrBooleanValue(mainTestValuesElem, "timeframe");
                instance.setup.useCustomDates = !getAttrBooleanValue(mainTestValuesElem, "dates");
                instance.setup.useCustomSubCharts = !getAttrBooleanValue(mainTestValuesElem, "subcharts");
                instance.setup.useCustomPrecision = !getAttrBooleanValue(mainTestValuesElem, "precision");

                var customSlippage = getAttrValue(mainTestValuesElem, "slippage");
                instance.setup.useCustomSlippage = getUseCustomAttribute(customSlippage);

                var customCommission = getAttrValue(mainTestValuesElem, "commissions");
                instance.setup.useCustomCommission = getUseCustomAttribute(customCommission);

                var customDistance = getAttrValue(mainTestValuesElem, "distance");
                instance.setup.useCustomDistance = getUseCustomAttribute(customDistance);

                var customSpread = getAttrValue(mainTestValuesElem, "spread");
                instance.setup.useCustomSpread = getUseCustomAttribute(customSpread);    
                
                var customSwap = getAttrValue(mainTestValuesElem, "swap");
                instance.setup.useCustomSwap = getUseCustomAttribute(customSwap); 
            }
        }

        configLoaded = true;
    }

    function getUseCustomAttribute(value){
        if (value=="false"){
            value = 'custom';
        }else if (value=="true"){
            value = 'strategy';
        }

        return value;
    }

    function setDefaultSettings() {
        instance.setup.engine = SQConstants.getConstants().engines[0];
        instance.setup.precision = SQConstants.getConstants().precisions[0].value;
        instance.setup.session = 'No Session';
        instance.setup.timeframe = 'H1';
        instance.setup.slippage = 0;
        instance.setup.minDistance = 0;
        instance.setup.commissions = instance.getCommissionConfig(null);
        instance.setup.swap = DataService.DEFAULT_SWAP_SETTINGS;
    }

    function loadSetup(setupElem) {
        if (setupElem) {
            instance.setup.dateFrom = getAttrValue(setupElem, 'dateFrom');
            instance.setup.dateTo = getAttrValue(setupElem, 'dateTo');
            instance.setup.precision = instance.correctPrecision(getAttrValue(setupElem, 'testPrecision'));
            instance.setup.session = getAttrValue(setupElem, 'session');
            instance.setup.engine = getAttrValue(setupElem, 'engine') || SQConstants.getConstants().engines[0];
            instance.setup.slippage = getAttrValue(setupElem, 'slippage');
            instance.setup.minDistance = getAttrValue(setupElem, 'minDist');
            instance.setup.commissions = instance.getCommissionConfig(setupElem);
            instance.setup.subcharts = [];

            var chartElems = getChildElements(setupElem, 'Chart');
            
            for (var i = 0; i < chartElems.length; i++) {
                var chartElem = chartElems[i];
                var chart = {};
                if (!i) {
                    loadChart(instance.setup, chartElem);
                }
                else {
                    loadChart(chart, chartElem);
                    instance.setup.subcharts.push(chart);
                }
            }

            instance.changeSetupDetails();      //corrects symbol settings if invalid

            instance.setup.swap = DataService.getSwapSettings(setupElem);
        }
    }

    function loadChart(chart, chartElem) {
        chart.symbol = getAttrValue(chartElem, 'symbol');
        chart.timeframe = getAttrValue(chartElem, 'timeframe');
        chart.spread = getAttrFloatValue(chartElem, 'spread', 0);
    }

    this.changeSetupDetails = function(oldSymbol){
        //console.error("changeSetupDetails", oldSymbol, instance.setup);
        if(!instance.setup.useCustomSymbol) {
            instance.setup.minTimeframe = 'TICK';
            loadToArray(instance.precisions, angular.copy(SQConstants.getConstants().precisions));
            return;
        }

        var symbolData = instance.getSymbolData(instance.setup.symbol);
        if (!symbolData || instance.setup.symbol==''){
            //petrs - unknown symbol - skipping
            console.log("data unknown - skip");
            return;
        }

        instance.setup.symbolReplaced = false;

        if(!symbolData || !symbolData.rows){
            instance.setup.symbolReplaced = true;
            symbolData = instance.getSymbolData();
        }

        if(oldSymbol && oldSymbol.symbol){
            oldSymbol = oldSymbol.symbol;
        }

        instance.precisions.length = 0;

        if(symbolData){
            instance.setup.symbol = symbolData.symbol;

            if(!instance.currentDateRangeCanBeUsed(symbolData, instance.setup)){
                instance.setup.dateFrom = timeToDateString(symbolData.dateFrom);
                instance.setup.dateTo = timeToDateString(symbolData.dateTo);
            }
            
            var symbolChanged = valueFilled(oldSymbol) && symbolData.symbol != oldSymbol;
            var instrumentDetails = symbolData.instrumentDetails;

            if(!valueFilled(instrumentDetails)){
                $rootScope.showError(L.tsq("Error - Instrument for symbol '" + symbolData.symbol + "' doesn't exist!"));
            }

            var commissions = (!symbolChanged && instance.setup.commissions) ? instance.setup.commissions : getCommissionsConfig(instrumentDetails ? instrumentDetails.commissions : instance.setup.commissions);
            instance.setup.commissions = commissions;

            instance.setup.timeframe = symbolData.timeframe ? getNewTimeframe(symbolData.timeframe, instance.setup.timeframe) : undefined;
            instance.setup.minTimeframe = symbolData.timeframe;
        
            instance.setup.spread = (symbolChanged && valueFilled(instrumentDetails)) ? instrumentDetails.defaultSpread : instance.setup.spread;
            instance.setup.spread = valueFilled(instance.setup.spread) ? instance.setup.spread : 0;

            instance.setup.slippage = (symbolChanged && valueFilled(instrumentDetails)) ? instrumentDetails.defaultSlippage : instance.setup.slippage;
            instance.setup.slippage = valueFilled(instance.setup.slippage) ? instance.setup.slippage : 0;

            instance.setup.minDistance = valueFilled(instance.setup.minDistance) ? instance.setup.minDistance : 0;
            instance.setup.barType = symbolData.barType;

            //load available precisions and set preferred precision
            instance.setup.precision = loadPrecisions(symbolData, instance.precisions, instance.setup.precision, instance.setup.engine);
        }
        else {
            console.warn("Symbol data of symbol '" + instance.setup.symbol + "' not found and no data available");

            var defaultDate = getDateString(new Date(0));
            instance.setup.dateFrom = defaultDate;
            instance.setup.dateTo = defaultDate;
        }

        if(arrayNotEmpty(instance.setup.subcharts)){
            instance.correctSubcharts();
        }
    }

    function loadPrecisions(symbolData, precisions, preferredPrecision, engine){
        var allPrecisions = angular.copy(SQConstants.getConstants().precisions);

        //for Tradestation and MultiCharts engine only SelectedTF is available
        if(SQConstants.limitPrecisions && (engine == "Tradestation" || engine == "MultiCharts")){
            var selectedTFPrecision = getItem(allPrecisions, "value", 1);
            if(selectedTFPrecision){
                precisions.push(selectedTFPrecision);
                return selectedTFPrecision.value;
            }
        }

        var tf = valueFilled(symbolData.timeframe) ? getTFSeconds(symbolData.timeframe) : 1000000001;

        var targetPrecision = null;

        for(var i=0; i<allPrecisions.length; i++){
            var minTF = allPrecisions[i].minTF != 'X' ? getTFSeconds(allPrecisions[i].minTF) : 1000000000;

            if(tf <= minTF) {
                if(allPrecisions[i].value == preferredPrecision){
                    targetPrecision = allPrecisions[i].value;
                }
                precisions.push(allPrecisions[i]);
            }
        }

        return targetPrecision || (precisions[0] ? precisions[0].value : null);
    }

    this.getSymbolDateRange = function(selectedSymbol){
        var data = instance.getSymbolData(selectedSymbol);
        if(!data) return "N/A";
        else return timeToDateString(data.dateFrom) + " - " + timeToDateString(data.dateTo);
    }

    this.correctSetupSymbolNames = function(setup, oldSymbolName, newSymbolName){
        var changesMade = false;

        if(setup.symbol == oldSymbolName){
            setup.symbol = newSymbolName;
            changesMade = true;
        }

        if(arrayNotEmpty(setup.subcharts)){
            for(var i=0; i<setup.subcharts.length; i++){
                var subchart = setup.subcharts[i];
                if(subchart.symbol == oldSymbolName){
                    subchart.symbol = newSymbolName;
                    changesMade = true;
                }
            }
        }

        return changesMade;
    }

    this.correctSubcharts = function(){
        //petrs
        var firstSymbolData = SQConstants.getConstants().data[0];
        var defaultSymbol = instance.setup.useCustomSymbol ? instance.setup.symbol : firstSymbolData.symbol;

        if(!instance.setup.subcharts) return;

        for (var i = 0; i < instance.setup.subcharts.length; i++) {
            instance.setup.subcharts[i] = instance.setup.subcharts[i] || {
                symbol: defaultSymbol,
                timeframe: 'H1',
                spread: instance.setup.spread
            };

            correctChart(instance.setup.subcharts[i], defaultSymbol);
        }
    }
    
    function correctChart(chart, setupSymbol){
        var realSymbol = chart.symbol == "Same as main chart" ? setupSymbol : chart.symbol;
        var symbolData = getItem(SQConstants.getConstants().data, 'symbol', realSymbol);

        if(!symbolData){
            symbolData = SQConstants.getConstants().data[0];
           // chart.symbol = symbolData ? symbolData.symbol : "";
        }

        if (symbolData) {
          //  chart.timeframe = getNewTimeframe(symbolData.timeframe, chart.timeframe);
            chart.minTimeframe = symbolData.timeframe;
            chart.barType = symbolData.barType;
        }
        else {
            chart.symbol = "";
            chart.timeframe = "";
        }
    }

    function getCommissionsConfig(instrumentCommissions){
        return "<Commissions>" + instrumentCommissions + "</Commissions>";
    }

    this.saveConfig = function () {
        var xmlDoc = AppService.xmlDoc;

        if(AppService.getTask().type != "AutomaticRetest") return;
        
        var settingsElem = AppService.getTaskConfig();
        if(!settingsElem) return;


        var dataElem = getChildElement(settingsElem, 'CustomData', true);
        if (dataElem) {
            removeAllChildren(dataElem);
        }
        else {
            dataElem = createChild(settingsElem, 'CustomData', xmlDoc, true);
        }

        var setupsElem = createChild(dataElem, 'Setups', xmlDoc);
        
        removeAllChildren(setupsElem);

        var setupElem = saveSetup(setupsElem);

        var mainTestValuesElem = createChild(setupElem, "MainTestValues", xmlDoc);
        mainTestValuesElem.setAttribute("engine", !instance.setup.useCustomEngine);
        mainTestValuesElem.setAttribute("symbol", !instance.setup.useCustomSymbol);
        mainTestValuesElem.setAttribute("timeframe", !instance.setup.useCustomTF);
        mainTestValuesElem.setAttribute("dates", !instance.setup.useCustomDates);
        mainTestValuesElem.setAttribute("subcharts", !instance.setup.useCustomSubCharts);
        mainTestValuesElem.setAttribute("precision", !instance.setup.useCustomPrecision);
       
        mainTestValuesElem.setAttribute("slippage", instance.setup.useCustomSlippage);
        mainTestValuesElem.setAttribute("distance", instance.setup.useCustomDistance);
        mainTestValuesElem.setAttribute("spread", instance.setup.useCustomSpread);        
        mainTestValuesElem.setAttribute("commissions", instance.setup.useCustomCommission);
        mainTestValuesElem.setAttribute("swap", instance.setup.useCustomSwap);        
    }

    function saveSetup(parentElem) {
        var setupElem = createChild(parentElem, 'Setup', AppService.xmlDoc, true);
        setupElem.setAttribute("dateFrom", instance.setup.dateFrom);
        setupElem.setAttribute("dateTo", instance.setup.dateTo);
        setupElem.setAttribute("testPrecision", instance.setup.precision);
        setupElem.setAttribute("session", instance.setup.session);
        setupElem.setAttribute("slippage", instance.setup.slippage);
        setupElem.setAttribute("minDist", instance.setup.engine=='Tradestation' || instance.setup.engine=='MultiCharts' ? 0 : instance.setup.minDistance);
        setupElem.setAttribute("engine", instance.setup.engine);

        var spread = instance.setup.spread;         //spread is equal for all charts

        saveChart(setupElem, instance.setup);                //create main chart elem

        instance.correctSubcharts();

        if (arrayNotEmpty(instance.setup.subcharts)) {
            for (var i = 0; i < instance.setup.subcharts.length; i++) {
                var chart = angular.copy(instance.setup.subcharts[i]);
                chart.spread = spread;

                saveChart(setupElem, chart);        //create subchart elem
            }
        }

        //Commission settings are saved as an xml string of property grid config in setup.commissions object
        setupElem.appendChild(xmlToObject(instance.setup.commissions).find("Commissions")[0]);
        
        if (instance.setup.useCustomSwap=='custom' && instance.setup.swap){
            var swapXml = xmlToObject(instance.setup.swap);
            var swapTmp = swapXml.find("Swap")[0]
            setupElem.appendChild(swapTmp);
        }

        return setupElem;
    }

    function saveChart(parentElem, chart) {
        var chartElem = createChild(parentElem, 'Chart', AppService.xmlDoc, true);
        chartElem.setAttribute("symbol", chart.symbol);
        chartElem.setAttribute("timeframe", chart.timeframe);
        chartElem.setAttribute("spread", chart.spread);
    }

    this.correctPrecision = function (precision) {
        var precisions = SQConstants.getConstants().precisions;
        for (var i = 0; i < precisions.length; i++) {
            if (precisions[i].value == precision || precisions[i].name == precision) return precisions[i].value;
        }
    }

    var instance = this;

    var configLoaded = false;

    this.commissionMethods = DataService.commissionMethods;
    this.precisions = angular.copy(SQConstants.getConstants().precisions);
    this.setup = {}

});