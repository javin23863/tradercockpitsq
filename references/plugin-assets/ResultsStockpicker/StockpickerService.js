angular.module('app.resultstabs.stockpicker').service('StockpickerService', function (BackendService) {

    this.print = function(data, callOnSuccess) {
        BackendService.sendRequest('stockpicker/print', data, callOnSuccess);
    }  
});