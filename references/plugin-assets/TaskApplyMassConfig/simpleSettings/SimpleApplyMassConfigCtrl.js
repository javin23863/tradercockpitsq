angular.module('app.settings').controller('SimpleApplyMassConfigCtrl', function ($rootScope, $scope, $timeout, AppService, SQConstants, SQEvents, ApplyMassConfigService, L) {
    console.log("SimpleApplyMassConfigCtrl controller initialized");

    $scope.instance = {};

    $scope.config = ApplyMassConfigService.config;    
});