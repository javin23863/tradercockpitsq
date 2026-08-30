angular.module('app.settings').controller('SimpleRetestSettingsCtrl', function ($scope, $timeout, AppService, SQConstants, SQEvents, L, SettingsWhatToRetestService) {

    function init(){
        if(window.appConfig.product != 'TASKMANAGER'){      //in task manager the data is loaded by SettingsWhatToRetestCtrl
            SettingsWhatToRetestService.loadAvailableDatabanks(function(){
                SettingsWhatToRetestService.loadSettings();
            });
        }
    }

    $scope.showAdvancedSettings = function (event) {
        AppService.switchToPanel('settings');
        event.stopPropagation();
        $timeout(tabsShowHiddenTabs, 0, false);
    }

    $scope.saveConfig = function(){
        SettingsWhatToRetestService.saveSettings();
        AppService.updateTaskXML();
    }

    function onEvent(event, data){
        if(event == SQEvents.get("RELOAD_DATABANKS")){  
            SettingsWhatToRetestService.loadAvailableDatabanks();
        }
        else if (event == SQEvents.get('CUSTOM_DATABANK_ACTION')) {
            switch (data.name) {
                case "rename": 
                    SettingsWhatToRetestService.loadAvailableDatabanks(function(){
                        $scope.config.inputDatabank = $scope.config.inputDatabank == data.oldName ? data.newName : $scope.config.inputDatabank;
                        $scope.config.outputDatabank = $scope.config.outputDatabank == data.oldName ? data.newName : $scope.config.outputDatabank;
                        $scope.saveConfig();
                    });
                    break;
            }
        }
        else return;
        
        try { $scope.$digest(); } catch(er) {}
    }

    $scope.isDashboard = function(){
        return AppService.getActivePanel() == "dashboard"
    }

    $scope.projectStates = SQConstants.getConstants().runningStatuses;

    $scope.retestOptions = [
        {
            title: L.tsq('Retest options'),
            settings: [ 
                { name : L.tsq("Trading options"), configElemName : "Options" },
                { name : L.tsq("Money Management"), configElemName : "RiskMoneyManagement" }
            ]
        }
    ];

    $scope.config = SettingsWhatToRetestService.config;
    $scope.availableDatabanks = SettingsWhatToRetestService.availableDatabanks;
    
    $scope.loaded = false;
    $scope.settingsPanelInstance = { onLoaded : function(){ $scope.loaded = true; } };

    $timeout(init, 0, false);

    var listenerId = "SimpleRetestSettingsCtrl";
    SQEvents.addListener(listenerId, [SQEvents.get("RELOAD_DATABANKS"), SQEvents.get('CUSTOM_DATABANK_ACTION')], onEvent);

});