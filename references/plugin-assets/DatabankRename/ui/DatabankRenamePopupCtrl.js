angular.module('app.resultsdatabankactions.rename').controller('DatabankRenamePopupCtrl', function ($scope, $rootScope, $timeout, SQEvents, SQConstants, L, DatabankActionsService, BackendService, AppService) {

    $scope.onRename = function(){
        $rootScope.setProgressInfo("Renaming strategies", "");
                
        //sending request to the backend
        BackendService.sendRequest("/databank/rename", $scope.config, function(result) {
            hidePopup('#databankRenamePopup');
        }, 'POST');
    }

    function onEvent(event, data) {
        if(event == "showDatabankRenamePopup") {
            //opening popup
            if(!isPopupOpen('#databankRenamePopup')) {
                //obtaining details about the project, databank and selected strategies
                $scope.config.projectName = AppService.getProject();
                $scope.config.databankName = AppService.getDatabank().title;
                $scope.config.strategies = DatabankActionsService.selectedStrategies;

                //reseting values
                $scope.config.name = $scope.config.strategies.split(',')[0];
                $scope.config.prefix = null;
                $scope.config.postfix = null;

                $scope.count = $scope.config.strategies.split(',').length;

                //refreshing UI
                try { $scope.$digest(); } catch (err) {}

                //showing popup
                showPopup('#databankRenamePopup');
            }        
        }
    }

    $scope.$on('$destroy', function(){
        SQEvents.removeListener(listenerId);
    });
    
    $scope.config = {
        projectName: null,
        databankName: null,
        strategies: null,
        name: null,
        prefix: null,
        postfix : null  
    }

    $scope.count = 0;

    var listenerId = "DatabankRenamePopupCtrl-" + window.appConfig.product;

    SQEvents.addListener(listenerId, ["showDatabankRenamePopup"], onEvent);
});