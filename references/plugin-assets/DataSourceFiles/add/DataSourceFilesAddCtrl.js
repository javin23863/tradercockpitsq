angular.module('app.datasource.files').controller('DataSourceFilesAddCtrl', function ($rootScope, $scope, $q, SQEvents, SQConstants, DataSourceFilesService) {

    $scope.dataSource = angular.copy(SQConstants.getConstants().dataSource);
    $scope.instrumentChooser = {};

    $scope.barDataTypes = angular.copy(SQConstants.getConstants().barDataTypes);

    $scope.dataDetails = {
        symbol: '',
        instrument: '',
        barType: $scope.barDataTypes.startOfBar,
        connection: 'History',
        source: $scope.dataSource.file
    };

   $scope.action.onSelect = function() {
        console.log("DataSourceFilesAddCtrl")
       
        $scope.instrumentChooser.setSymbol('');
        $scope.instrumentChooser.setDataType(3);
        $scope.instrumentChooser.reset();

       showPopup('#addSymbolModal');
   }

    $scope.onSave = function (form) {
        if (form) {
            $scope.errors = validate(form);
            if ($scope.errors) {
                console.error($scope.errors);
                return;
            }
        }

        if(!isSymbolNameValid($scope.dataDetails.symbol)) {
            $rootScope.showError("Symbol name cannot contain any special characters except for ._@");

            return;
        }

        try {
            $scope.dataDetails.instrument = $scope.instrumentChooser.getInstrument().instrument;
        } catch (err) {
            console.error("Cannot set symbol instrument. " + err);
        }

        DataSourceFilesService.add($scope.dataDetails);

        hidePopup('#addSymbolModal');
    }
});