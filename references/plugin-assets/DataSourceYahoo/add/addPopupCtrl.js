angular.module('app.datasource.yahoo').controller('yahooAddPopupCtrl', function ($rootScope, $scope, $q, SQEvents, YahooService) {

    $scope.dataDetails = {
        symbols: null,
        postfix: null
    }

    $scope.action.onSelect = function () {
        console.log("yahooAddPopupCtrl")

        $scope.dataDetails.symbols = null;

        showPopup('#yahooAddModal');
    }

    $scope.onSave = function () {
        $scope.dataDetails.postfix = valueFilled($scope.dataDetails.postfix) ? $scope.dataDetails.postfix : '';

        YahooService.add($scope.dataDetails, function (data) {

            $rootScope.showSuccess(data.success);
            hidePopup("#yahooAddModal");
        });
    }

    $("#yahooAddModal textarea").on("keyup keypress keydown", function (e) {
        e.stopPropagation();
    });
});