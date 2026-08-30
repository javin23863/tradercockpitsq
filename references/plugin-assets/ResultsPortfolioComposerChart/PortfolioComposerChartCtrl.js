angular.module('app.resultstabs.portfoliocomposerchart').controller('PortfolioComposerChartCtrl', function ($scope, $timeout, $q, PortfolioComposerChartService, SQResultsData, $element, SkinService, SQEvents, L) {
    console.log("PortfolioComposerChart controller initialized");

    function init() {
        initChart();
    }

    function initChart() {
       
    }

    /*
    {
        "bestX": 0.2134,
        "bestY": 0.0381,
        "labelMinimumRiskPortfolio": "Minimum Risk Portfolio",
        "labelX": "Standard Deviation of Daily Returns",
        "labelY": "Expected Daily Returns",
        "x": [0.4507, 0.2009, 0.2862, 0.1911, 0.227, 0.3667, 0.2134, 0.3757, 0.1758, 0.3404],
        "y": [-0.009, 0.0307, 0.0295, 0.0296, 0.0169, 0.0144, 0.0381, -0.0013, 0.0276, 0.0164],
        "labelTitle": "Minimum Variance Frontier",
        "labelOptimalPortfolio": "Optimal Portfolio",
        "MinimumRiskPortofolioY": 0.0276,
        "MinimumRiskPortofolioX": 0.1758,
        "weights": [
            "24 13 14 49",
            "31 12 35 22",
            "52 26 17 4",
            "45 12 16 26",
            "29 25 32 14",
            "29 27 22 22",
            "39 33 2 27",
            "40 32 7 21",
            "45 23 17 14",
            "58 24 9 9"
        ]
    }
    */
    function print(result) {
        var data = JSON.parse(result.data.chart);
        console.log("Portfolio composer chart data", data);

        options.series[0].data.length = 0;
        options.series[1].data.length = 0;
        options.series[2].data.length = 0;

        for (var i = 0; i < data.x.length; i++) {
            options.series[0].data.push([data.x[i], data.y[i]]);
          
            if (data.x[i] === data.bestX && data.y[i] === data.bestY) {
                bestIndex = i;
            }

            if (data.x[i] === data.MinRiskX && data.y[i] === data.MinRiskY) {
                minRiskIndex = i;
            }
        }

        options.series[1].data.push([data.MinRiskX, data.MinRiskY]); //Minimum risk portfolio
        options.series[2].data.push([data.bestX, data.bestY]); //Optimal portfolio

        options.title.text = data.labelTitle;
        options.xaxis.title.text = data.labelX;
        options.yaxis.title.text = data.labelY;

        dataLabels = data.weights;
        decimals = data.decimals;

        $scope.chart.print(options);

        try { $scope.$digest(); } catch (err) { }
    }

    $scope.onRefresh = function () {
        refreshContent(true);
    }

    //- Event Handlers --------------------------------------------------------------

    function refreshContent(force) {
        if (!$scope.tab.active) return;

        if (!loaded && !initCalled) {
            initCalled = true;
            init();
        }

        var dataInfo = SQResultsData.getDataInfo();
        if(!dataInfo.strategyName) {
            return;
        }
        
        SQResultsData.getData(
            handlerId, 
            [], 
            force
        ).then(function (result) {            
            print(result);
        });
    }

    function fetchData(params) {
        var deferred = $q.defer();

        PortfolioComposerChartService.print(params, function (result) {
            deferred.resolve(result);
        });

        return deferred.promise;
    }

    $scope.$on('$destroy', function () {
        destroyConfigWatch();

        if (window.parent && window.parent.removeListener) {
            window.parent.removeListener("PortfolioComposerLogCtrlSkin");
        }

        SQResultsData.removeHandler(handlerId);
    });


    //- Initialization --------------------------------------------------------------

    var initCalled = false;
    var loaded = false;
    $scope.chart = {};

    var handlerId = $scope.tab.title; //handler id must be set to tab title
    var handler = SQResultsData.addHandler(handlerId, fetchData, refreshContent, []);

    var watchersHandler = new WatchersHandler($($element));
    $timeout(function(){ watchersHandler.onActivated(false); }, 0, false);

    $scope.tab.tabChanged = function() {
       watchersHandler.onActivated($scope.tab.active);
    }

    var options = {
        series: [
          {
            name: 'Portfolio simulation',
            data: []
          },
          {
            name: 'Minimum-risk portfolio',
            data: [],
            color: '#90EE90', // green color
          }, 
          {
            name: 'Optimal portfolio',
            data: [],
            color: '#FFD700', // Gold color
          }
        ],
        chart: {
            type: 'scatter',
            width: '100%',
            height: '100%',
            toolbar: {
                show: false
            }
        },
        title: {
            text: 'NA',
            align: 'center',
            style: {
                fontSize: '12px',
                color: '#666'
            }
        },
        xaxis: {
            title: {
                text: 'NA',
                style: {
                    fontSize: '12px',
                    color: '#666'
                }
            },
            tickAmount: 10,
            labels: {
                formatter: function (val) {
                  return val.toFixed(decimals);
                }
            }
        },
        yaxis: {
            title: {
                text: 'NA',
                style: {
                    fontSize: '12px',
                    color: '#666'
                }
            },
            tickAmount: 10
        },
        markers: {
          size: [5, 10, 10]
        },
        tooltip: {
            custom: function({series, seriesIndex, dataPointIndex, w}) {
                const seriesName = w.config.series[seriesIndex].name;  // Get the series name

                const weightIndex = (
                    seriesIndex === 1 ? minRiskIndex :
                    seriesIndex === 2 ? bestIndex :
                    dataPointIndex
                );

                const weights = dataLabels[weightIndex];            // Get the weights for the data point
                
                return '<div class="arrow_box">' +
                       '<div><strong>' + seriesName + '</strong></div>' +
                       '<span>Weights: [' + weights + ']</span>' +
                       '</div>';
            }
          }
    }

    var dataLabels = [];
    let bestIndex = -1;
    let minRiskIndex = -1;
    var decimals = 0;
});