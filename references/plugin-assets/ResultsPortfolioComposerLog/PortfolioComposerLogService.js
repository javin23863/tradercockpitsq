angular.module('app.resultstabs.portfoliocomposer').service('PortfolioComposerLogService', function (BackendService) {

    this.print = function(data, callOnSuccess) {
        BackendService.sendRequest('portfoliocomposerlog/print', data, callOnSuccess);
    }  
});