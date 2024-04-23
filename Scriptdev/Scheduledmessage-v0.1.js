// ==SE_module==
// name: scheduled_message 
// displayName: Scheduled Message 
// description: A Script That Allows For Scheduling Messages 
// version: 0.1
// author: Jacob Thomas 
// ==/SE_module==

var networking = require("networking");
var messaging = require("messaging");
var config = require("config");
var im = require("interface-manager");
var ipc = require("ipc");
var javaInterfaces = require("java-interfaces");
var hooker = require("hooker");
var events = require("events");

(function () {
    'use strict';
    var conversationToolboxContext = {
        events: [],
    };

    // Replace Inside " " With You're Custom Message
    var inputMessage = "Jacob Thomas Aka Bocajthomas";
    
    // Replace Inside " " With Time In Ms ( Milliseconds )
    var time = "2000";
    
    // DO NOT CHANGE!! THIS IS A CONVERTER 
    var convertTime = parselnt(time);

    function createConversationToolboxUI() {
        conversationToolboxContext.events.push({
            start: function (builder, args) {
                builder.button("Send Message", function () {
                    var conversationId = args["conversationId"];
			sleep(convertTime)
			messaging.sendChatMessage(conversationId, inputMessage, function () { });
		});
	    }
	});
    }
				
    var snapActivityContext = {
        activity: null,
        events: [],
    };

    function start(_a) {
        _a.snapActivityContext; _a.conversationToolboxContext; _a.settingsContext;
        createInterface();
    }

    function createInterface() {
        createConversationToolboxUI();
    }

    var snapApplicationContext = {
        context: null,
        events: [],
    };

    var snapEnhancerContext = {
        context: null,
        events: [],
    };

    var unloadContext = {
        events: [],
    };

    start({
        snapActivityContext: snapActivityContext,
        snapApplicationContext: snapApplicationContext,
        snapEnhancerContext: snapEnhancerContext,
        unloadContext: unloadContext,
        conversationToolboxContext: conversationToolboxContext,
    });

    module.onSnapMainActivityCreate = function (activity) {
        snapActivityContext.activity = activity;
        snapActivityContext.events.forEach(function (event) {
            event.start(activity, null);
        });
    };

    module.onUnload = function () {
        unloadContext.events.forEach(function (event) {
            event.start(null, null);
        });
    };

    im.create("conversationToolbox", function (builder, args) {
        conversationToolboxContext.events.forEach(function (event) {
            event.start(builder, args);
        });
    });

})();
