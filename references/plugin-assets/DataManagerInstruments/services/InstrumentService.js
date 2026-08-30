angular.module('app.instruments').service('InstrumentService', function($rootScope, $q, BackendService, SQEvents) {
        
        this.clone = function (data, callback) {
            BackendService.sendRequest('/instruments/clone', data, function (result) {
                if (result.success) {
                    $rootScope.showSuccess(result.success);
                    callback();
                }
            });
        }
    
        this.addInstrument = function(instrumentDetails, callback){
            SQEvents.notifyListeners(SQEvents.get("INSTRUMENT_CREATED"), instrumentDetails);

            BackendService.sendRequest('/instruments/addInstrument', instrumentDetails, function(result){
                $rootScope.showSuccess("Instrument added");
                hidePopup('#addInstrumentModal');

                if(callback){
                    callback(result.success);
                }
            });
        }

        this.editInstrument = function(instrumentDetails, callback){
            BackendService.getPromise('/instruments/editInstrument', instrumentDetails).then(function(result){
                if(result.data.success){
                    $rootScope.showSuccess("Instrument modified");
                    hidePopup('#addInstrumentModal');

                    if(callback){
                        callback(result.data.success);
                    }
                }
                else {
                    if(result.data.error.length > 70){
                        $rootScope.showErrorModal('Error', result.data.error, false, true);
                    }
                    else {
                        $rootScope.showError(result.data.error);
                    }
                }
            });
        }

        this.massEditInstrument = function(instrumentDetails, callback){
            BackendService.getPromise('/instruments/massEditInstrument', instrumentDetails).then(function(result){
                if(result.data.success){
                    $rootScope.showSuccess("Instruments modified");

                    if(callback){
                        callback(result.data.success);
                    }
                }
                else {
                    if(result.data.error.length > 70){
                        $rootScope.showErrorModal('Error', result.data.error, false, true);
                    }
                    else {
                        $rootScope.showError(result.data.error);
                    }
                }
            });
        }

        this.removeInstruments = function(instruments){
            var data = {
                instruments: instruments
            }
            BackendService.sendRequest('/instruments/removeInstrument', data, function(result){
                    $rootScope.showSuccess(result.success);
            }, 'POST');
        }

        this.save = function (data) {
            BackendService.sendRequest('/instruments/save', data, function (result) {
                if (result.success) {
                    $rootScope.showSuccess(result.success);
                }
            }, 'POST');
        }

        this.load = function (data) {
            BackendService.sendRequest('/instruments/load', data, function (result) {
                if (result.success) {
                    $rootScope.showSuccess(result.success);
                }
            });
        }
    });