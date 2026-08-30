angular.module('app.data').controller('QualityCtrl', function ($scope, $rootScope, $timeout, DMDataService, BackendService, SQConstants) {
    console.log("Quality controller initialized");

    var TF = SQConstants.getConstants().timeframe;

    var widget;
    var grid;

    $scope.loading = false;
    $scope.tfNotSupported = false;

    $scope.tab.print = function (symbolInfo) {
        $scope.loading = false;

        grid.removeAllRows();

        $scope.tfNotSupported = (symbolInfo.timeframe == TF.TICK || symbolInfo.timeframe == TF.Intraday);
        if($scope.tfNotSupported) {
            return;
        }

        $scope.loading = true;

        DMDataService.checkQualitySummary(symbolInfo.symbol, symbolInfo.timeframe, symbolInfo.session, function (data) {
            $scope.problems = data.problems;

            widget.setData(data.problems.problems, data.problems.minHour, data.problems.maxHour, data.problems.pointOpacity);

            var url = "/data/checkQualityDetails?symbol=" + symbolInfo.symbol + "&timeframe=" + symbolInfo.timeframe + "&session=" + symbolInfo.session + "&totalErrors=" + data.problems.totalErrors + "&token=" + BackendService.getToken();

            
            grid.setSmartRendering(encodeURI(url));

            $timeout(function () {
                grid.bodyRedraw();
            }, 0);

            $scope.loading = false;
        });
    }

    function initGrid() {
        var columns = [{
                title: 'Date',
                type: "text"
            },
            {
                title: 'Open',
                type: "text"
            },
            {
                title: 'High',
                type: "text"
            },
            {
                title: 'Low',
                type: "text"
            },
            {
                title: 'Close',
                type: "text"
            },
            {
                title: 'Volume',
                type: "text"
            },
            {
                title: 'Problem',
                type: "text"
            },
        ];

        var widths = [164, 90, 90, 90, 90, 90, "*"];

        grid = new sqGrid('qualityGrid');
        grid.setFirstColumnAsId(true, false);
        grid.enableCheckboxes(false);
        grid.setColumns(columns);
        grid.setWidths(widths);
        grid.setEmptyGridText('Loading...');
    }

    function initWidget() {
        var config = {
            canvasId: 'qualityCanvasWrapper',
            width: '740px'
        };

        widget = new SQ4.QualitySummaryWidget(config);
    }

    function init() {
        initWidget();
        initGrid();
    }

    init();
});