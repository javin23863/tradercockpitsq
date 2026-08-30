angular.module('app.engine').controller('EngineChartPanelCtrl', function ($scope, $rootScope, EngineService, AppService, $timeout, $element, L) {
    console.log("EngineChartPanel controller initialized");

    $scope.type = null;
    $scope.items = null;
    $scope.selectedType = $scope.instance.selectedType;
    $scope.selectedTypeDisplay = '';
    $scope.chartWidth = 250;

    $scope.chart = {};

    $scope.getTypeLabel = function () {
        var type = getItem($scope.types, "type", $scope.selectedType);
        return type ? L.tsq(type.name) : '';
    }

    $(window).resize(function () {
        if (AppService.getActivePanel() == 'dashboard') {
            $element.find("chartjs").hide();
            $scope.chartWidth = Math.floor($element.width());
            $element.find("chartjs").show();
        }
    });

    $scope.onTypeSelect = function (selectedType) {
        $scope.type = null;

        var data = {
            projectName: window.appConfig.project,
            number: $scope.instance.number,
            type: selectedType,
        }

        EngineService.saveSelection(data);
    }

    $scope.instance.setSelectedType = function (type) {
        $scope.selectedType = type;
    }

    $scope.instance.getSelectedType = function () {
        return $scope.selectedType;
    }

    $scope.instance.setData = function (data) {
        if (!data) {
            return;
        }

        $scope.type = data.type;

        if (data.type == "chart") {
            if ($scope.chart.print) {
                $scope.chart.print(data.chart, $scope.selectedType);
            }
        } 
        else if (data.type == "grid" || data.type == "rows") {
            $scope.items = data.items;
        }
    }
});