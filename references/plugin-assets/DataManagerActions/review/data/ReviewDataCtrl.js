angular.module('app.data').controller('ReviewDataCtrl', function ($scope, $rootScope, BackendService, DMDataService, SQWebSocketService, L, $timeout) {
    console.log("ReviewData controller initialized");

    var grid = null;
	var currentSymbolInfo = null;
	var dataEditingObj;

	$scope.symbolDateRangeFrom = null;
	$scope.symbolDateRangeTo = null;
	$scope.skipDate = null;
	
	var deleted = [];
	var changed={};
	var modifiedRows = [];

	clearModification = function(){
		modifiedRows.forEach(function(element){
			grid.setRowClass(element, "");
		});
		modifiedRows=[];
		deleted=[];	
		changed={};
		dataEditingObj.changed = false;
	}

	$scope.doEditSelected = function(){
		var selection = grid.getSelectedRows();

		if (!selection || selection.selectedRowsData.length==0){
			return;
		}

		var row = selection.selectedRows[0];
		var selectedData = selection.selectedRowsData[0];
		
		changed[row] = JSON.parse(JSON.stringify(selectedData));

		var oldValue1 = selectedData[1];
		var oldValue2 = selectedData[2];
		var oldValue3 = selectedData[3];

		var newValue = "{{inputWidget}}";
		// posledni param je disableBodyRedraw - prekreslime az na konci
		grid.modifyRowByIndex(newValue, row, 1, true);
		grid.modifyRowByIndex(newValue, row, 2, true);
		grid.modifyRowByIndex(newValue, row, 3, true);

		// dobry nakonec nastavit value takto - parametry: grid, row, column, disableBodyRedraw
		sqGridInputValueSet(grid, row, 1, oldValue1, true);
		sqGridInputValueSet(grid, row, 2, oldValue2, true);
		sqGridInputValueSet(grid, row, 3, oldValue3, true);

		if (currentSymbolInfo.timeframe!='TICK'){
			var oldValue4 = selectedData[4];
			var oldValue5 = selectedData[5];

			grid.modifyRowByIndex(newValue, row, 4, true);
			grid.modifyRowByIndex(newValue, row, 5, true);

			sqGridInputValueSet(grid, row, 4, oldValue4, true);
			sqGridInputValueSet(grid, row, 5, oldValue5, true);
		}

		grid.setRowClass(row, "changed");
		if (!modifiedRows.includes(row)){
			modifiedRows.push(row);
		}
		dataEditingObj.changed = true;
		grid.bodyRedraw();
	}
	
	$scope.doDeleteSelected=function(){
		var selection = grid.getSelectedRows();
		if (!selection || selection.selectedRowsData.length==0){
			return;
		}

		var date = selection.selectedRowsData[0][0];
		var selectedIndex = selection.selectedRows[0];
		
		var alreadyContains = deleted.includes(date);
		if (alreadyContains){
			grid.setRowClass(selectedIndex, "");
			deleted = deleted.filter(function(value, index, arr){ 
				return value!=date;
			});
			modifiedRows = modifiedRows.filter(function(value, index, arr){ 
				return value!=selectedIndex;
			});
		}else{
			grid.setRowClass(selectedIndex, "deleted");
			deleted.push(date);
			modifiedRows.push(selectedIndex);
		}

		grid.selectRow(selection.selectedRows[0]+1, true);
		console.log(grid.getSelectedRows());
		grid.bodyRedraw();
		
		dataEditingObj.changed = modifiedRows.length>0;
	}
	
	$scope.onDateSkipChange=function(date){
		$scope.skipDate = date;
	}
	
	$scope.doSkip = function(){
		DMDataService.getIndexForDate(currentSymbolInfo.symbol, currentSymbolInfo.timeframe, 
				currentSymbolInfo.session, $scope.skipDate, function(data){
					grid.scrollToRowIndex(data.index, false);
				});
	}
	
	$scope.tab.dataEditingObj = function(dataEditing){
		dataEditingObj = dataEditing;
	}
	
	$scope.tab.save = function(){
		var changedArray = [];
		for (const row in changed) {
			var rowArr = changed[row];
			changedArray.push(rowArr);
		}

		DMDataService.saveDataChanges({
				deleted:deleted,
				changed:changedArray,
				symbol:currentSymbolInfo.symbol, 
				timeframe:currentSymbolInfo.timeframe, 
				session:currentSymbolInfo.session			
			},function(){
		});		
		clearModification();
	}
	
    $scope.tab.print = function (symbolInfo) {
		currentSymbolInfo = symbolInfo;
		
        if (!grid) {
            grid = new sqGrid('reviewDataGrid');
			grid.defineWidget('inputWidget', sqGridInput);
			// input updated
			grid.cellEventHandler = function(rowIndex, cellIndex, eventName, args) {
				if (eventName == "inputUpdated") {
					var changedText = args[0];
					var num = parseFloat(changedText)
					if (!isNaN(num)){
						changed[rowIndex][cellIndex] = changedText;
					}
				}
			};

			grid.onGridUserInteracted = function (eventType) {
				if (eventType=='scroll' && modifiedRows.length>0){
					for(var i=0;i<modifiedRows.length;i++){
						var row = modifiedRows[i];
						grid.setRowClass(row, "");
					}

					modifiedRows=[];
					changed={};
					dataEditingObj.changed = deleted.length>0 || modifiedRows.length>0;
					console.log(dataEditingObj.changed);
					grid.bodyRedraw();

					try { $scope.$digest(); } catch(err) {}
				}
			};
        }

        var columns = [];
        var widths = [];

        if (symbolInfo.timeframe == 'TICK') {
            columns = [{
                    title: 'Date',
                    type: "text"
                },
                {
                    title: 'Ask',
                    type: "text"
                },
                {
                    title: 'Bid',
                    type: "text"
                },
                {
                    title: 'Volume',
                    type: "text"
                },
            ];
            widths = [180, 100, 100, 100];
        } else {
            columns = [{
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
            ];
            widths = [180, 100, 100, 100, 100, 100];
        }

        grid.setFirstColumnAsId(true, false);
        grid.enableCheckboxes(false);
        grid.setColumns(columns);
        grid.setWidths(widths);
        grid.removeAllRows();
        grid.setEmptyGridText('Loading...');
        var url = "/data/reviewData?symbol=" + symbolInfo.symbol + "&timeframe=" + symbolInfo.timeframe + "&session=" + symbolInfo.session + "&token=" + BackendService.getToken();
        grid.setSmartRendering(encodeURI(url));
        grid.bodyRedraw(true);
		
		$scope.skipDate = symbolInfo.dateFrom;
		$scope.symbolDateRangeFrom = symbolInfo.dateFrom;
		$scope.symbolDateRangeTo = symbolInfo.dateTo;
		
		clearModification();
    }

	function onWebSocketData(newData) {
        try {
			if (newData.DMDataSave) {
				var data = newData.DMDataSave;

				if (data.percent == 100) {
					$rootScope.closeProgressModal();

					if (data.error) {
						$rootScope.showError(data.error);
					} else {
						$rootScope.showSuccess(L.tsq("Data saved."));
					}
					
					clearModification();
					$scope.tab.print(currentSymbolInfo);					
				} else {
					$rootScope.updateProgress(data.percent, data.info, null);
				}
			}
        }
        catch (err) {
            console.error("Error while processing data manager websocket updates - " + err);
        }
    }

	var listenerId = "ReviewDataCtrl-" + window.appConfig.appCode;
    SQWebSocketService.subscribeGeneral(listenerId, onWebSocketData);
});