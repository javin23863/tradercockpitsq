angular.module('app.resultsdatabankactions.tools.edit.parameters').controller('EditParamsPopupCtrl', function ($rootScope, $scope, EditParametersService, L) {

    function refreshUI(variables) {
        if (!parametersGrid) {
            parametersGrid = new sqGrid('strategyParametersGrid');
            parametersGrid.setFirstColumnAsId(true, false);
            parametersGrid.enableCheckboxes(false);
            parametersGrid.setColumns([
                { title: L.tsq('Name'), type: "text", align: "left" },
                { title: L.tsq('Type'), type: "text", align: "left" },
                { title: L.tsq('Value'), type: "text", align: "right" }
            ]);
            parametersGrid.setWidths(["*", 80, 80]);
            parametersGrid.setEmptyGridText('No parameters available');

            parametersGrid.defineWidget('inputWidget', sqGridInput);

            parametersGrid.cellEventHandler = function (rowIndex, cellIndex, eventName, args) {
                if (eventName == "inputUpdated") {
                    updateParametersInConfig();
                }
            };
        }

        parametersGrid.removeAllRows(true);

        for (var i = 0; i < variables.length; i++) {
            var variable = variables[i];
            parametersGrid.addRow([variable.name, variable.type, "{{inputWidget value='" + variable.value + "'}}"], true);
        }

        $scope.sortGrid(true);

        updateParametersInConfig();
    }

    function updateParametersInConfig() {
        $scope.config.parameters = '';

        for (var i = 0; i < parametersGrid.getNumberOfRows(); i++) {
            $scope.config.parameters += parametersGrid.getCellValue(i, 0) + "=" + sqGridInputValueGet(parametersGrid, i, 2);
            $scope.config.parameters += i < (parametersGrid.getNumberOfRows() - 1) ? "," : "";
        }
    }

    $scope.sortGrid = (notSave) => {
        if (!parametersGrid) {
            return;
        }
        parametersGrid.setSort(($scope.sortParams == "1") ? 0 : -1, false);

        if (!notSave) {
            EditParametersService.saveSort($scope.sortParams);
        }
    }

    $scope.onSymmetryChange = function () {
        EditParametersService.loadParameters(function (result) {
            refreshUI(result.variables);
        });
    }

    $scope.onCancel = function () {
        hidePopup("#EditStrategyParametersModal");
    }

    $scope.onSave = function () {
        EditParametersService.saveParameters(function (result) {
            $rootScope.showSuccess("Parameter settings saved");
            hidePopup("#EditStrategyParametersModal");
        });
    }

    $rootScope.prepareStrategyParametersPopup = function (projectName, databankName, strategyName) {
        $scope.config.project = projectName;
        $scope.config.databank = databankName;
        $scope.config.strategy = strategyName;
        $scope.config.parameters = '';

        EditParametersService.loadParameters(function (result) {
            refreshUI(result.variables);
        });
    }

    $scope.sortParams = EditParametersService.getSort(); // 0 = original; 1 = by name

    $scope.config = EditParametersService.config;

    var parametersGrid;

});