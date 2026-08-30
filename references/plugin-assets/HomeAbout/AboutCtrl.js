angular.module('app').controller('AboutCtrl', function ($scope, AboutService, SQEvents, L, SQConstants) {
    console.log("About controller initialized");

    $scope.license = {};

    $scope.verify = {
        code: "",
        updateLicense: false,
        verifyingLicense: false
    }

    $scope.updatingLicenseAfterPayment = false;
    $scope.broker = SQConstants.getConstants().broker;
    $scope.appPath = SQConstants.getConstants().appPath;

    $scope.verifyLicense = function () {
        $scope.verify.verifyingLicense = true;

        AboutService.updateLicense($scope.verify.code, function (data) {
            $scope.license = data;

            if (!$scope.license.updateSuccess) {
                $scope.license.updateResult = L.tsq("License Verification Failed!") + "<br>" + $scope.license.updateResult;
                $scope.verify.verifyingLicense = false;

                try { $scope.$digest(); } catch (err) { }
            } else {
                if (!$scope.updatingLicenseAfterPayment) {
                    $scope.close();
                } else {
                    $scope.verify.verifyingLicense = false;
                }
            }

        });
    }

    $scope.close = function () {
        $scope.verify.code = "";
        $scope.verify.updateLicense = false;
        $scope.verify.verifyingLicense = false;

        try { $scope.$digest(); } catch (err) { }

        hidePopup('#aboutModal');
    }

    $scope.$on('$destroy', function () {
        window.parent.removeListener("about-dialog");
    });

    window.parent.addListener("about-dialog", [SQEvents.get('OPEN_ABOUT_DIALOG'), SQEvents.get('LICENSE_CHANGED')], onEvent);

    function onEvent(event, data) {
        if (event == SQEvents.get('OPEN_ABOUT_DIALOG') || (event == SQEvents.get('LICENSE_CHANGED') && data && data.reasonLicVerifFailed)) {
            AboutService.license(function (response) {
                $scope.license = response;
                $scope.verify.code = response.code;

                if (data && (data.update || data.reasonLicVerifFailed)) {
                    $scope.verify.updateLicense = true;

                    if (data.reasonLicVerifFailed) {
                        $scope.license.updateSuccess = false;
                        $scope.license.updateResult = L.tsq("License Verification Failed!") + "<br>" + data.reasonLicVerifFailed;
                        $scope.verify.code = data.codeVerifFailed;
                    }
                } else {
                    $scope.verify.updateLicense = false;
                }

                $scope.updatingLicenseAfterPayment = false;

                if (data && ("enterNewLicence" in data)) {
                    $scope.updatingLicenseAfterPayment = true;
                    $scope.verify.updateLicense = true;
                    $scope.verify.code = data.enterNewLicence;
                    $scope.verifyLicense();
                }

                try { $scope.$digest(); } catch (err) { }
            });

            showPopup("#aboutModal");
        }
    }

});