angular.module('app.datasource.dukascopy').controller('dukascopyDataDisclaimerCtrl', function ($scope) {
    
    $scope.action.onSelect = function(symbols) {
        console.log("dukascopyDataDisclaimerCtrl")
       
        showPopup("#dukasDisclaimerModal");
   }
});