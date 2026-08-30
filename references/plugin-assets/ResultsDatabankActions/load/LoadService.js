angular.module('app.resultsdatabankactions.load').service('LoadService', function($rootScope, $q, BackendService, SQConstants, SQEvents, AppService, SQWebSocketService) {

    this.addAlias = function(alias, callOnSuccess){
        BackendService.sendRequest('/instruments/addAlias', alias, callOnSuccess);
    }

    this.addInstrument = function(instrumentDetails, callOnSuccess){
        BackendService.sendRequest('/instruments/addInstrument', instrumentDetails, callOnSuccess);
    }

    this.instrumentSelected = function(data) {
        return BackendService.getPromise('/project/instrumentSelected', data);
    }

    this.subscribeToProgressChannel = function(){
        if(!subscribed){
            subscribed = true;
            SQWebSocketService.subscribe(projectName, progressChannel, getCallbackId(), onProgressUpdate); 
        }
    }

    function onProgressUpdate(channelData) {
        if(channelData.action == "load") {
            window.parent.broadcastEvent(instance.progressUpdateEvent, channelData);
        }
    }

    function getCallbackId(){
        return "LoadService-" + window.appConfig.product;
    }

    var instance = this;
    var subscribed = false;

    var progressChannel = SQConstants.getConstants().channels.progress;
    var projectName = window.appConfig.project;

    this.progressUpdateEvent = "load-progress";

    if(window.appConfig.product != 'SQUANT'){
        SQEvents.addListener(getCallbackId(), [SQEvents.get('APP_PROJECT_CHANGED')], function(event, data){
            if(event == SQEvents.get("APP_PROJECT_CHANGED")){
                SQWebSocketService.unsubscribe(projectName, progressChannel, getCallbackId());
                if(data != null){
                    SQWebSocketService.subscribe(data, progressChannel, getCallbackId(), onProgressUpdate); 
                }
                projectName = data;
            }
        });
    }

});