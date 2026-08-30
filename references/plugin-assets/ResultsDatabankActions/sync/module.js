angular.module('app.resultsdatabankactions.sync', ['sqplugin']).config(function (sqPluginProvider, SQEventsProvider) {

    SQEventsProvider.add("CHANGE_AUTOSYNC_GLOBAL", "change-autosync-global");

    sqPluginProvider.plugin("ResultsDatabankOptionsAction", 200, {
        title: Ltsq("Sync")+":",
        class: 'pull-right sync-combo',
        enabledHelp: Ltsq('Databank synchronization type'),
        disabledHelp: Ltsq('Disabled when file-based configuration is used'),
        controller: AutoSyncCtrl
    });

    function AutoSyncCtrl($rootScope, $scope, DatabankSyncService, UniqueIdGenerator, SQEvents, SQConstants) {

        $scope.onChange = function (tab) {
            if ($scope.selectedOption.value == $scope.combo.options[0].title) {
                $scope.selectedOption.value = lastSelectedOption;

                DatabankSyncService.synchronizeDatabank(tab.title);
            }
            else {
                DatabankSyncService.setSyncTypeToDatabank(tab.title, $scope.selectedOption.value);
                lastSelectedOption = $scope.selectedOption.value;
            }
            checkActive();
        }

        function onEvent(event, data) {
            if (event == SQEvents.get('DATABANK_CHANGED') || (event == SQEvents.get("DATABANKS_LOADED") && data)) {
                $scope.selectedOption.value = data.syncType;
                checkActive();
                checkDisabled(data.title);
            }
            else return;

            try { $scope.$digest(); } catch (er) { }
        }

        function checkDisabled(databankTitle) {
            if (databankTitle == 'Last generation') {
                $scope.combo.disabled = true;
                $scope.combo.disabledHelp = Ltsq('Disabled for Last generation databank');
            }
            else {
                $scope.combo.disabled = $scope.selectedOption.value == SQConstants.getConstants().databankSyncTypes.immediately;
                $scope.combo.disabledHelp = Ltsq('Disabled when file-based configuration is used');
            }
            
            try { $scope.$digest(); } catch (er) { }
        }

        function checkActive() {
            $scope.combo.active = $scope.selectedOption.value && $scope.selectedOption.value.indexOf("never") < 0;
        }

        $scope.$on('$destroy', function () {
            UniqueIdGenerator.removeId(id);
            window.parent.removeListener(listenerId);
        });

        $scope.combo.options = DatabankSyncService.getDatabankOptions();
        $scope.combo.type = "dropdown";
        $scope.combo.disabled = false;

        $scope.selectedOption = {
            value: null
        };

        var lastSelectedOption = $scope.selectedOption.value;

        var listenerId = "AutoSyncCtrl-" + UniqueIdGenerator.generateId;
        SQEvents.addListener(listenerId, [SQEvents.get('DATABANK_CHANGED'), SQEvents.get("DATABANKS_LOADED")], onEvent);
    }
});