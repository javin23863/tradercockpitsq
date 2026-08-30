angular.module('app').service('IndicatorTesterService', function ($rootScope, BackendService, SQEvents) {

    this.reloadIndicators = function (callback) {
        BackendService.sendRequest('indyTester/listIndicators', {}, function (result) {
            //transform options to array
            for (var i = 0; i < result.indicators.length; i++) {
                var indicator = result.indicators[i];
                if (!indicator.parameters) continue;

                for (var p = 0; p < indicator.parameters.length; p++) {
                    var parameter = indicator.parameters[p];
                    transformOptions(parameter);
                }
            }

            loadToArray(indicators, result.indicators);
            callback(indicators);
        });
    }

    function transformOptions(parameter) {
        if (!valueFilled(parameter.options)) return;

        var paramOptions = parameter.options.split(",");
        var options = [];

        for (var i = 0; i < paramOptions.length; i++) {
            var option = paramOptions[i];
            var values = option.split('=');

            options.push({ name: values[0], value: values[1] });
        }

        parameter.options = options;
    }

    this.getIndicators = function (callback) {
        if (arrayNotEmpty(indicators)) {
            callback(indicators);
        }
        else instance.reloadIndicators(callback);
    }

    this.addTests = function (indicators, xmlConfig, grid) {
        BackendService.sendRequest('indyTester/addTests', { indicators: indicators, xmlConfig: xmlConfig }, function (result) {
            var xmlObj = xmlToObject(result.xmlConfig);
            var testsObj = xmlObj.find('Tests')[0];

            loadGrid(grid, testsObj);
        }, 'POST');
    }

    this.createNewConfig = function (config, grid, callback) {
        BackendService.sendRequest('indyTester/createNewConfig', config, function (result) {
            var xmlObj = xmlToObject(result.xmlConfig);
            var testsObj = xmlObj.find('Tests')[0];

            loadGrid(grid, testsObj);
            if (callback) callback()
        });
    }

    this.startTesting = function (xmlConfig) {
        BackendService.sendRequest('indyTester/startTesting', { xmlConfig: xmlConfig }, null, "POST");
    }

    this.stopTesting = function () {
        BackendService.sendRequest('indyTester/stopTesting');
    }

    this.downloadTests = function (callback) {
        BackendService.sendRequest('indyTester/downloadTests', null, callback);
    }

    this.loadConfig = function (config, grid, filePath, callback) {
        BackendService.sendRequest('indyTester/loadConfig', { filePath: filePath }, function (result) {
            var xmlObj = xmlToObject(result.xmlConfig);
            var indicatorTesterObj = xmlObj.find('IndicatorTester')[0];
            var testsObj = getChildElement(indicatorTesterObj, 'Tests');

            config.engine = getNodeValue(indicatorTesterObj, 'Engine', config.engine);
            config.barsToReserve = getNodeIntValue(indicatorTesterObj, 'BarsToReserve', config.barsToReserve);

            loadGrid(grid, testsObj);

            if (callback) callback();
        }, 'POST');
    }

    this.reloadGrid = function (grid, xmlConfig) {
        var xmlObj = xmlToObject(xmlConfig);
        var indicatorTesterObj = xmlObj.find('IndicatorTester')[0];
        var testsObj = getChildElement(indicatorTesterObj, 'Tests');

        loadGrid(grid, testsObj);
    }

    function loadGrid(grid, testsObj) {
        var testObjs = getChildElements(testsObj, 'Test');

        grid.removeAllRows(true, true);

        for (var i = 0; i < testObjs.length; i++) {
            var testObj = testObjs[i];

            var use = getAttrBooleanValue(testObj, 'use', true);
            var indicator = getAttrValue(testObj, 'indicator');
            var fileName = getAttrValue(testObj, 'fileName');
            var exists = getAttrValue(testObj, 'exists');
            var decimals = getAttrIntValue(testObj, 'decimals', 6);
            var result = getAttrValue(testObj, 'result');
            var shift = getAttrIntValue(testObj, 'shift', 1);
            var parameters = instance.formatParameters(indicator, getAttrValue(testObj, 'parameters'), shift);

            var passed = result ? result.indexOf("Passed") >= 0 : false;
            var notFound = result ? result.indexOf("File not found") >= 0 : false;

            grid.addRow([
                indicator,
                "{{inputWidget value='" + fileName + "'}}",
                exists,
                createActionLink(parameters, "", "editParams", parameters),
                "{{spinnerWidget value='" + decimals + "' min='0'}}",
                result ? (!passed && !notFound ? createActionLink(result, "", "showErrors") : result) : '',
                createActionLink("&times;", "", "removeTest")
            ], true);

            grid.setRowChecked(i, use);

            addErrors(grid, i, testObj);
            instance.updateRowStyle(grid, i);
        }

        grid.bodyRedraw();
    }

    this.updateRowStyle = function (grid, rowIndex) {
        var checked = grid.isRowChecked(rowIndex);
        var result = grid.getCellValue(rowIndex, 5);

        grid.setRowClass(rowIndex, checked ? "checked" : "unchecked");

        if (valueFilled(result)) {
            if (result.indexOf('Passed') == 0) {
                grid.setRowClass(rowIndex, "passed");
            }
            else if (result.indexOf('Failed') == 0) {
                grid.setRowClass(rowIndex, "failed");
            }
            else {
                grid.setRowClass(rowIndex, "other");
            }
        }
    }

    function addErrors(grid, rowIndex, testObj) {
        var errorsContent = "";

        var errorsElem = getChildElement(testObj, "Errors");
        if (errorsElem) {
            var errorElems = getChildElements(errorsElem, "Error");
            for (var i = 0; i < errorElems.length; i++) {
                var errorElem = errorElems[i];
                if (errorElem.childNodes[0]) {
                    errorsContent += errorElem.childNodes[0].nodeValue + (i != errorElems.length - 1 ? "\n" : "");
                }
            }
        }
        grid.setUserData(rowIndex, 'errors', errorsContent);
        grid.setUserData(rowIndex, 'showGrid', getAttrBooleanValue(errorsElem, 'showGrid', false));
    }

    this.formatParameters = function (indicatorName, parameters, shift) {
        return indicatorName.split(".")[0] + '(' + (valueFilled(parameters) ? parameters : '') + ")[" + shift + "]";
    }

    this.getRawParameters = function (parameters) {
        var startIndex = parameters.indexOf('(') + 1;
        var endIndex = parameters.indexOf(')');

        if (startIndex > 0 && endIndex > 0) {
            return parameters.substr(startIndex, endIndex - startIndex);
        }
        else {
            console.error("Cannot get raw parameters. Wrong grid row parameters format");
        }
    }

    this.getShift = function (parameters) {
        var startIndex = parameters.indexOf('[') + 1;
        var endIndex = parameters.indexOf(']');

        if (startIndex > 0 && endIndex > 0) {
            return parameters.substr(startIndex, endIndex - startIndex);
        }
        else {
            console.error("Cannot get shift. Wrong grid row parameters format");
        }
    }

    this.saveConfig = function (filePath, xmlConfig, callback) {
        BackendService.sendRequest('indyTester/saveConfig', { xmlConfig: xmlConfig, filePath: filePath }, function (result) {
            if (callback) callback();
        }, 'POST');
    }

    this.getXMLConfig = function (config, grid) {
        var defaultConfig = '<IndicatorTester><Tests/></IndicatorTester>';
        var xmlObj = xmlToObject(defaultConfig);
        var xmlDoc = getXMLDoc(defaultConfig);

        var indicatorTesterObj = xmlObj.find('IndicatorTester')[0];

        addNode('Engine', config.engine, indicatorTesterObj, xmlDoc);
        addNode('BarsToReserve', config.barsToReserve, indicatorTesterObj, xmlDoc);

        var testsObj = getChildElement(indicatorTesterObj, 'Tests');

        for (var i = 0; i < grid.getNumberOfRows(); i++) {
            var testObj = createChild(testsObj, 'Test', xmlDoc, true);
            testObj.setAttribute('use', grid.isRowChecked(i));
            testObj.setAttribute('indicator', grid.getCellValue(i, 0));
            testObj.setAttribute('fileName', sqGridInputValueGet(grid, i, 1));
            testObj.setAttribute('exists', grid.getCellValue(i, 2));
            testObj.setAttribute('parameters', instance.getRawParameters(getActionLinkValue(grid.getCellValue(i, 3))));
            testObj.setAttribute('shift', instance.getShift(grid.getCellValue(i, 3)));
            testObj.setAttribute('decimals', sqGridSpinnerValueGet(grid, i, 4));
            testObj.setAttribute('result', getActionLinkValue(grid.getCellValue(i, 5)));

            saveErrors(testObj, grid, i, xmlDoc);
        }

        return xmlToString(indicatorTesterObj);
    }

    function onEvent(event, data) {
        if (event == SQEvents.get('FILES_CHANGED')) {
            indicators = [];
        }
    }

    function saveErrors(testObj, grid, rowIndex, xmlDoc) {
        var errorsElem = createChild(testObj, 'Errors', xmlDoc, true);
        errorsElem.setAttribute("showGrid", grid.getUserData(rowIndex, 'showGrid'));

        var content = grid.getUserData(rowIndex, 'errors');
        var errors = content.split('\n');

        for (var i = 0; i < errors.length; i++) {
            var error = errors[i];
            var errorElem = createChild(errorsElem, 'Error', xmlDoc, true);
            errorElem.appendChild(xmlDoc.createTextNode(error));
        }
    }

    var instance = this;

    var removeLinkHtml = '<span class="row-delete-link">&times;</span>';
    var indicators = [];

    var listenerId = "IndicatorTesterService";
    SQEvents.addListener(listenerId, [SQEvents.get('FILES_CHANGED')], onEvent);

});