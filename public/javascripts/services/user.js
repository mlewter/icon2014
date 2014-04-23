/**
 * Manages user data for a logged in user
 */
angular.module('icon2014').factory('UserService', function ($cookies, $http, $q, $timeout) {

    var authToken;

    var getAuthToken = function() {
        return authToken;
    };

    var setAuthToken = function(t) {
        authToken = t;
    };

    return {
        getAuthToken: getAuthToken,
        setAuthToken: setAuthToken
    }

});