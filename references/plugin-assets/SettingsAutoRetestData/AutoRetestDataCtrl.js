angular.module('app.settings').controller('AutoRetestDataCtrl', function ($rootScope, $scope, $q, $timeout, AutoRetestDataService, AppService, UniqueIdGenerator, SQEvents, SQConstants, L) {
    console.log("Settings->Data controller initialized");
    
    function init(callback, forceReload) {
        $q.all([ AutoRetestDataService.loadCommissionMethods() ]).then(function () {
            AutoRetestDataService.loadDataConfig(forceReload);

            $scope.symbolDateRange = AutoRetestDataService.getSymbolDateRange($scope.setup.symbol);
            $scope.additionalCharts.count = $scope.setup.subcharts ? $scope.setup.subcharts.length : 0;

            initCommissions();
            updateDateRange();

            $timeout(function(){ initialized = true; }, 0, false);

            if (callback) callback();
        });
    }

    $scope.afterPGInit = function(){
        commGridLoaded = true;
    }

    $scope.onCommPGChange = function () {
        var newType = $scope.commPGInstance.getSelectedType(); //method name
        var configElement = getCommissionsConfigElement();

        //console.error("onCommPGChange", newType);

        //switching to other method
        if (curCommissionType != newType) {
            curCommissionType = newType;

            var commissionSettings = AutoRetestDataService.getCommissionSettings(newType, configElement);

            if (commissionSettings.method == newType) {
                $scope.commPGInstance.setSettings(commissionSettings);
            }
        }
        
        AutoRetestDataService.updateCommissionSettings(newType, $scope.commPGInstance, configElement);
        $scope.setup.commissions = xmlToString(configElement);
    }

    $scope.getSwapLabel = function(){
        if(!$scope.setup.swap){
            return L.tsq("No swap");
        }

        let elSwap = xmlToObject($scope.setup.swap).find("Swap")[0];
        if(!getAttrBooleanValue(elSwap, "use", false)){
            return L.tsq("No swap");
        }

        let swapType = getAttrStringValue(elSwap, "type", $scope.constants.swapTypes.money);
        let swapLong = getAttrFloatValue(elSwap, "long", 0);
        let swapShort = getAttrFloatValue(elSwap, "short", 0);
        let typeChar = swapType === $scope.constants.swapTypes.money ? "$" : (swapType === $scope.constants.swapTypes.percent ? "%" : "points");

        return L.tsq("long: %s, short: %s", [(swapLong + typeChar), (swapShort + typeChar)]); 
    }

    $scope.getCommissionLabel = function() {           
        if(!commGridLoaded) return "N/A";

        var type = $scope.commPGInstance.getSelectedType();     
        if(!type) return "N/A";

        var data = $scope.commPGInstance.getSelectedValues();       
        
        var commissionsLabel = type;

        var method = getItem($scope.commissionMethods, "class", type);
        if(method) {
            commissionsLabel = L.tsq(method.display);

            for(var i=0; i<data.length; i++) {
                var property = data[i];
                var value = property.value;
                var key = property.key;
                
                return commissionsLabel.replace("#"+key+"#", value);
            }
        }

        return commissionsLabel;
    }
    
    $scope.getPrecisionLabel = function(){
        var selectedPrecision = getItem($scope.precisions, "value", $scope.setup.precision);
        return selectedPrecision ? L.tsq(selectedPrecision.name) : '';
    }

    function initCommissions() {
        $timeout(function () {
            var config = AutoRetestDataService.getCommissionSettings(null, getCommissionsConfigElement());
            $scope.commPGInstance.setSettings(config);
        }, 0, false);
    }

    function getCommissionsConfigElement() {
        return xmlToObject($scope.setup.commissions).find("Commissions")[0];
    }

    $scope.showOtherSettingsPopup = function () {
        showPopup('#otherParamsPopup-' + $scope.id);
    }

    $scope.showSwapSettingsPopup = function () {
        showPopup('#swapParamsPopup-' + $scope.id);
    }

    $scope.onUseCustomSwap = function(type){
        $scope.setup.useCustomSwap = type;
        onSettingsChange();
    }    

    $scope.onUseCustomDistance = function(type){
        $scope.setup.useCustomDistance = type;
        onSettingsChange();
    }

    $scope.onUseCustomSlippage = function(type){
        $scope.setup.useCustomSlippage = type;
        onSettingsChange();
    }

    $scope.onUseCustomCommission = function(type){
        $scope.setup.useCustomCommission = type;
        onSettingsChange();
    }    

    $scope.onUseCustomSpread = function(type){
        $scope.setup.useCustomSpread = type;
        onSettingsChange();
    }        

    $scope.resetDates = function () {
        var symbolData = AutoRetestDataService.getSymbolData($scope.setup.symbol);
        if (symbolData) {
            $scope.setup.dateFrom = timeToDateString(symbolData.dateFrom);
            $scope.setup.dateTo = timeToDateString(symbolData.dateTo);

            if ($scope.mainSetup == 'true' && $scope.onMainDatesChange) {
                $scope.onMainDatesChange({
                    dateFrom: $scope.setup.dateFrom,
                    dateTo: $scope.setup.dateTo,
                    reset: true
                });
            }
        }
    }

    $scope.onSubchartsChange = function(){
        $scope.setup.subcharts.length = $scope.additionalCharts.count;
    }

    function updateDateRange(){
        if(!$scope.symbolDateRange || $scope.symbolDateRange == "N/A") return;

        var dates = $scope.symbolDateRange.split(" - ");
        if (dates.length == 2) {
            $scope.symbolDateRangeFrom = dates[0];
            $scope.symbolDateRangeTo = dates[1];
        }
        else {
            var defaultDate = getDateString(new Date(0));
            $scope.symbolDateRangeFrom = defaultDate;
            $scope.symbolDateRangeTo = defaultDate;
        }
    }

    //- Event Handlers --------------------------------------------------------------

    $scope.tab.shouldSaveSettings = function () {
        var shouldSaveSettings = settingsChanged;
        settingsChanged = false;
        return shouldSaveSettings;
    }

    function onSettingsChange() {
        settingsChanged = true;
        AutoRetestDataService.saveConfig();
        SQEvents.notifyListeners(SQEvents.get('AR_MAIN_SETUP_CHANGED'), $scope.setup);
    }

    var destroyConfigWatch = $scope.$watch("setup", function (newValue, oldValue) {
        if (initialized && !disableWatch) {
            if(valueFilled(newValue.symbol)){
                if(newValue.symbol != oldValue.symbol){                
                    disableWatch = true;
                    $scope.symbolDateRange = AutoRetestDataService.getSymbolDateRange($scope.setup.symbol);
                    disableWatch = false;
                }
            }
            
            if(!newValue.useCustomSymbol){             
                disableWatch = true;
                $scope.symbolDateRange = "";
                $scope.symbolDateRangeFrom = "";
                $scope.symbolDateRangeTo = "";
                disableWatch = false;
            }

            AutoRetestDataService.changeSetupDetails($scope.setup, $scope.precisions, oldValue.symbol);

            onSettingsChange();
        }
    }, true);

    var destroySymbolDataRangeWatch = $scope.$watch("symbolDateRange", function (newValue, oldValue) {
        if (valueFilled(newValue)){
            updateDateRange();
        }
    });

    function handleSessionChange(newSession) {
        $scope.setup.session = newSession;

        onSettingsChange();
    }

    function onEvent(event, data) {
        if (event == SQEvents.get('SETTINGS_TAB_RELOAD')) {
            if (data == $scope.tab.title || data == 'all') {
                initialized = false;

                init(function () { try { $scope.$digest(); } catch (err) { } }, true);
            }
            return;
        }
        else if (event == SQEvents.get('SETTINGS_TAB_ACTIVE')) {
            tabActive = data.title == $scope.tab.title;
            //watchersHandler.onActivated(tabActive);
        }
        else if (event == SQEvents.get('SETTINGS_SESSION_CHANGED')) {
            if (!initialized) {
                init(function () {
                    handleSessionChange(data);
                });
            } else {
                handleSessionChange(data);
            }
        }
        else return;

        try {
            $scope.$digest();
        } catch (er) { }
    }

    $scope.$on("$destroy", function () {
        destroyConfigWatch();
        destroySymbolDataRangeWatch();
        destroySubchartsWatch();

        SQEvents.removeListener(listenerId);
        UniqueIdGenerator.removeId($scope.id);
    });

    //- Initialization --------------------------------------------------------------

    var initialized = false;
    var tabActive = false;
    var disableWatch = false;
    var settingsChanged = false;
    var commGridLoaded = false;
    var curCommissionType;

    $scope.setup = AutoRetestDataService.setup;
    $scope.id = UniqueIdGenerator.generateId();
    
    $scope.commPGInstance = {};
    $scope.commissionMethods = AutoRetestDataService.commissionMethods;
    
    $scope.constants = angular.copy(SQConstants.getConstants());
    $scope.precisions = AutoRetestDataService.precisions;

    $scope.symbolDateRange = "";

    $scope.additionalCharts = { count : 0 };

    init(null, true);

    var listenerId = "CustomDataCtrl";
    SQEvents.addListener(listenerId, [
        SQEvents.get('SETTINGS_TAB_RELOAD'),
        SQEvents.get('SETTINGS_TAB_ACTIVE'),
        SQEvents.get('SETTINGS_SESSION_CHANGED')
    ], onEvent);

});