//product:BUILDER
//product:OPTIMIZER
//product:RETESTER
//product:TASKMANAGER
angular.module('app.settings').config(function(sqPluginProvider) {
	
	sqPluginProvider.plugin("SettingsTab", 50, {
        title: Ltsq('Money management'),
        help: Ltsq('Configure initial capital and desired position sizing method.'),
        helpURL: 'money-management',
        task: 'Build,Optimize,Retest,AutomaticRetest',
        configElemName: 'RiskMoneyManagement',
        dataItem: 'builder-settings-mm',
        templateUrl: '../../../plugins/SettingsMoneyManagement/views/moneyManagement.html',
        controller: 'MoneyManagementCtrl',
        getSettingsDescription : function(settingsElement, injector){
            var L = injector.get("L");
            var elMoneyManagement = getChildElement(settingsElement, "MoneyManagement");
            var MMService = injector.get("MoneyManagementService");
            return MMService.getDescription(elMoneyManagement);
        }
    });
});