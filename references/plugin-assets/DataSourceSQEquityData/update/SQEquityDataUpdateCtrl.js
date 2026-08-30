angular.module('app.datasource.sqEquityData').controller('SQEquityDataUpdateCtrl', function ($rootScope, $scope, SQEquityDataService) {

    $scope.action.onSelect = function(symbols, grid2) {
        console.log("SQEquityDataUpdateCtrl");
		
		SQEquityDataService.update();
    }
});