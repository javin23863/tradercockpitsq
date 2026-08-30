angular.module('app.resultsdatabankactions.load').controller('LoadPopupCtrl', function ($scope, $rootScope, $timeout, SQEvents, AppService, LoadService, L, DatabankService) {

    $scope.onSave = function() {
        saveAlias();
    }
    
    function saveAlias() {
        var selectedInstrument = $scope.instrumentChooser.getInstrument();

        var aliasData = {
            alias: $scope.info.symbol,
            instrument: selectedInstrument.instrument
        }

        LoadService.addAlias(aliasData, function(result) {
            $rootScope.showSuccess(result.success);

            $scope.selectInstrument = false;

            var instrumentData = {
                projectName: AppService.getProject(),
                reportName: $scope.info.reportName,
                symbol: $scope.info.symbol,
                instrument: selectedInstrument.instrument    
            }     

            LoadService.instrumentSelected(instrumentData, databankLoading).then(function(result){
                if(result.data.error){
                    hidePopup("loadModal");
                    $rootScope.showError(result.data.error);
                }  
            });
        });
    }

    function onEvent(event, data){
        if(event == LoadService.progressUpdateEvent){
            if(!isPopupOpen('#loadModal')){
                showPopup('#loadModal');
            }

            if(data.selectInstrument) {
                $scope.info.symbol = data.selectInstrument.symbol;
                $scope.info.reportName = data.selectInstrument.reportName;

                $scope.selectInstrument = true;
            } else {
                $scope.selectInstrument = false;
            }

            if(valueFilled(data.percent)) {
                $scope.info.progress = data.percent;

                if(data.percent == 100) {
                    if(data.error) {
                        if(data.error != 'canceled'){
                            $rootScope.showError(data.error);
                        }
                    } else {
                        $rootScope.showSuccess(L.tsq("Report(s) loaded"));
                    }

                    $scope.info.progress = 0;
                    hidePopup('#loadModal');
                } 
            }

            try { $scope.$digest(); } catch (err) {}
        }
    }

    $scope.$on('$destroy', function(){
        SQEvents.removeListener(listenerId);
    });

    $scope.cancel = function() {
        $rootScope.showConfirm(L.tsq("Cancel loading"), L.tsq("Do you really want to cancel loading records?"), function(confirmed){
            if(confirmed){
                DatabankService.cancelRecordsLoading().then(function(result){
                    if(result.data.success){
                        hidePopup('#loadModal');                                 
                    }
                    else {
                        $rootScope.showError(result.data.error);
                    }
                });
            }
        }, true);
    }

    $scope.selectInstrument = false;
    $scope.info = {
        progress : 0,
        symbol : null,
        reportName : null
    }

    $scope.instrumentChooser = {};

    var databankLoading = false;
    var listenerId = "LoadCtrl-" + window.appConfig.product;

    SQEvents.addListener(listenerId, [
        LoadService.progressUpdateEvent
    ], onEvent);

});