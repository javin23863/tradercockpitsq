angular.module('app.settings').controller('ProfitTargetCtrl', function ($rootScope, $scope, ProfitTargetService) {
    
    $scope.onSameAsSLChange = function(){
        if(ProfitTargetService.initialized){
            if($scope.config.sameAsSL){
                ProfitTargetService.applySLSettings();
            }
            else {
                ProfitTargetService.applyPTSettings();
            }
        }
    }

    $scope.settingPlugin.loadSettings = ProfitTargetService.loadSettings;
    $scope.settingPlugin.checkSettings = ProfitTargetService.checkSettings;
    $scope.settingPlugin.saveSettings = ProfitTargetService.saveSettings;
    $scope.settingPlugin.resetSettings = ProfitTargetService.resetSettings;
    $scope.settingPlugin.getDescription = ProfitTargetService.getDescription;

    $scope.config = ProfitTargetService.config;
    $scope.guiConfig = ProfitTargetService.guiConfig;

});