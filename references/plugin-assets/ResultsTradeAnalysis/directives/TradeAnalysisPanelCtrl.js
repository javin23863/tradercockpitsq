angular.module('app.resultstabs.tradeanalysis').controller('TradeAnalysisPanelCtrl', function ($scope, TradeAnalysisService, L) {
    console.log("Trade Analysis panel controller initialized");

    $scope.selectedType = $scope.instance.selectedType;

    $scope.chart = {};

    $scope.getTypeLabel = function () {
        var type = getItem($scope.types, "type", $scope.selectedType);
        return type ? L.tsq(type.name) : '';
    }

    $scope.onTypeSelect = function () {
        $scope.instance.onTypeSelect();
    }

    $scope.instance.print = function (data) {
        if (!$scope.chart.print) {
            return;
        }

        if (!$scope.types) {
            var dataAddition = {
                plugins: [ChartDataLabels],
                options: {
                    plugins: {
                        datalabels: {
                            align: 'end',
                            anchor: 'end',
                            formatter: function (value, context) {
                                return "$ " + value;
                            }
                        }
                    },
                    layout: {
                        padding: {
                            left: 0,
                            right: 0,
                            top: 22,
                            bottom: 0
                        }
                    }
                }
            }

            angular.merge(data, dataAddition);
        }

        $scope.chart.print(data, $scope.selectedType);

        try { $scope.$digest(); } catch (err) { }
    }

    $scope.instance.setSelectedType = function (type) {
        $scope.selectedType = type;
    }

    $scope.instance.getSelectedType = function () {
        return $scope.selectedType;
    }

    $scope.instance.reset = function () {
        $scope.chart.print(null);
    }
});