'use strict';

var icon2014app = angular.module('icon2014', ['ngRoute', 'ngCookies', 'ngSanitize']);

/**
 * Match URLs to Angular Controllers and specify restriction
 *
 */
icon2014app.config(['$routeProvider',
    function($routeProvider) {
        $routeProvider.
            when('/', {
                templateUrl: '/assets/login.html',
                controller: 'LoginCtrl'
            }).
            when('/login', {
                templateUrl: '/assets/login.html',
                controller: 'LoginCtrl'
            }).
            when('/oauth_orize', {
                templateUrl: '/assets/oauthorize.html',
                controller: 'OAuthCtrl'
            }).
            when('/main', {
                templateUrl: '/assets/main.html',
                controller: 'MainCtrl'
            }).
            otherwise({
                redirectTo: '/'
            });
    }
]);