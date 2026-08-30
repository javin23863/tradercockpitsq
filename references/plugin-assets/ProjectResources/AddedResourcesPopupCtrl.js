angular.module('app.projectresources').controller('AddedResourcesPopupCtrl', function ($rootScope, $scope, $timeout, SQEvents, SQConstants, BackendService, L) {
    console.log("AddedResourcesPopupCtrl initialized");

    $rootScope.onShowAddedResourcesPopup = function (addedSymbols) {
        loadToArray($scope.resources.newSymbols, addedSymbols);
        $scope.$evalAsync();
        
        showPopup("#addedResourcesPopup");
    }

    $scope.resources = { newSymbols : [] };

});