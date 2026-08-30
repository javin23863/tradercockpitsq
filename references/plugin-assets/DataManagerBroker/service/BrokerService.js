angular.module('app.brokers').service('BrokerService', function($rootScope, $q, BackendService, SQEvents) {
        
        this.saveBroker = function(details, callback){
            var id = details.id;
            var created = id===null;

            BackendService.sendRequest('/brokers/save', details, function(result){
                if (created){
                    $rootScope.showSuccess("Broker was added");
                }else{
                    $rootScope.showSuccess("Broker was updated");
                }

                hidePopup('#addBrokerModal');

                if(callback){
                    callback(result.success);
                }
            });
        }

        this.removeBrokers = function(brokers){
            var data = {
                brokers: brokers
            }
            BackendService.sendRequest('/brokers/delete', data, function(result){
                var count = brokers.split(",").length;
                $rootScope.showSuccess(count > 1 ? "Brokers removed" : "Broker removed");
            }, 'POST');
        }

        this.import = function(data){
            BackendService.sendRequest('/brokers/import', data, function(result){
                $rootScope.showSuccess("Stocks were imported");
            }, 'POST');
        }   

        this.importGetOverview = function(data,callback){
            BackendService.sendRequest('/brokers/importGetOverview', data, function(result){
                if(callback){
                    callback(result);
                }
            }, 'POST');
        }

        this.importSessionInstrument = function(data, callback){
            BackendService.sendRequest('/brokers/importSessionInstrument', data, function(result){
                if (callback){
                    callback(result);
                }
            }, 'POST');
        }

        this.updateData = function(data){
            BackendService.sendRequest('/brokers/updateData', data, function(result){
                $rootScope.showSuccess('Data update started, please check Log tab for more information.');
            }, 'POST');
        }  
        
        this.export = function(data){
            BackendService.sendRequest('/brokers/export', data, function(result){
                $rootScope.showSuccess("Stocks were exported");
            }, 'POST');
        } 
          
        this.listStocks = function(data, callback){
            BackendService.sendRequest('/brokers/listStocks', data, callback);
        }
        
        this.editStocks = function(data, callback){
            BackendService.sendRequest('/brokers/editStocks', data, callback, 'POST');
        }

        this.saveXml = function (data) {
            BackendService.sendRequest('/brokers/saveXml', data, function (result) {
                if (result.success) {
                    $rootScope.showSuccess(result.success);
                }
            });
        }

        this.loadXml = function (data) {
            BackendService.sendRequest('/brokers/loadXml', data, function (result) {
                if (result.success) {
                    $rootScope.showSuccess(result.success);
                }
            });
        }
    });