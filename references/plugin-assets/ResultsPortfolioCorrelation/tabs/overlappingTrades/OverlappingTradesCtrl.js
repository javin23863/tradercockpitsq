angular.module('app.resultstabs.portfoliocorrelation').controller('OverlappingTradesCtrl', function ($rootScope, $scope, $timeout, SQWebSocketService, SQEvents, BackendService, L) {
    console.log("PortfolioCorrelation -> OverlappingTrades controller initialized");

    $scope.symbol1 = null;
    $scope.symbol2 = null;

    var grid = null, gridDetails = null;

    function initGrid() {
        grid = new sqGrid("overlapping-grid");
        grid.setEmptyGridText('N/A');
        grid.disableSorting();

        grid.cellEventHandler = function (rowIndex, cellIndex, eventName, args) {
            $scope.symbol1 = grid.getColumnLabel(cellIndex);
            $scope.symbol2 = grid.getCellValue(rowIndex, 0);

            printDetails();

            return true;
        };
    }

    function initDetailsGrid() {
        if (gridDetails) {
            return;
        }

        var columns = [{
            title: '#',
            type: "text",
        },
        {
            title: L.tsq('Start Date 1'),
            type: "text",
        },
        {
            title:  L.tsq('End Date 1'),
            type: "text",
        },
        {
            title:  L.tsq('Start Date 2'),
            type: "text",
        },
        {
            title:  L.tsq('End Date 2'),
            type: "text",
        },
        {
            title:  L.tsq('Overlapped time'),
            type: "text",
        }
        ];

        var widths = [40, 130, 130, 130, 130, 140];

        gridDetails = new sqGrid("overlapping-detail-grid");
        gridDetails.setEmptyGridText(L.tsq('Loading...'));
        gridDetails.setColumns(columns);
        gridDetails.setWidths(widths);
    }

    function printDetails() {
        showPopup("#overlappingTradesPopup");

        initDetailsGrid();

        var url = "portfoliocorrelation/overlapping?symbol1=" + $scope.symbol1 + "&symbol2=" + $scope.symbol2 + "&token=" + BackendService.getToken();
        gridDetails.setSmartRendering(encodeUriCustom(url));

        $timeout(function () {
            gridDetails.bodyRedraw();
        });
    }

    function reset() {
        grid.removeAllRows();
        grid.bodyRedraw();
        try { $scope.$digest(); } catch (err) { }
    }

    function onWebSocketData(data) {
        if (data.portfolioCorrelation && data.portfolioCorrelation.overlapping) {
            data = data.portfolioCorrelation.overlapping;

            var columns = [];
            var widths = [];
            for (var i = 0; i < data.columns.length; i++) {
                columns.push({
                    title: data.columns[i],
                    type: "text",
                    sort: 'text',
                    width: "150"
                });

                widths.push(150);
            }

            grid.removeAllRows();
            grid.setColumns(columns, !!grid);
            grid.setWidths(widths, !!grid);

            for (var i = 0; i < data.rows.length; i++) {
                var rowData = data.rows[i].data;

                var row = [];
                for (var j = 0; j < rowData.length; j++) {

                    if (j == 0) {
                        row.push(rowData[j]);
                    } else if (rowData[j] == '') {
                        row.push("");
                    } else {
                        row.push(createActionLink(rowData[j], 'action-link', 'show-detail', i));
                    }
                }

                grid.appendData([row]);
            }

            grid.bodyRedraw();
            try { $scope.$digest(); } catch (err) { }
        }
    }

    function onEvent(event, data) {
        if(event == SQEvents.get('RESET_PORTFOLIO_CORRELATION_TABS')) {
            reset()

            try { $scope.$digest(); } catch(err){}
        }
    }

    SQEvents.addListener("OverlappingTrades", [SQEvents.get('RESET_PORTFOLIO_CORRELATION_TABS')], onEvent);

    initGrid();

    SQWebSocketService.subscribeGeneral("OverlappingTradesCtrl-" + window.appConfig.appCode, onWebSocketData);
});