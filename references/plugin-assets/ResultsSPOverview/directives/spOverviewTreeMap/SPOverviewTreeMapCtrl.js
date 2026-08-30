angular.module('app.resultstabs.spoverview').controller('SPOverviewTreeMapCtrl', function ($scope, L) {
    console.log("SPOverviewTreeMap controller initialized");

    $scope.chart = {};

    $scope.instance.init = function() {
    }

    $scope.instance.print = function (data) {
        if (!$scope.chart.print) {
            return;
        }


        let min = data.min;
        let max = data.max;
        data = data.data;
        
        options.series[0].data.length = 0;

        for (var i = 0; i < data.length; i++) {
          let item = data[i];
          item.fillColor = getHeatMapColor(item.pl, min, max);
          options.series[0].data.push(item);
        }

        $scope.chart.print(options);

        try { $scope.$digest(); } catch (err) { }
    }

    $scope.instance.reset = function () {
        $scope.chart.print(null);
    }

    function getHeatMapColor(value, min, max) {
      const colorStops = [
          { stop: -1, color: { r: 246, g: 53, b: 56 } },  // Intense red
          { stop: -0.5, color: { r: 191, g: 64, b: 69 } }, // Dark red
          { stop: -0.25, color: { r: 139, g: 68, b: 78 } },// Brownish-red
          { stop: 0, color: { r: 65, g: 69, b: 84 } },     // Neutral gray
          { stop: 0.25, color: { r: 47, g: 158, b: 79 } }, // Brownish-green (transition color)
          { stop: 0.5, color: { r: 48, g: 204, b: 90 } },  // Darker green
          { stop: 1, color: { r: 48, g: 204, b: 90 } }     // Intense green
      ];

      let normalizedValue;
      let lowerStop, upperStop;

      if (value < 0) {
          // Normalize the value to the range -1 to 0
          normalizedValue = value / Math.abs(min);

          // Find the two color stops between which the value falls
          for (let i = 0; i < 3; i++) {
              if (normalizedValue >= colorStops[i].stop && normalizedValue <= colorStops[i + 1].stop) {
                  lowerStop = colorStops[i];
                  upperStop = colorStops[i + 1];
                  break;
              }
          }
      } else {
          // Normalize the value to the range 0 to 1
          normalizedValue = value / max;

          // Find the two color stops between which the value falls
          for (let i = 3; i < colorStops.length - 1; i++) {
              if (normalizedValue >= colorStops[i].stop && normalizedValue <= colorStops[i + 1].stop) {
                  lowerStop = colorStops[i];
                  upperStop = colorStops[i + 1];
                  break;
              }
          }
      }

      //console.log("found", value, min, max, normalizedValue, lowerStop, upperStop);

      if(!lowerStop || !upperStop) {
          return `rgb(${colorStops[3].color.r},${colorStops[3].color.g},${colorStops[3].color.b})`;
      }

      // Calculate the ratio between the two stops
      const ratio = (normalizedValue - lowerStop.stop) / (upperStop.stop - lowerStop.stop);

      // Interpolate the color components
      const r = Math.floor(lowerStop.color.r + ratio * (upperStop.color.r - lowerStop.color.r));
      const g = Math.floor(lowerStop.color.g + ratio * (upperStop.color.g - lowerStop.color.g));
      const b = Math.floor(lowerStop.color.b + ratio * (upperStop.color.b - lowerStop.color.b));

      // Return the color in RGB format
      return `rgb(${r},${g},${b})`;
  }

    var options = {
      series: [
        {
          data: []
        }
      ],
      legend: {
        show: false
      },
      chart: {
        height: 400,
        type: 'treemap',
        toolbar: {
          show: false
        }
      },
      dataLabels: {
        enabled: true,
        format:  "scale",
        formatter: function(text, op) {
          var data = op.w.globals.initialSeries[0].data[op.dataPointIndex];
          let val = data.v;

          if(val > 0) {
            return [text,  '+'+val+'%'];
          } else {
            return [text,  val+'%'];
          }
        }
      },
      tooltip: {
        custom: function({series, seriesIndex, dataPointIndex, w}) {
          var data = w.globals.initialSeries[seriesIndex].data[dataPointIndex];
          let val = data.v;

          if(val > 0) {
            val = '+'+val+'%';
          } else {
            val = val+'%';
          }

          return '<b>' + data.x + '</b><br>' + val;
        }
      }
    };
});