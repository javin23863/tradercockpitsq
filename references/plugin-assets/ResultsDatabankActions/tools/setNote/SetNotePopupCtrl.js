angular.module('app.resultsdatabankactions.tools.setNote').controller('SetNotePopupCtrl', function ($rootScope, $scope, L, DatabankService, DatabankActionsService) {
    
    $rootScope.setNoteInit = function(projectName, databankName, strategies, tab) {
        $scope.data = {
            projectName: projectName,
            databankName: databankName,
            strategies: strategies,
            tab: tab
        }
    }

    $scope.onSubmit = function(){
        if (DatabankActionsService.selectedStrategies==''){
            $rootScope.showError(L.tsq("You must select some strategy."));
            return;
        }
        
        $scope.config.strategies = DatabankActionsService.selectedStrategies.split(','); 
        $scope.config.projectName = $scope.data.projectName;
        $scope.config.databankName = $scope.data.databankName;

        var tab = $scope.data.tab;

        DatabankService.setNote($scope.config).then(function(result){
            if (result.error) {
                $rootScope.showError(L.tsq("Error while set note.") + result.error);
            }else{
                window.parent.hidePopup('#SetNoteModal');

                tab.loadStrategies(false, function(){
                    tab.getGrid().bodyRedraw(true);
                    tab.refreshView();
                }, true);
            }
        });
    }

    $scope.config = {
        note: "",
        selectedStrategies:false
    }

    console.log('SetNotePopupCtrl', $scope);
});