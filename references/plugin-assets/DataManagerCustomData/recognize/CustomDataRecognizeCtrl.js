angular.module('app.data').controller('CustomDataRecognizeCtrl', function ($rootScope, $scope, SQConstants, DMCustomDataService, SQEvents, L, BackendService, AppService) {
    console.log("CustomDataRecognize controller initialized");
    
    $scope.action.onSelect = function(symbols2, grid2) {
        var fileExtension = {
            name: 'mq4',
            description: L.tsq('MQ4 Files')
        };

        $rootScope.showFilePicker(L.tsq('Select custom indicator file'), "CustomDataRecognize", true, false, null, fileExtension, null, function (paths) {
            if (paths) {
                DMCustomDataService.recognizeFromFile(paths[0], function(data) {
                    $rootScope.showSuccess(data.success);
                });
            }
        });
    }

});

               