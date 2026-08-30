angular.module('app.baskets').controller('BasketsController', function ($scope, $rootScope, $timeout, SQEvents, SQConstants, BasketService, L) {
    console.log("Baskets controller initialized");

    function initGrids() {
        $timeout(function () {
            initGrid();

            initialized = true;
            updateBasketsGridData();
        });
    }

    function initGrid() {
        var columns = [
            { title: L.tsq('Name'), type: "text", sort: 'text' },
            { title: L.tsq('Count'), type: "int", sort: 'number'},
            { title: L.tsq('Description'), type: "text", sort: 'text' },
            { title: L.tsq('Number of symbols'), type: "text", sort: 'text' },
            { title: L.tsq('Downloaded'), type: "text", sort: 'text' },    
            { title: L.tsq('Ready to use?'), type: "text", sort: 'text' },            
            { title: L.tsq('Data from'), type: "text", sort: 'text' },
            { title: L.tsq('Data to'), type: "text", sort: 'text' }
        ];

        var widths = [250, 100, "*",  130, 130, 100, 100, 100];

        if (!grid) {
            grid = new sqGrid("basketsGrid");
        } else {
            grid.removeAllRows(true, false, true);
        }

        grid.setFirstColumnAsId(true, false);
        grid.enableCheckboxes(true, false);
        grid.setColumns(columns, !!grid);
        grid.setWidths(widths, !!grid);
        grid.setEmptyGridText(L.tsq('No groups of stocks are defined.'));

        grid.onCellDoubleClick = function (rowIndex) {
            $scope.onEditBasket();
        };

        grid.onSelectionChanged = function () {
            var selection = grid.getSelectedRows();
            var data = selection.selectedRowsData[0];
            if (!data) return;

            var userData = grid.rows[selection.selectedRows[0]].userData;
            $scope.selectedBasket = {
                name: data[0],
                desc: data[2],
                system: userData.system,
                id: userData.id
            };
        };

        grid.cellEventHandler = function (rowIndex, cellIndex, eventName, args) {
            $scope.tab.callAction(eventName, rowIndex);
        }
    }

    function updateBasketsGridData() {
        if (!initialized || isQuantDataManager()) return;

        grid.removeAllRows(false, false, true);
        $scope.selectedBasket = null;

        grid.loadFromUrl('basketOfStocks/list', function () {
        }, true);
    }

    $scope.onEditBasket = function () {
        if (!$scope.selectedBasket) {
            $rootScope.showError(L.tsq('You have to select some group.'));
            return;
        }

        if ($scope.selectedBasket.system) {
            $rootScope.showError(L.tsq('This group can\'t be edited.'));
            return;
        }

        $scope.currentAction = 'Edit';
        $scope.currentBasket = $scope.selectedBasket;

        try {
            $scope.$digest();
        } catch (err) { } //needed because of doubleclick event

        showPopup('#addBasketModal');
    }

    $scope.onSaveBasket = function (form) {
        if (form) {
            $scope.errors = validate(form);
            if ($scope.errors) {
                return;
            }
        }

        $scope.currentBasket.stocks = null;

        BasketService.saveBasket($scope.currentBasket);
    }

    $scope.onEditStocks = function (form) {
        if (form) {
            $scope.errors = validate(form);
            if ($scope.errors) {
                return;
            }
        }

        if ($scope.selectedBasket.system) {
            $rootScope.showError(L.tsq('This group can\'t be edited.'));
            return;
        }
        
        BasketService.editStocks($scope.currentBasket, function(){
            hidePopup('#editStocksModal');
            $rootScope.showSuccess(L.tsq('Stocks were saved.'));
        });
    }

    $scope.onExport = function(){
        hidePopup('#editStocksModal');
        console.log("Export basket")

        var data = {
            basketId:$scope.selectedBasket.id
        };

        $rootScope.saveFile(
            L.tsq("Select file"), 
            { name: "xml", description: "XML Files" }, 
            "SaveGroupStocks", 
            "GroupStocks.xml", 
            null, 
            function(targetPath) {
                data.filePath = targetPath;
                BasketService.export(data);
            }
        );
    }

    $scope.onImport = function(){
        hidePopup('#editStocksModal');
        if ($scope.selectedBasket.system) {
            $rootScope.showError(L.tsq('Stocks can\'t be imported to this group.'));
            return;
        }
        console.log("Import basket")

        var data = {
            basketId:$scope.selectedBasket.id
        };

        var fileExtension = { name: 'csv', description: 'csv files' };
        $rootScope.showFilePicker(L.tsq('Select file'), "LoadBaskets", true, false, null, fileExtension, null, function (paths) {
            if (paths) {
                data.filePath = paths[0];
                BasketService.import(data);
            }
        });
    }

    //- Event Handlers --------------------------------------------------------------

    function onEvent(event, data) {
        if (event == SQEvents.get('BASKETS_CHANGED')) {
            $scope.baskets = angular.copy(data);
            updateBasketsGridData();
        } else return;

        try {
            $scope.$digest();
        } catch (err) { }
    }

    $scope.$on('$destroy', function () {
        SQEvents.removeListener(listenerId);
    });

    $scope.tab.callAction = function (actionName, rowIndex) {
        switch (actionName) {
            //----------------------------------------------------------------------------
            case 'updateSpecificGroup':
                let row = grid.getRowData(rowIndex);
                var data = {
                    basketId: row.userData.id
                };
                BasketService.updateData(data);
                break;
            case 'add':
                $scope.currentAction = 'Add';

                $scope.currentBasket.id = null;
                $scope.currentBasket.name = '';
                $scope.currentBasket.desc = '';

                showPopup('#addBasketModal');

                break;

            //----------------------------------------------------------------------------
            case 'delete':
                var selectedBasket = getSelectedBaskets(true);
                if (selectedBasket==""){
                    $rootScope.showError(L.tsq('You have to select some non default group.'));
                    return;
                }
                var count = selectedBasket.split(",").length;

                var text = L.tsq("Are you sure you want to remove selected groups (%d)?", [count]);
                $rootScope.showConfirm(count > 1 ? L.tsq("Remove groups") : L.tsq("Remove group"), text,
                    function (confirmed) {
                        if (confirmed) {
                            BasketService.removeBaskets(selectedBasket);
                        }
                    });

                break;    

            case 'update':
                console.log("Update data")

                var ids = [];
                var selection = grid.getSelectedRows();
                var selectedRows = selection.selectedRows;
                if (selectedRows.length==0){
                    $rootScope.showError(L.tsq('You have to select some group.'));
                    return;
                }

                for(var iRow of selectedRows){
                    var userData = grid.rows[iRow].userData;
                    ids.push(userData.id);
                }

                var data = {
                    basketId:ids.toString()
                };

                BasketService.updateData(data);

                break;

            case 'edit-stocks':
                if ($scope.selectedBasket==null){
                    $rootScope.showError(L.tsq('You have to select some group.'));
                    return;
                }

                console.log("List stocks")

                var data = {
                    basketId:$scope.selectedBasket.id
                };

                BasketService.listStocks(data, function(response) {
                    $scope.currentAction = 'EditStocks';
                    $scope.currentBasket = $scope.selectedBasket;
                    $scope.currentBasket.stocks = response.stocks;

                    showPopup('#editStocksModal');
                });

                break;
            case "save":
                console.log("Save groups");

                var selection = grid.getSelectedRows();
                var selectedRows = selection.selectedRows;
                if (selectedRows.length==0){
                    $rootScope.showError(L.tsq('You have to select some group.'));
                    return;
                }

                var ids = [];
                for(var iRow of selectedRows){
                    var userData = grid.rows[iRow].userData;
                    ids.push(userData.id);
                }
        
                var data = {
                    ids: ids,
                };

                $rootScope.saveFile(
                    L.tsq("Select file"), 
                    { name: "xml", description: "XML Files" }, 
                    "SaveGroups", 
                    "Groups.xml", 
                    null, 
                    function(targetPath) {
                        data.filePath = targetPath;
                        BasketService.saveXml(data);
                    }
                );
    
                break;
        
            //----------------------------------------------------------------------------
            case "load":
                console.log("Load groups");
    
                var data = {};
    
                var fileExtension = { name: "xml", description: "XML Files" };
                $rootScope.showFilePicker(L.tsq("Select file"), "LoadGroup", true, false, null, fileExtension, null, function (paths) {
                    if (paths) {
                        data.filePath = paths[0];
                        BasketService.loadXml(data);
                    }
                });
    
                break;

        }
    }

    function getSelectedBaskets(nonSystemOnly) {
        var selectedIndexes = grid.getSelectedRows().selectedRows;
        var result = "";

        for (var i = 0; i < selectedIndexes.length; i++) {
            var rowData = grid.rows[selectedIndexes[i]];
            if (!nonSystemOnly || rowData.userData.system==false){
                result += rowData.userData.id + ",";
            }
        }

        return result.substr(0, result.length - 1);
    }

    //- Initialization --------------------------------------------------------------

    var grid;
    var initialized = false;

    initGrids();

    $scope.currentBasket = {};
    $scope.selectedBasket = null;

    var listenerId = "BasketsCtrl";
    SQEvents.addListener(listenerId, [
        SQEvents.get('BASKETS_CHANGED')
    ], onEvent);

    $(document).on("keyup keypress keydown", "#addBasketModal textarea, #editStocksModal textarea", function (e) {
        e.stopPropagation();
    });

});