angular
  .module("app.dashboardResults")
  .controller(
    "ResultPanelCtrl",
    function (
      $scope,
      $rootScope,
      $state,
      $timeout,
      SkinService,
      SQConstants,
      DashboardResultsService,
      AppService,
      SQEvents,
      DatabankService,
      L
    ) {
      console.log("ResultPanel controller initialized");

      var chart = null;

      $scope.iframeID = "dashboardOverview" + $scope.instance.ranking;
      $scope.chartid = "chart" + $scope.instance.ranking;

      $scope.sampleTypes = SQConstants.getConstants().sampleTypes;
      $scope.sampleType = DashboardResultsService.sampleType;

      $scope.strategy = "";
      $scope.resultTitle = L.tsq("No results so far");

      $scope.onSampleSelect = function (sampleType) {
        DashboardResultsService.setSampleType(sampleType);
      };

      $scope.displayResults = function (strategy) {
        DatabankService.selectStrategy(strategy, "Results");

        AppService.switchToPanel("results");
      };

      $scope.instance.print = function (data) {
        if (!chart) {
          return;
        }

        if (!data) {
          $scope.strategy = "";
        } else {
          $scope.sampleType = DashboardResultsService.sampleType;
          $scope.strategy = data.strategy;

          if ($scope.strategy != "") {
            chart.setData(data.chart);

            var iframeHtml = $("#" + $scope.iframeID)
              .contents()
              .find("html")
              .get(0);
            iframeHtml.innerHTML = data.overview;
          }
        }

        applyStyles();
        printTitle();

        try {
          $scope.$digest();
        } catch (err) {}
      };

      function applyStyles() {
        var iframeHead = $("#" + $scope.iframeID)
          .contents()
          .find("head");

        var iframeLinks = iframeHead.find("link");
        for (var i = 0; i < iframeLinks.length; i++) {
          var iframeLink = iframeLinks[i];
          iframeLink.remove();
        }

        var styleSheets = top.document.styleSheets;
        for (var i = 0; i < styleSheets.length; i++) {
          if (styleSheets[i].title == "skinLink" && !styleSheets[i].disabled) {
            iframeHead.append(
              $("<link/>", {
                rel: "stylesheet",
                href: styleSheets[i].href,
                type: "text/css",
                title: "skinLink",
              })
            );
          }
        }

        var iframeBody = $("#" + $scope.iframeID)
          .contents()
          .find("body");
        iframeBody.css("zoom", $rootScope.zoom.current);
      }

      function printTitle() {
        if ($scope.instance.ranking == 0) {
          if ($scope.strategy == "") {
            $scope.resultTitle = L.tsq("No results so far");
          } else {
            $scope.resultTitle =
              L.tsq("Best strategy so far") + ": " + $scope.strategy;
          }
        } else if ($scope.instance.ranking == 1) {
          $scope.resultTitle =
            L.tsq("2nd Best strategy so far") + ": " + $scope.strategy;
        } else if ($scope.instance.ranking == 2) {
          $scope.resultTitle =
            L.tsq("3rd Best strategy so far") + ": " + $scope.strategy;
        }
      }

      function initChart() {
        var config = {
          canvasId: $scope.chartid,
          logLevel: "error",
          width: 320,
          height: 240,
          axis: {
            shortCaptions: true,
            font: "11px Arial",
            color: "#3e3e3e",
            dformat: 0,
            yAxisOutside: false,
          },
        };

        chart = new SQ4.EquityChart(config);

        updateChartSkin();
      }

      function updateChartSkin() {
        if (
          SkinService.selectedSkin.name == "Dark skin" &&
          chart.switchToDark
        ) {
          chart.switchToDark();
        } else if (chart.switchToLight) {
          chart.switchToLight();
        }
      }

      $scope.$on("$destroy", function () {
        window.parent.removeListener(listenerId);
      });

      $timeout(initChart, 0, false);

      var listenerId = "MiniResultPanel" + $scope.instance.ranking;

      window.parent.addListener(
        listenerId,
        [SQEvents.get("SKIN_CHANGED")],
        function () {
          $timeout(
            function () {
              applyStyles();
            },
            1000,
            false
          );
          updateChartSkin();
        }
      );
    }
  );
