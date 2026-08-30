angular
  .module("app.resultstabs.optimprof")
  .controller(
    "OptimizationProfileCtrl",
    function (
      $rootScope,
      $scope,
      $q,
      $timeout,
      OptimizationProfileService,
      SQEvents,
      SQConstants,
      SQResultsData,
      $element,
      L
    ) {
      console.log("Optimization profile controller initialized");

      $scope.simulations = {};

      var loaded = false;

      $scope.chart = {};

      $scope.chart2d = {};

      $scope.configChart = {
        xaxis: null,
        yaxis: null,
        zaxis: null,
        type: "dot-color",
        chart: "3D",
      };

      $scope.config = {
        profitOptPct: 30,
        avgProfit: 0,
        uniformDistrChanges: 5,
        stdevAvgProfit: 1,

        evalProfitOptCheck: true,
        evalAvgProfitCheck: true,
        evalUniformDistrCheck: true,
        evalTopProfitCheck: true,
      };

      $scope.checks = {
        profitableOptCheckResult: 0,
        avgProfitCheckResult: 0,
        uniformDistrCheckResult: 0,
        topProfitCheckResult: 0,

        profitableOptPct: 0,
        avgProfit: 0,
        uniformDistrChanges: 0,
        topProfit: 0,
        stdev: 0,
      };

      $scope.chartTypes = [
        { type: "3D", name: L.tsq("3D") },
        { type: "2D", name: L.tsq("2D") },
        { type: "Scatter", name: L.tsq("Scatter") },
      ];

      $scope.getCheckResultClass = function (result) {
        if (result == 1) return "optim-check-passed";
        if (result == 0) return "optim-check-failed";
        if (result == -1) return "optim-check-noteval";

        return "";
      };

      $scope.getCheckResult = function (result) {
        if (result == 1) return L.tsq("PASSED");
        if (result == 0) return L.tsq("FAILED");
        if (result == -1) return L.tsq("NOT EVALUATED");

        return "?";
      };

      $scope.onCustomizeChecksLevel = function () {
        showPopup("#optimProfileLevelsModal");
      };

      $scope.saveOptimProfileLevels = function () {
        print(true);

        hidePopup("#optimProfileLevelsModal");
      };

      ////// maximize 3D chart
      $scope.maximizeTitle = L.tsq("Maximize");
      $scope.maximizeText = "&#x2752;";
      $scope.isMaximized = false;
      $scope.maximizePerform = function () {
        var thisChartWrapper = $(".optim-3dchart-wrapper")[0];
        if (!$scope.isMaximized) {
          document.body.appendChild(thisChartWrapper);
          $scope.maximizeTitle = L.tsq("Close");
          $scope.maximizeText = "&#x2716;";
          $scope.isMaximized = true;
          document.getElementById("optim-3dchart").style.height="100%";
          graph.redraw();
        } else {
          $(".optim-3dchart-parent")[0].appendChild(thisChartWrapper);
          $scope.maximizeTitle = L.tsq("Maximize");
          $scope.maximizeText = "&#x2752;";
          $scope.isMaximized = false;
          document.getElementById("optim-3dchart").style.height="400px";
          graph.redraw();
        }
      };
      ///// END: maximize 3D chart

      function print(except3Dchart) {
        var data = {};

        var dataInfo = SQResultsData.getDataInfo();
        data.project = dataInfo.projectName;
        data.databank = dataInfo.databankName;
        data.strategy = dataInfo.strategyName;

        if (dataInfo.isAlgoWizardCloud || dataInfo.isAlgoWizard) data.backtestID = dataInfo.backtestID;

        if (!except3Dchart) {
          loaded = false;

          OptimizationProfileService.getAvailableParams(
            data,
            function (result) {
              loadToArray($scope.parameters, result.parameters);
              loadToArray($scope.values, result.values);

              if ($scope.parameters.length > 0) {
                if (!$scope.parameters.includes($scope.configChart.xaxis))
                  $scope.configChart.xaxis = $scope.parameters[0];
                if (!$scope.parameters.includes($scope.configChart.yaxis))
                  $scope.configChart.yaxis = $scope.parameters[1];
              }

              if ($scope.values.length > 0 && $scope.configChart.zaxis) {
                if (!getItem($scope.values, "type", $scope.configChart.zaxis)) {
                  $scope.configChart.zaxis = "NetProfit";
                }
                
              } else {
                $scope.configChart.zaxis = "NetProfit";
              }

              if (result.checks) {
                $scope.config = result.checks;
              }

              printChecks(data);

              printChart();

              printResults();

              $timeout(
                function () {
                  loaded = true;
                },
                0,
                true
              );
            }
          );
        } else {
          printChecks(data);
        }
      }

      function printChecks(data) {
        var data = {};

        var dataInfo = SQResultsData.getDataInfo();
        data.project = dataInfo.projectName;
        data.databank = dataInfo.databankName;
        data.strategy = dataInfo.strategyName;

        if (dataInfo.isAlgoWizardCloud || dataInfo.isAlgoWizard) data.backtestID = dataInfo.backtestID;

        angular.extend(data, $scope.config);

        OptimizationProfileService.print(data, function (data) {
          $scope.simulations = data.simulations;
          $scope.checks = data.checks;

          $scope.chart.print(data.chart, "histogram");
        });
      }

      function reset() {
        $scope.chart.print(null);
        $scope.chart2d.print(null);
      }

      function refreshContent(strategyChanged, handlerContent) {
        if (!$scope.tab.active) return;

        var dataInfo = SQResultsData.getDataInfo();
        if (!dataInfo.strategyName) {
          reset();
          return;
        }

        SQResultsData.getData(handlerId, []).then(function (result) {
          print();
          $timeout(
            function () {
              window.dispatchEvent(new Event("resize"));
            },
            200,
            false
          );
        });
      }

      function fetchData(params) {
        var deferred = $q.defer();

        $timeout(
          function () {
            deferred.resolve({});
          },
          0,
          false
        );

        return deferred.promise;
      }

      var handlerId = $scope.tab.title; //handler id must be set to tab title
      SQResultsData.addHandler(handlerId, fetchData, refreshContent, []);

      var watchersHandler = new WatchersHandler($($element));
      $timeout(
        function () {
          watchersHandler.onActivated(false);
        },
        0,
        false
      );

      $scope.tab.tabChanged = function () {
        watchersHandler.onActivated($scope.tab.active);
      };

      //------ 3D chart -----------------------------------------------------------------------------------------------------------------------
      $scope.parameters = [];
      $scope.values = [];

      var data = null;
      var graph = null;

      $scope.hasChart = false;

      function printChart() {
        var data = {};

        var dataInfo = SQResultsData.getDataInfo();
        data.project = dataInfo.projectName;
        data.databank = dataInfo.databankName;
        data.strategy = dataInfo.strategyName;

        if (dataInfo.isAlgoWizardCloud || dataInfo.isAlgoWizard) data.backtestID = dataInfo.backtestID;

        angular.extend(data, $scope.configChart);

        OptimizationProfileService.printChart(data, function (result) {
          if ("nochart" in result && result.nochart == "ok") {
            $scope.hasChart = false;
          } else {
            $scope.hasChart = true;

            $timeout(
              function () {
                if ($scope.configChart.chart == "3D") {
                  render3DChart(result.chart);
                } else {
                  render2DChart(result.chart);
                }
              },
              0,
              false
            );
          }
        });
      }

      function render2DChart(chart) {
        $scope.chart2d.print(chart, Date.now());
      }

      function render3DChart(chart) {
        data = new vis.DataSet();
        var values = chart.values;

        for (var i = 0; i < values.length; i++) {
          data.add(values[i]);
        }

        // specify options
        var options = {
          axisColor: "#808080",
          width: "100%",
          height: "100%",
          yCenter: "50%",
          xLabel: chart.xlabel,
          yLabel: chart.ylabel,
          zLabel: chart.zlabel,
          zMax: chart.zmax,
          zMin: chart.zmin,
          showPerspective: true,
          showGrid: true,
          keepAspectRatio: false,
          showLegend: true,
          legendLabel: "",
          cameraPosition: {
            horizontal: -0.35,
            vertical: 0.22,
            distance: 3.0,
          },
          reverseColorScale: !chart.reverseColorScale,
        };

        options.verticalRatio = 0.5;

        if ($scope.configChart.type == "top") {
          options.style = "surface";
          options.verticalRatio = 0.0001;
          options.yCenter = "40%";
          
        } else if ($scope.configChart.type == "dot-color-one") {
          options.style = "dot-color";
          
        } else {

          options.style = $scope.configChart.type;
        }

        // create our graph
        if (graph) {
          graph.setData(data);
          graph.setOptions(options);
          graph.redraw();
        } else {
          var container = document.getElementById("optim-3dchart");
          graph = new vis.Graph3d(container, data, options);
        }

        if ($scope.configChart.type == "top") {
          var pos = {
            horizontal: -0.001,
            vertical: 1.57,
            distance: 2.5,
          };
          graph.setCameraPosition(pos);
        }

        try {
          $scope.$digest();
        } catch (err) {}
      }

      $scope.chartTypeChanged = function (type) {
        $scope.configChart.type = type;
      };

      $scope.getZaxisLabel = function () {
        var value = getItem($scope.values, "type", $scope.configChart.zaxis);
        return value ? value.name : "";
      };

      $scope.getChartTypeLabel = function () {
        var value = getItem(
          $scope.chartTypes,
          "type",
          $scope.configChart.chart
        );
        return value ? value.name : "";
      };

      $scope.$watch(
        "configChart",
        function (newValue, oldValue) {
          if (!loaded) return;

          printChart();

          grid = null;
          printResults();
        },
        true
      );

      //results -------------------------------------------------------------------------------------
      var plTypes = SQConstants.getConstants().plTypes;

      var grid;

      $scope.manageviews = {};
      $scope.manageviews.viewChanged = function () {
        grid = null;

        printResults();
      };

      function printResults() {
        grid = null;

        if (!grid) {
          initResultsTable(function () {
            _printResults();
          });
        } else {
          _printResults();
        }
      }

      function _printResults() {
        var data = {};

        var dataInfo = SQResultsData.getDataInfo();
        data.project = dataInfo.projectName;
        data.databank = dataInfo.databankName;
        data.strategy = dataInfo.strategyName;
        data.view = $scope.manageviews.getSelectedViewName();

        if (dataInfo.isAlgoWizardCloud || dataInfo.isAlgoWizard) data.backtestID = dataInfo.backtestID;

        angular.extend(data, $scope.configChart);

        if (!data.project) return;

        OptimizationProfileService.printResults(data, function (result) {
          grid.removeAllRows();
          grid.loadFromJSON(result);
        });
      }

      function initResultsTable(callback) {
        grid = null;

        $timeout(function () {
          var gridColumns = [];
          var widths = [];

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

          if ($scope.configChart.chart == "3D") {
            gridColumns.push({
              title: $scope.configChart.xaxis,
              type: "text",
              sort: "text",
            });
            widths.push(100);

            gridColumns.push({
              title: $scope.configChart.yaxis,
              type: "text",
              sort: "text",
            });
            widths.push(100);
          } else if ($scope.configChart.chart == "2D") {
            gridColumns.push({
              title: $scope.configChart.xaxis,
              type: "text",
              sort: "text",
            });
            widths.push(100);
          }

          grid = new sqGrid("optimResultsGrid");
          grid.setColumns(gridColumns, !!grid);
          grid.setWidths(widths, !!grid);
          grid.setEmptyGridText("No results.");
          grid.enableHtmlCells();

          grid.headerRedraw();

          if (callback) {
            callback();
          }
        });
      }

      $scope.onExport = function () {
        $rootScope.showCSVXLSXExportDialog(
          "OptResultsExport",
          "OptResultsExport",
          function (path, useComma) {
            var data = {};
            var dataInfo = SQResultsData.getDataInfo();
            data.project = dataInfo.projectName;
            data.databank = dataInfo.databankName;
            data.strategy = dataInfo.strategyName;

            if (dataInfo.isAlgoWizardCloud || dataInfo.isAlgoWizard) data.backtestID = dataInfo.backtestID;

            data.view = $scope.manageviews.getSelectedViewName();
            angular.extend(data, $scope.configChart);

            data.path = path;
            data.useComma = useComma;

            OptimizationProfileService.export(data);
          }
        );
      };
    }
  );
