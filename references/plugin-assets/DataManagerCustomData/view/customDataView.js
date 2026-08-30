angular.module('app.data').directive('customDataView', function () {
    return {
        restrict: 'AE',
        templateUrl: '../../../plugins/DataManagerCustomData/view/customDataView.html',
        controller: 'CustomDataViewCtrl'
    }
});