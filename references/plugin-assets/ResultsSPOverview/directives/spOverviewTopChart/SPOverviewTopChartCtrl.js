angular.module('app.resultstabs.spoverview').controller('SPOverviewTopChartCtrl', function ($scope, L) {
    console.log("SPOverviewTopChart controller initialized");

    $scope.chart = {};

    $scope.instance.init = function(color) {
      options.fill.colors.push(color);
    }

    $scope.instance.print = function (data) {
        if (!$scope.chart.print) {
            return;
        }

        options.series[0].data.length = 0;
        options.xaxis.categories.length = 0;

        for (var i = 0; i < data.length; i++) {
            options.xaxis.categories.push(data[i].x);
            options.series[0].data.push(data[i].y);
        }

        $scope.chart.print(options);

        try { $scope.$digest(); } catch (err) { }
    }

    $scope.instance.reset = function () {
        $scope.chart.print(null);
    }

    var options = {
        series: [{
            data: []
        }],
        chart: {
        type: 'bar',
        height: 250,
        toolbar: {
          show: false
        }
      },
      plotOptions: {
        bar: {
          horizontal: true,
          dataLabels: {
            position: 'top',
          },
        }
      },
      dataLabels: {
        enabled: true,
        textAnchor: 'start',
        style: {
          colors: ['#000'],
        },
        offsetX: -20,
        formatter: function (val, opt) {
          return val == 0 ? '' : val+ ' %';
        }
      },
      stroke: {
        show: false,
        width: 1,
        colors: ['#fff']
      },
      tooltip: {
        shared: false,
        intersect: false,
      },
      xaxis: {
        categories: [],
        labels: {
          show: false,
        }
      },
      yaxis: {
        labels: {
          show: true,
        }
      },
      fill: {
        colors: []
      }
    };
});