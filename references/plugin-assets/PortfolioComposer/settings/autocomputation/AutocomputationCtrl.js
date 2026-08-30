angular.module('app.portfoliocomposer').controller('AutocomputationCtrl', function ($scope, PortfolioComposerService, SQConstants, L) {
    console.log("PortfolioComposer - Auto computation controller initialized");

    $scope.config = PortfolioComposerService.config;

    $scope.selectionTypes = [];
    loadToArray($scope.selectionTypes, SQConstants.getConstants().pcSelectionTypes);

    $scope.modelTypes = [{
        value: 'markowitz',
        name: L.tsq('Markowitz Efficient Frontier')
    }];

    $scope.getSelectionTypeLabel = function(){
        var type = getItem($scope.selectionTypes, 'value', $scope.config.selectionType);
        return type ? L.tsq(type.name) : '';
    }

    $scope.getModelTypeLabel = function(){
        var type = getItem($scope.modelTypes, 'value', $scope.config.modelType);
        return type ? L.tsq(type.name) : '';
    }

    function init() {
    }

    init();
});