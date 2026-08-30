angular.module('app.datasource.darwinex').controller('darwinexDisclaimerCtrl', function ($scope) {
    
    $scope.action.onSelect = function(symbols) {
        console.log("darwinexDisclaimerCtrl")
       
        showPopup("#darwinexDisclaimerModal");
   }
});