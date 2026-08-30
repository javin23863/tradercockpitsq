angular.module('app.data').controller('CustomDataViewCtrl', function ($rootScope, $scope, SQConstants, DMCustomDataService, SQEvents, BackendService, L) {
    console.log("CustomDataView controller initialized");
    
    $scope.dataDetails = {}
    var grid = null;

    function print() {
        if (!grid) {
            grid = new sqGrid('reviewCustomDataGrid');
        }

        var columns = [{
                title: 'Date',
                type: "text"
        }];
        var widths = [180];

        for (var i = 0; i < $scope.dataDetails.values; i++) {
            columns.push({
                title: 'Value '+(i+1),
                type: "text"
            });

            widths.push(100)
        }

        grid.setFirstColumnAsId(true, false);
        grid.enableCheckboxes(false);
        grid.setColumns(columns);
        grid.setWidths(widths);
        grid.removeAllRows();
        grid.setEmptyGridText('Loading...');
        var url = "/customdata/reviewData?name=" + $scope.dataDetails.name + "&token=" + BackendService.getToken();
        grid.setSmartRendering(encodeURI(url));
        grid.bodyRedraw(true);
    }

    function onEvent(event, data) {
        if (event == SQEvents.get('CUSTOM_DATA_VIEW')) {
            console.log("View custom indicator data", data)

            if (data.rows.length == 0) {
                $rootScope.showError('You have to select a data.');
                return;
            }

            $scope.dataDetails = angular.copy(data.rows[0]);

            if ($scope.dataDetails.totalRecords == 0) {
                $rootScope.showError(L.tsq("Custom indicator '%s' doesn't contain any data", [$scope.dataDetails.name]));
                return;
            }

            print();

            showPopup('#customDataViewModal');
        }
    }

    SQEvents.addListener('CustomDataViewCtrl', [SQEvents.get("CUSTOM_DATA_VIEW")], onEvent);
});

               