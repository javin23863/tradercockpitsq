angular.module('app.resultstabs.spoverview').controller('SPOverviewListCtrl', function ($scope, $timeout, L) {
    console.log("SPOverviewList controller initialized");

    var grid;

    $scope.instance.init = function() {
      console.log("SPOverviewList instance init");
      initGrid();
    }

    function initGrid() {
      $timeout(function() {
          grid = new sqGrid("sp-overview-grid");

          var columns = [
              { title: L.tsq('#'), type: "text", sort: 'text' },
              { title: L.tsq('Ticker'), type: "text", sort: 'text' },
              { title: L.tsq('Net profit'), type: "dollarsColoured", sort: 'number' },
              { title: L.tsq('Volume'), type: "number", sort: 'number' },
              { title: L.tsq('# of trades'), type: "number", sort: 'number' }
              
          ];

          var widths = [100, '*', 100, 100, 100];

          grid.setColumns(columns, !!grid);
          grid.setWidths(widths, !!grid);
          grid.setEmptyGridText(L.tsq('No stocks available.'));
      }, 0, false);
  }

  $scope.instance.print = function (data) {
    grid.removeAllRows();
    grid.loadFromJSON(data);
  }

  $scope.instance.reset = function () {
  }
});