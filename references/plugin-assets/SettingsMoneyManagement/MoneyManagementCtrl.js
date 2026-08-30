angular.module('app.settings').controller('MoneyManagementCtrl', function ($rootScope, $scope, $element, $q, $timeout, AppService, MoneyManagementService, SQConstants, SQEvents) {
    console.log("Money Management controller initialized");

    function init(forceReload) {
        $scope.isAutoRetestTask = AppService.getTask().type == 'AutomaticRetest';

        MoneyManagementService.init();

        var mmSettings = MoneyManagementService.getMMSettings();
        var rmSettings = MoneyManagementService.getRMSettings();

        if(!mmSettings || !rmSettings) return;

        $scope.config.mmPropGridInstance.setSettings(mmSettings);
        curMMMethod = mmSettings.method;

        $scope.config.rmPropGridInstance.setSettings(rmSettings);
        curRMMethod = rmSettings.method;

        $timeout(function(){ 
            loaded = true; 

            var engine = AppService.getCurrentEngine();
            enableOptionsBasedOnEngine(engine);
        }, 0, false);
    }

    $scope.onMMPropGridChange = function(){
        var type = $scope.config.mmPropGridInstance.getSelectedType();

        //switching to other method
        if(curMMMethod != type){
            curMMMethod = type;

            var mmSettings = MoneyManagementService.getMMSettings(type);
            if(mmSettings.method == type){
                $scope.config.mmPropGridInstance.setSettings(mmSettings);
                return;
            }
        }

        onSettingsChange();
    }

    $scope.onRMPropGridChange = function() {
        var type = $scope.config.rmPropGridInstance.getSelectedType();

        //switching to other method
        if(curRMMethod != type){
            curRMMethod = type;
            
            var rmSettings = MoneyManagementService.getRMSettings(type);
            if(rmSettings.method == type){
                $scope.config.rmPropGridInstance.setSettings(rmSettings);
                return;
            }
        }

        onSettingsChange();
    }

    $scope.onCustomSettingsChange = function(){
        onSettingsChange();
    }

    //- Event Handlers --------------------------------------------------------------
    
    $scope.tab.shouldSaveSettings = function(){
        var shouldSaveSettings = settingsChanged;
        settingsChanged = false;
        return shouldSaveSettings;
    }

    function onSettingsChange(){
        if(loaded){
            settingsChanged = true;
            MoneyManagementService.saveConfig();
        }
    }

    var destroyICWatcher = $scope.$watch('config.initialCapital', function() {
        if(loaded) {
            onSettingsChange();
        }
    });

    function onEvent(event, data){
        if(event == SQEvents.get('SETTINGS_TAB_RELOAD')){
            if($scope.tab.display && (data == $scope.tab.title || data == 'all')) {
                loaded = false;
                init();
            }
        }
        else if(event == SQEvents.get('SETTINGS_TAB_ACTIVE')){
            var active = data.title == $scope.tab.title;
            if(active &&!loaded){
                init();
            }
    
            watchersHandler.onActivated(active); 

        } 
        else if(event == SQEvents.get('MAIN_SETUP_CHANGED')) {
            enableOptionsBasedOnEngine(data.engine);
        }
        else return;
        
        try { $scope.$digest(); } catch(er) {}
    }

    function enableOptionsBasedOnEngine(engine) {
        var engineKey = SQConstants.getEngineKey(engine);

        var type = $scope.config.mmPropGridInstance.getSelectedType();
        var found = false;

        let methods = MoneyManagementService.mmMethods;
        $scope.mmMethods.length = 0;

        for (var i = 0; i < methods.length; i++) {
            let method = methods[i];

            if(checkForEngine(engineKey, method.engine)) {
                if(method.class == type) {
                    found = true;
                }

                $scope.mmMethods.push(method);
            }
        }

        if(!found &&  $scope.mmMethods.length > 0) {
            $scope.config.mmPropGridInstance.setSelectedType($scope.mmMethods[0].class);
        }

        try { $scope.$digest(); } catch(er) {}
    }

    $scope.$on('$destroy', function(){
        destroyICWatcher();
        SQEvents.removeListener(listenerId);
    });

    //- Initialization -------------------------------------------------------------

    var loaded = false;
    var settingsChanged = false;

    var curMMMethod, curRMMethod;

    $scope.mmMethods = angular.copy(MoneyManagementService.mmMethods);
    $scope.rmMethods = angular.copy(MoneyManagementService.rmMethods);

    $scope.config = MoneyManagementService.config;

    $scope.isAutoRetestTask = AppService.getTask().type == 'AutomaticRetest';

    var listenerId = "MoneyManagementCtrl";
    SQEvents.addListener(listenerId, [
        SQEvents.get('SETTINGS_TAB_RELOAD'), 
        SQEvents.get('SETTINGS_TAB_ACTIVE'), 
        SQEvents.get('MAIN_SETUP_CHANGED')]
    , onEvent);

    var watchersHandler = new WatchersHandler($($element));
    $timeout(function(){ watchersHandler.onActivated(false); }, 0, false);

});