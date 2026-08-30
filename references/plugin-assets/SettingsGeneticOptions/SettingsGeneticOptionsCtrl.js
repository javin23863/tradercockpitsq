angular.module('app.settings').controller('SettingsGeneticOptionsCtrl', function ($rootScope, $scope, $timeout, $element, L, SQEvents, SQConstants, SettingsGeneticOptionsService) {
    console.log("SettingsGeneticOptionsCtrl init");

    $scope.afterCGInit = function (data) {
        $scope.config.condGridInstance.setDefaultSampleType(constants.sampleTypes.in);

        var columns = SQConstants.getColumnsByType([constants.databankColumnTypes.general], null, true);
        $scope.config.condGridInstance.setData(columns, constants.recentColumns);

        $scope.config.condGridInstance.onlyMainData();

        if(SettingsGeneticOptionsService.conditionsToSet){
            var conditionsObj = angular.copy(SettingsGeneticOptionsService.conditionsToSet);
            SettingsGeneticOptionsService.conditionsToSet = null;
            $scope.config.condGridInstance.setConditionsObj(conditionsObj);
        }

        $timeout(function(){ SettingsGeneticOptionsService.controllerReady = true; }, 0, false);
    }

    $scope.onCGChange = function (grid) {
        onSettingsChange();
    }

    $scope.stopConditionChanged = function (value) {
        $scope.config.stopCondition.type = value;
    }

    $scope.printEvoFitnessRestartType = function() {
        var item = getItem(SettingsGeneticOptionsService.evoFitnessRestartTypes, "value", $scope.config.evoFitnessRestartType);
        return item ? item.name : $scope.config.evoFitnessRestartType;
    }

    //- Event Handlers --------------------------------------------------------------

    $scope.tab.shouldSaveSettings = function(){
        var shouldSaveSettings = settingsChanged;
        settingsChanged = false;
        return shouldSaveSettings;
    }

    function onSettingsChange() {
        if(!SettingsGeneticOptionsService.loaded || !SettingsGeneticOptionsService.controllerReady) return;

        settingsChanged = true;
        SettingsGeneticOptionsService.saveSettings();
    }

    function onEvent(event, data){
        if(event == SQEvents.get('SETTINGS_TAB_ACTIVE')){
            var active = data.title == $scope.tab.title;
            watchersHandler.onActivated(active);

            if(active && !SettingsGeneticOptionsService.loaded){
                SettingsGeneticOptionsService.loadSettings();
            }
        }
        else if(event == SQEvents.get('SETTINGS_TAB_RELOAD')){
            if($scope.tab.display && (data == $scope.tab.title || data == 'all')) {
                SettingsGeneticOptionsService.loadSettings();
            }
        }
        else return;
        
        try { $scope.$digest(); } catch(er) {}
    }
    
    var destroyConfigWatch = $scope.$watch("config", function(newValue, oldValue){
        onSettingsChange();
    }, true);
    
    $scope.$on("$destroy", function(){
        SQEvents.removeListener(listenerId);
        destroyConfigWatch();
        SettingsGeneticOptionsService.controllerReady = false;
    });

    //- Initialization --------------------------------------------------------------

    var settingsChanged = false;

    var constants = SQConstants.getConstants();

    $scope.config = SettingsGeneticOptionsService.config;
    $scope.evoFitnessRestartTypes = SettingsGeneticOptionsService.evoFitnessRestartTypes;

    var listenerId = "SettingsWhatToRetestCtrl";
    SQEvents.addListener(listenerId, [SQEvents.get('SETTINGS_TAB_RELOAD'), SQEvents.get('SETTINGS_TAB_ACTIVE')], onEvent);

    var watchersHandler = new WatchersHandler($($element));
    $timeout(function(){ watchersHandler.onActivated(false); }, 0, false);

});