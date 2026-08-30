angular.module('app.settings').controller('CrossCheckCtrl', function ($scope, $rootScope, SQEvents, UniqueIdGenerator) {
    console.log("CrossCheck controller initialized");

    $scope.settingsChanged = function () {
        $scope.crosscheck.settingsChanged();
    }

    $scope.openSettings = function (acceptanceTab) {
        $scope.crosscheck.openSettings($scope.crosscheck, acceptanceTab);
    }

    $scope.$on("$destroy", function () {
        UniqueIdGenerator.removeId($scope.modalId);
    });

    $scope.modalId = UniqueIdGenerator.generateId();
});