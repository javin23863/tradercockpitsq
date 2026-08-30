angular.module('app.settings').controller('AdvancedTMCtrl', function ($rootScope, $scope, $q, $element, SQEvents, AppService, $timeout, L, AdvancedTMService) {
    console.log("AdvancedTM controller initialized");

    function init() {
        AdvancedTMService.loadConfig();
        
        $scope.isBuildTask = AppService.getTask().type == 'Build';

        if((!$scope.isBuildTask || !$rootScope.license.isUltimateVersion) && $scope.config.atmType == $scope.atmTypes.generate){
            $scope.config.atmType = $scope.atmTypes.fromStrategy;
        }
    }

    function save(){
        AdvancedTMService.saveConfig();
        settingsChanged = true;
        AppService.updateTaskXML();
    }

    $scope.onAddNewExit = function(){
        if (!$scope.config.enableATM) {
            return;
        }

        if($scope.config.exits.length > 4){
            $rootScope.showError(L.tsq("You can define max. 5 exits"));
            return;
        }

        $scope.selectedExit.positionSizeType = $scope.atmPositionSizes.percents;
        $scope.selectedExit.positionPercents = 50;
        $scope.selectedExit.fixedLots = 0.01;
        $scope.selectedExit.exitLevel = $scope.atmExitLevels.slBased;
        $scope.selectedExit.slMultiplicator = 1;
        $scope.selectedExit.ptMultiplicator = 1;
        $scope.selectedExit.fixedProfitType = $scope.atmTypes.typeFixedPips;
        $scope.selectedExit.fixedProfitPips = 50;
        $scope.selectedExit.fixedProfitATRMultiplicator = 1.2;
        $scope.selectedExit.fixedProfitATRPeriod = 20;
        $scope.selectedExit.trailingStopType = $scope.atmTypes.typeFixedPips;
        $scope.selectedExit.trailingStopPips = 50;
        $scope.selectedExit.trailingStopATRMultiplicator = 1.2;
        $scope.selectedExit.trailingStopATRPeriod = 20;
        $scope.selectedExit.limitExitAfterBars = false;
        $scope.selectedExit.maximumBars = 50;
        $scope.selectedExit.moveSL2BE = false;
        $scope.selectedExit.moveSL2BEAddPips = false;
        $scope.selectedExit.sl2beType = $scope.atmMoveSL2BETypes.fixedPips;
        $scope.selectedExit.addPipsValue = 10;

        $scope.editing = false;

        showPopup('#addNewExitPopup');
    }

    $scope.getATRFixedPipsLabel = function(type){
        if(type == $scope.atmTypes.typeFixedPips){
            return L.tsq("Fixed value (in pips)");
        }
        else if(type == $scope.atmTypes.typeAtrBased){
            return L.tsq("ATR-Based");
        }
        else {
            return "";
        }
    }
    
    $scope.getSL2BETypeLabel = function(type){
        var _type = getItem($scope.atmMoveSL2BEOptions, "value", type);
        return _type ? _type.name : "";
    }

    $scope.onSaveExit = function(){
        var newExit = angular.copy($scope.selectedExit);
        newExit.description = AdvancedTMService.getDescription(newExit);

        if($scope.editing){
            $scope.config.exits.splice($scope.exitIndex, 1, newExit);
        }
        else {
            $scope.config.exits.push(newExit);
        }

        save();
        
        hidePopup('#addNewExitPopup');
    }

    $scope.onEditExit = function(index){
        if (!$scope.config.enableATM) {
            return;
        }
        $scope.selectedExit = angular.copy($scope.config.exits[index]);
        $scope.editing = true;
        $scope.exitIndex = index;
        showPopup('#addNewExitPopup');
    }

    $scope.onDeleteExit = function(index){
        if (!$scope.config.enableATM) {
            return;
        }
        $scope.config.exits.splice(index, 1);
        save();
    }

    $scope.onLoadFromFile = function(){
        var fileExtension = {
            name: 'xml',
            description: L.tsq('XML Files')
        };

        $rootScope.showFilePicker(L.tsq("Select file"), "loadATMs", true, false, null, fileExtension, null, function (paths) {
            if (paths) {
                AppService.loadFile(paths[0], function(fileContent){
                    AdvancedTMService.loadConfig(fileContent);
                    $rootScope.showSuccess(L.tsq("Settings loaded"));
                    save();
                });
            }
        });
    }

    $scope.onSaveToFile = function(){
        var fileExtension = {
            name: 'xml',
            description: L.tsq('XML Files')
        };

        var elATMs = AppService.getCurrentTaskTabSettings("ATMs");
        var fileContent = new XMLSerializer().serializeToString(elATMs);

        $rootScope.saveFile(L.tsq("Select file"), fileExtension, "saveATMs", "ATMConfig", fileContent);
    }

    $scope.onAddPipsTypeChanged = function(){

    }

    $scope.onHelp = function(){
        AppService.openBrowser("https://strategyquant.com/doc/strategyquant/settings-atm/#exit-types-and-configuration");
    }

    $scope.onHowItWorks = function(){
        AppService.openBrowser("https://strategyquant.com/doc/strategyquant/multiple-exits-generation-scale-out-atm");
    }

    $scope.settingsChanged = function(){
        settingsChanged = true;
        save();
    }

    //- Event Handlers --------------------------------------------------------------

    $scope.tab.shouldSaveSettings = function () {
        var shouldSaveSettings = settingsChanged;
        settingsChanged = false;
        return shouldSaveSettings;
    }
/*
    $scope.$watch("config.atmType", function(){
        if(loaded){
            save();
        }
    });
*/
    function onEvent(event, data) {
        if (event == SQEvents.get('SETTINGS_TAB_RELOAD')) {
            if ($scope.tab.display && (data == $scope.tab.title || data == 'all')) {
                init();
            }
        }
        else if (event == SQEvents.get('SETTINGS_TAB_ACTIVE')) {
            var active = data.title == $scope.tab.title;
            if (active) {
                if (!loaded) {
                    init();
                    loaded = true;
                }
            }

            watchersHandler.onActivated(active);
        }
        if(event == SQEvents.get('LICENSE_CHANGED')) {
            if((!$scope.isBuildTask || !$rootScope.license.isUltimateVersion) && $scope.config.atmType == $scope.atmTypes.generate){
                $scope.config.atmType = $scope.atmTypes.fromStrategy;
            }
        }
        else return;

        try { $scope.$digest(); } catch (er) { }
    }

    $scope.$on('$destroy', function () {
        SQEvents.removeListener(listenerId);
    });

    //- Initialization --------------------------------------------------------------

    $scope.atmTypes = AdvancedTMService.atmTypes;
    $scope.atmPositionSizes = AdvancedTMService.atmPositionSizes;
    $scope.atmExitLevels = AdvancedTMService.atmExitLevels;
    $scope.atmMoveSL2BETypes = AdvancedTMService.atmMoveSL2BETypes;
    $scope.atmMoveSL2BEOptions = AdvancedTMService.atmMoveSL2BEOptions;

    $scope.config = AdvancedTMService.config;

    $scope.selectedExit = {};
    $scope.editing = false;
    $scope.isBuildTask = false;

    var loaded = false;
    var settingsChanged = false;

    var listenerId = "AdvancedTMCtrl";
    SQEvents.addListener(listenerId, [SQEvents.get('SETTINGS_TAB_RELOAD'), SQEvents.get('SETTINGS_TAB_ACTIVE'), SQEvents.get('LICENSE_CHANGED')], onEvent);

    var watchersHandler = new WatchersHandler($($element));
    $timeout(function () { watchersHandler.onActivated(false); }, 0, false);

});