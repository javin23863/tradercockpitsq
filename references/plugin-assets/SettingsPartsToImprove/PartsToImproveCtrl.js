angular.module('app.settings').controller('PartsToImproveCtrl', function ($rootScope, $scope, $timeout, $q, $element, AppService, PartsToImproveService, SQEvents) {
    
    function init(){
        PartsToImproveService.init();
        $timeout(function(){ initialized = true; }, 0, false);
    }

    //- Event Handlers --------------------------------------------------------------
    
    $scope.getOptionName = function(value){
        var option = getItem($scope.actions, 'value', value);
        return option ? option.name : '';
    }

    $scope.onHowItWorks = function(){
        AppService.openBrowser("https://strategyquant.com/doc/strategyquant/multiple-exits-generation-scale-out-atm/");
    }

    $scope.tab.shouldSaveSettings = function(){
        var shouldSaveSettings = settingsChanged;
        settingsChanged = false;
        return shouldSaveSettings;
    }

    function onSettingsChange(){
        settingsChanged = true;
        PartsToImproveService.saveConfig();
    }

    var destroyConfigWatch = $scope.$watch("config", function(newValue, oldValue){
        if(initialized){
            if(newValue.entryRules.symmetry && (newValue.entryRules.long.use != oldValue.entryRules.long.use || newValue.entryRules.long.action != oldValue.entryRules.long.action)){
                $scope.config.entryRules.short.use = $scope.config.entryRules.long.use;
                $scope.config.entryRules.short.action = $scope.config.entryRules.long.action;
                return;
            }
            if(newValue.exitRules.symmetry && (newValue.exitRules.long.use != oldValue.exitRules.long.use || newValue.exitRules.long.action != oldValue.exitRules.long.action)){
                $scope.config.exitRules.short.use = $scope.config.exitRules.long.use;
                $scope.config.exitRules.short.action = $scope.config.exitRules.long.action;
                return;
            }

            onSettingsChange();
        }
    }, true);

    function onEvent(event, data){
        if(event == SQEvents.get('SETTINGS_TAB_RELOAD')){
            if($scope.tab.display && (data == $scope.tab.title || data == 'all')) {
                init();
                PartsToImproveService.updateMarketSides();
            }
        }
        else if(event == SQEvents.get('SETTINGS_TAB_ACTIVE')){
            var active = data.title == $scope.tab.title;
            if(active){
                if(!initialized){
                    init();
                }
                PartsToImproveService.updateMarketSides();
            }
    
            watchersHandler.onActivated(active);
        }
        if(event == SQEvents.get('LICENSE_CHANGED')) {
            if(!$rootScope.license.isUltimateVersion){
                $scope.config.improveATM = false;
            }
        }
        else return;
        
        try { $scope.$digest(); } catch(er) {}
    }
    
    $scope.$on("$destroy", function(){
        destroyConfigWatch();
        SQEvents.removeListener(listenerId);
    });
    
    //- Initialization --------------------------------------------------------------
    
    var initialized = false;
    var settingsChanged = false;
    
    $scope.actions = PartsToImproveService.actions;
    $scope.directions = PartsToImproveService.directions;
    $scope.getConditionText = PartsToImproveService.getConditionText;
    
    $scope.config = PartsToImproveService.config;

    var listenerId = "PartsToImproveCtrl";
    SQEvents.addListener(listenerId, [SQEvents.get('SETTINGS_TAB_RELOAD'), SQEvents.get('SETTINGS_TAB_ACTIVE'), SQEvents.get('LICENSE_CHANGED')], onEvent);

    var watchersHandler = new WatchersHandler($($element));
    $timeout(function(){ watchersHandler.onActivated(false); }, 0, false);
    
});