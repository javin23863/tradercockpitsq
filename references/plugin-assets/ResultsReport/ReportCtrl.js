angular.module('app.resultstabs.report').controller('ReportCtrl', function ($rootScope, $scope, SQWebSocketService) {
    console.log("Report controller initialized");

    var equityChart = null;
    $scope.charts = [];
    
    init();

    function init() {
        for(var i = 0; i < 12; i++) {
            var chart = {};
            
            $scope.charts.push(chart);
        }    

        initEquityChart();    
    }

    function initEquityChart() {
        var config = {
            canvasId: 'equitychart-report',
            logLevel: 'error',
            width: '100%',
            height: '100%',
            handleResize: false,
            tooltip: {
                show: true
            }
        };

        equityChart = new SQ4.EquityChart(config);
        equityChart.updateSizes();
    }

    function reset() {
        for(var i = 0; i < $scope.charts.length; i++) {
            $scope.charts[i].reset();
        }
    }

    function print(data) {
        var charts = data.charts;

        for(var i = 0; i < charts.length; i++) {
            $scope.charts[i].print(charts[i]);
        }

        equityChart.setData(data.equityChart);
        equityChart.updateSizes();
    } 

    function getChartsAsImages(types) {
        var charts = [];
            
        for(var i = 0; i < $scope.charts.length; i++) {
            var ch = $scope.charts[i];

            charts.push({key: types[i], img: ch.getAsImage()});
        }

        return charts;
    }

     function onWebSocketData(data) {
        if (data.GenerateCharts) {
            var format = data.GenerateCharts.format;
            var types = data.GenerateCharts.types;

            print(data.GenerateCharts);

            var data = {
                action: 'resolveCharts',
                format: format,
                charts: [],
            }

            var chartsImages = getChartsAsImages(types);

            for (var j = 0; j < chartsImages.length; j++) {
                data.charts.push(chartsImages[j]);
            }

            var c = document.getElementById("equitychart-report-canvas");
            var d = c.toDataURL("image/png");

            data.charts.push({key: 'equityChart', img: d});
            
            SQWebSocketService.sendRequest(JSON.stringify(data));
        }
    }

    SQWebSocketService.subscribeGeneral("ProjectResults", onWebSocketData);
});