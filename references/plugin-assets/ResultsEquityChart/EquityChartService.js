angular.module('app.resultstabs.equitychart').service('EquityChartService', function(BackendService) {
    
    this.print = function(data, callback) {
        BackendService.sendRequest('equitychart/print', data, callback, isMTAnalyzer() ? 'GET' : 'POST', true);
    }   
});
    