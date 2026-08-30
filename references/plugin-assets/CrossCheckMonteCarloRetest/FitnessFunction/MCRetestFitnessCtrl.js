angular.module('app').controller('MCRetestFitnessCtrl', function ($rootScope, $scope, MCRetestFitnessService, RankingService, AppService, $q) {
    console.log("MCRetestFitness controller initialized");
 
    $scope.types = [];
    $scope.typeSelected = null;

    $scope.otherOptions = [];
    $scope.otherSelected = null;

    $scope.confidenceLevel = 50;
    $scope.confidenceLevels = null;

    MCRetestFitnessService.getLevels(function(data) {
        $scope.confidenceLevels = data.levels;   
    });

    MCRetestFitnessService.list(function(data) {
        loadToArray($scope.types, data.types);

        if($scope.types.length>0) {
            $scope.typeSelected = $scope.types[0].key;
        }

        var type = getTypeByKey('Other');
        if(type) {
            $scope.otherOptions = type.options;

            if($scope.otherOptions.length>0) {
                $scope.otherSelected = $scope.otherOptions[0].key;
            }
        }

        try { $scope.$digest(); } catch (er) {}
    }); 

    $scope.onTypeChange = function(key) {
        var type = getTypeByKey(key);
        if(!type) {
            return;
        }

        $scope.typeSelected = key;
        saveSettings();
    }

    function getTypeByKey(key) {
        if(!key) {
            return key;
        }

        for(var i=0; i<$scope.types.length; i++){
            var type = $scope.types[i];

            if(type.key == key){
                return type;
            }
        }
    }

    $scope.save = function() { 
        saveSettings();
    }

    function saveSettings() {
        $scope.method.onFitnessChange($scope.method);
    }

    $scope.method.getSettings = function() {
        var xmlDoc = AppService.xmlDoc;

        var settingsObj =  xmlDoc.createElement('Settings');

        var rankingObj = settingsObj.appendChild(xmlDoc.createElement('Ranking'));
        rankingObj.setAttribute("type", $scope.typeSelected);

        if($scope.typeSelected=='Other') {
            rankingObj.setAttribute("value", $scope.otherSelected);
        }

        addNode('ConfidenceLevel', $scope.confidenceLevel, settingsObj, xmlDoc);

        return settingsObj;
    }

    $scope.method.applySettings = function(settingsObj) {
        if(!settingsObj || settingsObj.nodeName!='Settings') {
            return;
        }

        $scope.confidenceLevel = getNodeIntValue(settingsObj, "ConfidenceLevel", 50);

        var rankingObj = getChildElement(settingsObj, 'Ranking');
        var key = getAttrStringValue(rankingObj, "type", null);

        var type = getTypeByKey(key);
        if(!type) {
            return;
        }

        $scope.typeSelected = key;

        if($scope.typeSelected=='Other') {
            $scope.otherSelected = getAttrStringValue(rankingObj, "value", null);
        }
    }

});