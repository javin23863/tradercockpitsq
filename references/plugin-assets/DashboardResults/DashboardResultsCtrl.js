angular.module('app.dashboardResults').controller('DashboardResultsCtrl', function ($rootScope, $scope, sqPlugin, SQEvents, SQConstants, $timeout, DashboardResultsService, AppService, SQWebSocketService) {
    console.log("DashboardResults controller initialized");

    var projectName = window.appConfig.project;
    $scope.instances = [];

    function init() {
        for(var i = 0; i < 3; i++) {
            $scope.instances.push({
                'ranking' : i
            });
        }
    }   

    function getCallbackId(){
        return "DashboardResults"; 
    }

    function onBestResultsUpdate(channelData) {
        for(var i = 0; i < $scope.instances.length; i++) {
            $scope.instances[i].print(channelData[i]);
        }
    }

    function reset() {
        for(var i = 0; i < $scope.instances.length; i++) {
            $scope.instances[i].print(null);
        }
    }

    function onEvent(event, data){
        if(event == SQEvents.get('APP_PROJECT_CHANGED')){
            SQWebSocketService.unsubscribe(projectName, channels.bestResults, getCallbackId());
            
            projectName = data;
            reset();

            if(data != null){
                SQWebSocketService.subscribe(projectName, channels.bestResults, getCallbackId(), onBestResultsUpdate);
                DashboardResultsService.projectChanged(projectName);
            }
        }
        else if(event == SQEvents.get('APP_PANEL_CHANGED')){
            if(projectName == window.appConfig.project || projectName == 'TaskManager') {
                return;
            }
    
            panelActive = data == 'dashboard';
            projectName = window.appConfig.project;
           
            DashboardResultsService.getSettings({ projectName: projectName }, function(data) {
                DashboardResultsService.setSampleType(data.sampleType);
            });
        }
        else return;
        
        try { $scope.$digest(); } catch(er) {}
    }

    $scope.$on("$destroy", function(){
        SQWebSocketService.unsubscribe(AppService.getProject(), channels.bestResults, getCallbackId());
        SQEvents.removeListener(listenerId);
    });

    var channels = SQConstants.getConstants().channels;
    var panelActive = true;

    init();

    SQWebSocketService.subscribe(AppService.getProject(), channels.bestResults, getCallbackId(), onBestResultsUpdate);
    
    var listenerId = "DashboardResultsCtrl";
    SQEvents.addListener(listenerId, [SQEvents.get('APP_PROJECT_CHANGED'), SQEvents.get('APP_PANEL_CHANGED')], onEvent);

});