angular.module('app.resultsdatabankactions.tools.edit.strategy').controller('EditStrategyCtrl', function ($rootScope, $scope, AppService, DatabankActionsService, SQEvents, $interval, AlgoWizardService) {

    $scope.onClick = function () {
        var data = DatabankActionsService.getSelectedStrategy();
        if (!valueFilled(data.strategy)) {
            $rootScope.showError("No Strategy selected.");
            return;
        }

        var taskSettings = AppService.getTaskConfig();

        DatabankActionsService.getStrategyXml(data, function (result) {
            var app = getApp("AlgoWizard");
            if (app == null) {
                console.log("App Wizard not found.");
                return;
            }

            if (!result.lastSettings && taskSettings) {
                result.lastSettings = xmlToString(taskSettings, true);
            }

            switchToApp("AlgoWizard");
            window.parent.broadcastEvent(SQEvents.get('APP_SWITCHED'), "AlgoWizard");
            showResults(false);

            // console.log("strategy edit: ", result)

            AlgoWizardService.editorAction("loadStrategy", {
                strategyXML: result.xml,
                lastSettingsXML: result.lastSettings,
                fileName: result.strategy
            });
        });
    }
});