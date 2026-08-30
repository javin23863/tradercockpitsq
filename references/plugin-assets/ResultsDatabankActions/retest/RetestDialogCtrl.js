angular.module('app.resultsdatabankactions.retest').controller('RetestDialogCtrl', function ($rootScope, $scope, BackendService, AppService, SQConstants, SQEvents, L) {
    
    $scope.onMove = function(move){
        if(!valueFilled(task)){
            $rootScope.showError(L.tsq("Cannot get current task name"));
            return;
        }

        BackendService.sendRequest('project/retest', { 
            strategies : selectedStrategies, 
            srcProjectName : project, 
            srcDatabankName: AppService.getDatabank().title,
            srcTaskName : task,
            move : move 
        }, function(result){
            if($scope.config.applyConfig){
                goToRetester(result.newConfig);
            } 
            else {
                goToRetester();
            }
        }, 'POST');
    }

    function goToRetester(newConfig) {
        var actionName;
        var params = {
            showProgress: true,
            progressTitle: L.tsq('Loading strategies'),
            progressMessage: L.tsq('Loading strategies')
        }

        if(valueFilled(newConfig)){
            actionName = "loadConfig";
            params.config = newConfig;
        }
        else {
            actionName = "reloadDatabanks";
        }

        AppService.sendActionRequest('RETESTER', actionName, params);
        
        window.switchToApp("RETESTER");
        window.parent.broadcastEvent(SQEvents.get('APP_SWITCHED'), "RETESTER");
    }

    var project = null;
    var task = null;
    var selectedStrategies = null;

    $scope.config = {
        applyConfig : true
    }

    SQEvents.addListener("retestDialogCtrl", [
        SQEvents.get("RETEST_DIALOG_DATA")
    ], function(event, data){
        project = data.project;
        task = data.task;
        selectedStrategies = data.strategies;
    });

});