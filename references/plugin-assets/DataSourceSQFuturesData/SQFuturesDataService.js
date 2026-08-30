angular.module('app.datasource.sqFuturesData').service('SQFuturesDataService', function($rootScope, $q, BackendService) {
    this.getExchanges = function() {
        if (instance.exchanges.length>0){return;}

        BackendService.sendRequest('/sqFuturesData/getExchanges', null, function(data) {
            loadToArray(instance.exchanges, data.exchanges);
        });
    }

    this.add = function(data, callback) {
        BackendService.sendRequest('/sqFuturesData/add', data, callback, 'POST');
    }

    this.addCancel = function() {
        BackendService.sendRequest('/sqFuturesData/addCancel', null);
    }

    this.lookup = function(data, callback) {
        BackendService.sendRequest('/sqFuturesData/lookup', data, callback, 'POST');
    }

    this.update = function() {
        BackendService.sendRequest('/sqFuturesData/update');
    }

    this.updateBr = function() {
        BackendService.sendRequest('/sqFuturesData/updateBr');
    }

    this.verifySubscription = function(callOnSucces) {
        if(instance.subscription.verified) {
            callOnSucces();
            return;
        }

        BackendService.sendRequest('/sqFuturesData/verifySubscription', null, function(data) {
            instance.subscription.eodSubscriptionActive = data.eodSubscriptionActive;
            instance.subscription.minuteSubscriptionActive = data.minuteSubscriptionActive;
            instance.subscription.freeFutures = data.freeFutures;
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