angular.module('app.resultstabs.spoverview').service('SPOverviewService', function(BackendService) {
    
    this.print = function(data, callback) {
        BackendService.sendRequest('spoverview/print', data, callback, 'POST', true);
    }   
});
    