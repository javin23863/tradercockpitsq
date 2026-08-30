angular.module('app.datasource.sqEquityData').service('SQEquityDataService', function($rootScope, $q, BackendService) {

    this.getExchanges = function() {
        if (instance.exchanges.length>0){return;}

        BackendService.sendRequest('/sqEquityData/getExchanges', null, function(data) {
            loadToArray(instance.exchanges, data.exchanges);
        });
    }

    this.add = function(data, callback) {
        BackendService.sendRequest('/sqEquityData/add', data, callback, 'POST');
    }

    this.addCancel = function() {
        BackendService.sendRequest('/sqEquityData/addCancel', null);
    }

    this.lookup = function(data, callback) {
        BackendService.sendRequest('/sqEquityData/lookup', data, callback, 'POST');
    }

    this.update = function() {
        BackendService.sendRequest('/sqEquityData/update');
    }

    this.verifySubscription = function(callOnSucces) {
        if(instance.subscription.verified) {
            callOnSucces();
            return;
        }

        BackendService.sendRequest('/sqEquityData/verifySubscription', null, function(data) {
            instance.subscription.eodSubscriptionActive = data.eodSubscriptionActive;
            instance.subscription.minuteSubscriptionActive = data.minuteSubscriptionActive;
            instance.subscription.freeEquities = data.freeEquities;
            instance.subscription.verified = true;

            callOnSucces();
        });
    }

    var instance = this;
    this.exchanges = [];
    
    this.subscription = {
        eodSubscriptionActive: false,
        minuteSubscriptionActive: false,
        verified: false
    }

    this.verifySubscription(function() {});
});