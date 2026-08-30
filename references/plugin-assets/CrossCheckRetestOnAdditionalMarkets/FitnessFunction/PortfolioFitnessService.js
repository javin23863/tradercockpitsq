angular.module('app').service('PortfolioFitnessService', function(BackendService) {

    this.list = function(callback) {
        BackendService.sendRequest('retestOnAdditionalMarkets/list', null, callback);
    }
});