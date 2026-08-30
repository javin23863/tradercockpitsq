angular.module('app.databank.views').service('DatabankViewsService', function($rootScope, SQEvents, $q, BackendService, SQConstants, AppService) {

    this.removeView = function(name, callback){
        var args = {
            name: name 
        };

        return BackendService.sendRequest('/dbviews/removeView', args, callback);
    }

    this.addView = function(viewXML, callback){
        var args = {
            viewXML: formatXML(viewXML)
        };

        try {
            args.project = AppService.getProject();
            args.databank = AppService.getDatabank().title;
        } catch(err){}

        return BackendService.sendRequest('/dbviews/addView', args, callback, 'POST');
    }

    this.updateView = function(viewXML, callback){
        return BackendService.sendRequest('/dbviews/updateView', {  
            viewXML: formatXML(viewXML) 
        }, callback, 'POST');
    }

    this.changeView = function(projectName, databankName, viewName, callback){
        var args = {
            projectName: projectName,
            databankName: databankName,
            viewName: viewName
        };

        BackendService.sendRequest('/dbviews/changeView', args, callback);
    }

    this.getColumnText = function(column, direction, sampleType, resultType, chartSetup, chartSetupValue) {
        column = column || 'Unknown column';
        direction = getDirection(direction);
        sampleType = getSampleType(sampleType);
        resultType = getResultType(resultType);
        chartSetup = getChartSetup(chartSetup, chartSetupValue);
        
        if(!direction && !sampleType && !resultType && !chartSetup) return column;

        var text = column + '(';
        if(direction) text += direction + ', ';
        if(sampleType) text += sampleType + ', ';
        if(resultType) text += resultType + ', ';
        if(chartSetup) text += chartSetup;
        
        if(text.charAt(text.length - 2) == ','){
            text = text.substr(0, text.length - 2);
        }

        text += ')';

        return text;
    }

    
    function getDirection(direction){
        var directions = SQConstants.getConstants().directions;
        switch(parseInt(direction)){
            case directions.both:
                return null;
            case directions.long:
                return "Long";
            case directions.short:
                return "Short";
            default:
                return "?";
        }
    }

    function getSampleType(sampleType){
        var sampleTypes = SQConstants.getConstants().sampleTypes;
        switch(parseInt(sampleType)){
            case sampleTypes.full:
                return null;
            case sampleTypes.in:
                return "IS";
            case sampleTypes.out:
                return "OOS";
            default:
                return "?";
        }
    }

    function getResultType(resultType){
        var resultTypes = SQConstants.getConstants().plTypes;
        switch(parseInt(resultType)){
            case resultTypes.money:
                return null;
            case resultTypes.percent:
                return "Percent";
            case resultTypes.pips:
                return "Pips";
            case resultTypes.ticks:
                return "Ticks";
            default:
                return "?";
        }
    }
    
    function getChartSetup(chartSetup, chartSetupValue){
        var chartSetups = SQConstants.getConstants().resultTypes;
        switch(parseInt(chartSetup)){
            case chartSetups.main:
                return null;
            case chartSetups.portfolio:
                return "Portfolio";
            case chartSetups.chart:
                return "Chart setup - " + chartSetupValue;
            default:
                return "?";
        }
    }

    //- Initialization --------------------------------------------------------------

    var instance = this;

    this.renameDetails = null;      //old name and new name is stored here after renaming view

    new ListenersHandler(this);

});