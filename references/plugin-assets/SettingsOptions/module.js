//product:BUILDER
//product:RETESTER
//product:TASKMANAGER
angular.module('app.settings').config(function(sqPluginProvider, SQEventsProvider) {
	
    SQEventsProvider.add('SETTINGS_SESSION_CHANGED', 'settings-session-changed'); 
    SQEventsProvider.add('TRADING_OPTIONS_CHANGED', 'trading-options-changed'); 
    SQEventsProvider.add('TRADING_OPTIONS_RELOAD', 'trading-options-reload'); 

	sqPluginProvider.plugin("SettingsTab", 20, {
        title: Ltsq('Trading options'),
        help: Ltsq('Trading options define behavior of the strategy and affect how and when strategy trades. You can limit trading time to a range, close the trade at the end of range or limit maximum trades per day.'),
        helpURL: 'trading-options',
        task: 'Build,Retest,Optimize,AutomaticRetest,NeuralNetworkTrainer',
        configElemName: 'Options',
        dataItem: 'settings-options',
        templateUrl: '../../../plugins/SettingsOptions/views/options.html',
        controller: 'OptionsCtrl',
        getSettingsDescription : function(settingsElement, injector){
            var L = injector.get("L");
            var elBTOptions = getChildElement(settingsElement, "BuildTradingOptions");
            var elParams = getChildElement(elBTOptions, "Params");
            var paramElems = getChildElements(elParams, "Param");
            
            var settingsText = "";
            
			for(var i=0; i<paramElems.length; i++) {
				var optionKey = getAttrValue(paramElems[i], "key");
                var nodeValue = getNodeValue(paramElems[i]);

                switch(optionKey){
                    case "ExitAtEndOfDay": 
                        if(nodeValue == "true"){
                            settingsText += L.tsq("Exit at end of day<br/>"); 
                        }
                        break;
                    case "ExitOnFriday": 
                        if(nodeValue == "true"){
                            settingsText += L.tsq("Exit on Friday<br/>"); 
                        }
                        break;
                    case "LimitTimeRange": 
                        if(nodeValue == "true"){
                            var timeFrom = convertToTime(getParameterValue(paramElems, "SignalTimeRangeFrom"));
                            var timeTo = convertToTime(getParameterValue(paramElems, "SignalTimeRangeTo"))
                            settingsText += L.tsq("Limit time from %s to %s<br/>", [timeFrom, timeTo]); 
                        }
                        break;
                    case "MaxTradesPerDay": 
                        var intValue = parseInt(nodeValue);
                        if(intValue == nodeValue && intValue > 0){
                            settingsText += L.tsq("Max trades per day: %d<br/>", [nodeValue]); 
                        }
                        break;
                }
			}
			
			return settingsText == "" ? L.tsq("No trading limits") : settingsText;
        }
    });

    function getParameterValue(params, key){
        for(var i=0; i<params.length; i++) {
            if(getAttrValue(params[i], "key") == key){
                return getNodeValue(params[i]);
            }
        }

        return "???";
    }

    function getNodeValue(node){
        return node.childNodes[0] ? node.childNodes[0].nodeValue : "???";
    }

    function convertToTime(value){
        if(value != parseInt(value)) return "???";

        value = parseInt(value);

        var hour = parseInt(value / 3600);
		var min = parseInt((value - (hour * 3600)) / 60);
		
		var strHour = '' + hour;
		if(strHour.length == 1) {
			strHour = "0" + strHour;
        }
        
		var strMin = '' + min;
		if(strMin.length == 1) {
			strMin = "0" + strMin;
		}

		return strHour + ":" + strMin;
    }

});