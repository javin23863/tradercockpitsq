angular.module('app.settings').service('DataService', function ($rootScope, $q, AppService, BackendService, SQConstants, ComplexitiesService, L) {

    this.loadCommissionMethods = function (force) {
        if (force || this.commissionMethods.length == 0) {
            commissionsDeffered = $q.defer();

            BackendService.sendRequest('constants/listCommissionMethods', null, function (result) {
                loadToArray(instance.commissionMethods, result.methods);
                commissionsDeffered.resolve(instance.commissionMethods);
            });
        }

        return commissionsDeffered.promise;
    }

    this.getCommissionSettings = function (type, configElem) {
        var methods = parseSettingsMethods(configElem);
        var targetMethod = methods.length > 0 ? methods[0] : null;

        for (var i = 0; i < methods.length; i++) {
            var method = methods[i];

            if ((type && method.method == type) || (!type && method.use)) {
                targetMethod = method;
                break;
            }
        }

        var method = targetMethod ? targetMethod.method : 'None';
        var params = targetMethod ? targetMethod.params : [];

        return {
            method: method,
            params: params
        };
    }

    this.updateCommissionSettings = function (type, pgInstance, configElem) {
        var methodElems = getChildElements(configElem, 'Method');
        var targetMethodObj;

        for (var i = 0; i < methodElems.length; i++) {
            var methodElem = methodElems[i];
            var methodType = getAttrStringValue(methodElem, 'type');

            if (methodType == type) {
                targetMethodObj = methodElem;
                break;
            }
            methodElem.setAttribute("use", 'false');
        }

        if (!targetMethodObj) {
            var methodElem = AppService.xmlDoc.createElement('Method');
            targetMethodObj = configElem.appendChild(methodElem);
            targetMethodObj.setAttribute("type", type);
        }

        targetMethodObj.setAttribute("use", 'true');
        pgInstance.setPropertyValues(AppService.xmlDoc, targetMethodObj);
    }

    this.loadDataConfig = function (forceReload) {
        if (configLoaded && !forceReload) return;

        configLoaded = false;

        instance.EnableOOSSettings = AppService.getTask().type != 'Optimize';

        if(forceReload) ComplexitiesService.loadComplexities();

        var dataElem = AppService.getCurrentTaskTabSettings("Data");

        instance.config.setups.length = 0;

        instance.config.oosGraphShown = false;
        instance.config.oosRanges = [];

        addDefaultMainChart(instance.config.setups);

        if (dataElem) {
            var setupsElem = getChildElement(dataElem, 'Setups', true);
            if (setupsElem) {
                var setupElems = getChildElements(setupsElem, 'Setup');
                if (setupElems.length > 0) {
                    instance.config.setups.length = 0;

                    for (var i = 0; i < setupElems.length; i++) {
                        var setupElem = setupElems[i];
                        instance.loadSetup(instance.config.setups, setupElem);
                    }
                }
                else {
                    instance.saveSetup(setupsElem, instance.config.setups[0], AppService.xmlDoc);
                }
            }

            var oosElem = getChildElement(dataElem, 'OutOfSample', true);
            if (oosElem) {
                instance.config.oosGraphShown = getAttrBooleanValue(oosElem, 'showGraph', false);

                var rangeElems = getChildElements(oosElem, 'Range');
                for (var i = 0; i < rangeElems.length; i++) {
                    if(i == instance.MaxRanges) return;

                    var rangeElem = rangeElems[i];
                    instance.config.oosRanges.push({
                        type: getAttrStringValue(rangeElem, 'type', 'oos'),
                        dateFrom: getAttrValue(rangeElem, 'dateFrom'),
                        dateTo: getAttrValue(rangeElem, 'dateTo'),
                    });
                }
            }
        }

        configLoaded = true;

        ComplexitiesService.correctComplexities();

        instance.saveSetups(instance.config.setups, dataElem);  //config may have been corrected - save everything into XML
    }

    function addDefaultMainChart(setupsArray) {
        setupsArray.push({
            engine: SQConstants.getConstants().engines[0],
            precision: SQConstants.getConstants().precisions[0].value,
            session: 'No Session',
            timeframe: 'H1',
            slippage: 0,
            minDistance: 0,
            commissions: instance.getCommissionConfig(null),
            swap: instance.getSwapSettings(null),
            subcharts: []
        });

        instance.correctSubcharts(setupsArray[0]);
    }

    function correctedDate(date){
        if(!date) return date;
        let parts = date.split(".")
        if(parts.length != 3) return date;
        if(parts[0].length != 4) return date;
        parts[1] = (parts[1].length == 1 ? "0" : "") + parts[1]
        parts[2] = (parts[2].length == 1 ? "0" : "") + parts[2]
        return parts.join(".")
    }

    this.loadSetup = function (setupsArray, setupElem) {
        if (setupElem) {
            var setup = {
                dateFrom: correctedDate(getAttrValue(setupElem, 'dateFrom')),
                dateTo: correctedDate(getAttrValue(setupElem, 'dateTo')),
                precision: instance.correctPrecision(getAttrValue(setupElem, 'testPrecision')),
                session: getAttrValue(setupElem, 'session'),
                engine: getAttrValue(setupElem, 'engine') || SQConstants.getConstants().engines[0],
                slippage: getAttrValue(setupElem, 'slippage'),
                minDistance: getAttrValue(setupElem, 'minDist'),
                commissions: instance.getCommissionConfig(setupElem),
                swap: instance.getSwapSettings(setupElem),
                subcharts: []
            };

            var chartElems = getChildElements(setupElem, 'Chart');
            
            for (var i = 0; i < chartElems.length; i++) {
                var chartElem = chartElems[i];
                var chart = {};
                if (!i) {
                    loadChart(setup, chartElem);
                }
                else {
                    loadChart(chart, chartElem);
                    setup.subcharts.push(chart);
                }
            }

            instance.changeSetupDetails(setup, []);      //corrects symbol settings if invalid
            setupsArray.push(setup);
            return setup;
        }
    }

    this.getCommissionConfig = function (setupElem) {
        try {
            return xmlToString(getChildElement(setupElem, "Commissions"));
        } catch (err) {
            return "<Commissions><Method type=\"None\" use=\"true\" /></Commissions>";
        }
    }

    this.getSwapSettings = function (setupElem) {
        try {
            return xmlToString(getChildElement(setupElem, "Swap"));
        } catch (err) {
            return DEFAULT_SWAP_SETTINGS;
        }
    }

    function loadChart(chartObj, chartElem) {
        chartObj.symbol = getAttrValue(chartElem, 'symbol');
        chartObj.timeframe = getAttrValue(chartElem, 'timeframe');
        chartObj.spread = getAttrFloatValue(chartElem, 'spread', 0);
    }

    this.changeSetupDetails = function(object, precisions, oldSymbol){
        var symbolData = instance.getSymbolData(object.symbol, object.engine);
        object.symbolReplaced = false;

        if(!symbolData || !symbolData.rows){
            object.symbolReplaced = true;
            symbolData = instance.getSymbolData(null, object.engine);
        }

        precisions.length = 0;

        if(symbolData){
            if(!instance.currentDateRangeCanBeUsed(symbolData, object)){
                object.dateFrom = timeToDateString(symbolData.dateFrom);
                object.dateTo = timeToDateString(symbolData.dateTo);
            }
            
            var symbolChanged = valueFilled(oldSymbol) && symbolData.symbol != oldSymbol;
            var instrumentDetails = symbolData.instrumentDetails;

            if(!valueFilled(instrumentDetails)){
                $rootScope.showError(L.tsq("Error - Instrument for symbol '" + symbolData.symbol + "' doesn't exist!"));
            }

            var commissions = !symbolChanged && object.commissions ? object.commissions : getCommissionsConfig(instrumentDetails ? instrumentDetails.commissions : object.commissions);
            
            object.symbol = symbolData.symbol;
            object.timeframe = symbolData.timeframe ? getNewTimeframe(symbolData.timeframe, object.timeframe) : undefined;
            object.minTimeframe = symbolData.timeframe;
            object.commissions = commissions;
        
            object.spread = symbolChanged && valueFilled(instrumentDetails) ? instrumentDetails.defaultSpread : object.spread;
            object.spread = valueFilled(object.spread) ? object.spread : 0;
            
            object.slippage = symbolChanged && valueFilled(instrumentDetails) ? instrumentDetails.defaultSlippage : object.slippage;
            object.slippage = valueFilled(object.slippage) ? object.slippage : 0;

            object.minDistance = valueFilled(object.minDistance) ? object.minDistance : 0;
            object.barType = symbolData.barType;

            //load available precisions and set preferred precision
            object.precision = loadPrecisions(symbolData, precisions, object.precision, object.engine);

            let swap = !symbolChanged && object.swap ? object.swap : ((instrumentDetails.swap && instrumentDetails.swap !== "null" && instrumentDetails.swap !== "undefined") ? instrumentDetails.swap : DEFAULT_SWAP_SETTINGS);
            object.swap = swap;
        }
        else {
            console.warn("Symbol data of symbol '" + object.symbol + "' not found and no data available");

            var defaultDate = getDateString(new Date(0));
            object.dateFrom = defaultDate;
            object.dateTo = defaultDate;
        }

        if(arrayNotEmpty(object.subcharts)){
            instance.correctSubcharts(object);
        }
    }

    this.correctSubchartsOptim = function (dataObj) {
        configLoaded = false;
        var setup = instance.config.setups[0]

        var setupsElem = getChildElement(dataObj, 'Setups');
        var setupElem = getChildElements(setupsElem, 'Setup')[0];
        var chartElems = getChildElements(setupElem, 'Chart');

        setup.subcharts.length = 0;

        for (var i = 0; i < chartElems.length; i++) {
            var chartElem = chartElems[i];
            
            if (i>0) {
                var chart = {};
                loadChart(chart, chartElem);
                setup.subcharts.push(chart);
            }
        }

        configLoaded = true;

        var dataElem = AppService.getCurrentTaskTabSettings("Data");
        instance.saveSetups(instance.config.setups, dataElem);  //config may have been corrected - save everything into XML

        return chartElems.length-1;
    }

    this.correctSubcharts = function (setup) {
        if(instance.config.complexities) {
            setup.subcharts.length = instance.config.complexities.length - 1;
        }

        for (var i = 0; i < setup.subcharts.length; i++) {
            setup.subcharts[i] = setup.subcharts[i] || {
                symbol: 'Same as main chart',
                timeframe: 'H1',
                spread: setup.spread
            };

            correctChart(setup.subcharts[i], setup.symbol, setup.engine);
        }
    }

    function correctChart(chart, setupSymbol, engine){
        var realSymbol = chart.symbol == "Same as main chart" ? setupSymbol : chart.symbol;
        var symbolData = getItem(SQConstants.getConstants().data, 'symbol', realSymbol);

        if(!symbolData) {
            var data = SQConstants.getConstants().data;
            if(valueFilled(engine)) {
                for(var i=0; i<data.length; i++){
                    let curDataObj = data[i];
                    
                    if((engine === "Stockpicker" && curDataObj.symbol.indexOf("[") >= 0) || (engine !== "Stockpicker" && curDataObj.symbol.indexOf("[") < 0)){
                        symbolData = curDataObj;

                        break;
                    }
                }
            } else {
                symbolData = data[0];
            }

            chart.symbol = symbolData ? symbolData.symbol : "";
        }

        if (symbolData) {
            chart.timeframe = getNewTimeframe(symbolData.timeframe, chart.timeframe);
            chart.minTimeframe = symbolData.timeframe;
            chart.barType = symbolData.barType;
        }
        else {
            chart.symbol = "";
            chart.timeframe = "";
        }
    }

    this.getSymbolData = function(symbol, engine){
        var data = SQConstants.getConstants().data;
        if(arrayEmpty(data)) return;

        let instruments = SQConstants.getConstants().instruments;
        let dataObj = null;

        for(var i=0; i<data.length; i++){
            let curDataObj = data[i];

            if(valueFilled(symbol)){
                if(curDataObj.symbol === symbol){
                    dataObj = curDataObj;
                    break;
                }
            }
            else {
                if(valueFilled(engine)){
                    if((engine === "Stockpicker" && curDataObj.symbol.indexOf("[") >= 0) || (engine !== "Stockpicker" && curDataObj.symbol.indexOf("[") < 0)){
                        dataObj = curDataObj;
                    }
                }
                else {
                    if(curDataObj.rows > 0){
                        dataObj = curDataObj;
                        break;
                    }
                }
            }
        }

        if(dataObj){
            dataObj = angular.copy(dataObj);
            dataObj.instrumentDetails = getItem(instruments, 'instrument', dataObj.instrument);
            return dataObj;
        }
    }

    this.currentDateRangeCanBeUsed = function(symbolData, setup){
        if(!setup.dateFrom || !setup.dateTo) return false;
        
        try {
            var oldDateFrom = getDate(setup.dateFrom, true);
            var oldDateTo = getDate(setup.dateTo, true);
            var symbolDateFrom = new Date(symbolData.dateFrom);
            var symbolDateTo = new Date(symbolData.dateTo);

            return dateIsAfter(oldDateFrom, symbolDateFrom, true) && 
                   !dateIsAfter(oldDateFrom, symbolDateTo, false) &&      
                   dateIsAfter(oldDateTo, symbolDateFrom, true) &&
                   !dateIsAfter(oldDateTo, symbolDateTo, false);
        } catch(err){
            console.error("currentDateRangeCanBeUsed", err);
            return false;
        }
    }
    
    this.loadAvailablePrecisions = function(precisions, symbol, engine){
        precisions.length = 0;

        var symbolData = getItem(SQConstants.getConstants().data, 'symbol', symbol);
        if(symbolData) {
            loadPrecisions(symbolData, precisions, null, engine);
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
    
    this.setSubchartData = function(target, source){
        target.symbol = source.symbol || '';
        target.timeframe = source.timeframe || '';
        target.spread = source.spread;
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

    function getCommissionsConfig(instrumentCommissions){
        return "<Commissions>" + instrumentCommissions + "</Commissions>";
    }

    this.convertRanges = function (ranges) {
        var convertedRanges = [];

        for (var i = 0; i < ranges.length; i++) {
            var range = ranges[i];

            convertedRanges.push({
                dateFrom: range[0],
                dateTo: range[1],
                type: range[2].class
            });
        }
        return convertedRanges;
    }

    this.getOOSRanges = function (ranges) {
        var convertedRanges = [];

        for (var i = 0; i < ranges.length; i++) {
            var range = ranges[i];

            convertedRanges.push([
                range.dateFrom,
                range.dateTo,
                range.type
            ]);
        }
        return convertedRanges;
    }

    this.getOOSGraphData = function (dateFrom, dateTo, symbol, session) {
        var deferred = $q.defer();
        var url = 'data/getSymbolData';
        var data = {
            dateFrom: dateFrom,
            dateTo: dateTo,
            symbol: symbol,
            session: session
        }

        BackendService.getPromise(url, data, 'POST').then(function (result) {
            if (result.data.success) {
                deferred.resolve({
                    success: true,
                    data: removeOffset(result.data.data)
                });
            }
            else {
                console.error("Cannot get OOS graph data." + result.data.error);
                deferred.resolve({ success: false, error : result.data.error });
            }
        });

        return deferred.promise;
    }

    /**
     * Removes offset from chart data
     */
    function removeOffset(data) {
        //find global minimum
        var minimum = null;
        for (var i = 0; i < data.length; i++) {
            var row = data[i];
            if (row[1] && (row[1] < minimum || minimum == null)) {
                minimum = row[1];
            }
        }

        //remove offset
        for (var i = 0; i < data.length; i++) {
            var row = data[i];
            row[1] -= minimum;
        }

        return data;
    }

    this.saveConfig = function () {
        var xmlDoc = AppService.xmlDoc;

        var settingsElem = AppService.getTaskConfig();
        if(!settingsElem) return;

        var dataElem = getChildElement(settingsElem, 'Data', true);
        if (dataElem) {
            removeAllChildren(dataElem);
        }
        else {
            dataElem = createChild(settingsElem, 'Data', xmlDoc, true);
        }

        instance.saveSetups(instance.config.setups, dataElem);

        var oosElem = createChild(dataElem, 'OutOfSample', xmlDoc, true);
        oosElem.setAttribute("showGraph", instance.config.oosGraphShown);
        if (arrayNotEmpty(instance.config.oosRanges) && instance.EnableOOSSettings) {
            for (var i = 0; i < instance.config.oosRanges.length; i++) {
                var range = instance.config.oosRanges[i];
                saveOOSRange(oosElem, range, xmlDoc);
            }
        }
    }

    this.saveSetups = function (setups, dataElem) {
        var xmlDoc = AppService.xmlDoc;

        if (!dataElem) {
            var settingsElem = AppService.getTaskConfig();
            dataElem = getChildElement(settingsElem, 'Data', true);
            if(!dataElem) return;
        }

        var setupsElem = createChild(dataElem, 'Setups', xmlDoc);

        removeAllChildren(setupsElem);

        if (arrayNotEmpty(setups)) {
            for (var i = 0; i < setups.length; i++) {
                var setup = setups[i];
                instance.saveSetup(setupsElem, setup, xmlDoc);
            }
        }
    }

    this.saveSetup = function(parentElem, setup, xmlDoc) {
        var setupElem = createChild(parentElem, 'Setup', xmlDoc, true);
        setupElem.setAttribute("dateFrom", setup.dateFrom);
        setupElem.setAttribute("dateTo", setup.dateTo);
        setupElem.setAttribute("testPrecision", setup.precision);
        setupElem.setAttribute("session", setup.session);
        setupElem.setAttribute("slippage", setup.slippage);
        setupElem.setAttribute("minDist", setup.engine=='Tradestation' || setup.engine=='MultiCharts' ? 0 : setup.minDistance);
        setupElem.setAttribute("engine", setup.engine);

        saveChart(setupElem, setup, xmlDoc);        //create main chart elem

        var spread = setup.spread;                  //update: spread is equal for all charts

        if (arrayNotEmpty(setup.subcharts)) {
            for (var i = 0; i < setup.subcharts.length; i++) {
                var chart = angular.copy(setup.subcharts[i]);
                chart.spread = spread;
                saveChart(setupElem, chart, xmlDoc);        //create subchart elem
            }
        }

        //Commission settings are saved as an xml string of property grid config in setup.commissions object
        setupElem.appendChild(xmlToObject(setup.commissions).find("Commissions")[0]);
        setupElem.appendChild(xmlToObject(setup.swap || DEFAULT_SWAP_SETTINGS).find("Swap")[0]);

        return setupElem;
    }

    function saveChart(parentElem, chart, xmlDoc) {
        var chartElem = createChild(parentElem, 'Chart', xmlDoc, true);
        chartElem.setAttribute("symbol", chart.symbol);
        chartElem.setAttribute("timeframe", chart.timeframe);
        chartElem.setAttribute("spread", chart.spread);
    }

    function saveOOSRange(parentElem, range, xmlDoc) {
        var rangeElem = createChild(parentElem, 'Range', xmlDoc, true);
        rangeElem.setAttribute("dateFrom", range.dateFrom);
        rangeElem.setAttribute("dateTo", range.dateTo);

        if(range.type!='oos') rangeElem.setAttribute("type", range.type);
    }

    /**
     * Recalculates the ranges - they should remain at the same position, but will have different dates
     */
    this.recalculateOOSRanges = function (ranges, oldDateFrom, oldDateTo, newDateFrom, newDateTo) {
        if (!newDateFrom || !newDateTo) return [];

        if (oldDateFrom == newDateFrom && oldDateTo == newDateTo) {
            oldDateFrom = getDate(oldDateFrom, true);
            oldDateTo = getDate(oldDateTo, true);
            newDateFrom = getDate(newDateFrom, true);
            newDateTo = getDate(newDateTo, true);

            //remove OOS ranges that don't fit into current date range
            for(var i=ranges.length - 1; i>=0; i--){
                var range = ranges[i];
                var dateFrom = getDate(range[0], true);
                var dateTo = getDate(range[1], true);

                if(dateFrom.getTime() >= newDateTo.getTime() || dateTo.getTime() <= newDateFrom.getTime()){
                    //remove oos that lay outside current date range
                    ranges.splice(i, 1);
                }
                else {
                    //correct oos dates
                    range[0] = dateFrom.getTime() >= newDateFrom.getTime() ? range[0] : timeToDateString(newDateFrom.getTime());
                    range[1] = dateTo.getTime() <= newDateTo.getTime() ? range[1] : timeToDateString(newDateTo.getTime());
                }
            }    
            return ranges;
        }

        oldDateFrom = getDate(oldDateFrom, true);
        oldDateTo = getDate(oldDateTo, true);
        newDateFrom = getDate(newDateFrom, true);
        newDateTo = getDate(newDateTo, true);

        var oldTimePeriod = parseInt((oldDateTo.getTime() - oldDateFrom.getTime()) / (1000 * 3600 * 24));     //in days
        var newTimePeriod = parseInt((newDateTo.getTime() - newDateFrom.getTime()) / (1000 * 3600 * 24));     //in days

        var newRanges = [];
        if (oldTimePeriod > 0 && newTimePeriod > 0) {
            for (var i = 0; i < ranges.length; i++) {
                var rangeDateFrom = getDate(ranges[i][0], true);
                var rangeDateTo = getDate(ranges[i][1], true);

                var newRangeTimeFrom = calculateNewTime(rangeDateFrom.getTime(), oldDateFrom.getTime(), oldDateTo.getTime(), newDateFrom.getTime(), newDateTo.getTime());
                var newRangeTimeTo = calculateNewTime(rangeDateTo.getTime(), oldDateFrom.getTime(), oldDateTo.getTime(), newDateFrom.getTime(), newDateTo.getTime());

                var newRangeDateFrom = timeToDateString(newRangeTimeFrom > newDateFrom.getTime() ? newRangeTimeFrom : newDateFrom.getTime());
                var newRangeDateTo = timeToDateString(newRangeTimeTo < newDateTo.getTime() ? newRangeTimeTo : newDateTo.getTime());

                var type = ranges[i][2];

                newRanges.push([newRangeDateFrom, newRangeDateTo, type]);
            }
        }

        return newRanges;
    }

    /**
     * Calculation of the new range time (in ms).
     * All parameters must be time values (Date.getTime())
     */
    function calculateNewTime(oldDate, oldDateFrom, oldDateTo, newDateFrom, newDateTo) {
        var oldDatePosCoef =  (oldDate - oldDateFrom) / (oldDateTo - oldDateFrom);
        var newDate = newDateFrom + (oldDatePosCoef * (newDateTo - newDateFrom));
        return newDate;
    }

    /**
     * Creates new OOS range in the middle of the first free space greater than 10% of the total range.
     * If there's not enough free space, it returns null.
     * 
     * Note: OOS ranges are added from the end of the history
     */
    this.createNewOOSRange = function (oosBounds, oosRanges, reset) {
        var dateFrom = getDate(oosBounds.dateFrom);
        var dateTo = getDate(oosBounds.dateTo);

        var totalDays = getDaysBetweenDates(dateFrom, dateTo);

        if(reset) {
            var newDateFrom = addDays(dateTo, -totalDays / 3);

            return [getDateString(newDateFrom), getDateString(dateTo)];
        } else {
            if (oosRanges.length == 0) {
                var dateTo = addDays(dateFrom, totalDays / 3);

                return [getDateString(dateFrom), getDateString(dateTo)];
            }

            var spaceDateFrom;
            var spaceDateTo;
            var freeSpace;

            if(dateFrom != getDate(oosRanges[0].dateFrom)) {
                spaceDateFrom = dateFrom;
                spaceDateTo = getDate(oosRanges[0].dateFrom);
                freeSpace = getDaysBetweenDates(spaceDateFrom, spaceDateTo);

                if (freeSpace / totalDays > 0.1) {
                    return getNewOOSRange(spaceDateFrom, spaceDateTo, freeSpace);
                }
            }

            for (var i = 0; i < oosRanges.length; i++) {
                var range = oosRanges[i];

                spaceDateFrom = getDate(range.dateTo)
                spaceDateTo = i == oosRanges.length-1 ? dateTo : getDate(oosRanges[i+1].dateFrom);

                freeSpace = getDaysBetweenDates(spaceDateFrom, spaceDateTo);

                //free space should be greater than 10% of the whole range
                if (freeSpace / totalDays > 0.1) {
                    return getNewOOSRange(spaceDateFrom, spaceDateTo, freeSpace);
                }
            }
        }

        return null;
    }

    /**
     * Returns array of date strings [dateFrom, dateTo] in the middle of the range.
     * If there is less than 3 days between the dates, the dates returned are the original ones.
     */
    function getNewOOSRange(dateFrom, dateTo, daysCount) {
        if (daysCount >= 3) {
            var offsetDays = Math.round(daysCount / 3);

            var newDateFrom = addDays(dateFrom, offsetDays);
            var newDateTo = addDays(dateTo, -offsetDays);

            return [getDateString(newDateFrom), getDateString(newDateTo)];
        }
        else return [getDateString(dateFrom), getDateString(dateTo)];
    }

    this.correctPrecision = function (precision) {
        var precisions = SQConstants.getConstants().precisions;
        for (var i = 0; i < precisions.length; i++) {
            if (precisions[i].value == precision || precisions[i].name == precision) return precisions[i].value;
        }
    }

    var instance = this;

    var configLoaded = false;
    var commissionsDeffered = $q.defer();

    const DEFAULT_SWAP_SETTINGS = "<Swap use=\"false\" type=\"money\" long=\"0\" short=\"0\" tripleSwapOn=\"WEDNESDAY\" />";

    this.MaxRanges = 20;
    this.EnableOOSSettings = AppService.getTask().type != 'Optimize';

    this.generationTypes = {
        random: 'random',
        genetic: 'genetic'
    };

    this.config = {
        complexities: ComplexitiesService.settings.complexities,
        setups: []
    }

    this.commissionMethods = [];

});