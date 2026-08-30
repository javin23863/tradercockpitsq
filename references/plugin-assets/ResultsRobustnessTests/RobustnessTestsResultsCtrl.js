angular.module('app.resultstabs.robustnesstests').controller('RobustnessTestsResultsCtrl', function ($rootScope, $scope, $injector, $state, $timeout, RTService, SQEvents, $q, SQResultsData, $element) {
    console.log("Monte Carlo tests controller initialized");

    var strategy = null;

    $scope.robustnessAnalysis = {
        tab: $scope.tab
    };

    $scope.method = null;
    $scope.methods = [];
    
    var lastSelectedMethod = null;

    $scope.onMethodChange = function() {
        lastSelectedMethod = $scope.method;
        
        $scope.robustnessAnalysis.print($scope.method);
    }

    function refreshContent(strategyChanged, handlerContent) {  
        if(!$scope.tab.active) return;

        var dataInfo = SQResultsData.getDataInfo();
        if(!dataInfo.strategyName) {
            $scope.methods.length = 0;

            return;
        }

        SQResultsData.getData(handlerId, []).then(function(result) {
            if(result.error) {
                result.data.methods = [];
            }

            loadToArray($scope.methods, result.data.methods);

            if($scope.methods.length>0) {
                var method = getItem($scope.methods, "value", lastSelectedMethod);
                $scope.method = method ? method.value : $scope.methods[0].value;
                $scope.onMethodChange();
            }
        });
    }
    
    $scope.getMethodLabel = function(){
        var method = getItem($scope.methods, "value", $scope.method);
        return method ? method.name : '';
    }

    function fetchData(params){
        var deferred = $q.defer();

        RTService.getMethods(params, function(response) {
             deferred.resolve(response);
        });  

        return deferred.promise;
    }

    var handlerId = $scope.tab.title; 	//handler id must be set to tab title
    SQResultsData.addHandler(handlerId, fetchData, refreshContent, []);

    var watchersHandler = new WatchersHandler($($element));
    $timeout(function(){ watchersHandler.onActivated(false); }, 0, false);

    $scope.tab.tabChanged = function() {
       watchersHandler.onActivated($scope.tab.active);
    }
});