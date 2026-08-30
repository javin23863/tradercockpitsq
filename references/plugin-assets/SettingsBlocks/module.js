//product:BUILDER
//product:TASKMANAGER
angular.module('app.settings').config(function (sqPluginProvider, SQEventsProvider) {

    SQEventsProvider.add("SLPT_SETTINGS_CHANGED", 'slpt-settings-changed');

    sqPluginProvider.plugin("SettingsTab", 30, {
        title: Ltsq('Building blocks'),
        help: Ltsq('Here you can choose the building blocks that will be used to generate every strategy. You can affect the probability of choosing a block by increasing its weight.<br>Every block has also advanced parameter settings that allow you to modify how block parameters are generated or define some predefined parameter sets to choose from.'),
        helpURL: 'building-blocks',
        task: 'Build',
        configElemName: 'Blocks',
        dataItem: 'builder-settings-blocks',
        templateUrl: '../../../plugins/SettingsBlocks/views/blocks.html',
        controller: 'BlocksCtrl',
        getSettingsDescription: function (settingsElement, injector){
            var L = injector.get("L");
            var elBuildingBlocks = getChildElement(settingsElement, "BuildingBlocks");
            var buildingBlocks = getChildElements(elBuildingBlocks, "Block");
            var buildingBlocksCount = 0;

            for (var i = 0; i < buildingBlocks.length; i++) {
                if (getAttrBooleanValue(buildingBlocks[i], "use", false)) {
                    buildingBlocksCount++;
                }
            }

            var elExitTypes = getChildElement(settingsElement, "ExitTypes");
            var exitTypes = getChildElements(elExitTypes, "Block");
            var exitTypesCount = 0;

            for (var i = 0; i < exitTypes.length; i++) {
                if (getAttrBooleanValue(exitTypes[i], "use", false)) {
                    exitTypesCount++;
                }
            }

            return buildingBlocksCount + " " + L.tsq("entry blocks") + ", " + exitTypesCount + " " + L.tsq("exit types");
        }
    });

});