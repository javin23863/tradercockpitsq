//product:TASKMANAGER
angular.module('app.settings').config(function(sqPluginProvider) {
	
	sqPluginProvider.plugin("SettingsTab", 50, {
        title: Ltsq('Notification / Pause'),
        help: Ltsq('Notifies you when the project gets to a certain point. Optionally it can stop the project and wait for your confirmation to continue.'),
        helpURL: 'notification',
        task: 'Notification',
        configElemName: 'Notification',
        dataItem: 'tm-notification',
        templateUrl: '../../../plugins/SettingsNotification/settingsNotification.html',
        controller: 'SettingsNotificationCtrl',
        getSettingsDescription : function(settingsElement, injector){
            var L = injector.get("L");
            var type = getNodeValue(settingsElement, "Type");
            var email = getNodeValue(settingsElement, "Email");
            var wait = getNodeBooleanValue(settingsElement, "Wait");

            var elConditions = getChildElement(settingsElement, "Conditions");
            var conditionElems = getChildElements(elConditions, "Condition");

            var settingsText;

			if(type == "email") {
				settingsText = L.tsq("Send email to %s", [email ? email : "???"]);
			} else if(type == "popup") {
				settingsText = L.tsq("Show popup");
			} else if(wait) {
				settingsText = L.tsq("Wait for User Action");
			}

            return settingsText;
        }
    });
});