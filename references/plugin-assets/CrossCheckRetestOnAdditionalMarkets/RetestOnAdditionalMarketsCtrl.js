angular.module('app').controller('RetestOnAdditionalMarketsCtrl', function ($scope, $rootScope, RetestOnAdditionalMarketsService, DataService, L) {
    
    $scope.mainSetup = {};
    $scope.config = RetestOnAdditionalMarketsService.config;

    $scope.tab.applySettings = function(settingsElem) {
        RetestOnAdditionalMarketsService.applySettings(settingsElem, $scope.config);

        $scope.mainSetup = RetestOnAdditionalMarketsService.getMainSetup();
        
        try { $scope.$digest(); } catch(er) {}
    }

    $scope.tab.loadSettings = function(settingsElem) {
        RetestOnAdditionalMarketsService.loadSettings(settingsElem, $scope.config);
    }

    $scope.addSetup = function () {
        if(!$scope.mainSetup){
            $scope.mainSetup = RetestOnAdditionalMarketsService.getMainSetup();
        }

        var newSetup = {
            symbol: $scope.mainSetup.symbol,
            engine: $scope.mainSetup.engine,
            session: 'No Session',
            subcharts: JSON.parse(JSON.stringify($scope.mainSetup.subcharts))
        };

        DataService.changeSetupDetails(newSetup, []);

        $scope.config.setups.push(newSetup);

        updateSubcharts();
    }

    function updateSubcharts() {
        for (var i = 0; i < $scope.config.setups.length; i++) {
            var setup = $scope.config.setups[i];
            DataService.correctSubcharts(setup);
        }
    }

    $scope.removeSetup = function (index) {
        $rootScope.showConfirm(L.tsq("Remove setup"), L.tsq("Do you want to remove selected setup?"), function (confirmed) {
            if (confirmed) {
                $scope.config.setups.splice(index, 1);

                try { $scope.$digest(); } catch (er) {}
            }
        }, true);
    }

});