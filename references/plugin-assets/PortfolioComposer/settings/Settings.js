angular.module('app.portfoliocomposer').directive('pcSettings', function () {
    return {
        restrict: 'AE',
        templateUrl: '../../../plugins/PortfolioComposer/settings/settings.html',
        scope : {
            instance: '='
        },
        controller: 'PCSettingsCtrl'
    }
});