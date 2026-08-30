angular.module('app.resultsdatabankactions.tools.runCa').controller('RunCaPopupCtrl', function ($rootScope, $scope, L, DatabankService, DatabankActionsService) {
    
    $scope.getMethodLabel = function(value) {
        var method = getItem($scope.availableMethods, "value", value);
        return method ? method.name : '';
    }

    $rootScope.customAnalyzeInit = function(projectName, databankName, strategies) {
        $scope.data = {
            projectName: projectName,
            databankName: databankName,
            strategies: strategies
        }
    }

    function init() {
        var noneMethod = {"name": L.tsq("None"), "value": "none"}
        $scope.availableMethods.push(noneMethod);
        $scope.availableMethodsPerStrategy.push(noneMethod);
        $scope.availableMethodsFullDatabank.push(noneMethod);

        var methods = InitializationData().settings.CustomAnalysis.methods;        
        for (var i = 0; i < methods.length; i++) {
            let method = methods[i];
            $scope.availableMethods.push(method);

            if(method.type==20 || method.type==30) $scope.availableMethodsPerStrategy.push(method);
            if(method.type==10 || method.type==30) $scope.availableMethodsFullDatabank.push(method);
        }
    }

    function validate(config){
        if (config.analyseType=='strategy'){
            if (config.perStrategy1=='none'){
                $rootScope.showError(L.tsq("You have to choose some 'Per strategy analysis' method. "));
                return false;
            }

            return true;
        }else{
            if (config.inputArgsFullDatabank1=='none'){
                $rootScope.showError(L.tsq("You have to choose some 'Full databank analysis' method. "));
                return false;
            }

            return true;
        }
    }

    $scope.onSubmit = function(){
        if (!validate($scope.config)){
            return;
        }

        if ($scope.config.selectedStrategies){
            if (DatabankActionsService.selectedStrategies==''){
                $rootScope.showError(L.tsq("You must select some stratgey. "));
                return;
            }
            $scope.config.strategies = DatabankActionsService.selectedStrategies.split(','); 
        }

        $scope.config.projectName = $scope.data.projectName;
        $scope.config.databankName = $scope.data.databankName;

        DatabankService.performCustomAnalysis($scope.config).then(function(result){
            if (result.error) {
                $rootScope.showError(L.tsq("Error while running custom analysis. ") + result.error);
            }else{
                window.parent.hidePopup('#RunCaModal');
            }
        });
    }

    $scope.availableMethods=[];
    $scope.availableMethodsPerStrategy=[];
    $scope.availableMethodsFullDatabank=[];

    $scope.config = {
        filter: true,
        selectedStrategies:false,
        inputArgsFullDatabank1:'',
        fullDatabank1:'none',
        perStrategy1:'none',
        inputArgsPerStrategy1:'',

        analyseType:'strategy'
    }

    console.log('RunCaPopupCtrl', $scope);
    init();
});