angular.module('app.settings').controller('SimpleAutoPortfolioBuilderCtrl', function ($scope, $timeout, AppService, SQConstants, SQEvents, L, AutomaticPortfolioBuilderService, DatabankService) {

    console.log("Loaded SimpleAutoPortfolioBuilderCtrl...")

    function init(){
        if(window.appConfig.product != 'TASKMANAGER') {      //in task manager the data is loaded by SettingsWhatToRetestCtrl
            AutomaticPortfolioBuilderService.loadSettings();
        }
    }

    $scope.showAdvancedSettings = function (event) {
        AppService.switchToPanel('settings');
        event.stopPropagation();
        $timeout(tabsShowHiddenTabs, 0, false);
    }

    $scope.saveConfig = function(){
        AutomaticPortfolioBuilderService.saveSettings();
        AppService.updateTaskXML();
    }

    $scope.refreshNumberOfPossiblePortfolios = function() {
        let data = {
            project: AppService.getProject(),
            databank: DatabankService.getTaskInputDatabank(),
            min: $scope.config.minStrategies,
            max: $scope.config.maxStrategies,
            selected: -1
        }

        if(DatabankService.getRetestSelected()) {
            var selectedStrategies = DatabankService.getStrategiesToRetest();
            if (selectedStrategies) {
                data.selected = selectedStrategies.length;
            }

        }

        AutomaticPortfolioBuilderService.getNumberOfPossiblePortfolios(data, function(data) {
            $scope.config.numberOfPossiblePortfolios = data.numberOfPossiblePortfolios;
            try { $scope.$digest(); } catch(er) {}
        });
    }

    function onEvent(event, data){
        try { $scope.$digest(); } catch(er) {}
    }

    $scope.isDashboard = function(){
        return AppService.getActivePanel() == "dashboard"
    }

    $scope.projectStates = SQConstants.getConstants().runningStatuses;

    $scope.config = AutomaticPortfolioBuilderService.config;
    
    $scope.loaded = true;
    $scope.settingsPanelInstance = { onLoaded : function(){ $scope.loaded = true; } };

    $timeout(init, 0, false);

    var listenerId = "SimpleAutoPortfolioBuilderCtrl";
    SQEvents.addListener(listenerId, [SQEvents.get('CUSTOM_DATABANK_ACTION')], onEvent);

});