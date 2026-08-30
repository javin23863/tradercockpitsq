angular.module('app.portfoliocomposer').directive('pcResults', function () {
    return {
        restrict: 'AE',
        templateUrl: '../../../plugins/PortfolioComposer/results/results.html',
        scope : {
            instance: '='
        },
        controller: 'PCResultsCtrl'
    }
});