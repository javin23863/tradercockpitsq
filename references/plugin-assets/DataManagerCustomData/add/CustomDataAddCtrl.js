angular.module('app.data').controller('CustomDataAddCtrl', function ($rootScope, $scope, SQConstants, DMCustomDataService, SQEvents, L, BackendService, AppService) {
    console.log("CustomDataAdd controller initialized");
    
    $scope.actionName = 'add';

    $scope.customDataTypes = SQConstants.getConstants().customDataTypes;

    $scope.config = {
        name: null,
        type: $scope.customDataTypes[0].value,
        values: [
            {name: null, mt4: null, mt5: null, el: null},
            {name: null, mt4: null, mt5: null, el: null},
            {name: null, mt4: null, mt5: null, el: null}
        ]
    }

    $scope.printTypeLabel = function () {
        var type = getItem($scope.customDataTypes, "value", $scope.config.type);
        return type ? type.name : '';
    }

    $scope.action.onSelect = function(symbols2, grid2) {
        $scope.actionName = 'add';

        $scope.config.name = null;
        $scope.config.type = $scope.customDataTypes[0].value;
        for (var i = 0; i < $scope.config.values.length; i++) {
            $scope.config.values[i].name = null;
            $scope.config.values[i].mt4 = null;
            $scope.config.values[i].mt5 = null;
            $scope.config.values[i].el = null;
        }
        $scope.config.editable = true;

        showPopup('#customDataAddModal');
    }

    $scope.openHelp = function () {
        AppService.openHelp('external-indicators');
    }

    function isNameValid(session) {
        var format = /^[a-zA-Z0-9]*$/;

        return format.test(session);
    }

    $scope.onSave = function () {
        var valuesEntered = 0;

        for (let i = 0; i < $scope.config.values.length; i++) {
            var name = $scope.config.values[i].name;

            if(!isNameValid(name)) {
                $rootScope.showError(L.tsq("Value name cannot contain any special characters!"));

                return;
            }

            if(name && name.length > 0) {
                valuesEntered++;
            }
        }

        if(valuesEntered==0) {
            $rootScope.showError(L.tsq("At least one value must be specified!"));

            return;
        }

        for (let i = 0; i < $scope.config.values.length; i++) {
            for (var j = 0; j < $scope.config.values.length; j++) {

                if(i==j) continue;

                if(!$scope.config.values[i].name || !$scope.config.values[j].name || $scope.config.values[i].name=="" || $scope.config.values[j].name=="") continue;

                if($scope.config.values[i].name == $scope.config.values[j].name) {
                    $rootScope.showError(L.tsq("Value names must be unique!"));

                    return;
                }
            }
        }
        
        var data = angular.copy($scope.config);
        data.values = JSON.stringify($scope.config.values);
        
        switch ($scope.actionName) {
            //----------------------------------------------------------------------------
            case 'add':
                DMCustomDataService.add(data, function(result) {
                    $rootScope.showSuccess(result.success);

                    hidePopup('#customDataAddModal');
                });    

                break;

            //----------------------------------------------------------------------------
            case 'edit':
                DMCustomDataService.edit(data, function(result) {
                    $rootScope.showSuccess(result.success);

                    hidePopup('#customDataAddModal');
                }); 

                break;
        }
    }

    function onEvent(event, data) {
        if (event == SQEvents.get('CUSTOM_DATA_EDIT')) {
            $scope.actionName = 'edit';

            $scope.config.oldName = data.name;
            $scope.config.name = data.name;
            $scope.config.type = data.dataType;
            $scope.config.values = data.valuesObject;
            $scope.config.editable = parseInt(data.totalRecords) == 0
        }

        try { $scope.$digest(); } catch (err) { }

        showPopup('#customDataAddModal');
    }

    SQEvents.addListener('CustomDataAddCtrl', [SQEvents.get("CUSTOM_DATA_EDIT")], onEvent);
});

               