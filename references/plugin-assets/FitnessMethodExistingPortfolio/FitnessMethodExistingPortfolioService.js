angular.module('app.fitnessExistingPortfolio').service('FitnessMethodExistingPortfolioService', function(BackendService) {

    this.list = function(callback) {
        BackendService.sendRequest('fitnessExistingPortfolio/list', null, callback);
    }
});