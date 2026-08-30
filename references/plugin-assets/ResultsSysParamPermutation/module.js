angular.module('app.resultstabs.sysparampermutation', ['sqplugin']).config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin("ResultsTab", 12, {
        title: Ltsq('Sys. Param Permutation'),
        dataItem: 'sysParamPermutation',
        controller: 'SysParamPermutationCtrl',
        templateUrl: '../../../plugins/ResultsSysParamPermutation/views/sysParamPermutation.html',
        hideAfterInit: true
    });
});