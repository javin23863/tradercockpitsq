angular.module('app.resultstabs.robustnesstests').controller('RobustnessAnalysisCtrl', function($scope, $rootScope, SQEvents, RTService, SkinService, $timeout, $window, $document, SQResultsData, SQConstants, L) {
    console.log("RobustnessAnalysisCtrl controller initialized");

    var plTypes = SQConstants.getConstants().plTypes;

    var grid;
    var chart = null;
    var methodName = null;

    $scope.settings = {};

    $scope.manageviews = {};
    $scope.manageviews.viewChanged = function() {
        grid = null;

        throttlePrint();
    }

    $scope.instance.print = function(data) {
        methodName = data;

        throttlePrint();
    }

    $scope.print = function(callback) {
        var dataInfo = SQResultsData.getDataInfo();
        if (!dataInfo.strategyName || !methodName || !$scope.instance.tab.active) {
            return;
        }

        if (!callback) {
            chart.updateSizes();
        }

        var data = {};
        data.project = dataInfo.projectName;
        data.databank = dataInfo.databankName;
        data.strategy = dataInfo.strategyName;
        data.width = chart.getChartWidth();
        data.height = 800;
        data.methodName = methodName;
        data.view = $scope.manageviews.getSelectedViewName();

        if (data.width <= 0 || data.height <= 0) {
            console.log("Chart size is too small.");
            return;
        }

        if (!grid) {
            initConfLevelsTable(throttlePrint);
            return;
        }

        RTService.printChart(data, render);
        printConfidenceLevelsTable(data);
    }

    angular.element($window).bind('resize', function() {
        throttlePrint();
    });

    var throttlePrint = sqThrottle(function() {
        $scope.print();
    }, 100, false);

    //--------------------------------------------------------------------------------------------------

    var render = function(data) {
        chart.setData(data.chart);
    }

    //-------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------

    function printConfidenceLevelsTable(data) {
        grid.removeAllRows();

        RTService.printConfLevels(data, function(data) {
            $scope.settings = data.settings;

            grid.loadFromJSON(data);
            grid.setRowClass(7, "highlight");
        });
    }

    function initConfLevelsTable(callback) {
        grid = null;

        $timeout(function() {
            var gridColumns = [{ title: L.tsq("Confidence level"), type: "text", sort: "text" }];
            var widths = [150];

            var view = $scope.manageviews.getSelectedView();
            if (view) {
                var columns = view.columns;
                for (var i = 0; i < columns.length; i++) {
                    gridColumns.push({
                        title: columns[i].entryName,
                        type: getColumnType(columns[i].type, plTypes.money),
                        sort: getSortType(columns[i].type),
                    });

                    widths.push(columns[i].width || 150);
                }
            }

            grid = new sqGrid("rtGrid");
            grid.setColumns(gridColumns, !!grid);
            grid.setWidths(widths, !!grid);
            grid.setEmptyGridText('No results.');
            grid.enableHtmlCells();
            grid.disableSorting();

            grid.headerRedraw();

            if (callback) {
                callback();
            }
        });
    }

    function initChart() {

        var config = {
            canvasId: 'rtchart',
            logLevel: 'error',
            width: '100%',
            height: '100%',
            handleResize: false,
            legend: {
                show: false
            },
            allowMaximize: true,
            zoom: {
                zoomAllowed: false
            },
            dataLoader: function(ctx) {
                $timeout.cancel($scope.resizeTimer);
                $scope.resizeTimer = $timeout(function() {
                    $scope.print(function(data) {
                        //ctx.dataLoadedHandler(data.chart);
                    });
                }, 300);
            },

        };

        chart = new SQ4.EquityChart(config);

        updateChartSkin();
    }

    function updateChartSkin() {
        if (SkinService.selectedSkin.name == "Dark skin" && chart.switchToDark) {
            chart.switchToDark();
        } else if (chart.switchToLight) {
            chart.switchToLight();
        }
    }

    $scope.$on('$destroy', function() {
        if (!isMainApp() && !isAlgoWizard()) {
            window.parent.removeListener("RbAnalysisCtrl");
        }
    });

    initChart();

    if (!isMainApp() && !isAlgoWizard()) {
        window.parent.addListener("RbAnalysisCtrl", [SQEvents.get("SKIN_CHANGED")], updateChartSkin);
    }

});