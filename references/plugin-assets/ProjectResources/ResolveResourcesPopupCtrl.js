angular.module('app.projectresources').controller('ResolveResourcesPopupCtrl', function ($rootScope, $scope, $timeout, SQEvents, SQConstants, BackendService, L, SQEquityDataService, SQFuturesDataService) {
    console.log("ResolveResourcesPopupCtrl initialized");

    $scope.sqDataRequired = true;
    $scope.sqFuturesDataSubscription = SQFuturesDataService.subscription;
    $scope.sqEquityDataSubscription = SQEquityDataService.subscription;

    $rootScope.onResolveResources = function (xml, resources, action, callback, configFilePath) {
        configXML = xml;
        resourcesXML = resources;
        actionType = action;
        loadCallback = callback;
        filePath = configFilePath;

        if (resourcesXML) {
            loadXMLResources();
            $scope.$evalAsync();

            showPopup("#resolveResourcesPopup");

            $timeout(function () {
                $('#resolveResourcesPopup [data-toggle="tooltip"]').tooltip({
                    "trigger": "hover",
                    "template": '<div class="tooltip" role="tooltip"><div class="arrow"></div><div class="tooltip-inner larger-left"></div></div>',
                });
            }, 0, false);
        }
        else {
            $rootScope.onCheckCustomResources(configXML, actionType, loadCallback, filePath);
        }
    }

    $scope.onSymbolAction = function (symbol, action) {
        symbol.action = action;
        updateNote();
    }

    $scope.onSubmit = function () {
        var elResources = xmlToObject(resourcesXML).find("Resources")[0];

        var symbolsAdded = [];

        if ($scope.resources.missingSymbols.length) {
            var elSymbols = getChildElement(elResources, "Symbols");
            var symbolElems = getChildElements(elSymbols, "Symbol");

            for (var a = 0; a < $scope.resources.missingSymbols.length; a++) {
                var action = $scope.resources.missingSymbols[a].action;

                if (!symbolElems[a]) {
                    $rootScope.showError("Error while resolving resources! Symbol element doesn't exist");
                    break;
                }

                symbolElems[a].setAttribute("action", action);

                if (action == 'replace') {
                    symbolElems[a].setAttribute("replaceSymbol", $scope.resources.missingSymbols[a].replaceSymbol);
                }
                else if(action == 'add' || action == 'overwrite'){
                    symbolsAdded.push($scope.resources.missingSymbols[a].name);
                }
            }
        }

        if ($scope.resources.missingSessions.length) {
            var elSessions = getChildElement(elResources, "Sessions");
            var sessionElems = getChildElements(elSessions, "Session");

            for (var a = 0; a < $scope.resources.missingSessions.length; a++) {
                var action = $scope.resources.missingSessions[a].action;

                if (!sessionElems[a]) {
                    $rootScope.showError("Error while resolving resources! Session element doesn't exist");
                    break;
                }

                sessionElems[a].setAttribute("action", action);

                if (action == 'replace') {
                    sessionElems[a].setAttribute("replaceSession", $scope.resources.missingSessions[a].replaceSession);
                }
            }
        }

        if ($scope.resources.missingBrokers.length) {
            var elBrokers = getChildElement(elResources, "Brokers");
            var brokerElems = getChildElements(elBrokers, "Broker");

            for (var a = 0; a < $scope.resources.missingBrokers.length; a++) {
                var action = $scope.resources.missingBrokers[a].action;

                if (!brokerElems[a]) {
                    $rootScope.showError("Error while resolving resources! Broker element doesn't exist");
                    break;
                }

                brokerElems[a].setAttribute("action", action);

                if (action == 'replace') {
                    brokerElems[a].setAttribute("replaceBroker", $scope.resources.missingBrokers[a].replaceSession);
                }
            }
        }

        if ($scope.resources.missingInstruments.length) {
            var elInstruments = getChildElement(elResources, "Instruments");
            var instrumentsElems = getChildElements(elInstruments, "InstrumentInfo");

            for (var a = 0; a < $scope.resources.missingInstruments.length; a++) {
                var action = $scope.resources.missingInstruments[a].action;

                if (!instrumentsElems[a]) {
                    $rootScope.showError("Error while resolving resources! Instrument element doesn't exist");
                    break;
                }

                instrumentsElems[a].setAttribute("action", action);

                if (action == 'replace') {
                    instrumentsElems[a].setAttribute("replaceInstrument", $scope.resources.missingInstruments[a].replaceInstrument);
                }
            }
        }

        BackendService.sendRequest('project/resolveResources', { configXML: configXML, resourcesXML: xmlToString(elResources), actionType: actionType, filePath }, function (data) {
            symbolsCreated = data.newSymbols || [];

            $rootScope.showSuccess(data.success);

            if(data.continueLoading){
                $rootScope.onCheckCustomResources(data.configXML, actionType, loadCallback, filePath, data.taskConfigs);
            }
            else {
                loadCallback(data);
            }

            if(symbolsAdded.length){
                $rootScope.onShowAddedResourcesPopup(symbolsAdded);
            }

        }, 'POST');
    }

    $scope.onCancelImport = function () {
        hidePopup("#resolveResourcesPopup");
        /*
        $rootScope.showConfirm(L.tsq("Cancel import"), L.tsq("Do you really want to cancel the import?"), function (confirmed) {
            if (confirmed) {
                //TMProjectsService.cancelProjectImport(function(result){
                    $rootScope.showSuccess("Import canceled");
                    hidePopup("#resolveResourcesPopup");
                //});
            }
        }, true);
        */
    }

    $scope.onLoadAsIs = function(){
        loadCallback({ loadAsIs : true });  
    }

    function updateNote() {
        for (var i = 0; i < $scope.resources.missingSymbols.length; i++) {
            var symbolInfo = $scope.resources.missingSymbols[i];

            if (symbolInfo.action == 'add') {
                $scope.resources.noteType = 1;
                $scope.resources.submitText = L.tsq("Add missing symbols (stops loading the config)");
                return;
            }
        }

        $scope.resources.submitText = L.tsq("Load config using these settings");
        $scope.resources.noteType = 2;
    }

    function loadXMLResources() {
        var elResources = xmlToObject(resourcesXML).find("Resources")[0];
        var elSymbols = getChildElement(elResources, "Symbols");
        var symbolElems = getChildElements(elSymbols, "Symbol");

        $scope.sqDataRequired = false;
        $scope.resources.missingSymbols.length = 0;

        if (symbolElems.length) {
            for (var i = 0; i < symbolElems.length; i++) {
                var elSymbol = symbolElems[i];
                var name = getAttrStringValue(elSymbol, "name");
                var status = getAttrIntValue(elSymbol, "status");
                var missingDays = getAttrIntValue(elSymbol, "missingDays");

                var source = getAttrIntValue(elSymbol, "source");
                if((source == SQConstants.getConstants().dataSource.sqequity && !$scope.sqEquityDataSubscription.eodSubscriptionActive) || 
                   (source == SQConstants.getConstants().dataSource.sqfutures && !$scope.sqFuturesDataSubscription.eodSubscriptionActive)) {
                    $scope.sqDataRequired = true;
                }

                source = getItem(SQConstants.getConstants().dataSources, "value", source);
                source = source ? source.name : "N/A";

                var symbol = {
                    name: name,
                    source: source,
                    statusCode: status,
                    status: getStatusText(status, missingDays),
                    action: getSymbolAction(status, name), 
                    info: getSymbolInfo(elSymbol),
                    replaceSymbol: getReplaceSymbol(status, name),
                    missingDays: missingDays
                }

                $scope.resources.missingSymbols.push(symbol);
            }
        }

        var elSessions = getChildElement(elResources, "Sessions");
        var sessionElems = getChildElements(elSessions, "Session");

        $scope.resources.missingSessions.length = 0;

        if (sessionElems.length) {
            for (var i = 0; i < sessionElems.length; i++) {
                var elSession = sessionElems[i];
                var name = getAttrStringValue(elSession, "name");
                var status = getAttrIntValue(elSession, "status");

                var session = {
                    name: name,
                    status: getStatusText(status),
                    action: "add",
                    info: getSessionInfo(elSession).replace(new RegExp("<br>", 'g'), "\n"),
                    replaceSession: $scope.sessions[0].name
                }

                $scope.resources.missingSessions.push(session);
            }
        }

        var elBrokers = getChildElement(elResources, "Brokers");
        var brokerElems = getChildElements(elBrokers, "Broker");
        $scope.resources.missingBrokers.length = 0;
        if (brokerElems.length) {
            for (var i = 0; i < brokerElems.length; i++) {
                var elBroker = brokerElems[i];
                var name = getAttrStringValue(elBroker, "name");
                var status = getAttrIntValue(elBroker, "status");

                var broker = {
                    name: name,
                    status: getStatusText(status),
                    action: "add",
                    replaceBroker: $scope.brokers[0].name
                }

                $scope.resources.missingBrokers.push(broker);
            }
        }

        var elInstruments = getChildElement(elResources, "Instruments");
        var instrumentElems = getChildElements(elInstruments, "InstrumentInfo");
        $scope.resources.missingInstruments.length = 0;
        if (instrumentElems.length) {
            for (var i = 0; i < instrumentElems.length; i++) {
                var elInstrument = instrumentElems[i];
                var name = getAttrStringValue(elInstrument, "instrument");
                var status = getAttrIntValue(elInstrument, "status");

                var instrument = {
                    name: name,
                    status: getStatusText(status),
                    action: "add",
                    replaceBroker: $scope.brokers[0].name
                }

                $scope.resources.missingInstruments.push(instrument);
            }
        }


        updateNote();
    }

    function getSymbolAction(status, symbolName){
        if(status == $scope.resourceStatuses.notEnoughData || getItem(symbolsCreated, 'originalName', symbolName)) return "replace";
        else return "add";
    }

    function getReplaceSymbol(status, symbolName){
        if(!$scope.symbols || !$scope.symbols.length) return null;

        var replaceSymbol = $scope.symbols[0].symbol;

        if(status != $scope.resourceStatuses.doesntExist) {
            for(var i=0; i<symbolsCreated.length; i++){
                var info = symbolsCreated[i];
                if(info.originalName == symbolName){
                    return info.createdName;
                }
            }

            var symbolData = getItem($scope.symbols, 'symbol', symbolName);

            if(symbolData && symbolData.rows > 0){
                return symbolName;
            }
        }
        
        return replaceSymbol;
    }

    $scope.showDifferences = function(symbol) {
        $scope.symbol = symbol;

        showPopup("#diffResourcesPopup");
    }

    function getStatusText(status, missingDays) {
        switch (status) {
            case $scope.resourceStatuses.doesntExist: return L.tsq('Not found'); 
            case $scope.resourceStatuses.differs: return L.tsq('Differences'); 
            case $scope.resourceStatuses.notEnoughData: return L.tsq('Missing %d days', [missingDays]); 
            default: return L.tsq("Unknown status '%d'", [status]);
        }
    }

    function getSymbolInfo(elSymbol) {
        var constants = SQConstants.getConstants();
        var elInstrumentInfo = getChildElement(elSymbol, "InstrumentInfo");

        //config ------------------------------------------------------------------------
        var infoConfig = {};
        infoConfig.symbol = getAttrStringValue(elSymbol, "name");
        infoConfig.dateRange = timeToDateString(getAttrIntValue(elSymbol, "dateFrom")) + " - " + timeToDateString(getAttrIntValue(elSymbol, "dateTo"));
        infoConfig.instrument = getAttrStringValue(elInstrumentInfo, "instrument", "N/A");

        var source = getItem(constants.dataSources, "value", getAttrIntValue(elSymbol, "source"));
        infoConfig.source = source ? source.name : "N/A";

        var barType = getAttrIntValue(elSymbol, "barType");
        infoConfig.barType = barType == constants.barDataTypes.startOfBar ? L.tsq("Start of bar time") : L.tsq("End of bar time");
        infoConfig.precision = getAttrStringValue(elSymbol, "precision", "N/A");
        infoConfig.timezone = getAttrStringValue(elSymbol, "timezone", "N/A");

        //sq ------------------------------------------------------------------------
        var symbolData = getItem(constants.data, 'symbol', infoConfig.symbol);
        var infoSQ = {};
        infoSQ.symbol = symbolData ? symbolData.symbol : "";
        infoSQ.dateRange = symbolData ? (symbolData.dateFrom>0 ? timeToDateString(symbolData.dateFrom) + " - " + timeToDateString(symbolData.dateTo) : L.tsq("no data"))  : "";
        infoSQ.instrument = symbolData ? symbolData.instrument  : "";

        var source = symbolData ? getItem(constants.dataSources, "value", symbolData.source) : null;
        infoSQ.source = source ? source.name : "";

        infoSQ.barType = symbolData ? (symbolData.barType == constants.barDataTypes.startOfBar ? L.tsq("Start of bar time") : L.tsq("End of bar time")) : "";
        infoSQ.precision = symbolData ? symbolData.timeframe  : "";
        infoSQ.timezone = symbolData ? symbolData.timezone  : "";

        return {
            name: [infoConfig.symbol, infoSQ.symbol, infoConfig.symbol!=infoSQ.symbol],
            dateRange: [infoConfig.dateRange, infoSQ.dateRange, infoConfig.dateRange!=infoSQ.dateRange],
            instrument: [infoConfig.instrument, infoSQ.instrument, infoConfig.instrument!=infoSQ.instrument],
            source: [infoConfig.source, infoSQ.source, infoConfig.source!=infoSQ.source],
            barType: [infoConfig.barType, infoSQ.barType, infoConfig.barType!=infoSQ.barType],
            precision: [infoConfig.precision, infoSQ.precision, infoConfig.precision!=infoSQ.precision],
            timezone: [infoConfig.timezone, infoSQ.timezone, infoConfig.timezone!=infoSQ.timezone]
        };
    }

    function getSessionInfo(elSession) {
        var info = "";
        var elementElems = getChildElements(elSession, "Element");

        for (var i = 0; i < elementElems.length; i++) {
            var elElement = elementElems[i];
            var dayFrom = getAttrStringValue(elElement, "dayFrom");
            var dayTo = getAttrStringValue(elElement, "dayTo");
            var timeFrom = getAttrStringValue(elElement, "timeFrom");
            var timeTo = getAttrStringValue(elElement, "timeTo");
            var eod = getAttrBooleanValue(elElement, "eod");

            info += dayFrom + " " + timeFrom + " - " + dayTo + " " + timeTo + (eod ? " EOD" : "") + "<br>";
        }

        return info;
    }

    $scope.resourceStatuses = SQConstants.getConstants().projectResourceStatuses;
    $scope.sessions = SQConstants.getConstants().sessions;
    $scope.symbols = SQConstants.getConstants().data;
    $scope.instruments = SQConstants.getConstants().instruments.filter(i => !i.instrument.startsWith('[') && !i.instrument.endsWith(']'));
    $scope.brokers = SQConstants.getConstants().brokers.filter(b => b.mtUse);

    $scope.symbol = null;

    $scope.isBasketAlias = isBasketAlias;

    $scope.resources = {
        missingSymbols: [],
        missingSessions: [],
        missingBrokers:[],
        missingInstruments:[] 
    };

    var configXML;
    var resourcesXML;
    var loadCallback;
    var actionType;
    var filePath;

    var symbolsCreated = [];

    SQEvents.addListener("ResolveResourcesPopupCtrl", [SQEvents.get('SESSIONS_CHANGED'), SQEvents.get('DATA_CHANGED')], function (event, data) {
        if(event == SQEvents.get('SESSIONS_CHANGED')){
            $scope.sessions.length = 0;
            var sessions = angular.copy(SQConstants.getConstants().sessions);

            for (let i = 0; i < sessions.length; i++) {
                var session = sessions[i];
                if (session.elements && session.elements.length) {
                    $scope.sessions.push(session);
                }
            }
        }
        else {
            loadToArray($scope.symbols, SQConstants.getConstants().data);

            for(let i=symbolsCreated.length-1; i>=0; i--){
                let symbolCreated = symbolsCreated[i];
                if(!getItem($scope.symbols, "symbol", symbolCreated)){          //handling situation when the symbol was deleted in Data Manager
                    symbolsCreated.splice(i, 1);
                }
            }
        }
    });

});