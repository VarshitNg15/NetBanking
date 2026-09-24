/**
 * NetBanking Oracle JET Redwood Application Bootstrap
 * Powered by Oracle JET Official CDN Distribution
 */
'use strict';

(function () {
    window["oj_whenReady"] = true;

    requirejs.config({
      baseUrl: 'js',
      paths: {
        'ojs': 'https://static.oracle.com/cdn/jet/16.0.0/default/js/min',
        'ojL10n': 'https://static.oracle.com/cdn/jet/16.0.0/default/js/ojL10n',
        'ojtranslations': 'https://static.oracle.com/cdn/jet/16.0.0/default/js/resources',
        'knockout': 'https://static.oracle.com/cdn/jet/16.0.0/3rdparty/knockout/knockout-3.5.3',
        'jquery': 'https://static.oracle.com/cdn/jet/16.0.0/3rdparty/jquery/jquery-3.6.4.min',
        'jqueryui-amd': 'https://static.oracle.com/cdn/jet/16.0.0/3rdparty/jquery/jqueryui-amd-1.13.2.min',
        'text': 'https://static.oracle.com/cdn/jet/16.0.0/3rdparty/require/text',
        'hammerjs': 'https://static.oracle.com/cdn/jet/16.0.0/3rdparty/hammer/hammer-2.0.8.min',
        'signals': 'https://static.oracle.com/cdn/jet/16.0.0/3rdparty/js-signals/signals.min',
        'touchr': 'https://static.oracle.com/cdn/jet/16.0.0/3rdparty/touchr/touchr',
        'preact': 'https://static.oracle.com/cdn/jet/16.0.0/3rdparty/preact/dist/preact.umd',
        'preact/hooks': 'https://static.oracle.com/cdn/jet/16.0.0/3rdparty/preact/hooks/dist/hooks.umd',
        'preact/compat': 'https://static.oracle.com/cdn/jet/16.0.0/3rdparty/preact/compat/dist/compat.umd',
        '@oracle/oraclejet-preact': 'https://static.oracle.com/cdn/jet/16.0.0/3rdparty/oraclejet-preact/amd'
      }
    });
}());

/**
 * Load root module
 */
require(['./root']);
