angular.module('app.resultstabs.portfoliocorrelation').controller('CorrelationMatrixCtrl', function ($rootScope, $scope, $timeout, SQWebSocketService, SQEvents, BackendService, L, PortfolioCorrelationService) {
    console.log("PortfolioCorrelation -> CorrelationMatrix controller initialized");

    $scope.symbol1 = null;
    $scope.symbol2 = null;

    var grid = null, gridDetails = null;
    var dataType = "Data";

    function initGrid() {
        grid = new sqGrid("correlation-grid");
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
        if (!gridDetails) {
            gridDetails = new sqGrid("correlation-detail-grid");
            gridDetails.setEmptyGridText(L.tsq('Loading...'));
        }

        var columns = [{
            title: '#',
            type: "text",
        },
        {
            title: L.tsq('Period'),
            type: "text",
        },
        {
            title: dataType + ' 1',
            type: "text",
        },
        {
            title: dataType + ' 2',
            type: "text",
        },
        ];

        var widths = [40, 130, 130, 130];

        gridDetails.setColumns(columns);
        gridDetails.setWidths(widths);
    }

    function printDetails() {
        showPopup("#correlationMatrixPopup");

        initDetailsGrid();

        var url = "portfoliocorrelation/correlation?symbol1=" + $scope.symbol1 + "&symbol2=" + $scope.symbol2 + "&token=" + BackendService.getToken();
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
        if (data.portfolioCorrelation && data.portfolioCorrelation.correlation) {
            data = data.portfolioCorrelation.correlation;

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

            $scope.type = data.type;
            $scope.period = data.period;
            dataType = data.dataType;

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

    $scope.save = function() {
        $rootScope.saveFile(
            L.tsq("Select file"), 
            { name : 'csv', description : L.tsq('Comma Separated File') }, 
            "saveCorrelationMatrix", 
            "Correlation_matrix.csv", 
            null, 
            function(targetPath) {
                var data = { path: targetPath }

                PortfolioCorrelationService.correlationSave(data, function (result) {
                    $rootScope.showSuccess(L.tsq("Correlation matrix saved."));
                });
            }
        );
    }

    SQEvents.addListener("CorrelationMatrix", [SQEvents.get('RESET_PORTFOLIO_CORRELATION_TABS')], onEvent);

    initGrid();

    SQWebSocketService.subscribeGeneral("CorrelationMatrixCtrl-" + window.appConfig.appCode, onWebSocketData);
});