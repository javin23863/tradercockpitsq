angular.module('app.baskets').service('BasketService', function($rootScope, $q, BackendService, SQEvents) {
        
        this.saveBasket = function(details, callback){
            var id = details.id;
            var created = id===null;

            BackendService.sendRequest('/basketOfStocks/save', details, function(result){
                if (created){
                    $rootScope.showSuccess("Stock group was added");
                }else{
                    $rootScope.showSuccess("Stock group was updated");
                }

                hidePopup('#addBasketModal');

                if(callback){
                    callback(result.success);
                }
            });
        }

        this.removeBaskets = function(baskets){
            var data = {
                baskets: baskets
            }
            BackendService.sendRequest('/basketOfStocks/delete', data, function(result){
                var count = baskets.split(",").length;
                $rootScope.showSuccess(count > 1 ? "Groups removed" : "Group removed");
            }, 'POST');
        }

        this.import = function(data){
            BackendService.sendRequest('/basketOfStocks/import', data, function(result){
                $rootScope.showSuccess("Stocks were imported");
            }, 'POST');
        }   

        this.updateData = function(data){
            BackendService.sendRequest('/basketOfStocks/updateData', data, function(result){
                $rootScope.showSuccess('Data update started, please check Log tab for more information.');
            }, 'POST');
        }  
        
        this.export = function(data){
            BackendService.sendRequest('/basketOfStocks/export', data, function(result){
                $rootScope.showSuccess("Stocks were exported");
            }, 'POST');
        } 
          
        this.listStocks = function(data, callback){
            BackendService.sendRequest('/basketOfStocks/listStocks', data, callback);
        }
        
        this.editStocks = function(data, callback){
            BackendService.sendRequest('/basketOfStocks/editStocks', data, callback, 'POST');
        }

        this.saveXml = function (data) {
            BackendService.sendRequest('/basketOfStocks/saveXml', data, function (result) {
                if (result.success) {
                    $rootScope.showSuccess(result.success);
                }
            });
        }

        this.loadXml = function (data) {
            BackendService.sendRequest('/basketOfStocks/loadXml', data, function (result) {
                if (result.success) {
                    $rootScope.showSuccess(result.success);
                }
            });
        }
    });