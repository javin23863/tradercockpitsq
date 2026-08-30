angular.module('app.portfoliocomposer').controller('ConfigurationCtrl', function ($scope, PortfolioComposerService) {
    console.log("PortfolioComposer - Configuration controller initialized");

    $scope.config = PortfolioComposerService.config;

    function init() {
    }

    init();
});