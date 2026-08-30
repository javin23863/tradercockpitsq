angular.module('app').controller('WhatIfCtrl', function ($scope, $rootScope, $timeout, WhatIfService, $q, SQConstants, DataService) {
    
    var methods = [];

    $scope.config = {
        settingsTableInstance : {}
    };

    $scope.tab.applySettings = function(settingsElem) {
        deferred.promise.then(function() {
            $scope.config.settingsTableInstance.load({rows : methods});    
            
            WhatIfService.applySettings(settingsElem, $scope.config);

            try { $scope.$digest(); } catch(er) {}
        });
    }

    $scope.tab.loadSettings = function(settingsElem) {
        WhatIfService.loadSettings(settingsElem, $scope.config);
    }

    $scope.onChange = function() {

    }
        
    $scope.afterInit = function() {   
        $timeout(init, 100, false);
    };

    function init() {
        WhatIfService.list(function(data) {
            methods = data.rows;
        
            deferred.resolve();
        });
    }

     var deferred = $q.defer();
});

