angular.module('app').controller('CEExportPopupCtrl', function ($rootScope, $scope, $timeout, ImportExportService, L, SQEvents) {

    function init(){
        tree = new sqTree("exportTree");
        tree.enableCheckboxes();
        tree.onCheckChanged = onSelectionChanged;

        $scope.onFilterChange();
    }

    function onSelectionChanged(){
        var fileCount = tree.getCheckedItems().length;
        $scope.filter.text = L.tsq("%d files selected", [fileCount]);

        try { $scope.$digest(); } catch(err) {}
    }

    $scope.onFilterChange = function(){
        ImportExportService.listFiles($scope.filter.phrase, function(result) {
            $scope.emptyList = (result.item.length==0);            

            tree.loadJson(result);

            if ($scope.filter.phrase == "") {
                tree.collapseAll();
                tree.expandLevels(1);
                tree.expandItemWithName("user");
            } else {
                tree.expandAll();
            }

            tree.redraw();

            onSelectionChanged();
        });
    }

    $scope.onPluginClicked = function(item){
        $scope.filter.phrase = "";
        $scope.onFilterChange();

        showPopup('#CEExportPopup');
    }

    $scope.onExport = function(){
        tree.getUserData();
        
        var checkedItems = tree.getCheckedItems();
        if(checkedItems.length==0) {
            $rootScope.showError(L.tsq("There are no files to export."));

            return;
        }

        $rootScope.saveFile(L.tsq("Choose target file"), ImportExportService.fileExtension, ImportExportService.pathKey, "SQExtension", null, function(targetPath){
            var filePaths = ImportExportService.IDsToPaths(checkedItems, tree);

            ImportExportService.export(filePaths, targetPath, function(result){
                $rootScope.showSuccess(L.tsq("Extensions exported"));

                window.parent.hidePopup("#saveFileDialog");
                hidePopup("#CEExportPopup");
                
                SQEvents.notifyListeners(SQEvents.get('EDITOR_RELOAD_NAVIGATOR'), null);
            });
        });
    }

    var tree;

    $scope.filter = {
        phrase : "",
        text : ""
    };

    $scope.emptyList = false;

    $timeout(init, 1000, false);

});