angular.module('app.settings').controller('FitnessFunctionCtrl', function ($scope, $rootScope, sqPlugin, RankingService, AppService, FitnessFunctionService, CrossChecksService, L, SQConstants) {
    console.log("FitnessFunction controller initialized");

    $scope.methods = [];

    function init() {
        if(isOptimizeTask) {
            for(var i=0; i<FitnessFunctionService.methods.length; i++) {
                var method = FitnessFunctionService.methods[i];
                if(method.crosscheck) {
                    continue;
                }

                $scope.methods.push(method);
            }
        } else {
            loadToArray($scope.methods, FitnessFunctionService.methods);
        }

        for(var i=0; i<$scope.methods.length; i++) {
            var method = $scope.methods[i];

            var plugin = getItem(sqPlugin.getPlugins("FitnessMethod"), "key", method.value);
            if(plugin) {
                method.templateUrl = plugin.templateUrl;
                method.controller = plugin.controller;
                method.onFitnessChange = onFitnessChange;
            }
        }

        try { $scope.$digest(); } catch(er) {}

        CrossChecksService.list(function(data) {
            $scope.afterInit();
        });
    }

    $scope.onMethodChange = function() {
        var method = getItem($scope.methods, "value", $scope.selectedMethod);
        if(!method) {
            return;
        }

        //crosscheck has to be enabled
        var crossChecksConfig = CrossChecksService.config;

        var crossCheck = getItem(crossChecksConfig.crosschecks, "name", $scope.selectedMethod);
        if(!crossCheck || (crossCheck.use && !crossChecksConfig.disabled)) {
            $scope.fitness.method = $scope.selectedMethod;

            if(method.getSettings) {
                $scope.fitness.xml = method.getSettings();
            } else {
                $scope.fitness.xml = AppService.xmlDoc.createElement('Settings');
            }
            
            $scope.onChange();
        } 
        else {
            $scope.selectedMethod = $scope.fitness.method;

            if(method.value=='RetestOnAdditionalMarkets') {
                $rootScope.showError(L.tsq("To use fitness '%s' you have to enable cross check '%s' in Settings -> Cross checks.", [method.name, L.tsq('Retest on additional markets')]));
            } else {
                $rootScope.showError(L.tsq("To use fitness '%s' you have to enable this cross check in Settings -> Cross checks.", [method.name]));
            }

            try { $scope.$digest(); } catch(er) {}
        }

        window.dispatchEvent(new Event('resize'));
    }

    function onFitnessChange(method) {
        if(!method) {
            return;
        }
        
        if(method.getSettings) {
            $scope.fitness.xml = method.getSettings();
        } else {
            $scope.fitness.xml = AppService.xmlDoc.createElement('Settings');
        }

        $scope.onChange();
    }

    $scope.instance.applySettings = function(key, settingsElem) {
        $scope.selectedMethod = key;

        var method = getItem($scope.methods, "value", key);
        if(!method) {
            return;
        }       

        if(method.applySettings) method.applySettings(settingsElem);
    }

    $scope.instance.resetSettings = function() {
        for(var i=0; i<$scope.methods.length; i++) {
            let method = $scope.methods[i];

            if(method.resetSettings) method.resetSettings();
        }
    }

    $scope.getMethodLabel = function() {
        var method = getItem($scope.methods, "value", $scope.selectedMethod);
        return method ? L.tsq(method.name) : '';
    }

    var taskType = AppService.getTask().type;
    var isOptimizeTask = taskType == 'Optimize';

    $scope.fitness = RankingService.fitness;
    $scope.selectedMethod = null;

    init();
});