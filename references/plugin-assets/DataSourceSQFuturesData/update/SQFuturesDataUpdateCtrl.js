angular.module('app.datasource.sqFuturesData').controller('SQFuturesDataUpdateCtrl', function ($rootScope, $scope, SQFuturesDataService) {

    $scope.action.onSelect = function(symbols, grid2) {
        console.log("SQFuturesDataUpdateCtrl");
		
		SQFuturesDataService.update();
    }
});