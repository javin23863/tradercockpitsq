angular.module('app.resultstabs.portfoliocomposerchart').service('PortfolioComposerChartService', function (BackendService) {

    this.print = function(data, callOnSuccess) {
        BackendService.sendRequest('portfoliocomposerchart/print', data, callOnSuccess);
    }  
});