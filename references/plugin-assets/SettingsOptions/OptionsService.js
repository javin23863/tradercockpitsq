angular.module('app.settings').service('OptionsService', function($q, BackendService, AppService, $rootScope, $timeout, L, SQEvents, DataService, SQConstants) {
    
    this.list = function(force) {
        var deferred = $q.defer();

        if(!optionsLoaded || force){
            BackendService.sendRequest('options/list', { projectName: $rootScope.project.name }, function(result){
                instance.parameters = result.parameters;
                
                optionsLoaded = true;
                deferred.resolve();
            });
        }
        else {
            deferred.resolve();
        }

        return deferred.promise;
    }

    this.replaceBTOptions = function(strategyLastSettings){
        var optionsElem = AppService.getCurrentTaskTabSettings("Options");
        var oldElem = getChildElement(optionsElem, 'BuildTradingOptions');

        if(!strategyLastSettings){
            console.error("Last settings not set");
            return;
        }

        var lastSettings = xmlToObject(strategyLastSettings).find("Settings")[0];
        var lastOptionsElem = getChildElement(lastSettings, "Options");
        var newElem = getChildElement(lastOptionsElem, "BuildTradingOptions");

        optionsElem.replaceChild(newElem, oldElem);
    }
    
    this.applySettings = function(config) {
        var optionsElem = AppService.getCurrentTaskTabSettings("Options");
        
        config.useCustomSettings = getAttrBooleanValue(optionsElem, "customSettings", false);

        var buildTradingOptionsElem = getChildElement(optionsElem, 'BuildTradingOptions');
        var parameters = parseParameters(buildTradingOptionsElem);
  
        config.propertyGridInstance.setValues(parameters);

        $timeout(function(){        //save settings - some values may have been corrected
            instance.saveConfig(config);
            SQEvents.notifyListeners(SQEvents.get('TRADING_OPTIONS_CHANGED'), buildTradingOptionsElem);
        }, 0, false);
    }
    
    this.saveConfig = function(config) {
        var xmlObj = AppService.xmlConfig;
        var xmlDoc = AppService.xmlDoc;
        var settingsObj = AppService.getTaskConfig();
        var optionsObj = getChildElement(settingsObj, 'Options');

        if(!settingsObj) return;
        
        if(!optionsObj){
            var optionsElem = xmlDoc.createElement('Options');
            optionsObj = settingsObj.appendChild(optionsElem);
        } else {
            removeAllChildren(optionsObj);
        }

        optionsObj.setAttribute("customSettings", config.useCustomSettings);   //applies only for AutomaticRetest task

        var buildTradingOptionsObj = optionsObj.appendChild(xmlDoc.createElement('BuildTradingOptions'));
        config.propertyGridInstance.getValuesXml(xmlDoc, buildTradingOptionsObj); //save settings from property grid 
        
        SQEvents.notifyListeners(SQEvents.get('TRADING_OPTIONS_CHANGED'), buildTradingOptionsObj);
    }


    var instance = this;

    var optionsLoaded = false;

    this.valueTypes = {
        pips : 'pips',
        money : 'money'
    }

    //pre MT5 pri zmene symbolu automaticky nastavit TradingOption.MarketOpenSession
    function presetMarketOpenSession(symbol) {
        if(!symbol) {
            return;
        }

        console.log("Preset MarketOpenSession by symbol", symbol);

        var symbolData = DataService.getSymbolData(symbol);     
        let instrumentName = symbolData?.instrumentDetails?.instrument;
        let instrumentDescription = symbolData?.instrumentDetails?.description;
        var sesion = null;

        var sessions = SQConstants.getConstants().sessions;
        if (sessions) {
            if(instrumentName) {
                //ako prve to skusi najstpodla nazvu instrumentu
                for (var i = 0; i < sessions.length; i++) {
                    if (sessions[i].name == instrumentName) {
                        sesion = sessions[i].name;
                        break;
                    }
                }
            }

            if(instrumentDescription && !sesion) {
                //az ako druhe podla description
                for (var i = 0; i < sessions.length; i++) {
                    if (sessions[i].name.startsWith(instrumentDescription)) {
                        sesion = sessions[i].name;
                        break;
                    }
                }
            }
        }

        if(sesion) {
            var optionsElem = AppService.getCurrentTaskTabSettings("Options");
            var buildTradingOptionsElem = getChildElement(optionsElem, 'BuildTradingOptions');

            var paramsElem = getChildElement(buildTradingOptionsElem, 'Params');
            var paramElems = getChildElements(paramsElem, 'Param');

            for(var p=0; p<paramElems.length; p++){
                var paramElem = paramElems[p];
                var key = correctAttrValue(paramElem.attributes["key"]);

                if(key == "MarketOpenSession") {
                    setNodeValue(paramElem, sesion, AppService.xmlDoc);
                }
            }

            console.log("MT5 TradingOption.MarketOpenSession set to", sesion);

            SQEvents.notifyListeners(SQEvents.get('TRADING_OPTIONS_RELOAD'));
        } else {
            console.log("MT5 TradingOption.MarketOpenSession not updated by symbol");
        }
    }

    this.lastSymbolChanged = null;

    function onEvent(event, data) {
        if(event == SQEvents.get('MAIN_SYMBOL_CHANGED')) {
            //console.log('Main symbol changed', data);

            var symbol = data;

            if(instance.lastSymbolChanged == symbol) {
                return;
            }

            instance.lastSymbolChanged = symbol;

            var dataElem = AppService.getCurrentTaskTabSettings("Data");
            if (dataElem) {
                var setupsElem = getChildElement(dataElem, 'Setups', true);
                if (setupsElem) {
                    var setupElems = getChildElements(setupsElem, 'Setup');
                    if (setupElems.length > 0) {
                        var setupElem = setupElems[0];
                        var engine = getAttrValue(setupElem, 'engine');

                        if(engine.startsWith("MetaTrader5")) {
                            presetMarketOpenSession(symbol);
                        }
                    }
                }
            }
        } else if(event == SQEvents.get('SETTINGS_TASK_CHANGED')) {
            instance.lastSymbolChanged = null;
        }
    }

    SQEvents.addListener("OptionsService", [
        SQEvents.get('MAIN_SYMBOL_CHANGED'),
        SQEvents.get('SETTINGS_TASK_CHANGED')
    ], onEvent);

});